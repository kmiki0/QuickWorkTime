package com.example.quickworktime.domain.usecase

import com.example.quickworktime.room.WorkInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ウィジェット用退勤記録UseCase
 * ウィジェットからの退勤記録における複合処理を担当
 * - 既存データの有無による処理分岐
 * - 新規作成時のデフォルト値設定
 * - 既存データ更新時の値保持
 * - 時間計算の実行
 */
class RecordClockOutUseCase(
	private val calculateBreakTimeUseCase: CalculateBreakTimeUseCase,
	private val calculateWorkTimeUseCase: CalculateWorkTimeUseCase,
	private val recordWorkTimeUseCase: RecordWorkTimeUseCase
) : BaseUseCase<RecordClockOutParams, RecordClockOutResult>() {

	companion object {
		const val DEFAULT_START_TIME = "09:00"
	}

	override suspend fun execute(parameters: RecordClockOutParams): RecordClockOutResult {
		validateInputParameters(parameters)

		try {
			val today = getCurrentDate()

			return if (parameters.existingWorkInfo != null) {
				// 既存データがある場合：更新処理
				handleExistingDataUpdate(parameters, today)
			} else {
				// 既存データがない場合：新規作成処理
				handleNewDataCreation(parameters, today)
			}

		} catch (e: Exception) {
			throw Exception("退勤記録処理中にエラーが発生しました: ${e.message}", e)
		}
	}

	/**
	 * 既存データ更新時の処理
	 */
	private suspend fun handleExistingDataUpdate(
		parameters: RecordClockOutParams,
		today: String
	): RecordClockOutResult {
		val existingData = parameters.existingWorkInfo!!

		// 休憩時間を計算
		val breakTime = calculateBreakTimeUseCase.execute(
			CalculateBreakTimeParams(parameters.clockOutTime)
		)

		// 勤務時間を計算
		val workingTime = calculateWorkTimeUseCase.execute(
			CalculateWorkTimeParams(
				startTime = existingData.startTime,
				endTime = parameters.clockOutTime,
				breakTime = breakTime
			)
		)

		// 既存データを基に更新版WorkInfoを作成
		val updatedWorkInfo = WorkInfo(
			date = existingData.date,
			startTime = existingData.startTime,
			endTime = parameters.clockOutTime,
			workingTime = workingTime,
			breakTime = breakTime,
			isHoliday = existingData.isHoliday,
			isNationalHoliday = existingData.isNationalHoliday,
			weekday = existingData.weekday
		)

		return RecordClockOutResult(
			workInfo = updatedWorkInfo,
			operation = ClockOutOperation.UPDATE,
			success = true,
			message = "既存データを更新しました - 日付: $today, 終了時間: ${parameters.clockOutTime}"
		)
	}

	/**
	 * 新規データ作成時の処理
	 */
	private suspend fun handleNewDataCreation(
		parameters: RecordClockOutParams,
		today: String
	): RecordClockOutResult {
		val startTime = parameters.defaultStartTime ?: DEFAULT_START_TIME

		// RecordWorkTimeUseCaseを使用して新規WorkInfoを作成
		val newWorkInfo = recordWorkTimeUseCase.execute(
			RecordWorkTimeParams(
				date = today,
				startTime = startTime,
				endTime = parameters.clockOutTime
			)
		)

		return RecordClockOutResult(
			workInfo = newWorkInfo,
			operation = ClockOutOperation.INSERT,
			success = true,
			message = "新規データを作成しました - 日付: $today, 終了時間: ${parameters.clockOutTime}"
		)
	}

	/**
	 * 入力パラメータの妥当性検証
	 */
	private fun validateInputParameters(parameters: RecordClockOutParams) {
		// 退勤時間の形式検証
		validateTimeFormat(parameters.clockOutTime, "退勤時間")

		// デフォルト開始時間の検証（指定されている場合）
		parameters.defaultStartTime?.let { startTime ->
			validateTimeFormat(startTime, "デフォルト開始時間")
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
	 * 現在の日付をyyyyMMdd形式で取得
	 */
	private fun getCurrentDate(): String {
		return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
	}
}

/**
 * 退勤記録処理の結果
 */
data class RecordClockOutResult(
	val workInfo: WorkInfo,           // 処理後の勤務情報
	val operation: ClockOutOperation, // 実行された操作
	val success: Boolean,             // 処理成功フラグ
	val message: String              // 処理結果メッセージ
)

/**
 * 退勤記録時の操作種別
 */
enum class ClockOutOperation {
	INSERT,  // 新規作成
	UPDATE   // 更新
}