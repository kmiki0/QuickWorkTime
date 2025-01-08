package com.example.quickworktime.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WorkInfoDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertWorkInfo(workInfo: WorkInfo)

	@Update
	suspend fun updateWorkInfo(workInfo: WorkInfo)

	// work_infoでdateが一番上のデータを取得
	@Query("SELECT date FROM work_info ORDER BY date DESC LIMIT 1")
    suspend fun getLatestDate(): String

	@Query("SELECT * FROM work_info WHERE date = :date")
	suspend fun getWorkInfoByDate(date: String): WorkInfo

	@Query("SELECT * FROM work_info")
	fun getAllWorkInfo(): LiveData<List<WorkInfo>>

	// 日付をyyyyMM範囲で検索
	@Query("SELECT * FROM work_info WHERE date LIKE :date || '%' ORDER BY date ASC")
	suspend fun getMonthWorkInfo(date: String): List<WorkInfo>

	@Delete
	suspend fun deleteWorkInfo(workInfo: WorkInfo)
}

@Dao
interface WorkSettingDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertWorkSetting(workSetting: WorkSetting)

	// 1件のみ取得
	@Query("SELECT * FROM work_settings LIMIT 1")
	suspend fun getWorkSetting(): WorkSetting?
}

@Dao
interface HolidayInfoDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertHolidayInfo(holidayInfo: HolidayInfo)

	@Query("SELECT * FROM holiday_info WHERE date = :date")
	suspend fun getHolidayInfoByDate(date: String): HolidayInfo
}