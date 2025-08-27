package com.example.quickworktime.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.util.Log
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
 * Now includes comprehensive error handling and retry mechanisms
 */
class WidgetRepository(private val workInfoDao: WorkInfoDao) {
    
    companion object {
        private const val TAG = "WidgetRepository"
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        /**
         * WidgetRepositoryインスタンスを作成
         */
        fun create(context: Context): WidgetRepository {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = WidgetRepository(database.workInfo())
                repository.initialize(context) // 自動的に初期化
                return repository
            } catch (e: Exception) {
                Log.e(TAG, "WidgetRepository作成エラー", e)
                throw e
            }
        }
    }
    
    private val errorHandler = WidgetErrorHandler()
    private lateinit var stateCache: WidgetStateCache

    /**
     * コンテキスト依存コンポーネントでリポジトリを初期化
     * より良いエラーハンドリングで強化
     */
    fun initialize(context: Context) {
        try {
            if (!::stateCache.isInitialized) {
                stateCache = WidgetStateCache(context)
                Log.d(TAG, "WidgetRepository初期化成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "WidgetRepository初期化エラー", e)
            // フォールバック用ステートキャッシュを作成
            try {
                stateCache = WidgetStateCache(context)
            } catch (fallbackException: Exception) {
                Log.e(TAG, "フォールバックステートキャッシュ作成失敗", fallbackException)
            }
        }
    }

    /**
     * Gets today's work information from the database with error handling
     * 
     * @return WidgetResult containing WorkInfo for today if exists, or error information
     */
    suspend fun getTodayWorkInfo(): WidgetErrorHandler.WidgetResult<WorkInfo?> = withContext(Dispatchers.IO) {
        errorHandler.executeWithRetry(
            operation = {
                val today = LocalDate.now().format(dateFormatter)
                workInfoDao.getWorkInfoByDate(today)
            }
        )
    }
    
    /**
     * Gets today's work information with fallback to null (for backward compatibility)
     * 
     * @return WorkInfo for today if exists, null otherwise
     */
    suspend fun getTodayWorkInfoOrNull(): WorkInfo? = withContext(Dispatchers.IO) {
        when (val result = getTodayWorkInfo()) {
            is WidgetErrorHandler.WidgetResult.Success -> result.data
            is WidgetErrorHandler.WidgetResult.Error -> {
                Log.w(TAG, "Failed to get today's work info: ${result.error.message}")
                null
            }
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
     * Records clock-out time for today with comprehensive error handling
     * Creates new WorkInfo if doesn't exist, updates existing one if it does
     * 
     * @param time Clock-out time in "HH:mm" format
     * @return WidgetResult indicating success or failure
     */
    suspend fun recordClockOut(time: String): WidgetErrorHandler.WidgetResult<Unit> = withContext(Dispatchers.IO) {
        errorHandler.executeWithRetry(
            operation = {
                val today = LocalDate.now().format(dateFormatter)
                
                // Try to get existing work info, create new if not found
                val existingWorkInfo = try {
                    workInfoDao.getWorkInfoByDate(today)
                } catch (e: Exception) {
                    null
                }
                
                if (existingWorkInfo != null) {
                    // Update existing record with new end time
                    val updatedWorkInfo = existingWorkInfo.copy(endTime = time)
                    workInfoDao.updateWorkInfo(updatedWorkInfo)
                } else {
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
            }
        )
    }
    
    /**
     * Triggers widget update for all instances with error handling and retry
     * 
     * @param context Application context
     * @return WidgetResult indicating success or failure
     */
    suspend fun updateWidget(context: Context): WidgetErrorHandler.WidgetResult<Unit> = withContext(Dispatchers.Main) {
        if (!::stateCache.isInitialized) {
            initialize(context)
        }
        
        errorHandler.executeWithRetry(
            operation = {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (appWidgetIds.isNotEmpty()) {
                    val widgetProvider = WorkTimeWidgetProvider()
                    widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds)
                }
            },
            maxRetries = 2 // Fewer retries for UI updates
        )
    }
    
    /**
     * Checks if today has any work record with error handling
     * 
     * @return true if today has work record, false otherwise or on error
     */
    suspend fun hasTodayRecord(): Boolean = withContext(Dispatchers.IO) {
        when (val result = getTodayWorkInfo()) {
            is WidgetErrorHandler.WidgetResult.Success -> result.data != null
            is WidgetErrorHandler.WidgetResult.Error -> {
                Log.w(TAG, "Failed to check today's record: ${result.error.message}")
                false
            }
        }
    }
    
    /**
     * Gets today's end time if exists with error handling
     * 
     * @return End time string if exists, null otherwise or on error
     */
    suspend fun getTodayEndTime(): String? = withContext(Dispatchers.IO) {
        when (val result = getTodayWorkInfo()) {
            is WidgetErrorHandler.WidgetResult.Success -> result.data?.endTime
            is WidgetErrorHandler.WidgetResult.Error -> {
                Log.w(TAG, "Failed to get today's end time: ${result.error.message}")
                null
            }
        }
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
     * Forces widget update when theme changes with error handling
     * Should be called when system theme changes are detected
     * 
     * @param context Application context
     * @return WidgetResult indicating success or failure
     */
    suspend fun updateWidgetTheme(context: Context): WidgetErrorHandler.WidgetResult<Unit> = withContext(Dispatchers.Main) {
        if (!::stateCache.isInitialized) {
            initialize(context)
        }
        
        errorHandler.executeWithRetry(
            operation = {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WorkTimeWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (appWidgetIds.isNotEmpty()) {
                    // Force update all widgets to apply new theme
                    val widgetProvider = WorkTimeWidgetProvider()
                    widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds)
                }
            },
            maxRetries = 1 // Single retry for theme updates
        )
    }
    
    /**
     * Gets widget display state with comprehensive error handling and caching
     * 
     * @param context Application context
     * @return WidgetResult containing display state or error information
     */
    suspend fun getWidgetDisplayState(context: Context): WidgetErrorHandler.WidgetResult<WidgetDisplayState> = withContext(Dispatchers.IO) {
        if (!::stateCache.isInitialized) {
            initialize(context)
        }

        val result = errorHandler.executeWithRetry(
            operation = {
                val todayWorkInfo = getTodayWorkInfoOrNull()
                val dateFormatter = DateTimeFormatter.ofPattern("M/d")
                val todayText = LocalDate.now().format(dateFormatter)

                // 現在時刻と計算結果をログ出力
                val currentTime = java.time.LocalTime.now()
                val optimalTime = TimeCalculationUtils.getCurrentRoundedTime()
                Log.i("WidgetDebug", "現在時刻: $currentTime -> 計算結果: $optimalTime")

                if (todayWorkInfo?.endTime != null) {
                    // Has record state - show recorded end time
                    Log.i("WidgetDebug", "記録済み状態: endTime=${todayWorkInfo.endTime}")
                    WidgetDisplayState(
                        displayTime = todayWorkInfo.endTime,
                        dateText = todayText,
                        hasRecord = true,
                        buttonText = "記録済み"
                    )
                } else {
                    // No record state - show calculated optimal time
                    Log.i("WidgetDebug", "未記録状態: 計算時間=$optimalTime")
                    WidgetDisplayState(
                        displayTime = optimalTime,
                        dateText = todayText,
                        hasRecord = false,
                        buttonText = "Exit"
                    )
                }
            }
        )

        // 結果をログ出力
        when (result) {
            is WidgetErrorHandler.WidgetResult.Success -> {
                Log.i("WidgetDebug", "getWidgetDisplayState成功: ${result.data}")
                stateCache.saveLastGoodState(result.data)
                result
            }
            is WidgetErrorHandler.WidgetResult.Error -> {
                Log.e("WidgetDebug", "getWidgetDisplayState失敗: ${result.error.message}")
                val cachedState = stateCache.getLastGoodState()
                if (errorHandler.shouldUseCachedState(result.error, cachedState)) {
                    Log.i("WidgetDebug", "キャッシュ状態使用: $cachedState")
                    WidgetErrorHandler.WidgetResult.Success(cachedState!!)
                } else {
                    result
                }
            }
        }
    }
}