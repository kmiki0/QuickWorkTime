package com.example.quickworktime.ui.home.input

import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.quickworktime.databinding.FragmentHomeBinding
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.ui.home.HomeViewModel
import com.example.quickworktime.ui.home.animation.HomeAnimationController

/**
 * HomeFragment の時間入力処理を専門に行うクラス
 * 責任: 時間テキストのクリック処理、GridLayoutボタン管理、データ挿入
 */
class TimeInputHandler(
	private val fragment: Fragment,
	private val binding: FragmentHomeBinding,
	private val viewModel: HomeViewModel,
	private val animationController: HomeAnimationController
) {

	/**
	 * 時間テキストのクリックリスナーを設定
	 */
	fun setupTimeTextClickListeners() {
		setupStartHourClickListener()
		setupStartMinuteClickListener()
		setupEndHourClickListener()
		setupEndMinuteClickListener()
	}

	/**
	 * StartTime (HH) タップ イベント
	 */
	private fun setupStartHourClickListener() {
		binding.textTimeStartHH.setOnClickListener {
			val state = viewModel.activeTextState.value
			val txtView = binding.textTimeStartHH
			viewModel.setActiveTextView(binding.textTimeStartHH)

			when {
				state == HomeViewModel.ActiveText.START && txtView.textSize == HomeAnimationController.TXT_SIZE_MAX -> {
					// StateをDefaultに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
				state == HomeViewModel.ActiveText.START && txtView.textSize == HomeAnimationController.TXT_SIZE_NORMAL -> {
					// アニメーション実行
					animationController.animateStartTimeText(binding.textTimeStartHH, true)
					// ボタンのラベルを設定
					setGridBtnLabel(0)
					viewModel.setActiveTextView(binding.textTimeStartHH)
				}
				state == HomeViewModel.ActiveText.DEFAULT -> {
					// StateをSTARTに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.START)
					animationController.animateStartTimeText(binding.textTimeStartHH, false)
					// ボタンのラベルを設定
					setGridBtnLabel(0)
					viewModel.setActiveTextView(binding.textTimeStartHH)
				}
				state == HomeViewModel.ActiveText.END -> {
					// StateをDEFAULTに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
			}
		}
	}

	/**
	 * StartTime (MM) タップ イベント
	 */
	private fun setupStartMinuteClickListener() {
		binding.textTimeStartMM.setOnClickListener {
			val state = viewModel.activeTextState.value
			val txtView = binding.textTimeStartMM
			viewModel.setActiveTextView(binding.textTimeStartMM)

			when {
				state == HomeViewModel.ActiveText.START && txtView.textSize == HomeAnimationController.TXT_SIZE_MAX -> {
					// StateをDefaultに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
				state == HomeViewModel.ActiveText.START && txtView.textSize == HomeAnimationController.TXT_SIZE_NORMAL -> {
					// アニメーション実行
					animationController.animateStartTimeText(binding.textTimeStartMM, true)
					// ボタンのラベルを設定
					setGridBtnLabel(2)
					viewModel.setActiveTextView(binding.textTimeStartMM)
				}
				state == HomeViewModel.ActiveText.DEFAULT -> {
					// StateをSTARTに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.START)
					animationController.animateStartTimeText(binding.textTimeStartMM, false)
					// ボタンのラベルを設定
					setGridBtnLabel(2)
					viewModel.setActiveTextView(binding.textTimeStartMM)
				}
				state == HomeViewModel.ActiveText.END -> {
					// StateをDEFAULTに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
			}
		}
	}

	/**
	 * EndTime (HH) タップ イベント
	 */
	private fun setupEndHourClickListener() {
		binding.textTimeEndHH.setOnClickListener {
			val state = viewModel.activeTextState.value
			val txtView = binding.textTimeEndHH
			viewModel.setActiveTextView(binding.textTimeEndHH)

			when {
				state == HomeViewModel.ActiveText.END && txtView.textSize == HomeAnimationController.TXT_SIZE_MAX -> {
					// StateをDefaultに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
				state == HomeViewModel.ActiveText.END && txtView.textSize == HomeAnimationController.TXT_SIZE_NORMAL -> {
					// アニメーション実行
					animationController.animateEndTimeText(binding.textTimeEndHH, true)
					// ボタンのラベルを設定
					setGridBtnLabel(1)
					viewModel.setActiveTextView(binding.textTimeEndHH)
				}
				state == HomeViewModel.ActiveText.DEFAULT -> {
					// StateをENDに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.END)
					animationController.animateEndTimeText(binding.textTimeEndHH, false)
					// ボタンのラベルを設定
					setGridBtnLabel(1)
					viewModel.setActiveTextView(binding.textTimeEndHH)
				}
				state == HomeViewModel.ActiveText.START -> {
					// StateをDEFAULTに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
			}
		}
	}

	/**
	 * EndTime (MM) タップ イベント
	 */
	private fun setupEndMinuteClickListener() {
		binding.textTimeEndMM.setOnClickListener {
			val state = viewModel.activeTextState.value
			val txtView = binding.textTimeEndMM
			viewModel.setActiveTextView(binding.textTimeEndMM)

			when {
				state == HomeViewModel.ActiveText.END && txtView.textSize == HomeAnimationController.TXT_SIZE_MAX -> {
					// StateをDefaultに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
				state == HomeViewModel.ActiveText.END && txtView.textSize == HomeAnimationController.TXT_SIZE_NORMAL -> {
					// アニメーション実行
					animationController.animateEndTimeText(binding.textTimeEndMM, true)
					// ボタンのラベルを設定
					setGridBtnLabel(2)
					viewModel.setActiveTextView(binding.textTimeEndMM)
				}
				state == HomeViewModel.ActiveText.DEFAULT -> {
					// StateをENDに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.END)
					animationController.animateEndTimeText(binding.textTimeEndMM, false)
					// ボタンのラベルを設定
					setGridBtnLabel(2)
					viewModel.setActiveTextView(binding.textTimeEndMM)
				}
				state == HomeViewModel.ActiveText.START -> {
					// StateをDEFAULTに変更
					viewModel.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
				}
			}
		}
	}

	/**
	 * GridLayoutのボタンにラベルを設定する
	 * @param lblTimeNum ラベルの種類 [0: AM, 1: PM, 2: min]
	 */
	private fun setGridBtnLabel(lblTimeNum: Int) {
		val lblTime = listOf(
			listOf("01 h", "02 h", "03 h", "04 h", "05 h", "06 h", "07 h", "08 h", "09 h", "10 h", "11 h", "12 h"),
			listOf("13 h", "14 h", "15 h", "16 h", "17 h", "18 h", "19 h", "20 h", "21 h", "22 h", "23 h", "00 h"),
			listOf("00 m", "05 m", "10 m", "15 m", "20 m", "25 m", "30 m", "35 m", "40 m", "45 m", "50 m", "55 m")
		)

		// gridLayoutの要素を取得
		for ((viewBtnCnt, viewBtn) in binding.gridBtnLayout.touchables.withIndex()) {
			// Type が Button の場合
			if (viewBtn is Button) {
				// ボタンのテキストを変更
				viewBtn.text = lblTime[lblTimeNum][viewBtnCnt]
				viewBtn.setOnClickListener {
					// ボタンをタップした際の振動
					it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
					// テキストを変更
					if (isChangedActiveTextValue(viewBtn.text.substring(0, 2))) {
						insertWorkInfo()
					}
				}
			}
		}
	}

	/**
	 * アクティブなテキストの値が変更されたかをチェック
	 * @param value 変更しようとしている値
	 * @return 値が変更された場合 true
	 */
	private fun isChangedActiveTextValue(value: String): Boolean {
		return when (viewModel.activeTextView.value?.id) {
			binding.textTimeStartHH.id -> {
				if (value != viewModel.startHour.value) {
					viewModel.setStartHour(value)
					true
				} else {
					false
				}
			}
			binding.textTimeStartMM.id -> {
				if (value != viewModel.startMinute.value) {
					viewModel.setStartMinute(value)
					true
				} else {
					false
				}
			}
			binding.textTimeEndHH.id -> {
				if (value != viewModel.endHour.value) {
					viewModel.setEndHour(value)
					true
				} else {
					false
				}
			}
			binding.textTimeEndMM.id -> {
				if (value != viewModel.endMinute.value) {
					viewModel.setEndMinute(value)
					true
				} else {
					false
				}
			}
			else -> {
				false
			}
		}
	}

	/**
	 * 勤務情報をデータベースに挿入
	 */
	private fun insertWorkInfo() {
		val data = WorkInfo(
			viewModel.date.value!!.replace("/", ""),
			viewModel.startHour.value + ":" + viewModel.startMinute.value,
			viewModel.endHour.value + ":" + viewModel.endMinute.value,
			"",
			"",
			false,
			false,
			viewModel.week.value!!.replace("(", "").replace(")", "")
		)

		viewModel.insertWorkInfo(data)
		Toast.makeText(fragment.requireContext(), "${binding.textDate.text} を登録しました", Toast.LENGTH_SHORT).show()
	}
}