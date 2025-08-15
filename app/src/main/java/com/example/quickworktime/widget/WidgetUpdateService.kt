package com.example.quickworktime.widget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Service for handling widget update operations in the background
 * This service handles clock-out recording and widget refresh operations
 */
class WidgetUpdateService : Service() {
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    companion object {
        const val ACTION_RECORD_CLOCK_OUT = "com.example.quickworktime.widget.ACTION_RECORD_CLOCK_OUT"
        const val ACTION_UPDATE_WIDGET = "com.example.quickworktime.widget.ACTION_UPDATE_WIDGET"
        const val EXTRA_CLOCK_OUT_TIME = "clock_out_time"
        
        /**
         * Helper method to start clock-out recording
         */
        fun startClockOutRecording(context: Context, clockOutTime: String? = null) {
            val intent = Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_RECORD_CLOCK_OUT
                clockOutTime?.let { putExtra(EXTRA_CLOCK_OUT_TIME, it) }
            }
            context.startService(intent)
        }
        
        /**
         * Helper method to trigger widget update
         */
        fun startWidgetUpdate(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RECORD_CLOCK_OUT -> {
                handleClockOutRecording(intent, startId)
            }
            ACTION_UPDATE_WIDGET -> {
                handleWidgetUpdate(startId)
            }
            else -> {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
    
    /**
     * Handles clock-out recording in background thread
     */
    private fun handleClockOutRecording(intent: Intent, startId: Int) {
        serviceScope.launch {
            try {
                val repository = WidgetRepository.create(this@WidgetUpdateService)
                
                // Check if today already has a record
                if (repository.hasTodayRecord()) {
                    // Already recorded, just update widget to show current state
                    repository.updateWidget(this@WidgetUpdateService)
                    stopSelf(startId)
                    return@launch
                }
                
                // Get clock-out time from intent or calculate optimal time
                val clockOutTime = intent.getStringExtra(EXTRA_CLOCK_OUT_TIME) 
                    ?: repository.calculateOptimalClockOutTime()
                
                // Record the clock-out time
                repository.recordClockOut(clockOutTime)
                
                // Update all widget instances to reflect the new state
                repository.updateWidget(this@WidgetUpdateService)
                
            } catch (e: Exception) {
                // If recording fails, still try to update widgets to show current state
                try {
                    val repository = WidgetRepository.create(this@WidgetUpdateService)
                    repository.updateWidget(this@WidgetUpdateService)
                } catch (updateException: Exception) {
                    // Log error but don't crash the service
                    // In production, you might want to use proper logging
                }
            } finally {
                stopSelf(startId)
            }
        }
    }
    
    /**
     * Handles widget update in background thread
     */
    private fun handleWidgetUpdate(startId: Int) {
        serviceScope.launch {
            try {
                val repository = WidgetRepository.create(this@WidgetUpdateService)
                repository.updateWidget(this@WidgetUpdateService)
            } catch (e: Exception) {
                // Log error but don't crash the service
                // In production, you might want to use proper logging
            } finally {
                stopSelf(startId)
            }
        }
    }
}