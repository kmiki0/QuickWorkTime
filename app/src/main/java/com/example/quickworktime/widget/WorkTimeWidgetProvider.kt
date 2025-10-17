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
        const val ACTION_TIME_SELECTED = "com.example.quickworktime.widget.ACTION_TIME_SELECTED"
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
     * StackViewで時刻が選択された時の処理
     */
    private fun handleTimeSelected(context: Context, intent: Intent) {
        val selectedTime = intent.getStringExtra("selected_time") ?: return
        val position = intent.getIntExtra("position", -1)

        Log.i("WorkTimeWidgetProvider", "時刻選択: $selectedTime (position: $position)")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetRepository.create(context)
                val stateCache = WidgetStateCache(context)

                // 現在の表示状態を取得
                val displayStateResult = repository.getDisplayState()

                when (displayStateResult) {
                    is WidgetErrorHandler.WidgetResult.Success<*> -> {
                        val displayState = displayStateResult.data as? WidgetDisplayState
                        if (displayState == null) {
                            Log.e("WorkTimeWidgetProvider", "表示状態の取得に失敗")
                            return@launch
                        }

                        // 選択された時刻で状態を更新
                        val adjustedState = displayState.copy(
                            displayTime = selectedTime,
                            isAdjusted = true  // 調整済みフラグを立てる
                        )
                        stateCache.saveLastGoodState(adjustedState)

                        // ウィジェットを更新
                        withContext(Dispatchers.Main) {
                            updateAllWidgets(context)
                        }
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        Log.e("WorkTimeWidgetProvider", "時刻選択エラー: ${displayStateResult.error}")
                    }
                }
            } catch (e: Exception) {
                Log.e("WorkTimeWidgetProvider", "時刻選択処理エラー", e)
            }
        }
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


    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.i("WidgetDebug", "updateAppWidget開始: appWidgetId=$appWidgetId")


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetRepository.create(context)
                val stateCache = WidgetStateCache(context)  // 追加

                // 【重要】キャッシュに調整済み状態があれば、それを優先的に使用
                val cachedState = stateCache.getLastGoodState()
                val displayStateResult = if (cachedState != null && cachedState.isAdjusted) {
                    Log.i("WidgetDebug", "キャッシュされた調整済み状態を使用: ${cachedState.displayTime}")
                    WidgetErrorHandler.WidgetResult.Success(cachedState)
                } else {
                    // キャッシュがない、または調整されていない場合は通常通り取得
                    repository.getDisplayState()
                }

                // ここで layoutId を取得
                val layoutId = getWidgetLayoutId(context, appWidgetId, appWidgetManager)

                withContext(Dispatchers.Main) {
                    when (displayStateResult) {
                        is WidgetErrorHandler.WidgetResult.Success<*> -> {
                            val displayState = displayStateResult.data as? WidgetDisplayState
                            if (displayState == null) {
                                Log.e("WidgetDebug", "表示状態の型変換失敗")
                                return@withContext
                            }

                            Log.i("WidgetDebug", "表示状態取得成功: displayTime=${displayState.displayTime}, hasRecord=${displayState.hasRecord}, isAdjusted=${displayState.isAdjusted}")

                            // RemoteViewsを作成
                            val views = RemoteViews(context.packageName, layoutId)

                            // ウィジェットの表示内容を更新
                            updateWidgetViews(views, displayState)

                            // ボタンクリック時のPendingIntent設定
                            setupButtonIntents(context, views, appWidgetId)

                            // StackViewの初期位置を設定
                            setupStackViewPosition(context, views, displayState.displayTime)

                            // Widgetを更新
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                        is WidgetErrorHandler.WidgetResult.Error -> {
                            Log.e("WidgetDebug", "表示状態取得エラー: ${displayStateResult.error.message}")
                            updateWidgetError(context, appWidgetManager, appWidgetId, displayStateResult.error)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WidgetDebug", "updateAppWidget予期しないエラー", e)
                withContext(Dispatchers.Main) {
                    updateWidgetError(context, appWidgetManager, appWidgetId,
                        WidgetErrorHandler.WidgetError.UnknownError("Widget update failed: ${e.message}"))
                }
            }
        }
    }

    /**
     * ウィジェットのサイズに応じたレイアウトIDを取得
     */
    private fun getWidgetLayoutId(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ): Int {
        // ウィジェットのサイズ情報を取得
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        // 最小幅を取得（dp単位）
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        // サイズに応じてレイアウトを選択
        // 幅が180dp未満、または高さが60dp未満の場合は小サイズレイアウト
        return if (minWidth < 180 || minHeight < 60) {
            R.layout.work_time_widget_small
        } else {
            R.layout.work_time_widget
        }
    }

    /**
     * 正常な状態でWidgetのViewを更新
     */
    private fun updateWidgetViews(
        views: RemoteViews,
        displayState: WidgetDisplayState
    ) {
        // 日付テキストを設定
        views.setTextViewText(R.id.widget_date_text, displayState.dateText)

        // ステータステキストの表示/非表示を設定
        if (displayState.hasRecord) {
            views.setViewVisibility(R.id.widget_status_text, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_status_text, "退勤予定")
        }

        // Exitボタンのテキスト設定
        views.setTextViewText(R.id.widget_clock_out_button, displayState.buttonText)
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
     * StackViewの表示位置を設定
     */
    private fun setupStackViewPosition(
        context: Context,
        views: RemoteViews,
        currentTime: String
    ) {
        try {
            // 時刻リストの中から現在時刻に対応するインデックスを計算
            val timeParts = currentTime.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            // 5分刻みでのインデックスを計算
            val position = (hour * 12) + (minute / 5)

            // StackViewの表示位置を設定
            views.setDisplayedChild(R.id.widget_time_stack, position)
        } catch (e: Exception) {
            Log.e("WorkTimeWidgetProvider", "StackView位置設定エラー", e)
        }
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

        // StackView用のRemoteAdapter設定
        val serviceIntent = Intent(context, WidgetRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_time_stack, serviceIntent)

        // StackViewアイテムクリック時のPendingIntent設定
        val clickIntent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
            action = ACTION_TIME_SELECTED
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val clickPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE  // MUTABLEに変更
        )
        views.setPendingIntentTemplate(R.id.widget_time_stack, clickPendingIntent)
    }

    /**
     * 退勤処理のハンドリング
     * 非同期で退勤時間を記録し、ウィジェットを更新
     */
    private fun handleClockOut(context: Context) {

        Log.i("WorkTimeWidgetProvider", "退勤ボタンタップ - サービス開始")

        // キャッシュをクリア(調整済み状態をリセット)
        val stateCache = WidgetStateCache(context)
        stateCache.clearCache()

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