package com.example.quickworktime.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holiday_info")
data class HolidayInfo(
	@PrimaryKey val date: String,  // yyyyMMdd形式（例: "20241225"）
	val name: String               // 祝日名（例: "クリスマス"）
)