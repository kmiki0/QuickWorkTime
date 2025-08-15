package com.example.quickworktime.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver for handling automatic widget updates
 * Responds to system events like date changes, boot completion, and data updates
 */
class WidgetUpdateReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_DATA_UPDATED = "com.example.quickworktime.DATA_UPDATED"
        const val ACTION_FORCE_UPDATE = "com.example.quickworktime.widget.FORCE_UPDATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED -> {
                // Handle date change - update widget to show new date's data
                handleDateChange(context)
            }
            Intent.ACTION_TIME_CHANGED -> {
                // Handle time change (including timezone changes)
                handleTimeChange(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Handle system boot - restore widget functionality
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Handle app update - restore widget functionality
                handleAppUpdated(context)
            }
            ACTION_DATA_UPDATED -> {
                // Handle app data update - refresh widget display
                handleDataUpdated(context)
            }
            ACTION_FORCE_UPDATE -> {
                // Handle forced update request
                handleForceUpdate(context)
            }
        }
    }
    
    /**
     * Handles date change event
     * Updates widget to show new date's work information
     */
    private fun handleDateChange(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }
    
    /**
     * Handles time change event
     * Updates widget to reflect current time-based calculations
     */
    private fun handleTimeChange(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }
    
    /**
     * Handles system boot completion
     * Restores widget functionality after system restart
     */
    private fun handleBootCompleted(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Restore widget state and update display
                updateAllWidgets(context)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }
    
    /**
     * Handles app update/replacement
     * Restores widget functionality after app update
     */
    private fun handleAppUpdated(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }
    
    /**
     * Handles data update from the main app
     * Refreshes widget when work data is modified in the app
     */
    private fun handleDataUpdated(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }
    
    /**
     * Handles forced update request
     * Immediately updates all widget instances
     */
    private fun handleForceUpdate(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }
    
    /**
     * Updates all widget instances
     * Common method used by all event handlers
     */
    private suspend fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val widgetProvider = WorkTimeWidgetProvider()
                widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds)
            }
        } catch (e: Exception) {
            // Log error but don't throw to prevent crashes
        }
    }
}