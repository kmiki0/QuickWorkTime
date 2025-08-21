package com.example.quickworktime.widget

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.sql.SQLException

/**
 * Demo class to showcase error handling functionality
 * This can be used to verify that the error handling system works correctly
 */
class ErrorHandlingDemo {
    
    companion object {
        private const val TAG = "ErrorHandlingDemo"
        
        /**
         * Demonstrates various error handling scenarios
         */
        fun runErrorHandlingDemo(context: Context) {
            val errorHandler = WidgetErrorHandler()
            
            Log.i(TAG, "Starting error handling demonstration...")
            
            // Demo 1: Successful operation
            runBlocking {
                Log.i(TAG, "Demo 1: Successful operation")
                val result = errorHandler.executeWithRetry {
                    "Success!"
                }
                when (result) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
                        Log.i(TAG, "✓ Success: ${result.data}")
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        Log.e(TAG, "✗ Unexpected error: ${result.error.message}")
                    }
                }
            }
            
            // Demo 2: Database error with retry
            runBlocking {
                Log.i(TAG, "Demo 2: Database error with retry")
                var attemptCount = 0
                val result = errorHandler.executeWithRetry(
                    operation = {
                        attemptCount++
                        if (attemptCount < 3) {
                            throw SQLException("Database connection failed")
                        }
                        "Database operation succeeded after retries"
                    },
                    maxRetries = 3,
                    retryDelay = 100
                )
                when (result) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
                        Log.i(TAG, "✓ Success after $attemptCount attempts: ${result.data}")
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        Log.e(TAG, "✗ Failed after retries: ${result.error.message}")
                    }
                }
            }
            
            // Demo 3: Permanent error (no retry)
            runBlocking {
                Log.i(TAG, "Demo 3: Permanent error (no retry)")
                var attemptCount = 0
                val result = errorHandler.executeWithRetry(
                    operation = {
                        attemptCount++
                        throw SecurityException("Permission denied")
                    },
                    maxRetries = 3,
                    retryDelay = 100
                )
                when (result) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
                        Log.i(TAG, "✓ Unexpected success: ${result.data}")
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        Log.i(TAG, "✓ Expected error after $attemptCount attempts: ${result.error.message}")
                    }
                }
            }
            
            // Demo 4: Error state creation
            Log.i(TAG, "Demo 4: Error state creation")
            val databaseError = WidgetErrorHandler.WidgetError.DatabaseError("Database connection failed")
            val errorState = errorHandler.createErrorWidgetState(databaseError)
            Log.i(TAG, "✓ Error state created: time=${errorState.displayTime}, date=${errorState.dateText}, button=${errorState.buttonText}")
            
            // Demo 5: Cache usage decision
            Log.i(TAG, "Demo 5: Cache usage decision")
            val networkError = WidgetErrorHandler.WidgetError.NetworkError("Network unavailable")
            val cachedState = WidgetDisplayState("18:15", "今日", false, "Exit")
            val shouldUseCache = errorHandler.shouldUseCachedState(networkError, cachedState)
            Log.i(TAG, "✓ Should use cached state for network error: $shouldUseCache")
            
            val updateError = WidgetErrorHandler.WidgetError.UpdateError("Update failed")
            val shouldUseCacheForUpdate = errorHandler.shouldUseCachedState(updateError, cachedState)
            Log.i(TAG, "✓ Should use cached state for update error: $shouldUseCacheForUpdate")
            
            Log.i(TAG, "Error handling demonstration completed!")
        }
        
        /**
         * Demonstrates widget state cache functionality
         */
        fun runStateCacheDemo(context: Context) {
            Log.i(TAG, "Starting state cache demonstration...")
            
            runBlocking {
                val stateCache = WidgetStateCache(context)
                
                // Demo 1: Save and retrieve state
                Log.i(TAG, "Demo 1: Save and retrieve state")
                val testState = WidgetDisplayState("18:30", "12/25", false, "Exit")
                stateCache.saveLastGoodState(testState)
                
                val retrievedState = stateCache.getLastGoodState()
                if (retrievedState != null) {
                    Log.i(TAG, "✓ State retrieved: time=${retrievedState.displayTime}, date=${retrievedState.dateText}")
                } else {
                    Log.e(TAG, "✗ Failed to retrieve state")
                }
                
                // Demo 2: Check cache availability
                Log.i(TAG, "Demo 2: Check cache availability")
                val hasCachedState = stateCache.hasCachedState()
                Log.i(TAG, "✓ Has cached state: $hasCachedState")
                
                // Demo 3: Check cache age
                Log.i(TAG, "Demo 3: Check cache age")
                val cacheAge = stateCache.getCacheAge()
                Log.i(TAG, "✓ Cache age: ${cacheAge}ms")
                
                // Demo 4: Clear cache
                Log.i(TAG, "Demo 4: Clear cache")
                stateCache.clearCache()
                val hasCachedStateAfterClear = stateCache.hasCachedState()
                Log.i(TAG, "✓ Has cached state after clear: $hasCachedStateAfterClear")
            }
            
            Log.i(TAG, "State cache demonstration completed!")
        }
    }
}