package com.example.quickworktime.widget

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * ウィジェットの表示状態をキャッシュするクラス
 *  キャッシュはSharedPreferencesに保存され、一定時間内であれば再利用される
 *  キャッシュの有効期限は 5分
 */
class WidgetStateCache(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "widget_state_cache"
        private const val KEY_LAST_GOOD_STATE = "last_good_state"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 min
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * キャッシュにウィジェットの状態を保存
     */
    suspend fun saveLastGoodState(state: WidgetDisplayState) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("displayTime", state.displayTime)
                put("dateText", state.dateText)
                put("hasRecord", state.hasRecord)
                put("buttonText", state.buttonText)
            }
            
            prefs.edit()
                .putString(KEY_LAST_GOOD_STATE, json.toString())
                .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // Ignore cache save errors - they shouldn't affect widget functionality
        }
    }
    
    /**
     * キャッシュからウィジェットの状態を取得
     * 
     * @return 有効なキャッシュが存在する場合はWidgetDisplayState、存在しないか無効な場合はnull
     */
    suspend fun getLastGoodState(): WidgetDisplayState? = withContext(Dispatchers.IO) {
        try {
            val jsonString = prefs.getString(KEY_LAST_GOOD_STATE, null) ?: return@withContext null
            val lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0)
            
            // Check if cache is still valid
            if (System.currentTimeMillis() - lastUpdateTime > CACHE_VALIDITY_MS) {
                return@withContext null
            }
            
            val json = JSONObject(jsonString)
            WidgetDisplayState(
                displayTime = json.getString("displayTime"),
                dateText = json.getString("dateText"),
                hasRecord = json.getBoolean("hasRecord"),
                buttonText = json.getString("buttonText")
            )
        } catch (e: Exception) {
            // Return null if cache is corrupted or invalid
            null
        }
    }
    
    /**
     * キャッシュをクリア
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            prefs.edit()
                .remove(KEY_LAST_GOOD_STATE)
                .remove(KEY_LAST_UPDATE_TIME)
                .apply()
        } catch (e: Exception) {
            // Ignore cache clear errors
        }
    }
    
    /**
     * キャッシュが存在するかどうか
     */
    suspend fun hasCachedState(): Boolean = withContext(Dispatchers.IO) {
        getLastGoodState() != null
    }
    
    /**
     * キャッシュの経過時間を取得（ミリ秒）
     */
    suspend fun getCacheAge(): Long = withContext(Dispatchers.IO) {
        val lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0)
        if (lastUpdateTime == 0L) return@withContext Long.MAX_VALUE
        System.currentTimeMillis() - lastUpdateTime
    }
}