package com.example.quickworktime.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_settings")
data class WorkSetting(
	@PrimaryKey val id: Int = 0, // 自動生成される主キー
	val defaultStartTime: String,     // 開始時間の設定値（例: HH:mm）
	val defaultEndTime: String,       // 終了時間の設定値（例: HH:mm）
	val defaultBreakTime: String,     // 休憩時間の設定値（例: HH:mm）
	val workDays: String              // 出勤曜日の設定値（例: "0111110" → 月〜金が出勤）
)
