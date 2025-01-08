package com.example.quickworktime.room.repository

import android.util.Log
import com.example.quickworktime.room.WorkSetting
import com.example.quickworktime.room.WorkSettingDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkSettingRepository(private val dao: WorkSettingDao) {

	/** ============================================
	 *  データ を登録
	 *  @param workSetting WorkSetting
	 *  ============================================ */
	suspend fun insertWorkSetting(workSetting: WorkSetting) {
		 try {
			dao.insertWorkSetting(workSetting)
		} catch (e: Exception) {
			Log.d("DebugLog", "Error: ${e.message}")
		}
	}

	/** ============================================
	 *  データ 1件を取得
	 *  @param workSetting WorkInfo
	 *  ============================================ */
	 suspend fun getWorkSetting(): WorkSetting? {
		 return try {
				dao.getWorkSetting()
		} catch (e: Exception) {
			Log.d("DebugLog", "Error: ${e.message}")
			 null
		}
	}
}