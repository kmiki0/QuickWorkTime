package com.example.quickworktime.domain.usecase

import com.example.quickworktime.room.WorkInfo

/**
 * 勤務時間計算パラメータ
 */
data class CalculateWorkTimeParams(
	val startTime: String,  // HH:mm形式
	val endTime: String,    // HH:mm形式
	val breakTime: String   // HH:mm形式
)

/**
 * 休憩時間計算パラメータ
 */
data class CalculateBreakTimeParams(
	val endTime: String     // HH:mm形式
)

/**
 * 勤務時間記録パラメータ
 */
data class RecordWorkTimeParams(
	val date: String,       // yyyyMMdd形式
	val startTime: String,  // HH:mm形式
	val endTime: String     // HH:mm形式
)

/**
 * ウィジェット用退勤記録パラメータ
 */
data class RecordClockOutParams(
	val clockOutTime: String,              // HH:mm形式 - 記録したい退勤時間
	val existingWorkInfo: WorkInfo? = null, // 既存の勤務情報（あれば）
	val defaultStartTime: String? = null    // デフォルト開始時間（新規作成時用）
)