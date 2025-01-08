package com.example.quickworktime.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_info")
data class WorkInfo(
	@PrimaryKey val date: String, // yyyyMMdd形式
	val startTime: String,        // 開始時間（例: HH:mm）
	val endTime: String,          // 終了時間（例: HH:mm）
	val workingTime: String,      // 勤務時間（例: "8:45"）
	val breakTime: String,        // 休憩時間（例: "1:00"）
	val isHoliday: Boolean,       // 休みフラグ
	val isNationalHoliday: Boolean, // 祝日フラグ
	val weekday: String           // 曜日（例: "Monday"）
)
