package com.example.quickworktime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.quickworktime.MainActivity
import com.example.quickworktime.R

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
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.work_time_widget)
        
        // Set up click listeners
        setupClickListeners(context, views, appWidgetId)
        
        // Update widget display
        updateWidgetDisplay(context, views)
        
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
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
    
    private fun updateWidgetDisplay(context: Context, views: RemoteViews) {
        // TODO: This will be implemented in later tasks
        // For now, show placeholder text
        views.setTextViewText(R.id.widget_time_display, "18:15")
        views.setTextViewText(R.id.widget_clock_out_button, "Exit")
    }
    
    private fun handleClockOut(context: Context) {
        // TODO: This will be implemented in later tasks
        // For now, just update all widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, WorkTimeWidgetProvider::class.java)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
    }
    
    private fun handleTimeAdjustment(context: Context, intent: Intent) {
        // TODO: This will be implemented in later tasks
        val timeComponent = intent.getStringExtra(EXTRA_TIME_COMPONENT)
        val direction = intent.getStringExtra(EXTRA_DIRECTION)
        
        // Update all widgets after adjustment
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, WorkTimeWidgetProvider::class.java)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
    }
}