package com.example.quickworktime.ui.home.test

import android.content.Context
import android.util.Log
import com.example.quickworktime.domain.usecase.CalculateBreakTimeParams
import com.example.quickworktime.domain.usecase.CalculateBreakTimeUseCase
import com.example.quickworktime.domain.usecase.CalculateWorkTimeParams
import com.example.quickworktime.domain.usecase.CalculateWorkTimeUseCase
import com.example.quickworktime.domain.usecase.RecordWorkTimeParams
import com.example.quickworktime.domain.usecase.RecordWorkTimeUseCase
import com.example.quickworktime.room.AppDatabase
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.repository.WorkInfoRepository

class Test {

	// ============================================
	// テスト用メソッド群（HomeFragment内に追加）
	// ============================================

	/**
	 * UseCase個別テスト（Context不要）
	 */
	suspend fun quickUseCaseTest() {
		Log.d("Step3Test", "=== UseCase個別テスト ===")

		try {
			// 1. 休憩時間計算テスト
			val breakTimeUseCase = CalculateBreakTimeUseCase()
			Log.d("Step3Test", "11:30終了の休憩時間: ${breakTimeUseCase.execute(CalculateBreakTimeParams("11:30"))}")
			Log.d("Step3Test", "17:00終了の休憩時間: ${breakTimeUseCase.execute(CalculateBreakTimeParams("17:00"))}")
			Log.d("Step3Test", "18:30終了の休憩時間: ${breakTimeUseCase.execute(CalculateBreakTimeParams("18:30"))}")

			// 2. 勤務時間計算テスト
			val workTimeUseCase = CalculateWorkTimeUseCase()
			Log.d("Step3Test", "9:00-18:00(休憩1時間)の勤務時間: ${workTimeUseCase.execute(CalculateWorkTimeParams("09:00", "18:00", "01:00"))}")
			Log.d("Step3Test", "9:30-17:45(休憩45分)の勤務時間: ${workTimeUseCase.execute(CalculateWorkTimeParams("09:30", "17:45", "00:45"))}")

			// 3. RecordWorkTimeUseCaseテスト
			val calculateBreakTimeUseCase = CalculateBreakTimeUseCase()
			val calculateWorkTimeUseCase = CalculateWorkTimeUseCase()
			val recordWorkTimeUseCase = RecordWorkTimeUseCase(
				calculateBreakTimeUseCase = calculateBreakTimeUseCase,
				calculateWorkTimeUseCase = calculateWorkTimeUseCase
			)

			val result = recordWorkTimeUseCase.execute(
				RecordWorkTimeParams(
					date = "20241203",
					startTime = "09:00",
					endTime = "18:30"
				)
			)

			Log.d("Step3Test", "RecordWorkTimeUseCase結果:")
			Log.d("Step3Test", "  勤務時間: ${result.workingTime}") // 期待値: 8:30
			Log.d("Step3Test", "  休憩時間: ${result.breakTime}")   // 期待値: 01:00
			Log.d("Step3Test", "  曜日: ${result.weekday}")

			Log.d("Step3Test", "✅ UseCase個別テスト完了")

		} catch (e: Exception) {
			Log.e("Step3Test", "UseCase個別テストエラー: ${e.message}", e)
		}
	}

	/**
	 * Repository統合テスト（Context使用）
	 */
	suspend fun testRepositoryIntegrationWithContext(context: Context) {
		Log.d("Step3Test", "=== Repository統合テスト ===")

		try {
			// ✅ Context を使って Database インスタンスを作成
			val database = AppDatabase.getDatabase(context)
			val repository = WorkInfoRepository(database.workInfo())

			// テスト用のWorkInfo作成
			val testWorkInfo = WorkInfo(
				date = "20241203",
				startTime = "09:30",
				endTime = "18:45",
				workingTime = "", // UseCase で自動計算される
				breakTime = "",   // UseCase で自動計算される
				isHoliday = false,
				isNationalHoliday = false,
				weekday = ""      // UseCase で自動設定される
			)

			Log.d("Step3Test", "予想される計算結果:")
			Log.d("Step3Test", "  休憩時間: 01:00 (18:45終了)")
			Log.d("Step3Test", "  勤務時間: 8:15 (9:30-18:45-1:00)")

			// insertWorkInfoテスト（UseCase統合版）
			repository.insertWorkInfo(testWorkInfo)
			Log.d("Step3Test", "✅ insertWorkInfo 成功")

			// 登録データの取得確認
			val retrievedData = repository.getWorkInfoByDate("20241203")
			retrievedData?.let { data ->
				Log.d("Step3Test", "取得成功:")
				Log.d("Step3Test", "  日付: ${data.date}")
				Log.d("Step3Test", "  開始時間: ${data.startTime}")
				Log.d("Step3Test", "  終了時間: ${data.endTime}")
				Log.d("Step3Test", "  勤務時間: ${data.workingTime}")
				Log.d("Step3Test", "  休憩時間: ${data.breakTime}")
				Log.d("Step3Test", "  曜日: ${data.weekday}")

				// 期待値チェック
				if (data.workingTime == "8:15" && data.breakTime == "01:00") {
					Log.d("Step3Test", "✅ 計算結果が期待通りです！")
				} else {
					Log.w("Step3Test", "⚠️ 計算結果が予想と異なります")
				}
			} ?: run {
				Log.w("Step3Test", "⚠️ データ取得に失敗しました")
			}

			// ウィジェット退勤記録テスト
			val widgetResult = repository.recordClockOutForWidget("19:30")
			Log.d("Step3Test", "ウィジェット記録結果: $widgetResult")
			if (widgetResult) {
				Log.d("Step3Test", "✅ ウィジェット記録成功")
			} else {
				Log.w("Step3Test", "⚠️ ウィジェット記録失敗")
			}

			Log.d("Step3Test", "✅ Repository統合テスト完了")

		} catch (e: Exception) {
			Log.e("Step3Test", "Repository統合テストエラー: ${e.message}", e)
		}
	}
}
