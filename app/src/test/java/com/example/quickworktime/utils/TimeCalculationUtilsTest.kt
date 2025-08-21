package com.example.quickworktime.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalTime

/**
 * Unit tests for TimeCalculationUtils
 * 
 * Tests the time calculation logic according to requirements:
 * - 2.3: WHEN 現在時刻が18:17の場合 THEN ウィジェットは18:15を表示する
 * - 2.4: WHEN 現在時刻が18:24の場合 THEN ウィジェットは18:20を表示する  
 * - 2.5: WHEN 時間計算を行う場合 THEN システムは現在時刻を5分単位で切り下げて表示する
 */
class TimeCalculationUtilsTest {

    @Test
    fun `roundDownToFiveMinutes should round down 18_17 to 18_15`() {
        // Requirement 2.3: WHEN 現在時刻が18:17の場合 THEN ウィジェットは18:15を表示する
        val input = LocalTime.of(18, 17)
        val expected = LocalTime.of(18, 15)
        val result = TimeCalculationUtils.roundDownToFiveMinutes(input)
        assertEquals(expected, result)
    }

    @Test
    fun `roundDownToFiveMinutes should round down 18_24 to 18_20`() {
        // Requirement 2.4: WHEN 現在時刻が18:24の場合 THEN ウィジェットは18:20を表示する
        val input = LocalTime.of(18, 24)
        val expected = LocalTime.of(18, 20)
        val result = TimeCalculationUtils.roundDownToFiveMinutes(input)
        assertEquals(expected, result)
    }

    @Test
    fun `roundDownToFiveMinutes should handle exact 5 minute intervals`() {
        val input = LocalTime.of(18, 20)
        val expected = LocalTime.of(18, 20)
        val result = TimeCalculationUtils.roundDownToFiveMinutes(input)
        assertEquals(expected, result)
    }

    @Test
    fun `roundDownToFiveMinutes should round down to nearest 5 minute interval`() {
        // Test various cases for 5-minute rounding down
        val testCases = mapOf(
            LocalTime.of(9, 0) to LocalTime.of(9, 0),   // 0 minutes -> 0
            LocalTime.of(9, 1) to LocalTime.of(9, 0),   // 1 minute -> 0
            LocalTime.of(9, 4) to LocalTime.of(9, 0),   // 4 minutes -> 0
            LocalTime.of(9, 5) to LocalTime.of(9, 5),   // 5 minutes -> 5
            LocalTime.of(9, 6) to LocalTime.of(9, 5),   // 6 minutes -> 5
            LocalTime.of(9, 9) to LocalTime.of(9, 5),   // 9 minutes -> 5
            LocalTime.of(9, 10) to LocalTime.of(9, 10), // 10 minutes -> 10
            LocalTime.of(9, 14) to LocalTime.of(9, 10), // 14 minutes -> 10
            LocalTime.of(9, 15) to LocalTime.of(9, 15), // 15 minutes -> 15
            LocalTime.of(9, 19) to LocalTime.of(9, 15), // 19 minutes -> 15
            LocalTime.of(9, 59) to LocalTime.of(9, 55)  // 59 minutes -> 55
        )

        testCases.forEach { (input, expected) ->
            val result = TimeCalculationUtils.roundDownToFiveMinutes(input)
            assertEquals("Failed for input $input", expected, result)
        }
    }

    @Test
    fun `roundDownToFiveMinutes should clear seconds and nanoseconds`() {
        val input = LocalTime.of(18, 17, 30, 500000000) // 18:17:30.5
        val result = TimeCalculationUtils.roundDownToFiveMinutes(input)
        assertEquals(0, result.second)
        assertEquals(0, result.nano)
        assertEquals(LocalTime.of(18, 15), result)
    }

    @Test
    fun `roundDownToFiveMinutesString should return formatted string`() {
        val input = LocalTime.of(18, 17)
        val expected = "18:15"
        val result = TimeCalculationUtils.roundDownToFiveMinutesString(input)
        assertEquals(expected, result)
    }

    @Test
    fun `roundDownToFiveMinutesString should handle single digit hours and minutes`() {
        val input = LocalTime.of(9, 7)
        val expected = "09:05"
        val result = TimeCalculationUtils.roundDownToFiveMinutesString(input)
        assertEquals(expected, result)
    }

    @Test
    fun `adjustTime should add hours correctly`() {
        val baseTime = "18:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, 1, 0)
        assertEquals("19:15", result)
    }

    @Test
    fun `adjustTime should subtract hours correctly`() {
        val baseTime = "18:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, -1, 0)
        assertEquals("17:15", result)
    }

    @Test
    fun `adjustTime should add minutes correctly`() {
        val baseTime = "18:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, 0, 5)
        assertEquals("18:20", result)
    }

    @Test
    fun `adjustTime should subtract minutes correctly`() {
        val baseTime = "18:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, 0, -5)
        assertEquals("18:10", result)
    }

    @Test
    fun `adjustTime should handle combined hour and minute adjustments`() {
        val baseTime = "18:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, 1, 5)
        assertEquals("19:20", result)
    }

    @Test
    fun `adjustTime should handle negative adjustments`() {
        val baseTime = "18:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, -1, -5)
        assertEquals("17:10", result)
    }

    @Test
    fun `adjustTime should handle hour overflow`() {
        val baseTime = "23:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, 2, 0)
        assertEquals("01:15", result)
    }

    @Test
    fun `adjustTime should handle minute overflow`() {
        val baseTime = "18:55"
        val result = TimeCalculationUtils.adjustTime(baseTime, 0, 10)
        assertEquals("19:05", result)
    }

    @Test
    fun `adjustTime should handle hour underflow`() {
        val baseTime = "01:15"
        val result = TimeCalculationUtils.adjustTime(baseTime, -2, 0)
        assertEquals("23:15", result)
    }

    @Test
    fun `adjustTime should handle minute underflow`() {
        val baseTime = "18:05"
        val result = TimeCalculationUtils.adjustTime(baseTime, 0, -10)
        assertEquals("17:55", result)
    }

    @Test
    fun `getCurrentRoundedTime should return properly formatted string`() {
        // This test verifies the method runs without error and returns a properly formatted string
        val result = TimeCalculationUtils.getCurrentRoundedTime()
        
        // Verify format is HH:mm
        assertTrue("Result should match HH:mm format", result.matches(Regex("\\d{2}:\\d{2}")))
        
        // Verify the minutes are in 5-minute intervals
        val minutes = result.substring(3, 5).toInt()
        assertEquals("Minutes should be divisible by 5", 0, minutes % 5)
    }
}