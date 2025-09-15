package com.example.quickworktime.room.repository

import android.util.Log
import com.example.quickworktime.domain.usecase.CalculateBreakTimeParams
import com.example.quickworktime.domain.usecase.CalculateBreakTimeUseCase
import com.example.quickworktime.domain.usecase.CalculateWorkTimeUseCase
import com.example.quickworktime.domain.usecase.ClockOutOperation
import com.example.quickworktime.domain.usecase.RecordClockOutParams
import com.example.quickworktime.domain.usecase.RecordClockOutUseCase
import com.example.quickworktime.domain.usecase.RecordWorkTimeParams
import com.example.quickworktime.domain.usecase.RecordWorkTimeUseCase
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.WorkInfoDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * WorkInfo関連のRepository（最終整理版）
 * 責任：データアクセス操作のみに特化
 * ビジネスロジックはすべてUseCase層に移行済み
 */
class WorkInfoRepository(private val dao: WorkInfoDao) {

	// UseCase層のインスタンス（依存性注入）
	private val calculateBreakTimeUseCase = CalculateBreakTimeUseCase()
	private val calculateWorkTimeUseCase = CalculateWorkTimeUseCase()
	private val recordWorkTimeUseCase = RecordWorkTimeUseCase(
		calculateBreakTimeUseCase = calculateBreakTimeUseCase,
		calculateWorkTimeUseCase = calculateWorkTimeUseCase
	)
	private val recordClockOutUseCase = RecordClockOutUseCase(
		calculateBreakTimeUseCase = calculateBreakTimeUseCase,
		calculateWorkTimeUseCase = calculateWorkTimeUseCase,
		recordWorkTimeUseCase = recordWorkTimeUseCase
	)

	/** ============================================
	 *  勤務情報を登録（UseCase統合版）
	 *  @param workInfo WorkInfo（基本情報のみ必要）
	 *  ============================================ */
	suspend fun insertWorkInfo(workInfo: WorkInfo) {
		try {
			// RecordWorkTimeUseCaseで完全なWorkInfoを作成
			val completeWorkInfo = recordWorkTimeUseCase.execute(
				RecordWorkTimeParams(
					date = workInfo.date,
					startTime = workInfo.startTime,
					endTime = workInfo.endTime
				)
			)

			// データベースに登録
			dao.insertWorkInfo(completeWorkInfo)
			Log.d("DebugLog", "勤務情報登録完了: ${completeWorkInfo.date} ${completeWorkInfo.startTime}-${completeWorkInfo.endTime}")

		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "勤務情報登録エラー: ${e.message}", e)
			throw e // エラーを上位層に伝播
		}
	}

	/** ============================================
	 *  ウィジェット用退勤記録（UseCase統合版）
	 *  @param endTime 退勤時刻 (HH:mm形式)
	 *  @return Boolean 成功/失敗
	 *  ============================================ */
	suspend fun recordClockOutForWidget(endTime: String): Boolean {
		return try {
			val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

			// 既存データの確認
			val existingWorkInfo = getWorkInfoByDate(today)

			// RecordClockOutUseCaseで処理実行
			val result = recordClockOutUseCase.execute(
				RecordClockOutParams(
					clockOutTime = endTime,
					existingWorkInfo = existingWorkInfo,
					defaultStartTime = "09:00"
				)
			)

			// データベース操作の実行
			when (result.operation) {
				ClockOutOperation.UPDATE -> {
					dao.updateWorkInfo(result.workInfo)
					Log.d("DebugLog", "ウィジェット更新: ${result.message}")
				}
				ClockOutOperation.INSERT -> {
					dao.insertWorkInfo(result.workInfo)
					Log.d("DebugLog", "ウィジェット挿入: ${result.message}")
				}
			}

			result.success

		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "ウィジェット退勤記録エラー: ${e.message}", e)
			false
		}
	}

	// ============================================
	// データアクセス専用メソッド群
	// ビジネスロジックを含まない純粋なCRUD操作
	// ============================================

	/** ============================================
	 *  月ごとの範囲検索したデータを取得
	 *  @param date String (yyyyMM形式)
	 *  @return List<WorkInfo>
	 *  ============================================ */
	suspend fun getMonthWorkInfo(date: String): List<WorkInfo> {
		return try {
			dao.getMonthWorkInfo(date)
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "月次データ取得エラー: ${e.message}", e)
			emptyList()
		}
	}

	/** ============================================
	 *  データの最大値を取得
	 *  @return String 最新日付（yyyyMMdd形式）
	 *  ============================================ */
	suspend fun getLatestDate(): String {
		return try {
			dao.getLatestDate()
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "最新日付取得エラー: ${e.message}", e)
			""
		}
	}

	/** ============================================
	 *  指定日付のデータを取得
	 *  @param date String? (yyyyMMdd形式、nullの場合は最新データ)
	 *  @return WorkInfo?
	 *  ============================================ */
	suspend fun getWorkInfoByDate(date: String?): WorkInfo? {
		return try {
			val searchDate = date ?: getLatestDate()
			if (searchDate.isEmpty()) return null

			dao.getWorkInfoByDate(searchDate)
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "日付別データ取得エラー: ${e.message}", e)
			null
		}
	}

	/** ============================================
	 *  勤務情報を直接登録
	 *  @param workInfo WorkInfo
	 *  ============================================ */
	suspend fun insertWorkInfoDirect(workInfo: WorkInfo) {
		try {
			dao.insertWorkInfo(workInfo)
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "直接登録エラー: ${e.message}", e)
			throw e
		}
	}

	/** ============================================
	 *  データ 1件を削除
	 *  @param workInfo WorkInfo
	 *  ============================================ */
	suspend fun deleteWorkInfo(workInfo: WorkInfo) {
		try {
			dao.deleteWorkInfo(workInfo)
			Log.d("DebugLog", "勤務情報削除完了: ${workInfo.date}")
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "削除エラー: ${e.message}", e)
			throw e
		}
	}

	/** ============================================
	 *  データ 1件を更新
	 *  @param workInfo WorkInfo
	 *  ============================================ */
	suspend fun updateWorkInfo(workInfo: WorkInfo) {
		try {
			dao.updateWorkInfo(workInfo)
			Log.d("DebugLog", "勤務情報更新完了: ${workInfo.date}")
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "更新エラー: ${e.message}", e)
			throw e
		}
	}

	// ============================================
	// ヘルパーメソッド（UseCase使用）
	// ============================================

	/** ============================================
	 *  勤務時間の再計算（UseCase使用）
	 *  @param workInfo WorkInfo
	 *  @return WorkInfo 再計算後のWorkInfo
	 *  ============================================ */
	suspend fun recalculateWorkInfo(workInfo: WorkInfo): WorkInfo {
		return try {
			recordWorkTimeUseCase.execute(
				RecordWorkTimeParams(
					date = workInfo.date,
					startTime = workInfo.startTime,
					endTime = workInfo.endTime
				)
			)
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "再計算エラー: ${e.message}", e)
			workInfo // エラー時は元のデータを返す
		}
	}

	/** ============================================
	 *  指定時刻での休憩時間を計算（UseCase使用）
	 *  @param endTime String (HH:mm形式)
	 *  @return String 休憩時間（HH:mm形式）
	 *  ============================================ */
	suspend fun calculateBreakTimeForEndTime(endTime: String): String {
		return try {
			calculateBreakTimeUseCase.execute(CalculateBreakTimeParams(endTime))
		} catch (e: Exception) {
			Log.e("WorkInfoRepository", "休憩時間計算エラー: ${e.message}", e)
			"01:00" // デフォルト値
		}
	}
}