package com.example.quickworktime.domain.usecase

/**
 * 勤務時間計算UseCase
 * 開始時間、終了時間、休憩時間から実際の勤務時間を計算する
 */
class CalculateWorkTimeUseCase : BaseUseCase<CalculateWorkTimeParams, String>() {

	override suspend fun execute(parameters: CalculateWorkTimeParams): String {
		val startTime = parameters.startTime
		val endTime = parameters.endTime
		val breakTime = parameters.breakTime

		// 時間文字列の妥当性チェック
		validateTimeFormat(startTime, "startTime")
		validateTimeFormat(endTime, "endTime")
		validateTimeFormat(breakTime, "breakTime")

		// 開始時間をパース
		val startParts = startTime.split(":")
		val startHH = startParts[0].toInt()
		val startMM = startParts[1].toInt()

		// 終了時間をパース
		val endParts = endTime.split(":")
		val endHH = endParts[0].toInt()
		val endMM = endParts[1].toInt()

		// 休憩時間をパース
		val breakParts = breakTime.split(":")
		val breakTimeMinutes = breakParts[0].toInt() * 60 + breakParts[1].toInt()

		// 開始・終了時間を分に変換
		val startMinutes = startHH * 60 + startMM
		val endMinutes = endHH * 60 + endMM

		// 日をまたぐ場合の処理
		val adjustedEndMinutes = if (endMinutes < startMinutes) {
			endMinutes + 24 * 60 // 翌日とみなす
		} else {
			endMinutes
		}

		// 実勤務時間を計算（分単位）
		val workMinutes = adjustedEndMinutes - startMinutes - breakTimeMinutes

		if (workMinutes < 0) {
			throw IllegalArgumentException("勤務時間が負の値になります。時間設定を確認してください。")
		}

		// 【修正】分単位から直接HH:mm形式に変換（60分進法で正確に計算）
		val hoursPart = workMinutes / 60
		val minutesPart = workMinutes % 60

		return String.format("%d:%02d", hoursPart, minutesPart)
	}

	/**
	 * 時間形式（HH:mm）の妥当性をチェック
	 */
	private fun validateTimeFormat(time: String, fieldName: String) {
		val parts = time.split(":")
		if (parts.size != 2) {
			throw IllegalArgumentException("$fieldName の形式が不正です: $time. 期待する形式: HH:mm")
		}

		val hour = parts[0].toIntOrNull()
		val minute = parts[1].toIntOrNull()

		if (hour == null || minute == null || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
			throw IllegalArgumentException("$fieldName の値が不正です: $time")
		}
	}
}