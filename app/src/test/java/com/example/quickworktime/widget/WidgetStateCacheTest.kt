package com.example.quickworktime.widget

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for WidgetStateCache
 */
class WidgetStateCacheTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var stateCache: WidgetStateCache

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences behavior
        `when`(mockContext.getSharedPreferences("widget_state_cache", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        
        stateCache = WidgetStateCache(mockContext)
    }

    @Test
    fun `saveLastGoodState should save state to SharedPreferences`() = runTest {
        // Given
        val state = WidgetDisplayState(
            displayTime = "18:15",
            dateText = "今日",
            hasRecord = false,
            buttonText = "Exit"
        )

        // When
        stateCache.saveLastGoodState(state)

        // Then
        verify(mockEditor).putString(eq("last_good_state"), anyString())
        verify(mockEditor).putLong(eq("last_update_time"), anyLong())
        verify(mockEditor).apply()
    }

    @Test
    fun `getLastGoodState should return null when no cached state exists`() = runTest {
        // Given
        `when`(mockSharedPreferences.getString("last_good_state", null)).thenReturn(null)

        // When
        val result = stateCache.getLastGoodState()

        // Then
        assertNull(result)
    }

    @Test
    fun `getLastGoodState should return null when cache is expired`() = runTest {
        // Given
        val oldTimestamp = System.currentTimeMillis() - (10 * 60 * 1000L) // 10 minutes ago
        val jsonString = """{"displayTime":"18:15","dateText":"今日","hasRecord":false,"buttonText":"Exit"}"""
        
        `when`(mockSharedPreferences.getString("last_good_state", null)).thenReturn(jsonString)
        `when`(mockSharedPreferences.getLong("last_update_time", 0)).thenReturn(oldTimestamp)

        // When
        val result = stateCache.getLastGoodState()

        // Then
        assertNull(result)
    }

    @Test
    fun `getLastGoodState should return cached state when valid`() = runTest {
        // Given
        val recentTimestamp = System.currentTimeMillis() - (2 * 60 * 1000L) // 2 minutes ago
        val jsonString = """{"displayTime":"18:15","dateText":"今日","hasRecord":false,"buttonText":"Exit"}"""
        
        `when`(mockSharedPreferences.getString("last_good_state", null)).thenReturn(jsonString)
        `when`(mockSharedPreferences.getLong("last_update_time", 0)).thenReturn(recentTimestamp)

        // When
        val result = stateCache.getLastGoodState()

        // Then
        assertNotNull(result)
        assertEquals("18:15", result?.displayTime)
        assertEquals("今日", result?.dateText)
        assertFalse(result?.hasRecord ?: true)
        assertEquals("Exit", result?.buttonText)
    }

    @Test
    fun `getLastGoodState should return null when JSON is corrupted`() = runTest {
        // Given
        val recentTimestamp = System.currentTimeMillis() - (2 * 60 * 1000L)
        val corruptedJson = """{"displayTime":"18:15","dateText":"""
        
        `when`(mockSharedPreferences.getString("last_good_state", null)).thenReturn(corruptedJson)
        `when`(mockSharedPreferences.getLong("last_update_time", 0)).thenReturn(recentTimestamp)

        // When
        val result = stateCache.getLastGoodState()

        // Then
        assertNull(result)
    }

    @Test
    fun `clearCache should remove cached data`() = runTest {
        // When
        stateCache.clearCache()

        // Then
        verify(mockEditor).remove("last_good_state")
        verify(mockEditor).remove("last_update_time")
        verify(mockEditor).apply()
    }

    @Test
    fun `hasCachedState should return true when valid cache exists`() = runTest {
        // Given
        val recentTimestamp = System.currentTimeMillis() - (2 * 60 * 1000L)
        val jsonString = """{"displayTime":"18:15","dateText":"今日","hasRecord":false,"buttonText":"Exit"}"""
        
        `when`(mockSharedPreferences.getString("last_good_state", null)).thenReturn(jsonString)
        `when`(mockSharedPreferences.getLong("last_update_time", 0)).thenReturn(recentTimestamp)

        // When
        val result = stateCache.hasCachedState()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasCachedState should return false when no cache exists`() = runTest {
        // Given
        `when`(mockSharedPreferences.getString("last_good_state", null)).thenReturn(null)

        // When
        val result = stateCache.hasCachedState()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasCachedState should return false when cache is expired`() = runTest {
        // Given
        val oldTimestamp = System.currentTimeMillis() - (10 * 60 * 1000L)
        val jsonString = """{"displayTime":"18:15","dateText":"今日","hasRecord":false,"buttonText":"Exit"}"""
        
        `when`(mockSharedPreferences.getString("last_good_state", null)).thenReturn(jsonString)
        `when`(mockSharedPreferences.getLong("last_update_time", 0)).thenReturn(oldTimestamp)

        // When
        val result = stateCache.hasCachedState()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getCacheAge should return correct age`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis() - 120000L // 2 minutes ago
        `when`(mockSharedPreferences.getLong("last_update_time", 0)).thenReturn(timestamp)

        // When
        val age = stateCache.getCacheAge()

        // Then
        assertTrue("Cache age should be approximately 2 minutes", age >= 115000L && age <= 125000L)
    }

    @Test
    fun `getCacheAge should return MAX_VALUE when no timestamp exists`() = runTest {
        // Given
        `when`(mockSharedPreferences.getLong("last_update_time", 0)).thenReturn(0L)

        // When
        val age = stateCache.getCacheAge()

        // Then
        assertEquals(Long.MAX_VALUE, age)
    }

    @Test
    fun `saveLastGoodState should handle exceptions gracefully`() = runTest {
        // Given
        val state = WidgetDisplayState("18:15", "今日", false, "Exit")
        `when`(mockEditor.putString(anyString(), anyString())).thenThrow(RuntimeException("Storage error"))

        // When & Then - should not throw exception
        assertDoesNotThrow {
            runTest {
                stateCache.saveLastGoodState(state)
            }
        }
    }

    @Test
    fun `clearCache should handle exceptions gracefully`() = runTest {
        // Given
        `when`(mockEditor.remove(anyString())).thenThrow(RuntimeException("Storage error"))

        // When & Then - should not throw exception
        assertDoesNotThrow {
            runTest {
                stateCache.clearCache()
            }
        }
    }

    private fun assertDoesNotThrow(executable: () -> Unit) {
        try {
            executable()
        } catch (e: Exception) {
            fail("Expected no exception to be thrown, but got: ${e.message}")
        }
    }
}