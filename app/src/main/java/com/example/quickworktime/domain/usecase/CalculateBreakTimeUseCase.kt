package com.example.quickworktime.domain.usecase

/**
 * 休憩時間計算UseCase
 * ビジネスルール：
 * - 12時以前の終了 → 休憩なし（00:00）
 * - 17:30以前の終了 → 45分休憩（00:45）
 * - それ以降の終了 → 1時間休憩（01:00）
 */
class CalculateBreakTimeUseCase : BaseUseCase<CalculateBreakTimeParams, String>() {

	override suspend fun execute(parameters: CalculateBreakTimeParams): String {
		val endTime = parameters.endTime

		// 時間と分を分離
		val timeParts = endTime.split(":")
		if (timeParts.size != 2) {
			throw IllegalArgumentException("Invalid time format: $endTime. Expected format: HH:mm")
		}

		val endHH = timeParts[0]
		val endMM = timeParts[1]

		// 時間の妥当性チェック
		val hour = endHH.toIntOrNull()
		val minute = endMM.toIntOrNull()

		if (hour == null || minute == null || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
			throw IllegalArgumentException("Invalid time values: $endTime")
		}

		return when {
			// 12時以前の終了は休憩なし
			endHH <= "12" -> "00:00"

			// 17:30以前の終了は45分休憩
			endHH + endMM <= "1730" -> "00:45"

			// それ以降は1時間休憩
			else -> "01:00"
		}
	}
}