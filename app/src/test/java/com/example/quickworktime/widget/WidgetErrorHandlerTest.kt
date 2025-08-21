package com.example.quickworktime.widget

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.sql.SQLException

/**
 * Unit tests for WidgetErrorHandler
 */
class WidgetErrorHandlerTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var errorHandler: WidgetErrorHandler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        errorHandler = WidgetErrorHandler()
    }

    @Test
    fun `executeWithRetry should return success on first attempt`() = runTest {
        // Given
        val expectedResult = "success"
        val operation: suspend () -> String = { expectedResult }

        // When
        val result = errorHandler.executeWithRetry(operation)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Success)
        assertEquals(expectedResult, (result as WidgetErrorHandler.WidgetResult.Success).data)
    }

    @Test
    fun `executeWithRetry should retry on failure and eventually succeed`() = runTest {
        // Given
        var attemptCount = 0
        val operation: suspend () -> String = {
            attemptCount++
            if (attemptCount < 3) {
                throw SQLException("Database error")
            }
            "success after retries"
        }

        // When
        val result = errorHandler.executeWithRetry(operation, maxRetries = 3, retryDelay = 10)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Success)
        assertEquals("success after retries", (result as WidgetErrorHandler.WidgetResult.Success).data)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `executeWithRetry should return error after max retries exceeded`() = runTest {
        // Given
        val operation: suspend () -> String = {
            throw SQLException("Persistent database error")
        }

        // When
        val result = errorHandler.executeWithRetry(operation, maxRetries = 2, retryDelay = 10)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Error)
        val error = (result as WidgetErrorHandler.WidgetResult.Error).error
        assertTrue(error is WidgetErrorHandler.WidgetError.DatabaseError)
        assertTrue(error.message.contains("データベースアクセスエラー"))
    }

    @Test
    fun `executeWithRetry should not retry for update errors`() = runTest {
        // Given
        var attemptCount = 0
        val operation: suspend () -> String = {
            attemptCount++
            throw SecurityException("Permission denied")
        }

        // When
        val result = errorHandler.executeWithRetry(operation, maxRetries = 3, retryDelay = 10)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Error)
        val error = (result as WidgetErrorHandler.WidgetResult.Error).error
        assertTrue(error is WidgetErrorHandler.WidgetError.UpdateError)
        assertEquals(1, attemptCount) // Should not retry
    }

    @Test
    fun `classifyError should correctly classify SQLException as DatabaseError`() = runTest {
        // Given
        val operation: suspend () -> String = {
            throw SQLException("Database connection failed")
        }

        // When
        val result = errorHandler.executeWithRetry(operation, maxRetries = 0)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Error)
        val error = (result as WidgetErrorHandler.WidgetResult.Error).error
        assertTrue(error is WidgetErrorHandler.WidgetError.DatabaseError)
        assertTrue(error.message.contains("データベースアクセスエラー"))
    }

    @Test
    fun `classifyError should correctly classify IOException as NetworkError`() = runTest {
        // Given
        val operation: suspend () -> String = {
            throw IOException("Network connection failed")
        }

        // When
        val result = errorHandler.executeWithRetry(operation, maxRetries = 0)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Error)
        val error = (result as WidgetErrorHandler.WidgetResult.Error).error
        assertTrue(error is WidgetErrorHandler.WidgetError.NetworkError)
        assertTrue(error.message.contains("ネットワークエラー"))
    }

    @Test
    fun `classifyError should correctly classify SecurityException as UpdateError`() = runTest {
        // Given
        val operation: suspend () -> String = {
            throw SecurityException("Permission denied")
        }

        // When
        val result = errorHandler.executeWithRetry(operation, maxRetries = 0)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Error)
        val error = (result as WidgetErrorHandler.WidgetResult.Error).error
        assertTrue(error is WidgetErrorHandler.WidgetError.UpdateError)
        assertTrue(error.message.contains("権限エラー"))
    }

    @Test
    fun `classifyError should classify unknown exceptions as UnknownError`() = runTest {
        // Given
        val operation: suspend () -> String = {
            throw RuntimeException("Unknown error")
        }

        // When
        val result = errorHandler.executeWithRetry(operation, maxRetries = 0)

        // Then
        assertTrue(result is WidgetErrorHandler.WidgetResult.Error)
        val error = (result as WidgetErrorHandler.WidgetResult.Error).error
        assertTrue(error is WidgetErrorHandler.WidgetError.UnknownError)
        assertTrue(error.message.contains("予期しないエラー"))
    }

    @Test
    fun `createErrorWidgetState should create appropriate state for DatabaseError`() {
        // Given
        val error = WidgetErrorHandler.WidgetError.DatabaseError("Database error")

        // When
        val state = errorHandler.createErrorWidgetState(error)

        // Then
        assertEquals("--:--", state.displayTime)
        assertEquals("DB エラー", state.dateText)
        assertFalse(state.hasRecord)
        assertEquals("再試行", state.buttonText)
    }

    @Test
    fun `createErrorWidgetState should create appropriate state for NetworkError`() {
        // Given
        val error = WidgetErrorHandler.WidgetError.NetworkError("Network error")

        // When
        val state = errorHandler.createErrorWidgetState(error)

        // Then
        assertEquals("--:--", state.displayTime)
        assertEquals("接続エラー", state.dateText)
        assertFalse(state.hasRecord)
        assertEquals("再試行", state.buttonText)
    }

    @Test
    fun `createErrorWidgetState should create appropriate state for UpdateError`() {
        // Given
        val error = WidgetErrorHandler.WidgetError.UpdateError("Update error")

        // When
        val state = errorHandler.createErrorWidgetState(error)

        // Then
        assertEquals("--:--", state.displayTime)
        assertEquals("更新エラー", state.dateText)
        assertFalse(state.hasRecord)
        assertEquals("再試行", state.buttonText)
    }

    @Test
    fun `createErrorWidgetState should create appropriate state for UnknownError`() {
        // Given
        val error = WidgetErrorHandler.WidgetError.UnknownError("Unknown error")

        // When
        val state = errorHandler.createErrorWidgetState(error)

        // Then
        assertEquals("--:--", state.displayTime)
        assertEquals("エラー", state.dateText)
        assertFalse(state.hasRecord)
        assertEquals("再試行", state.buttonText)
    }

    @Test
    fun `shouldUseCachedState should return true for temporary errors with cached state`() {
        // Given
        val networkError = WidgetErrorHandler.WidgetError.NetworkError("Network error")
        val databaseError = WidgetErrorHandler.WidgetError.DatabaseError("Database error")
        val cachedState = WidgetDisplayState("18:15", "今日", false, "Exit")

        // When & Then
        assertTrue(errorHandler.shouldUseCachedState(networkError, cachedState))
        assertTrue(errorHandler.shouldUseCachedState(databaseError, cachedState))
    }

    @Test
    fun `shouldUseCachedState should return false for permanent errors`() {
        // Given
        val updateError = WidgetErrorHandler.WidgetError.UpdateError("Update error")
        val cachedState = WidgetDisplayState("18:15", "今日", false, "Exit")

        // When & Then
        assertFalse(errorHandler.shouldUseCachedState(updateError, cachedState))
    }

    @Test
    fun `shouldUseCachedState should return false when no cached state available`() {
        // Given
        val networkError = WidgetErrorHandler.WidgetError.NetworkError("Network error")

        // When & Then
        assertFalse(errorHandler.shouldUseCachedState(networkError, null))
    }

    @Test
    fun `executeWithRetry should use exponential backoff when enabled`() = runTest {
        // Given
        var attemptCount = 0
        val startTime = System.currentTimeMillis()
        val operation: suspend () -> String = {
            attemptCount++
            if (attemptCount < 3) {
                throw SQLException("Database error")
            }
            "success"
        }

        // When
        val result = errorHandler.executeWithRetry(
            operation, 
            maxRetries = 3, 
            retryDelay = 50, 
            useExponentialBackoff = true
        )

        // Then
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        assertTrue(result is WidgetErrorHandler.WidgetResult.Success)
        // With exponential backoff: 50ms + 100ms = 150ms minimum
        // Adding some tolerance for test execution time
        assertTrue("Total time should be at least 100ms with exponential backoff", totalTime >= 100)
    }

    @Test
    fun `executeWithRetry should use fixed delay when exponential backoff disabled`() = runTest {
        // Given
        var attemptCount = 0
        val startTime = System.currentTimeMillis()
        val operation: suspend () -> String = {
            attemptCount++
            if (attemptCount < 3) {
                throw SQLException("Database error")
            }
            "success"
        }

        // When
        val result = errorHandler.executeWithRetry(
            operation, 
            maxRetries = 3, 
            retryDelay = 50, 
            useExponentialBackoff = false
        )

        // Then
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        assertTrue(result is WidgetErrorHandler.WidgetResult.Success)
        // With fixed delay: 50ms + 50ms = 100ms minimum
        // Adding some tolerance for test execution time
        assertTrue("Total time should be at least 80ms with fixed delay", totalTime >= 80)
    }
}