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
        
        // Set up click listeners
        setupClickListeners(context, views, appWidgetId)
        
        // Update widget display asynchronously
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val widgetState = getWidgetDisplayState(context)
                updateWidgetDisplay(views, widgetState)
                
                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                // Fallback to default display if data loading fails
                val fallbackState = WidgetDisplayState(
                    displayTime = "--:--",
                    dateText = "エラー",
                    hasRecord = false
                )
                updateWidgetDisplay(views, fallbackState)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
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
        
        // Set up click listener for clock out button
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
     * Gets the current widget display state based on today's work data
     */
    private suspend fun getWidgetDisplayState(context: Context): WidgetDisplayState {
        val repository = WidgetRepository.create(context)
        val todayWorkInfo = repository.getTodayWorkInfo()
        val dateFormatter = DateTimeFormatter.ofPattern("M/d")
        val todayText = LocalDate.now().format(dateFormatter)
        
        return if (todayWorkInfo?.endTime != null) {
            // Has record state - show recorded end time
            WidgetDisplayState(
                displayTime = todayWorkInfo.endTime,
                dateText = todayText,
                hasRecord = true,
                buttonText = "記録済み"
            )
        } else {
            // No record state - show calculated optimal time
            val optimalTime = repository.calculateOptimalClockOutTime()
            WidgetDisplayState(
                displayTime = optimalTime,
                dateText = todayText,
                hasRecord = false,
                buttonText = "Exit"
            )
        }
    }
    
    /**
     * Updates the widget display using RemoteViews
     */
    private fun updateWidgetDisplay(views: RemoteViews, state: WidgetDisplayState) {
        // Update time display
        views.setTextViewText(R.id.widget_time_text, state.displayTime)
        
        // Update date display
        views.setTextViewText(R.id.widget_date_text, state.dateText)
        
        // Update button text and state
        views.setTextViewText(R.id.widget_clock_out_button, state.buttonText)
        
        // Disable button if already recorded
        if (state.hasRecord) {
            views.setInt(R.id.widget_clock_out_button, "setAlpha", 128) // Make button semi-transparent
            views.setBoolean(R.id.widget_clock_out_button, "setEnabled", false)
        } else {
            views.setInt(R.id.widget_clock_out_button, "setAlpha", 255) // Make button fully opaque
            views.setBoolean(R.id.widget_clock_out_button, "setEnabled", true)
        }
        
        // Set content description for accessibility
        val contentDescription = if (state.hasRecord) {
            "退勤時刻: ${state.displayTime} (記録済み)"
        } else {
            "退勤予定時刻: ${state.displayTime}"
        }
        views.setContentDescription(R.id.widget_time_display, contentDescription)
    }
    
    private fun handleClockOut(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetRepository.create(context)
                
                // Only record if no record exists for today
                if (!repository.hasTodayRecord()) {
                    val optimalTime = repository.calculateOptimalClockOutTime()
                    repository.recordClockOut(optimalTime)
                }
                
                // Update all widgets to reflect the new state
                repository.updateWidget(context)
            } catch (e: Exception) {
                // If recording fails, still try to update widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, WorkTimeWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
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
}