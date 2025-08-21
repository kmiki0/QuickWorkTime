package com.example.quickworktime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.quickworktime.MainActivity
import com.example.quickworktime.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data class representing the current state of the widget
 */
data class WidgetDisplayState(
    val displayTime: String,
    val dateText: String,
    val hasRecord: Boolean,
    val buttonText: String = "Exit"
)

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [WorkTimeWidgetConfigureActivity]
 */
class WorkTimeWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_CLOCK_OUT = "com.example.quickworktime.widget.ACTION_CLOCK_OUT"
        const val ACTION_ADJUST_TIME = "com.example.quickworktime.widget.ACTION_ADJUST_TIME"
        const val ACTION_RETRY_UPDATE = "com.example.quickworktime.widget.ACTION_RETRY_UPDATE"
        const val EXTRA_TIME_COMPONENT = "time_component"
        const val EXTRA_DIRECTION = "direction"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_CLOCK_OUT -> {
                // Handle clock out action
                handleClockOut(context)
            }
            ACTION_ADJUST_TIME -> {
                // Handle time adjustment action
                handleTimeAdjustment(context, intent)
            }
            ACTION_RETRY_UPDATE -> {
                // Handle retry update action
                handleRetryUpdate(context)
            }
            Intent.ACTION_CONFIGURATION_CHANGED -> {
                // Handle theme/configuration changes
                handleConfigurationChanged(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Determine widget size and layout
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val layoutId = if (minWidth < 180) R.layout.work_time_widget_small else R.layout.work_time_widget
        
        // Construct the RemoteViews object with appropriate layout
        val views = RemoteViews(context.packageName, layoutId)
        
        // Update widget display asynchronously with comprehensive error handling
        CoroutineScope(Dispatchers.Main).launch {
            val widgetState = getWidgetDisplayState(context)
            
            // Check if this is an error state to set up appropriate click listeners
            val isErrorState = widgetState.displayTime == "--:--" && 
                              (widgetState.dateText.contains("エラー") || widgetState.dateText.contains("DB") || 
                               widgetState.dateText.contains("接続") || widgetState.dateText.contains("更新"))
            
            if (isErrorState) {
                setupErrorStateClickListeners(context, views, appWidgetId)
            } else {
                setupClickListeners(context, views, appWidgetId)
            }
            
            updateWidgetDisplay(views, widgetState)
            
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
    
    private fun setupClickListeners(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Set up click listener for opening the main app
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_time_display, appPendingIntent)
        
        // Set up click listener for clock out button (will be dynamically changed for error states)
        val clockOutIntent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
            action = ACTION_CLOCK_OUT
        }
        val clockOutPendingIntent = PendingIntent.getBroadcast(
            context, 0, clockOutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_clock_out_button, clockOutPendingIntent)
    }
    
    /**
     * Sets up click listeners for error state (retry functionality)
     */
    private fun setupErrorStateClickListeners(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Keep the app launch listener
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_time_display, appPendingIntent)
        
        // Set up retry click listener for the button
        val retryIntent = Intent(context, WorkTimeWidgetProvider::class.java).apply {
            action = ACTION_RETRY_UPDATE
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context, 1, retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_clock_out_button, retryPendingIntent)
    }
    
    /**
     * Gets the current widget display state with error handling
     */
    private suspend fun getWidgetDisplayState(context: Context): WidgetDisplayState {
        val repository = WidgetRepository.create(context)
        
        return when (val result = repository.getWidgetDisplayState(context)) {
            is WidgetErrorHandler.WidgetResult.Success -> result.data
            is WidgetErrorHandler.WidgetResult.Error -> {
                // Create error state for display
                val errorHandler = WidgetErrorHandler()
                errorHandler.createErrorWidgetState(result.error)
            }
        }
    }
    
    /**
     * Updates the widget display using RemoteViews with enhanced error state handling
     */
    private fun updateWidgetDisplay(views: RemoteViews, state: WidgetDisplayState) {
        // Check if this is an error state
        val isErrorState = state.displayTime == "--:--" && 
                          (state.dateText.contains("エラー") || state.dateText.contains("DB") || 
                           state.dateText.contains("接続") || state.dateText.contains("更新"))
        
        // Split time into hour and minute components
        val timeParts = state.displayTime.split(":")
        val hour = if (timeParts.size >= 2) timeParts[0] else "--"
        val minute = if (timeParts.size >= 2) timeParts[1] else "--"
        
        // Update time display components
        views.setTextViewText(R.id.widget_hour_text, hour)
        views.setTextViewText(R.id.widget_minute_text, minute)
        
        // Update date display
        views.setTextViewText(R.id.widget_date_text, state.dateText)
        
        // Update status text based on state
        val statusText = when {
            isErrorState -> "データ取得失敗"
            state.hasRecord -> "記録済み"
            else -> "退勤予定"
        }
        views.setTextViewText(R.id.widget_status_text, statusText)
        
        // Update button text and state
        views.setTextViewText(R.id.widget_clock_out_button, state.buttonText)
        
        // Handle button state based on error or record status
        when {
            isErrorState -> {
                // Error state - enable retry button
                views.setInt(R.id.widget_clock_out_button, "setAlpha", 255)
                views.setBoolean(R.id.widget_clock_out_button, "setEnabled", true)
                // Change button action to retry for error states
                // This would require additional intent handling for retry functionality
            }
            state.hasRecord -> {
                // Already recorded - disable button
                views.setInt(R.id.widget_clock_out_button, "setAlpha", 128)
                views.setBoolean(R.id.widget_clock_out_button, "setEnabled", false)
            }
            else -> {
                // Normal state - enable button
                views.setInt(R.id.widget_clock_out_button, "setAlpha", 255)
                views.setBoolean(R.id.widget_clock_out_button, "setEnabled", true)
            }
        }
        
        // Set content description for accessibility
        val contentDescription = when {
            isErrorState -> "エラー状態: ${state.dateText}. 再試行するにはボタンをタップしてください"
            state.hasRecord -> "退勤時刻: ${state.displayTime} (記録済み)"
            else -> "退勤予定時刻: ${state.displayTime}"
        }
        views.setContentDescription(R.id.widget_time_display, contentDescription)
    }
    
    private fun handleClockOut(context: Context) {
        // Delegate clock-out processing to WidgetUpdateService for better performance
        WidgetUpdateService.startClockOutRecording(context)
    }
    
    private fun handleTimeAdjustment(context: Context, intent: Intent) {
        // This will be fully implemented in task 6 (フリック操作による時間調整機能の実装)
        // For now, just refresh the widget display
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, WorkTimeWidgetProvider::class.java)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
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
    
    private fun handleConfigurationChanged(context: Context) {
        // Handle theme changes by updating all widget instances with error handling
        CoroutineScope(Dispatchers.Main).launch {
            val repository = WidgetRepository.create(context)
            when (val result = repository.updateWidgetTheme(context)) {
                is WidgetErrorHandler.WidgetResult.Success -> {
                    // Theme update successful
                }
                is WidgetErrorHandler.WidgetResult.Error -> {
                    // Fallback to standard update if theme update fails
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(context, WorkTimeWidgetProvider::class.java)
                    )
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
            }
        }
    }
}