package com.example.quickworktime.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetUpdateServiceTest {

    @Test
    fun testActionConstants() {
        // Test that action constants are properly defined
        assertEquals("com.example.quickworktime.widget.ACTION_RECORD_CLOCK_OUT", 
                    WidgetUpdateService.ACTION_RECORD_CLOCK_OUT)
        assertEquals("com.example.quickworktime.widget.ACTION_UPDATE_WIDGET", 
                    WidgetUpdateService.ACTION_UPDATE_WIDGET)
        assertEquals("clock_out_time", WidgetUpdateService.EXTRA_CLOCK_OUT_TIME)
    }

    @Test
    fun testServiceInstantiation() {
        // Test that service can be instantiated
        val service = WidgetUpdateService()
        assertNotNull(service)
    }

    @Test
    fun testServiceBinderReturnsNull() {
        // Test that onBind returns null (this is a started service, not bound service)
        val service = WidgetUpdateService()
        assertNull(service.onBind(null))
    }
}