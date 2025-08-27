
package com.example.quickworktime.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.example.quickworktime.room.AppDatabase
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.repository.WorkInfoRepository
import com.example.quickworktime.utils.TimeCalculationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ウィジェット関連のデータ操作
 * データベース操作はWorkInfoRepositoryに委譲し、ウィジェット固有の機能を処理
 */
class WidgetRepository(
    private val workInfoRepository: WorkInfoRepository,
    private val context: Context
) {
    companion object {
        private const val TAG = "WidgetRepository"
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        /**
         * データベースコンテキストからWidgetRepositoryインスタンスを作成
         */
        fun create(context: Context): WidgetRepository {
            val database = AppDatabase.getDatabase(context)
            val workInfoRepository = WorkInfoRepository(database.workInfo())
            return WidgetRepository(workInfoRepository, context)
        }
    }

    private val errorHandler = WidgetErrorHandler()
    private lateinit var stateCache: WidgetStateCache

    /**
     * コンテキスト依存コンポーネントでRepositoryを初期化
     */
    fun initialize(context: Context) {
        stateCache = WidgetStateCache(context)
    }

    /**
     * エラーハンドリングを含む今日の勤務情報をデータベースから取得
     * @return 今日のWorkInfoが存在する場合はそれを、エラー情報を含むWidgetResultを返す
     */
    suspend fun getTodayWorkInfo(): WidgetErrorHandler.WidgetResult<WorkInfo?> = withContext(Dispatchers.IO) {
        errorHandler.executeWithRetry(
            operation = {
                val today = LocalDate.now().format(dateFormatter)
                workInfoRepository.getWorkInfoByDate(today)
            }
        )
    }

    /**
     * 今日の勤務情報を取得（後方互換性のためnullにフォールバック）
     * @return 今日のWorkInfoが存在する場合はそれを、それ以外はnullを返す
     */
    suspend fun getTodayWorkInfoOrNull(): WorkInfo? = withContext(Dispatchers.IO) {
        when (val result = getTodayWorkInfo()) {
            is WidgetErrorHandler.WidgetResult.Success -> result.data
            is WidgetErrorHandler.WidgetResult.Error -> {
                Log.w(TAG, "Failed to get today's work info: ${result.error.message}")
                null
            }
        }
    }

    /**
     * 最適な退勤時刻を計算（5分単位で切り下げ）
     *
     * @return フォーマット済み時刻文字列 (HH:mm)
     */
    suspend fun calculateOptimalClockOutTime(): WidgetErrorHandler.WidgetResult<String> = withContext(Dispatchers.IO) {
        errorHandler.executeWithRetry(
            operation = {
                TimeCalculationUtils.getCurrentRoundedTime()
            }
        )
    }

    /**
    * エラーハンドリングを含む指定されたデルタ値による時刻調整
    * @param currentTime 現在時刻 (HH:mm形式)
    * @param hourDelta 加算/減算する時間
    * @param minuteDelta 加算/減算する分
    * @return 調整後の時刻文字列またはエラー
    */
    suspend fun adjustTime(
        currentTime: String,
        hourDelta: Int,
        minuteDelta: Int
    ): WidgetErrorHandler.WidgetResult<String> = withContext(Dispatchers.IO) {
        errorHandler.executeWithRetry(
            operation = {
                TimeCalculationUtils.adjustTime(currentTime, hourDelta, minuteDelta)
            }
        )
    }

    /**
     * 包括的なエラーハンドリングを含むWorkInfoRepositoryを使用した退勤時刻記録
     *
     * @param time 退勤時刻 (HH:mm形式)
     * @return 成功または失敗の詳細を示すWidgetResult
     */
    suspend fun recordClockOut(time: String): WidgetErrorHandler.WidgetResult<Unit> = withContext(Dispatchers.IO) {
        if (!::stateCache.isInitialized) {
            initialize(context)
        }

        errorHandler.executeWithRetry(
            operation = {
                val success = workInfoRepository.recordClockOutForWidget(time)
                if (!success) {
                    throw Exception("Failed to record clock out time: $time")
                }
            }
        )
    }

    /**
     * エラーハンドリングとリトライ機能を含む全ウィジェットインスタンスの更新をトリガー
     *
     * @param context アプリケーションコンテキスト
     * @return 成功または失敗を示すWidgetResult
     */
    suspend fun updateWidget(context: Context): WidgetErrorHandler.WidgetResult<Unit> = withContext(Dispatchers.Main) {
        if (!::stateCache.isInitialized) {
            initialize(context)
        }

        errorHandler.executeWithRetry(
            operation = {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                if (appWidgetIds.isNotEmpty()) {
                    val widgetProvider = WorkTimeWidgetProvider()
                    widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds)
                }
            },
            maxRetries = 2 // Fewer retries for UI updates
        )
    }

    /**
     * エラーハンドリングを含む今日の勤務記録の存在確認
     *
     * @return 今日の勤務記録が存在する場合true、それ以外またはエラー時はfalse
     */
    suspend fun hasTodayRecord(): Boolean = withContext(Dispatchers.IO) {
        when (val result = getTodayWorkInfo()) {
            is WidgetErrorHandler.WidgetResult.Success -> result.data != null
            is WidgetErrorHandler.WidgetResult.Error -> {
                Log.w(TAG, "Failed to check today's record: ${result.error.message}")
                false
            }
        }
    }

    /**
     * エラーハンドリングを含む今日の終了時刻の取得
     *
     * @return 終了時刻文字列が存在する場合はそれを、それ以外またはエラー時はnull
     */
    suspend fun getTodayEndTime(): String? = withContext(Dispatchers.IO) {
        when (val result = getTodayWorkInfo()) {
            is WidgetErrorHandler.WidgetResult.Success -> result.data?.endTime
            is WidgetErrorHandler.WidgetResult.Error -> {
                Log.w(TAG, "Failed to get today's end time: ${result.error.message}")
                null
            }
        }
    }

    /**
     * エラーハンドリングとキャッシュ機能を含む現在のウィジェット表示状態の取得
     *
     * @param context アプリケーションコンテキスト
     * @return UI描画用のウィジェット表示状態
     */
    suspend fun getWidgetDisplayState(context: Context): WidgetErrorHandler.WidgetResult<WidgetDisplayState> = withContext(Dispatchers.IO) {
        if (!::stateCache.isInitialized) {
            initialize(context)
        }

        errorHandler.executeWithRetry(
            operation = {
                val today = LocalDate.now().format(dateFormatter)
                val workInfo = workInfoRepository.getWorkInfoByDate(today)

                val displayState = if (workInfo?.endTime != null) {
                    // 既存の記録がある場合
                    WidgetDisplayState(
                        displayTime = workInfo.endTime,
                        dateText = formatDateText(today),
                        hasRecord = true,
                        isError = false,
                        errorMessage = null
                    )
                } else {
                    // 最適な時刻を計算
                    val optimalTime = TimeCalculationUtils.getCurrentRoundedTime()
                    WidgetDisplayState(
                        displayTime = optimalTime,
                        dateText = formatDateText(today),
                        hasRecord = false,
                        isError = false,
                        errorMessage = null
                    )
                }

                // 正常な状態をキャッシュ
                stateCache.saveLastGoodState(displayState)
                displayState
            }
        )
    }

    /**
     * エラーハンドリングを含むウィジェットテーマの更新（設定変更用）
     *
     * @param context アプリケーションコンテキスト
     * @return 成功または失敗を示すWidgetResult
     */
    suspend fun updateWidgetTheme(context: Context): WidgetErrorHandler.WidgetResult<Unit> = withContext(Dispatchers.Main) {
        errorHandler.executeWithRetry(
            operation = {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                if (appWidgetIds.isNotEmpty()) {
                    // プロバイダーを再作成してテーマ更新を強制実行
                    val widgetProvider = WorkTimeWidgetProvider()
                    widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds)
                }
            }
        )
    }

    /**
     * メインアプリ統合のためのデータ更新通知
     *
     * @param context アプリケーションコンテキスト
     */
    suspend fun notifyDataUpdated(context: Context) {
        updateWidget(context)
    }

    private fun formatDateText(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, dateFormatter)
            val formatter = DateTimeFormatter.ofPattern("M月d日")
            date.format(formatter)
        } catch (e: Exception) {
            dateString
        }
    }
}

