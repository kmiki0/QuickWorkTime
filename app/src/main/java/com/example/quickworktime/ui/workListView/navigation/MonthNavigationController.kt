package com.example.quickworktime.ui.workListView.navigation

import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.quickworktime.databinding.FragmentDashboardBinding
import com.example.quickworktime.ui.workListView.WorkListViewViewModel
import java.util.Calendar

/**
 * WorkListViewFragment の月次ナビゲーション制御を専門に行うクラス
 * 責任: 月次移動ボタン、履歴ボタンの状態管理、月変更処理
 */
class MonthNavigationController(
	private val fragment: Fragment,
	private val binding: FragmentDashboardBinding,
	private val viewModel: WorkListViewViewModel,
	private val lifecycleOwner: LifecycleOwner,
	private val onDataLoad: () -> Unit
) {

	/**
	 * 月次ナビゲーション関連のセットアップ
	 */
	fun setupMonthNavigation() {
		setupNavigationButtons()
		observeMonthChanges()
	}

	/**
	 * 前月/次月ボタンのクリックリスナーを設定
	 */
	private fun setupNavigationButtons() {
		// 前月ボタン
		binding.btnBackMonth.setOnClickListener { view ->
			// isEnabledでチェックするように変更
			if (!binding.btnBackMonth.isEnabled) return@setOnClickListener

			val yyyyMM = viewModel.monthText.value.toString().replace("/", "")
			// 表示する月を変更
			viewModel.setMonthText(changeMonth(yyyyMM, -1).substring(0, 4) + "/" + changeMonth(yyyyMM, -1).substring(4, 6))
			// データをロードして表示
			onDataLoad()
			// 履歴ボタンの表示非表示の設定
			setHistoryButton()
		}

		// 次月ボタン
		binding.btnNextMonth.setOnClickListener { view ->
			// isEnabledでチェックするように変更
			if (!binding.btnNextMonth.isEnabled) return@setOnClickListener

			val yyyyMM = viewModel.monthText.value.toString().replace("/", "")
			// 表示する月を変更
			viewModel.setMonthText(changeMonth(yyyyMM, 1).substring(0, 4) + "/" + changeMonth(yyyyMM, 1).substring(4, 6))
			// データをロードして表示
			onDataLoad()
			// 履歴ボタンの表示非表示の設定
			setHistoryButton()
		}
	}

	/**
	 * 月表示の変更を監視し、履歴ボタン状態を更新
	 */
	private fun observeMonthChanges() {
		// 画面のmonthTextにvmのmonthTextをバインド
		viewModel.monthText.observe(lifecycleOwner) { monthText ->
			binding.monthText.text = monthText
		}
	}

	/**
	 * 履歴ボタンの表示非表示の設定
	 */
	fun setHistoryButton() {
		// LiveDataの監視
		viewModel.monthText.observe(lifecycleOwner) { monthText ->
			// yyyyMM を取得
			val yyyyMM = monthText.replace("/", "")

			// 前月のデータ存在チェック
			val backMonth = changeMonth(yyyyMM, -1)
			viewModel.getMonthDataCount(backMonth).observe(lifecycleOwner) { count ->
				updateButtonState(binding.btnBackMonth, count > 0)
			}

			// 次の月のデータ存在チェック
			val nextMonth = changeMonth(yyyyMM, 1)
			viewModel.getMonthDataCount(nextMonth).observe(lifecycleOwner) { count ->
				updateButtonState(binding.btnNextMonth, count > 0)
			}
		}
	}

	/**
	 * ボタンの状態を更新する共通メソッド
	 * @param button 対象のボタン
	 * @param hasData データが存在するかどうか
	 */
	private fun updateButtonState(button: Button, hasData: Boolean) {
		button.isEnabled = hasData
		// XMLでセレクターを使用している場合は、以下の手動設定は不要
		// ただし、より明確にするために残すことも可能
		if (hasData) {
			button.alpha = 1.0f
		} else {
			button.alpha = 0.4f
		}
	}

	/**
	 * 引数によって、月を変更する
	 * @param yyyyMM 現在の年月（yyyyMM形式）
	 * @param month 変更する月数（+1で次月、-1で前月）
	 * @return 変更後の年月（yyyyMM形式）
	 */
	private fun changeMonth(yyyyMM: String, month: Int): String {
		// yyyyMM をDate型に変換
		val date = java.text.SimpleDateFormat("yyyyMM").parse(yyyyMM)
		val calendar = Calendar.getInstance()
		calendar.time = date
		calendar.add(Calendar.MONTH, month)

		return java.text.SimpleDateFormat("yyyyMM").format(calendar.time)
	}

	/**
	 * 現在の月表示を取得
	 */
	fun getCurrentMonth(): String {
		return viewModel.monthText.value?.replace("/", "") ?: ""
	}

	/**
	 * 月表示を設定
	 */
	fun setMonth(yyyyMM: String) {
		val formattedMonth = yyyyMM.substring(0, 4) + "/" + yyyyMM.substring(4, 6)
		viewModel.setMonthText(formattedMonth)
	}
}