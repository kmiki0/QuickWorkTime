package com.example.quickworktime.widget

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.example.quickworktime.R

/**
 * Utility class for managing widget theme consistency
 * Provides theme-aware color and resource management for widgets
 */
object WidgetThemeManager {
    
    /**
     * Checks if the system is currently in dark mode
     * 
     * @param context Application context
     * @return true if dark mode is active, false otherwise
     */
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
    
    /**
     * Gets the appropriate primary text color for the current theme
     * 
     * @param context Application context
     * @return Color resource ID for primary text
     */
    fun getPrimaryTextColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.widget_text_primary)
    }
    
    /**
     * Gets the appropriate secondary text color for the current theme
     * 
     * @param context Application context
     * @return Color resource ID for secondary text
     */
    fun getSecondaryTextColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.widget_text_secondary)
    }
    
    /**
     * Gets the appropriate accent color for the current theme
     * 
     * @param context Application context
     * @return Color resource ID for accent color
     */
    fun getAccentColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.widget_accent)
    }
    
    /**
     * Gets the appropriate surface color for the current theme
     * 
     * @param context Application context
     * @return Color resource ID for surface color
     */
    fun getSurfaceColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.widget_surface)
    }
    
    /**
     * Determines if the widget should use light or dark theme resources
     * This can be used for programmatic theme switching if needed
     * 
     * @param context Application context
     * @return Theme identifier string ("light" or "dark")
     */
    fun getCurrentTheme(context: Context): String {
        return if (isDarkMode(context)) "dark" else "light"
    }
    
    /**
     * Gets theme-appropriate alpha value for disabled elements
     * 
     * @param context Application context
     * @return Alpha value (0-255) for disabled elements
     */
    fun getDisabledAlpha(context: Context): Int {
        return if (isDarkMode(context)) 100 else 128
    }
    
    /**
     * Gets theme-appropriate alpha value for enabled elements
     * 
     * @param context Application context
     * @return Alpha value (0-255) for enabled elements
     */
    fun getEnabledAlpha(context: Context): Int {
        return 255
    }
}