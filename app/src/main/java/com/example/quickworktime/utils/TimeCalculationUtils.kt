package com.example.quickworktime.utils

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Utility class for time calculations used in the widget
 */
object TimeCalculationUtils {
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    /**
     * Rounds down the current time to the nearest 5-minute interval
     * 
     * Examples:
     * - 18:17 → 18:15
     * - 18:24 → 18:20
     * - 18:20 → 18:20 (already rounded)
     * - 18:03 → 18:00
     * 
     * @param time The time to round down
     * @return The rounded down time
     */
    fun roundDownToFiveMinutes(time: LocalTime): LocalTime {
        val minutes = time.minute
        val roundedMinutes = (minutes / 5) * 5
        return time.withMinute(roundedMinutes).withSecond(0).withNano(0)
    }
    
    /**
     * Rounds down the current time to the nearest 5-minute interval and returns as formatted string
     * 
     * @param time The time to round down
     * @return The rounded down time as "HH:mm" format string
     */
    fun roundDownToFiveMinutesString(time: LocalTime): String {
        return roundDownToFiveMinutes(time).format(timeFormatter)
    }
    
    /**
     * Gets the current time rounded down to the nearest 5-minute interval
     * 
     * @return The current time rounded down to 5-minute interval as "HH:mm" format string
     */
    fun getCurrentRoundedTime(): String {
        return roundDownToFiveMinutesString(LocalTime.now())
    }
    
    /**
     * Adjusts time by the specified hour and minute deltas
     * 
     * @param timeString The base time as "HH:mm" format string
     * @param hourDelta The number of hours to add (can be negative)
     * @param minuteDelta The number of minutes to add (can be negative)
     * @return The adjusted time as "HH:mm" format string
     */
    fun adjustTime(timeString: String, hourDelta: Int, minuteDelta: Int): String {
        val time = LocalTime.parse(timeString, timeFormatter)
        val adjustedTime = time.plusHours(hourDelta.toLong()).plusMinutes(minuteDelta.toLong())
        return adjustedTime.format(timeFormatter)
    }
}