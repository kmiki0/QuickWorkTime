package com.example.quickworktime.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.quickworktime.MainActivity
import com.example.quickworktime.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ウィジェット表示状態データクラス
 */
data class WidgetDisplayState(
    val displayTime: String,
    val dateText: String,
    val hasRecord: Boolean,
    val buttonText: String = "Exit",
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isAdjusted: Boolean = false
)

/**
 * ウィジェットの更新・操作を管理
 * 定期更新のセットアップ・停止、ウィジェットの状態更新、ボタン操作の処理
 */
class WorkTimeWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_CLOCK_OUT = "com.example.quickworktime.widget.ACTION_CLOCK_OUT"
        const val ACTION_RETRY_UPDATE = "com.example.quickworktime.widget.ACTION_RETRY_UPDATE"
        const val EXTRA_TIME_COMPONENT = "time_component"
        const val EXTRA_DIRECTION = "direction"
        const val ACTION_ADJUST_TIME_MINUS_5 = "com.example.quickworktime.widget.ACTION_ADJUST_TIME_MINUS_5"
        const val ACTION_ADJUST_TIME_PLUS_5 = "com.example.quickworktime.widget.ACTION_ADJUST_TIME_PLUS_5"
    }

    /**
     * ウィジェットの更新処理
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 複数のウィジェットがアクティブな場合があるため、すべて更新する
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * 退勤ボタン、再試行更新、定期更新アクションを設定
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_CLOCK_OUT -> {
                // 退勤ボタンタップ
                Log.i("WidgetUpdate", "退勤ボタンタップ")
                handleClockOut(context)
            }
            ACTION_RETRY_UPDATE -> {
                // 再試行更新
                handleRetryUpdate(context)
            }
            ACTION_ADJUST_TIME_MINUS_5 -> {
                // -5分ボタンタップ
                Log.i("WidgetUpdate", "-5分ボタンタップ")
                handleTimeAdjustment(context, -5)
            }
            ACTION_ADJUST_TIME_PLUS_5 -> {
                // +5分ボタンタップ
                Log.i("WidgetUpdate", "+5分ボタンタップ")
                handleTimeAdjustment(context, 5)
            }
            // 定期更新アクション追加
            "com.example.quickworktime.widget.PERIODIC_UPDATE" -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, WorkTimeWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    /**
     * Widget有効化時に定期更新システムをセットアップ
     */
    override fun onEnabled(context: Context) {
        setupPeriodicUpdates(context)
    }

    /**
     * Widget無効化時に定期更新システムを停止
     */
    override fun onDisabled(context: Context) {
        cancelPeriodicUpdates(context)
    }



    /**
     * AlarmManagerを使用した定期更新システムのセットアップ
     */
    private fun setupPeriodicUpdates(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 定期更新用のPendingIntent作成
            val updateIntent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
                action = "com.example.quickworktime.widget.PERIODIC_UPDATE"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                12345, // ユニークなrequest code
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Widget設定から更新間隔を取得
            val intervalMillis = getUpdateIntervalFromConfig(context)

            // 通常のアプリで使用可能なsetRepeating（不正確だが権限不要）
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,                                    // デバイスがスリープ中でも起動
                System.currentTimeMillis() + intervalMillis, // 初回実行時間
                intervalMillis,                                              // 繰り返し間隔
                pendingIntent
            )

        } catch (e: Exception) {
            Log.e("WidgetUpdate", "定期更新設定失敗", e)
        }
    }

    /**
     * work_time_widget_info.xml から更新間隔を取得する
     */
    private fun getUpdateIntervalFromConfig(context: Context): Long {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)

            // この Widget Provider の設定情報を取得
            val appWidgetProviderInfo = appWidgetManager.getInstalledProvidersForPackage(
                context.packageName, null
            ).find { it.provider == componentName }

            val intervalMillis = appWidgetProviderInfo?.updatePeriodMillis ?: 0L

            return intervalMillis.toLong()
        } catch (e: Exception) {
            Log.e("WidgetUpdate", "ERROR : 設定値取得エラー", e)
            // エラーの場合、5分を返す
            return 5 * 60 * 1000L
        }
    }

    /**
     * 定期更新システムの停止
     */
    private fun cancelPeriodicUpdates(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val updateIntent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
                action = "com.example.quickworktime.widget.PERIODIC_UPDATE"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                12345,
                updateIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                Log.i("WidgetUpdate", "定期更新アラーム停止完了")
            }
        } catch (e: Exception) {
            Log.e("WidgetUpdate", "定期更新停止失敗", e)
        }
    }

    /**
     * 時間調整処理
     * @param context コンテキスト
     * @param minutesDelta 調整する分数(正の値で加算、負の値で減算)
     */
    private fun handleTimeAdjustment(context: Context, minutesDelta: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stateCache = WidgetStateCache(context)
                val repository = WidgetRepository.create(context)
                repository.initialize(context)

                // 現在の表示状態を取得
                when (val displayStateResult = repository.getDisplayState()) {
                    is WidgetErrorHandler.WidgetResult.Success<*> -> {
                        val displayState = displayStateResult.data as? WidgetDisplayState
                        if (displayState == null) {
                            Log.e("WorkTimeWidgetProvider", "表示状態の取得に失敗")
                            return@launch
                        }

                        // 時間を調整
                        val adjustedTime = adjustTime(displayState.displayTime, minutesDelta)

                        // 調整後の状態を保存
                        val adjustedState = displayState.copy(
                            displayTime = adjustedTime,
                            isAdjusted = true
                        )
                        stateCache.saveLastGoodState(adjustedState)

                        // ウィジェットを更新
                        withContext(Dispatchers.Main) {
                            updateAllWidgets(context)
                        }
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        Log.e("WorkTimeWidgetProvider", "時間調整エラー: ${displayStateResult.error}")
                    }
                }
            } catch (e: Exception) {
                Log.e("WorkTimeWidgetProvider", "時間調整処理エラー", e)
            }
        }
    }

    /**
     * 時間文字列を指定分数だけ調整する
     * @param timeString 時間文字列 (HH:mm形式)
     * @param minutesDelta 調整する分数
     * @return 調整後の時間文字列 (HH:mm形式)
     */
    private fun adjustTime(timeString: String, minutesDelta: Int): String {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            val time = java.time.LocalTime.parse(timeString, formatter)
            val adjustedTime = if (minutesDelta > 0) {
                time.plusMinutes(minutesDelta.toLong())
            } else {
                time.minusMinutes((-minutesDelta).toLong())
            }
            adjustedTime.format(formatter)
        } catch (e: Exception) {
            Log.e("WorkTimeWidgetProvider", "時間調整計算エラー", e)
            timeString
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 更新開始ログ
        val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        Log.i("WidgetDebug", "=== updateAppWidget開始: $currentTime ===")

        // 非同期でWidget更新を実行
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Repository のインスタンスを取得
                val repository = WidgetRepository.create(context)
                repository.initialize(context)

                Log.i("WidgetDebug", "Repository作成・初期化完了")

                // Widget表示状態を取得
                val displayStateResult = repository.getWidgetDisplayState(context)

                when (displayStateResult) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
                        // 成功時：正常なWidget表示を更新
                        val displayState = displayStateResult.data
                        Log.i("WidgetDebug", "表示状態取得成功: displayTime=${displayState.displayTime}, hasRecord=${displayState.hasRecord}")

                        updateWidgetViews(context, appWidgetManager, appWidgetId, displayState)
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        // エラー時：エラー表示を更新
                        Log.e("WidgetDebug", "表示状態取得エラー: ${displayStateResult.error.message}")
                        updateWidgetError(context, appWidgetManager, appWidgetId, displayStateResult.error)
                    }
                }
            } catch (e: Exception) {
                // 予期しないエラー時の処理
                Log.e("WidgetDebug", "updateAppWidget予期しないエラー", e)
                updateWidgetError(context, appWidgetManager, appWidgetId,
                    WidgetErrorHandler.WidgetError.UnknownError("Widget update failed: ${e.message}"))
            }
        }
    }


    /**
     * 正常な状態でWidgetのViewを更新
     */
    private fun updateWidgetViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        displayState: WidgetDisplayState
    ) {
        Log.i("WidgetDebug", "updateWidgetViews開始: displayTime=${displayState.displayTime}")

        // RemoteViewsを作成
        val views = RemoteViews(context.packageName, R.layout.work_time_widget)

        // 時間表示の更新
        val timeParts = displayState.displayTime.split(":")
        if (timeParts.size >= 2) {
            val hour = timeParts[0]
            val minute = timeParts[1]

            Log.i("WidgetDebug", "時間更新: ${hour}:${minute}")
            views.setTextViewText(R.id.widget_hour_text, hour)
            views.setTextViewText(R.id.widget_minute_text, minute)
        } else {
            Log.w("WidgetDebug", "時間形式エラー: ${displayState.displayTime}")
            views.setTextViewText(R.id.widget_hour_text, "--")
            views.setTextViewText(R.id.widget_minute_text, "--")
        }

        // 日付表示の更新
        Log.i("WidgetDebug", "日付更新: ${displayState.dateText}")
        views.setTextViewText(R.id.widget_date_text, displayState.dateText)

        // ステータステキストの更新
        if (displayState.hasRecord) {
            Log.i("WidgetDebug", "ステータス: 記録済み")
            views.setTextViewText(R.id.widget_status_text, "記録済み")
            views.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
        } else {
            Log.i("WidgetDebug", "ステータス: 退勤予定")
            views.setTextViewText(R.id.widget_status_text, "退勤予定")
            views.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
        }

        // ボタンテキストの更新
        Log.i("WidgetDebug", "ボタン更新: ${displayState.buttonText}")
        views.setTextViewText(R.id.widget_clock_out_button, displayState.buttonText)

        // ボタンクリック時のPendingIntent設定
        setupButtonIntents(context, views, appWidgetId)

        // Widgetを更新
        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.i("WidgetDebug", "=== updateWidgetViews完了 ===")
    }

    /**
     * エラー状態でWidgetを更新
     */
    private fun updateWidgetError(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        error: WidgetErrorHandler.WidgetError
    ) {
        val views = RemoteViews(context.packageName, R.layout.work_time_widget)

        // エラー表示
        views.setTextViewText(R.id.widget_hour_text, "--")
        views.setTextViewText(R.id.widget_minute_text, "--")
        views.setTextViewText(R.id.widget_date_text, "エラー")
        views.setTextViewText(R.id.widget_status_text, "再試行してください")
        views.setTextViewText(R.id.widget_clock_out_button, "再試行")

        // 再試行用のPendingIntent設定
        val retryIntent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
            action = ACTION_RETRY_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_clock_out_button, retryPendingIntent)

        // Widgetを更新
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * ボタンのPendingIntentを設定
     */
    private fun setupButtonIntents(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        // 退勤ボタンのクリックイベント
        val clockOutIntent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
            action = ACTION_CLOCK_OUT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val clockOutPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clockOutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_clock_out_button, clockOutPendingIntent)

        // Widget全体のクリック時にアプリを開く
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_time_display, appPendingIntent)

        // -5分ボタンのクリックイベント
        val minus5Intent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
            action = ACTION_ADJUST_TIME_MINUS_5
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val minus5PendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 1000, // ユニークなrequest code
            minus5Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_minus_5_button, minus5PendingIntent)

        // +5分ボタンのクリックイベント
        val plus5Intent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
            action = ACTION_ADJUST_TIME_PLUS_5
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val plus5PendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 2000, // ユニークなrequest code
            plus5Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_plus_5_button, plus5PendingIntent)
    }

    /**
     * 退勤処理のハンドリング
     * 非同期で退勤時間を記録し、ウィジェットを更新
     */
    private fun handleClockOut(context: Context) {

        Log.i("WorkTimeWidgetProvider", "退勤ボタンタップ - サービス開始")
        // WidgetUpdateServiceを使用して退勤記録処理を実行
        WidgetUpdateService.startClockOutRecording(context)
    }

    /**
     * 強制的にすべてのウィジェットを更新
     */
    private suspend fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        } catch (e: Exception) {
            Log.e("WorkTimeWidgetProvider", "ウィジェット更新エラー", e)
        }
    }
    
    private fun handleRetryUpdate(context: Context) {
        // Handle retry update by forcing a widget refresh
        CoroutineScope(Dispatchers.Main).launch {
            val repository = WidgetRepository.create(context)
            // Clear any cached state to force fresh data retrieval
            val stateCache = WidgetStateCache(context)
            stateCache.clearCache()
            
            // Force widget update
            when (val result = repository.updateWidget(context)) {
                is WidgetErrorHandler.WidgetResult.Success -> {
                    // Retry successful
                }
                is WidgetErrorHandler.WidgetResult.Error -> {
                    // Retry failed, but widget will show error state
                    val errorHandler = WidgetErrorHandler()
                    errorHandler.logError(context, result.error, "retryUpdate")
                }
            }
        }
    }
}