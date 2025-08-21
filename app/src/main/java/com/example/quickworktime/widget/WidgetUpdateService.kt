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
     * Handles clock-out recording in background thread with comprehensive error handling
     */
    private fun handleClockOutRecording(intent: Intent, startId: Int) {
        serviceScope.launch {
            val repository = WidgetRepository.create(this@WidgetUpdateService)
            val errorHandler = WidgetErrorHandler()
            
            try {
                // Check if today already has a record
                if (repository.hasTodayRecord()) {
                    // Already recorded, just update widget to show current state
                    when (val updateResult = repository.updateWidget(this@WidgetUpdateService)) {
                        is WidgetErrorHandler.WidgetResult.Error -> {
                            errorHandler.logError(this@WidgetUpdateService, updateResult.error, "updateWidget after duplicate record check")
                        }
                        is WidgetErrorHandler.WidgetResult.Success -> {
                            // Update successful
                        }
                    }
                    stopSelf(startId)
                    return@launch
                }
                
                // Get clock-out time from intent or calculate optimal time
                val clockOutTime = intent.getStringExtra(EXTRA_CLOCK_OUT_TIME) 
                    ?: repository.calculateOptimalClockOutTime()
                
                // Record the clock-out time with error handling
                when (val recordResult = repository.recordClockOut(clockOutTime)) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
                        // Recording successful, notify and update widgets
                        repository.notifyDataUpdated(this@WidgetUpdateService)
                        
                        when (val updateResult = repository.updateWidget(this@WidgetUpdateService)) {
                            is WidgetErrorHandler.WidgetResult.Error -> {
                                errorHandler.logError(this@WidgetUpdateService, updateResult.error, "updateWidget after successful record")
                            }
                            is WidgetErrorHandler.WidgetResult.Success -> {
                                // Update successful
                            }
                        }
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        // Recording failed, log error and still try to update widgets
                        errorHandler.logError(this@WidgetUpdateService, recordResult.error, "recordClockOut")
                        
                        // Try to update widgets to show current state (might show error state)
                        when (val updateResult = repository.updateWidget(this@WidgetUpdateService)) {
                            is WidgetErrorHandler.WidgetResult.Error -> {
                                errorHandler.logError(this@WidgetUpdateService, updateResult.error, "updateWidget after failed record")
                            }
                            is WidgetErrorHandler.WidgetResult.Success -> {
                                // Update successful
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                // Unexpected error, create error and log it
                val unexpectedError = WidgetErrorHandler.WidgetError.UnknownError("Unexpected error in clock-out recording", e)
                errorHandler.logError(this@WidgetUpdateService, unexpectedError, "handleClockOutRecording")
                
                // Still try to update widgets
                repository.updateWidget(this@WidgetUpdateService)
            } finally {
                stopSelf(startId)
            }
        }
    }
    
    /**
     * Handles widget update in background thread with error handling
     */
    private fun handleWidgetUpdate(startId: Int) {
        serviceScope.launch {
            val repository = WidgetRepository.create(this@WidgetUpdateService)
            val errorHandler = WidgetErrorHandler()
            
            when (val result = repository.updateWidget(this@WidgetUpdateService)) {
                is WidgetErrorHandler.WidgetResult.Success -> {
                    // Update successful
                }
                is WidgetErrorHandler.WidgetResult.Error -> {
                    errorHandler.logError(this@WidgetUpdateService, result.error, "handleWidgetUpdate")
                }
            }
            
            stopSelf(startId)
        }
    }
}