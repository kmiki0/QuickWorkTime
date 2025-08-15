package com.example.quickworktime.widget

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager class for triggering widget updates from the main application
 * Provides methods to notify widgets when data changes occur
 */
object WidgetUpdateManager {
    
    /**
     * Notifies widgets that work data has been updated
     * Should be called whenever work information is modified in the main app
     * 
     * @param context Application context
     */
    fun notifyDataUpdated(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                    action = WidgetUpdateReceiver.ACTION_DATA_UPDATED
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                // Log error but don't crash the main app
            }
        }
    }
    
    /**
     * Forces an immediate update of all widget instances
     * Useful for manual refresh or error recovery
     * 
     * @param context Application context
     */
    fun forceUpdate(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                    action = WidgetUpdateReceiver.ACTION_FORCE_UPDATE
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                // Log error but don't crash the main app
            }
        }
    }
    
    /**
     * Schedules periodic widget updates
     * Sets up alarms for regular widget refresh (if needed)
     * 
     * @param context Application context
     */
    fun schedulePeriodicUpdates(context: Context) {
        // For now, we rely on system events and manual triggers
        // Future enhancement could add AlarmManager for periodic updates
        // if real-time updates are needed beyond system events
    }
    
    /**
     * Cancels any scheduled periodic updates
     * 
     * @param context Application context
     */
    fun cancelPeriodicUpdates(context: Context) {
        // Placeholder for future AlarmManager cancellation
        // Currently no periodic updates are scheduled
    }
}