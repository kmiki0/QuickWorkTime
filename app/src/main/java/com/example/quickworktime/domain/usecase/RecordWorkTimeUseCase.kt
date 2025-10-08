package com.example.quickworktime.domain.usecase

import com.example.quickworktime.room.WorkInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 勤務時間記録UseCase
 * 最低限のパラメータ(これ以外は自動計算)
 * 	- date: yyyyMMdd形式の日付
 * 	- startTime: HH:mm形式の開始時間
 * 	- endTime: HH:mm形式の終了時間
 * 勤務情報の登録における複合処理を担当
 * - 休憩時間の自動計算
 * - 勤務時間の自動計算
 * - 曜日の自動設定
 * - データの整合性確保
 */
class RecordWorkTimeUseCase(
	private val calculateBreakTimeUseCase: CalculateBreakTimeUseCase,
	private val calculateWorkTimeUseCase: CalculateWorkTimeUseCase
) : BaseUseCase<RecordWorkTimeParams, WorkInfo>() {

	override suspend fun execute(parameters: RecordWorkTimeParams): WorkInfo {
		// 入力パラメータの検証
		validateInputParameters(parameters)

		try {
			// 1. 休憩時間を自動計算
			val breakTime = calculateBreakTimeUseCase.execute(
				CalculateBreakTimeParams(parameters.endTime)
			)

			// 2. 勤務時間を自動計算
			val workingTime = calculateWorkTimeUseCase.execute(
				CalculateWorkTimeParams(
					startTime = parameters.startTime,
					endTime = parameters.endTime,
					breakTime = breakTime
				)
			)

			// 3. 曜日を自動設定
			val weekday = calculateWeekday(parameters.date)

			// 4. 完全な勤務情報を作成
			return WorkInfo(
				date = parameters.date,
				startTime = parameters.startTime,
				endTime = parameters.endTime,
				workingTime = workingTime,
				breakTime = breakTime,
				isHoliday = false,           // デフォルト値
				isNationalHoliday = false,   // デフォルト値
				weekday = weekday
			)

		} catch (e: Exception) {
			throw Exception("勤務時間記録処理中にエラーが発生しました: ${e.message}", e)
		}
	}

	/**
	 * 入力パラメータの妥当性検証
	 */
	private fun validateInputParameters(parameters: RecordWorkTimeParams) {
		// 日付形式の検証（yyyyMMdd）
		if (!parameters.date.matches(Regex("\\d{8}"))) {
			throw IllegalArgumentException("日付形式が不正です: ${parameters.date}. 期待する形式: yyyyMMdd")
		}

		// 時間形式の検証（HH:mm）
		validateTimeFormat(parameters.startTime, "開始時間")
		validateTimeFormat(parameters.endTime, "終了時間")

		// 日付の妥当性検証
		try {
			LocalDate.parse(parameters.date, DateTimeFormatter.ofPattern("yyyyMMdd"))
		} catch (e: Exception) {
			throw IllegalArgumentException("無効な日付です: ${parameters.date}")
		}
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

	/**
	 * yyyyMMdd形式の日付から曜日を計算
	 */
	private fun calculateWeekday(dateString: String): String {
		return try {
			val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
			val date = LocalDate.parse(dateString, formatter)
			date.dayOfWeek.toString()
		} catch (e: Exception) {
			throw Exception("曜日計算に失敗しました: $dateString", e)
		}
	}
}