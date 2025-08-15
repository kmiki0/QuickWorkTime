package com.example.quickworktime.widget

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class TimeAdjustmentHandlerTest {
    
    private lateinit var timeAdjustmentHandler: TimeAdjustmentHandler
    
    @Before
    fun setUp() {
        timeAdjustmentHandler = TimeAdjustmentHandler()
    }
    
    // Requirement 5.1: 分部分を上にフリック → 5分増加
    @Test
    fun `handleFlickGesture should increase minutes by 5 when flicking up on minute component`() {
        val baseTime = "18:15"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.UP, 
            TimeComponent.MINUTE
        )
        assertEquals("18:20", result)
    }
    
    // Requirement 5.2: 分部分を下にフリック → 5分減少
    @Test
    fun `handleFlickGesture should decrease minutes by 5 when flicking down on minute component`() {
        val baseTime = "18:20"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.DOWN, 
            TimeComponent.MINUTE
        )
        assertEquals("18:15", result)
    }
    
    // Requirement 5.3: 時部分を上にフリック → 1時間増加
    @Test
    fun `handleFlickGesture should increase hour by 1 when flicking up on hour component`() {
        val baseTime = "18:15"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.UP, 
            TimeComponent.HOUR
        )
        assertEquals("19:15", result)
    }
    
    // Requirement 5.4: 時部分を下にフリック → 1時間減少
    @Test
    fun `handleFlickGesture should decrease hour by 1 when flicking down on hour component`() {
        val baseTime = "18:15"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.DOWN, 
            TimeComponent.HOUR
        )
        assertEquals("17:15", result)
    }
    
    @Test
    fun `handleFlickGesture should handle minute overflow correctly`() {
        val baseTime = "18:55"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.UP, 
            TimeComponent.MINUTE
        )
        assertEquals("19:00", result)
    }
    
    @Test
    fun `handleFlickGesture should handle minute underflow correctly`() {
        val baseTime = "18:00"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.DOWN, 
            TimeComponent.MINUTE
        )
        assertEquals("17:55", result)
    }
    
    @Test
    fun `handleFlickGesture should handle hour overflow correctly`() {
        val baseTime = "23:30"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.UP, 
            TimeComponent.HOUR
        )
        assertEquals("00:30", result)
    }
    
    @Test
    fun `handleFlickGesture should handle hour underflow correctly`() {
        val baseTime = "00:30"
        val result = timeAdjustmentHandler.handleFlickGesture(
            baseTime, 
            FlickDirection.DOWN, 
            TimeComponent.HOUR
        )
        assertEquals("23:30", result)
    }
    
    @Test
    fun `handleFlickGesture should return original time on invalid input`() {
        val invalidTime = "invalid"
        val result = timeAdjustmentHandler.handleFlickGesture(
            invalidTime, 
            FlickDirection.UP, 
            TimeComponent.MINUTE
        )
        assertEquals("invalid", result)
    }
    
    @Test
    fun `calculateRoundedTime with LocalTime should round down to 5 minute intervals`() {
        // Test cases from requirements: 18:17 → 18:15, 18:24 → 18:20
        val time1 = LocalTime.of(18, 17)
        val result1 = timeAdjustmentHandler.calculateRoundedTime(time1)
        assertEquals("18:15", result1)
        
        val time2 = LocalTime.of(18, 24)
        val result2 = timeAdjustmentHandler.calculateRoundedTime(time2)
        assertEquals("18:20", result2)
        
        // Additional test cases
        val time3 = LocalTime.of(9, 0)
        val result3 = timeAdjustmentHandler.calculateRoundedTime(time3)
        assertEquals("09:00", result3)
        
        val time4 = LocalTime.of(14, 59)
        val result4 = timeAdjustmentHandler.calculateRoundedTime(time4)
        assertEquals("14:55", result4)
        
        val time5 = LocalTime.of(12, 33)
        val result5 = timeAdjustmentHandler.calculateRoundedTime(time5)
        assertEquals("12:30", result5)
    }
    
    @Test
    fun `calculateRoundedTime with String should round down to 5 minute intervals`() {
        val result1 = timeAdjustmentHandler.calculateRoundedTime("18:17")
        assertEquals("18:15", result1)
        
        val result2 = timeAdjustmentHandler.calculateRoundedTime("18:24")
        assertEquals("18:20", result2)
        
        val result3 = timeAdjustmentHandler.calculateRoundedTime("09:00")
        assertEquals("09:00", result3)
    }
    
    @Test
    fun `calculateRoundedTime should return original string on invalid input`() {
        val invalidTime = "invalid"
        val result = timeAdjustmentHandler.calculateRoundedTime(invalidTime)
        assertEquals("invalid", result)
    }
    
    @Test
    fun `multiple flick gestures should work correctly`() {
        var currentTime = "18:15"
        
        // 分を2回上にフリック: 18:15 → 18:20 → 18:25
        currentTime = timeAdjustmentHandler.handleFlickGesture(
            currentTime, FlickDirection.UP, TimeComponent.MINUTE
        )
        assertEquals("18:20", currentTime)
        
        currentTime = timeAdjustmentHandler.handleFlickGesture(
            currentTime, FlickDirection.UP, TimeComponent.MINUTE
        )
        assertEquals("18:25", currentTime)
        
        // 時を1回上にフリック: 18:25 → 19:25
        currentTime = timeAdjustmentHandler.handleFlickGesture(
            currentTime, FlickDirection.UP, TimeComponent.HOUR
        )
        assertEquals("19:25", currentTime)
        
        // 分を1回下にフリック: 19:25 → 19:20
        currentTime = timeAdjustmentHandler.handleFlickGesture(
            currentTime, FlickDirection.DOWN, TimeComponent.MINUTE
        )
        assertEquals("19:20", currentTime)
    }
}