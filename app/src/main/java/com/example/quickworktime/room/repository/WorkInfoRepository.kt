package com.example.quickworktime.room.repository

import android.util.Log
import com.example.quickworktime.domain.usecase.*
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.WorkInfoDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class WorkInfoRepository(private val dao: WorkInfoDao) {

	// UseCaseインスタンス
	private val calculateBreakTimeUseCase = CalculateBreakTimeUseCase()
	private val calculateWorkTimeUseCase = CalculateWorkTimeUseCase()

	/** ============================================
	 *  データ 1件を登録
	 *  @param workInfo WorkInfo
	 *  ============================================ */
	suspend fun insertWorkInfo(workInfo: WorkInfo) {
		try {
			// UseCaseを使用して計算
			val breakTime = calculateBreakTimeUseCase.execute(
				CalculateBreakTimeParams(workInfo.endTime)
			)

			val workingTime = calculateWorkTimeUseCase.execute(
				CalculateWorkTimeParams(
					startTime = workInfo.startTime,
					endTime = workInfo.endTime,
					breakTime = breakTime
				)
			)

			// 作業情報を登録
			val insertData = WorkInfo(
				workInfo.date,           // 日付
				workInfo.startTime,      // 開始時間
				workInfo.endTime,        // 終了時間
				workingTime,             // 作業時間（UseCase計算）
				breakTime,               // 休憩時間（UseCase計算）
				false,                   // 休日フラグ
				false,                   // 祝日フラグ
				getWeekday(workInfo.date) // 曜日(英語)
			)
			dao.insertWorkInfo(insertData)
		} catch (e: Exception) {
			Log.d("DebugLog", "Error: ${e.message}")
		}
	}

	/** ============================================
	 *  月ごとの範囲検索したデータを取得
	 *  @param date String
	 *  ============================================ */
	suspend fun getMonthWorkInfo(date: String): List<WorkInfo> {
		return try {
			dao.getMonthWorkInfo(date)
		} catch (e: Exception) {
			Log.d("DebugLog", "Error: ${e.message}")
			listOf()
		}
	}

	/** ============================================
	 *  データの最大値を取得
	 *  @return LiveData<String>
	 *  ============================================ */
	suspend fun getLatestDate(): String {
		return try {
			dao.getLatestDate()
		} catch (e: Exception) {
			Log.d("DebugLog", "Error: ${e.message}")
			""
		}
	}

	/** ============================================
	 *  PKに沿ったデータを取得 (引数がNULLの場合、最新のデータを取得)
	 *  @return WorkInfo
	 *  ============================================ */
	suspend fun getWorkInfoByDate(date: String?): WorkInfo? {
		var searchDate: String? = date
		return try {
			if (searchDate == null) {
				searchDate = dao.getLatestDate()
			}
			dao.getWorkInfoByDate(searchDate)
		} catch (e: Exception) {
			Log.d("DebugLog", "Error: ${e.message}")
			null
		}
	}

	/** ============================================
	 *  データ 1件を削除
	 *  @param workInfo WorkInfo
	 *  ============================================ */
	suspend fun deleteWorkInfo(workInfo: WorkInfo) {
		try {
			dao.deleteWorkInfo(workInfo)
		} catch (e: Exception) {
			Log.d("DebugLog", "Error: ${e.message}")
		}
	}

	/** ============================================
	 *  ウィジェット用退勤記録メソッド
	 *  @param time 退勤時刻 (HH:mm形式)
	 *  @return Boolean 成功/失敗
	 *  ============================================ */
	suspend fun recordClockOutForWidget(endTime: String): Boolean {
		return try {
			val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
			val existingWorkInfo = getWorkInfoByDate(today)

			if (existingWorkInfo != null) {
				// UseCaseを使用して計算
				val breakTime = calculateBreakTimeUseCase.execute(
					CalculateBreakTimeParams(endTime)
				)
				val workingTime = calculateWorkTimeUseCase.execute(
					CalculateWorkTimeParams(
						startTime = existingWorkInfo.startTime,
						endTime = endTime,
						breakTime = breakTime
					)
				)

				// 既存データの場合：終了時間を更新してUpdateを使用
				val updatedWorkInfo = WorkInfo(
					date = existingWorkInfo.date,
					startTime = existingWorkInfo.startTime,
					endTime = endTime,
					workingTime = workingTime,
					breakTime = breakTime,
					isHoliday = existingWorkInfo.isHoliday,
					isNationalHoliday = existingWorkInfo.isNationalHoliday,
					weekday = existingWorkInfo.weekday
				)
				dao.updateWorkInfo(updatedWorkInfo)
				Log.d("DebugLog", "ウィジェット：既存データを更新 - 日付: $today, 終了時間: $endTime")
			} else {
				// 新規データの場合：デフォルト値でInsertを使用
				val newWorkInfo = WorkInfo(
					date = today,
					startTime = "09:00", // デフォルト開始時間
					endTime = endTime,
					workingTime = "", // insertWorkInfoで自動計算される（ダミー値）
					breakTime = "",   // insertWorkInfoで自動計算される（ダミー値）
					isHoliday = false,
					isNationalHoliday = false,
					weekday = ""      // insertWorkInfoで自動設定される（ダミー値）
				)
				// 既存のinsertWorkInfoメソッドを使用して正しい計算処理を実行
				insertWorkInfo(newWorkInfo)
				Log.d("DebugLog", "ウィジェット：新規データを挿入 - 日付: $today, 終了時間: $endTime")
			}
			true
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "Error recording clock out for widget: ${e.message}")
			false
		}
	}

	// yyyyMMdd形式から曜日(取得)を取得
	private fun getWeekday(date: String): String {
		val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
		val formatdate = LocalDate.parse(date, formatter)

		return formatdate.dayOfWeek.toString()
	}
}
