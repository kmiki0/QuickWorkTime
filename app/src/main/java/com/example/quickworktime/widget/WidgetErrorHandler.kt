package com.example.quickworktime.widget

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.sql.SQLException

/**
 * Centralized error handling for widget operations
 * Provides retry mechanisms and error state management
 */
class WidgetErrorHandler {
    
    companion object {
        private const val TAG = "WidgetErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val EXPONENTIAL_BACKOFF_MULTIPLIER = 2
    }
    
    /**
     * Represents different types of widget errors
     */
    sealed class WidgetError(val message: String, val cause: Throwable? = null) {
        class DatabaseError(message: String, cause: Throwable? = null) : WidgetError(message, cause)
        class NetworkError(message: String, cause: Throwable? = null) : WidgetError(message, cause)
        class UpdateError(message: String, cause: Throwable? = null) : WidgetError(message, cause)
        class UnknownError(message: String, cause: Throwable? = null) : WidgetError(message, cause)
    }
    
    /**
     * Represents the result of a widget operation
     */
    sealed class WidgetResult<T> {
        data class Success<T>(val data: T) : WidgetResult<T>()
        data class Error<T>(val error: WidgetError) : WidgetResult<T>()
    }
    
    /**
     * Executes an operation with retry logic and error handling
     * 
     * @param operation The operation to execute
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param retryDelay Initial delay between retries in milliseconds (default: 1000)
     * @param useExponentialBackoff Whether to use exponential backoff for retry delays
     * @return WidgetResult containing either success data or error information
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        maxRetries: Int = MAX_RETRY_ATTEMPTS,
        retryDelay: Long = RETRY_DELAY_MS,
        useExponentialBackoff: Boolean = true
    ): WidgetResult<T> {
        var lastException: Exception? = null
        var currentDelay = retryDelay
        
        repeat(maxRetries + 1) { attempt ->
            try {
                val result = operation()
                if (attempt > 0) {
                    Log.i(TAG, "Operation succeeded after $attempt retries")
                }
                return WidgetResult.Success(result)
            } catch (e: Exception) {
                lastException = e
                val error = classifyError(e)
                
                Log.w(TAG, "Operation failed (attempt ${attempt + 1}/${maxRetries + 1}): ${error.message}", e)
                
                // Don't retry for certain types of errors
                if (!shouldRetry(error) || attempt == maxRetries) {
                    return WidgetResult.Error(error)
                }
                
                // Wait before retrying
                if (attempt < maxRetries) {
                    delay(currentDelay)
                    if (useExponentialBackoff) {
                        currentDelay *= EXPONENTIAL_BACKOFF_MULTIPLIER
                    }
                }
            }
        }
        
        val finalError = lastException?.let { classifyError(it) } 
            ?: WidgetError.UnknownError("Operation failed with unknown error")
        
        Log.e(TAG, "Operation failed after $maxRetries retries: ${finalError.message}", lastException)
        return WidgetResult.Error(finalError)
    }
    
    /**
     * Classifies an exception into a specific WidgetError type
     */
    private fun classifyError(exception: Exception): WidgetError {
        return when (exception) {
            is SQLException, 
            is android.database.SQLException -> {
                WidgetError.DatabaseError("データベースアクセスエラーが発生しました", exception)
            }
            is IOException -> {
                WidgetError.NetworkError("ネットワークエラーが発生しました", exception)
            }
            is SecurityException -> {
                WidgetError.UpdateError("権限エラーが発生しました", exception)
            }
            is IllegalStateException -> {
                WidgetError.UpdateError("ウィジェット更新エラーが発生しました", exception)
            }
            else -> {
                WidgetError.UnknownError("予期しないエラーが発生しました: ${exception.message}", exception)
            }
        }
    }
    
    /**
     * Determines whether an error should trigger a retry
     */
    private fun shouldRetry(error: WidgetError): Boolean {
        return when (error) {
            is WidgetError.DatabaseError -> true  // Database errors might be temporary
            is WidgetError.NetworkError -> true   // Network errors might be temporary
            is WidgetError.UpdateError -> false   // Update errors are usually permanent
            is WidgetError.UnknownError -> true   // Unknown errors might be temporary
        }
    }
    
    /**
     * Logs error information for debugging purposes
     */
    fun logError(context: Context, error: WidgetError, operation: String) {
        val errorMessage = "Widget operation '$operation' failed: ${error.message}"
        Log.e(TAG, errorMessage, error.cause)
        
        // In a production app, you might want to send this to a crash reporting service
        // or store it in a local error log for debugging
    }
    
    /**
     * Creates a fallback widget state for error conditions
     */
    fun createErrorWidgetState(error: WidgetError): WidgetDisplayState {
        val errorMessage = when (error) {
            is WidgetError.DatabaseError -> "DB エラー"
            is WidgetError.NetworkError -> "接続エラー"
            is WidgetError.UpdateError -> "更新エラー"
            is WidgetError.UnknownError -> "エラー"
        }
        
        return WidgetDisplayState(
            displayTime = "--:--",
            dateText = errorMessage,
            hasRecord = false,
            buttonText = "再試行"
        )
    }
    
    /**
     * Determines if a cached state should be used instead of showing an error
     */
    fun shouldUseCachedState(error: WidgetError, cachedState: WidgetDisplayState?): Boolean {
        // Use cached state for temporary errors if available
        return cachedState != null && (error is WidgetError.NetworkError || error is WidgetError.DatabaseError)
    }
}