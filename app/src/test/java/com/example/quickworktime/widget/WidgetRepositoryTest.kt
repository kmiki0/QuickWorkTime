package com.example.quickworktime.widget

import com.example.quickworktime.utils.TimeCalculationUtils
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class WidgetRepositoryTest {
    
    @Test
    fun `calculateOptimalClockOutTime returns rounded time format`() {
        // This test verifies that the time calculation utility works correctly
        // Since we can't easily mock the current time, we test the underlying utility
        val testTime = LocalTime.of(18, 17)
        val result = TimeCalculationUtils.roundDownToFiveMinutesString(testTime)
        
        assertEquals("18:15", result)
    }
    
    @Test
    fun `adjustTime correctly adjusts time with positive deltas`() {
        val baseTime = "18:15"
        val hourDelta = 1
        val minuteDelta = 5
        
        val result = TimeCalculationUtils.adjustTime(baseTime, hourDelta, minuteDelta)
        
        assertEquals("19:20", result)
    }
    
    @Test
    fun `adjustTime correctly adjusts time with negative deltas`() {
        val baseTime = "18:15"
        val hourDelta = -1
        val minuteDelta = -5
        
        val result = TimeCalculationUtils.adjustTime(baseTime, hourDelta, minuteDelta)
        
        assertEquals("17:10", result)
    }
    
    @Test
    fun `adjustTime handles hour boundary correctly`() {
        val baseTime = "00:30"
        val hourDelta = -1
        val minuteDelta = 0
        
        val result = TimeCalculationUtils.adjustTime(baseTime, hourDelta, minuteDelta)
        
        assertEquals("23:30", result)
    }
    
    @Test
    fun `adjustTime handles minute boundary correctly`() {
        val baseTime = "18:05"
        val hourDelta = 0
        val minuteDelta = -10
        
        val result = TimeCalculationUtils.adjustTime(baseTime, hourDelta, minuteDelta)
        
        assertEquals("17:55", result)
    }
    
    @Test
    fun `roundDownToFiveMinutes works correctly for various times`() {
        // Test cases for 5-minute rounding
        val testCases = mapOf(
            LocalTime.of(18, 17) to "18:15",
            LocalTime.of(18, 24) to "18:20", 
            LocalTime.of(18, 20) to "18:20",
            LocalTime.of(18, 3) to "18:00",
            LocalTime.of(9, 59) to "09:55",
            LocalTime.of(0, 2) to "00:00"
        )
        
        testCases.forEach { (input, expected) ->
            val result = TimeCalculationUtils.roundDownToFiveMinutesString(input)
            assertEquals("Failed for input $input", expected, result)
        }
    }
    
    @Test
    fun `getCurrentRoundedTime returns valid time format`() {
        val result = TimeCalculationUtils.getCurrentRoundedTime()
        
        assertNotNull(result)
        assertTrue("Time should be in HH:mm format", result.matches(Regex("\\d{1,2}:\\d{2}")))
        
        // Verify the minutes are in 5-minute intervals
        val minutes = result.split(":")[1].toInt()
        assertEquals("Minutes should be divisible by 5", 0, minutes % 5)
    }
}