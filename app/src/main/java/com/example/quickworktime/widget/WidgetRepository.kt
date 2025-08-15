package com.example.quickworktime.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import com.example.quickworktime.room.AppDatabase
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.WorkInfoDao
import com.example.quickworktime.utils.TimeCalculationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository class for widget-related data operations
 * Handles database interactions and time calculations for the widget
 */
class WidgetRepository(private val workInfoDao: WorkInfoDao) {
    
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        
        /**
         * Creates a WidgetRepository instance with the database context
         */
        fun create(context: Context): WidgetRepository {
            val database = AppDatabase.getDatabase(context)
            return WidgetRepository(database.workInfo())
        }
    }
    
    /**
     * Gets today's work information from the database
     * 
     * @return WorkInfo for today if exists, null otherwise
     */
    suspend fun getTodayWorkInfo(): WorkInfo? = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(dateFormatter)
            workInfoDao.getWorkInfoByDate(today)
        } catch (e: Exception) {
            // Return null if no record found or any other database error occurs
            null
        }
    }
    
    /**
     * Calculates the optimal clock-out time based on current time
     * Rounds down current time to the nearest 5-minute interval
     * 
     * @return Formatted time string (HH:mm) representing optimal clock-out time
     */
    suspend fun calculateOptimalClockOutTime(): String = withContext(Dispatchers.Default) {
        TimeCalculationUtils.getCurrentRoundedTime()
    }
    
    /**
     * Adjusts the given time by specified hour and minute deltas
     * Used for flick gesture time adjustments
     * 
     * @param currentTime Base time in "HH:mm" format
     * @param hourDelta Hours to add/subtract (±1 for flick gestures)
     * @param minuteDelta Minutes to add/subtract (±5 for flick gestures)
     * @return Adjusted time in "HH:mm" format
     */
    suspend fun adjustTime(currentTime: String, hourDelta: Int, minuteDelta: Int): String = 
        withContext(Dispatchers.Default) {
            TimeCalculationUtils.adjustTime(currentTime, hourDelta, minuteDelta)
        }
    
    /**
     * Records clock-out time for today
     * Creates new WorkInfo if doesn't exist, updates existing one if it does
     * 
     * @param time Clock-out time in "HH:mm" format
     */
    suspend fun recordClockOut(time: String) = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(dateFormatter)
            
            // Try to get existing work info, create new if not found
            try {
                val existingWorkInfo = workInfoDao.getWorkInfoByDate(today)
                // Update existing record with new end time
                val updatedWorkInfo = existingWorkInfo.copy(endTime = time)
                workInfoDao.updateWorkInfo(updatedWorkInfo)
            } catch (e: Exception) {
                // No existing record found, create new one
                val newWorkInfo = WorkInfo(
                    date = today,
                    startTime = "09:00", // Default start time
                    endTime = time,
                    workingTime = "0:00", // Will be calculated later if needed
                    breakTime = "1:00",   // Default break time
                    isHoliday = false,
                    isNationalHoliday = false,
                    weekday = LocalDate.now().dayOfWeek.name
                )
                workInfoDao.insertWorkInfo(newWorkInfo)
            }
        } catch (e: Exception) {
            // Log error but don't throw to prevent widget crashes
            // In a production app, you might want to use proper logging
        }
    }
    
    /**
     * Triggers widget update for all instances
     * 
     * @param context Application context
     */
    suspend fun updateWidget(context: Context) = withContext(Dispatchers.Main) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val widgetProvider = WorkTimeWidgetProvider()
                widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds)
            }
        } catch (e: Exception) {
            // Log error but don't throw to prevent crashes
        }
    }
    
    /**
     * Checks if today has any work record
     * 
     * @return true if today has work record, false otherwise
     */
    suspend fun hasTodayRecord(): Boolean = withContext(Dispatchers.IO) {
        getTodayWorkInfo() != null
    }
    
    /**
     * Gets today's end time if exists
     * 
     * @return End time string if exists, null otherwise
     */
    suspend fun getTodayEndTime(): String? = withContext(Dispatchers.IO) {
        getTodayWorkInfo()?.endTime
    }
    
    /**
     * Notifies all widgets that data has been updated
     * Should be called after any work data modification
     * 
     * @param context Application context
     */
    suspend fun notifyDataUpdated(context: Context) = withContext(Dispatchers.Main) {
        WidgetUpdateManager.notifyDataUpdated(context)
    }
    
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
     * Forces widget update when theme changes
     * Should be called when system theme changes are detected
     * 
     * @param context Application context
     */
    suspend fun updateWidgetTheme(context: Context) = withContext(Dispatchers.Main) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                // Force update all widgets to apply new theme
                val widgetProvider = WorkTimeWidgetProvider()
                widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds)
            }
        } catch (e: Exception) {
            // Log error but don't throw to prevent crashes
        }
    }
}