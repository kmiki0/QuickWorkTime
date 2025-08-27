package com.example.quickworktime.room.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.WorkInfoDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WorkInfoRepository(private val dao: WorkInfoDao) {

	/** ============================================
	 *  データ 1件を登録
	 *  @param workInfo WorkInfo
	 *  ============================================ */
	suspend fun insertWorkInfo(workInfo: WorkInfo) {
		try {
			// 作業情報を登録
			val insertData = WorkInfo(
				workInfo.date, 			  // 日付
				workInfo.startTime, 	  // 開始時間
				workInfo.endTime, 		  // 終了時間
				calcWorkTime(workInfo),   // 作業時間
				calcBreakTime(workInfo),  // 休憩時間
				false,		  // 休日フラグ
				false,  // 祝日フラグ
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
	suspend fun recordClockOutForWidget(time: String): Boolean {
		return try {
			val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
			val existingWorkInfo = getWorkInfoByDate(today)

			if (existingWorkInfo != null) {
				// 既存データの場合は終了時間のみ更新
				// calcWorkTime、calcBreakTimeは既存のロジックを使用
				val updatedWorkInfo = existingWorkInfo.copy(endTime = time)
				// 既存のinsertWorkInfoを使って計算処理も含めて更新
				insertWorkInfo(updatedWorkInfo)
			} else {
				// 新規データの場合、デフォルト値で作成
				val newWorkInfo = WorkInfo(
					date = today,
					startTime = "09:00", // デフォルト開始時間
					endTime = time,
					workingTime = "", // 自動計算
					breakTime = "",   // 自動計算
					isHoliday = false,
					isNationalHoliday = false,
					weekday = ""      // 自動設定
				)
				insertWorkInfo(newWorkInfo)
			}
			true
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "Error recording clock out for widget: ${e.message}")
			false
		}
	}

	// 休憩時間を計算
	fun calcWorkTime(workInfo: WorkInfo) : String {
		val startHH = workInfo.startTime.split(":")[0].toInt()
		val startMM = workInfo.startTime.split(":")[1].toInt()
		val endHH = workInfo.endTime.split(":")[0].toInt()
		val endMM = workInfo.endTime.split(":")[1].toInt()

		// 休憩時間を計算
		val breSplit = calcBreakTime(workInfo).split(":")
		val breakTime: Float = (breSplit[0].toInt() * 60 + breSplit[1].toInt()).toFloat()

		// 作業時間を計算
		val calcTime: Float = (((endHH * 60 + endMM) - (startHH * 60 + startMM) - breakTime) / 60)

		// 作業時間を "HH:mm" 形式に変換
		val calcSplit = truncateToTwoDecimalPlaces(calcTime).toString().split(".")
		return calcSplit[0] + ":" + calcSplit[1].padStart(2, '0')
	}

	fun calcBreakTime(workInfo: WorkInfo) : String {
		val endHH = workInfo.endTime.split(":")[0]
		val endMM = workInfo.endTime.split(":")[1]

		// endHH が 12時以内の場合、0時間
		if (endHH <= "12") {
			return "00:00"
		}else if (endHH + endMM <= "1730") {
			return "00:45"
		} else {
			return "01:00"
		}
	}

	private fun truncateToTwoDecimalPlaces(value: Float): Float {
		val strValue = value.toString()
		val decimalIndex = strValue.indexOf('.')

		return if (decimalIndex != -1 && strValue.length - decimalIndex - 1 > 2) {
			strValue.substring(0, decimalIndex + 3).toFloat()
		} else {
			value
		}
	}

	// yyyyMMdd形式から曜日(取得)を取得
	private fun getWeekday(date: String): String {
		val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
		val formatdate = LocalDate.parse(date, formatter)

		return formatdate.dayOfWeek.toString()
	}
}
