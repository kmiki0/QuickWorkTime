package com.example.quickworktime.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Integration helper for connecting main app operations with widget updates
 * Provides methods to notify widgets when data changes occur in the main application
 */
object WidgetIntegration {
    
    /**
     * Should be called after any work data modification in the main app
     * Ensures widgets are updated to reflect the latest data
     * 
     * @param context Application context
     */
    fun onWorkDataChanged(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                WidgetUpdateManager.notifyDataUpdated(context)
            } catch (e: Exception) {
                // Log error but don't crash the main app
            }
        }
    }
    
    /**
     * Should be called when work data is inserted
     * 
     * @param context Application context
     */
    fun onWorkDataInserted(context: Context) {
        onWorkDataChanged(context)
    }
    
    /**
     * Should be called when work data is updated
     * 
     * @param context Application context
     */
    fun onWorkDataUpdated(context: Context) {
        onWorkDataChanged(context)
    }
    
    /**
     * Should be called when work data is deleted
     * 
     * @param context Application context
     */
    fun onWorkDataDeleted(context: Context) {
        onWorkDataChanged(context)
    }
    
    /**
     * Forces immediate widget refresh
     * Useful for manual refresh or error recovery scenarios
     * 
     * @param context Application context
     */
    fun forceWidgetRefresh(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                WidgetUpdateManager.forceUpdate(context)
            } catch (e: Exception) {
                // Log error but don't crash the main app
            }
        }
    }
    
    /**
     * Initializes widget integration
     * Should be called when the main app starts
     * 
     * @param context Application context
     */
    fun initialize(context: Context) {
        // Set up any necessary initialization for widget integration
        // Currently no specific initialization needed
        // Future enhancements might include setting up periodic updates
    }
    
    /**
     * Cleans up widget integration resources
     * Should be called when the main app is being destroyed
     * 
     * @param context Application context
     */
    fun cleanup(context: Context) {
        // Clean up any resources used by widget integration
        // Currently no specific cleanup needed
        // Future enhancements might include canceling periodic updates
    }
}