package com.example.quickworktime.ui.home.menu

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.quickworktime.databinding.FragmentHomeBinding
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.ui.home.HomeViewModel

/**
 * HomeFragment のFABメニュー処理を専門に行うクラス
 * 責任: FABメニューの展開/収納、新規作成、削除、設定遷移
 */
class FabMenuController(
	private val fragment: Fragment,
	private val binding: FragmentHomeBinding,
	private val viewModel: HomeViewModel
) {

	private var isFabMenuOpen = false

	// TODO: DBから取得するように変更
	companion object {
		private const val DEFAULT_START_TIME = "09:00"
		private const val DEFAULT_END_TIME = "18:00"
		private const val DEFAULT_BREAK_TIME = "01:00"

		// アニメーション定数
		private const val ANIMATION_DURATION = 300L
		private const val FAB_SPACING = 180f  // FAB間の距離（dp相当）
		private const val LABEL_MARGIN = 120f     // ラベルとFABの間隔（この値を調整）
	}

	/**
	 * FABメニューのセットアップ
	 */
	fun setupFabMenu() {
		setupMainFabClickListener()
		setupSubFabClickListeners()
		initializeFabPositions()
	}

	/**
	* サブFABの初期位置を設定（すべて非表示状態）
	*/
	private fun initializeFabPositions() {
		// postを使用してビューが完全に初期化された後に実行
		binding.fabNewDay.post {
			binding.fabNewDay.visibility = View.GONE
			binding.fabDelDay.visibility = View.GONE
			binding.fabSetting.visibility = View.GONE

			binding.labelNewDay?.visibility = View.GONE
			binding.labelDelDay?.visibility = View.GONE
			binding.labelSetting?.visibility = View.GONE

			// 初期状態では全てメインFABと同じ位置に配置
			listOf(binding.fabNewDay, binding.fabDelDay, binding.fabSetting).forEach { fab ->
				fab.translationY = 0f
				fab.translationX = 0f
				fab.alpha = 0f
				fab.scaleX = 0f
				fab.scaleY = 0f
			}

			// ラベルも初期化
			listOf(binding.labelNewDay, binding.labelDelDay, binding.labelSetting).forEach { label ->
				label?.let {
					it.translationY = 0f
					it.translationX = 0f
					it.alpha = 0f
				}
			}
		}
	}

	/**
	 * メインFABのクリックリスナー設定
	 */
	private fun setupMainFabClickListener() {
		binding.fabMain.setOnClickListener {
			toggleFabMenu()
		}
	}

	/**
	 * サブFABのクリックリスナー設定
	 */
	private fun setupSubFabClickListeners() {
		// 新規作成
		binding.fabNewDay.setOnClickListener {
			createNewWorkData()
		}

		// 削除
		binding.fabDelDay.setOnClickListener {
			showDeleteConfirmDialog(viewModel.date.value.toString())
		}

		// 設定
		binding.fabSetting.setOnClickListener {
			navigateToSettings()
		}
	}

	/**
	 * FABメニューの展開/収納を切り替え
	 */
	private fun toggleFabMenu() {
		isFabMenuOpen = !isFabMenuOpen

		if (isFabMenuOpen) {
			showFabMenuWithAnimation()
		} else {
			hideFabMenuWithAnimation()
		}
	}

	/**
	 * FABメニューをアニメーション付きで展開
	 */
	private fun showFabMenuWithAnimation() {
		// アニメーション開始前に初期状態を確実に設定
		listOf(binding.fabNewDay, binding.fabDelDay, binding.fabSetting).forEach { fab ->
			fab.visibility = View.VISIBLE
			fab.alpha = 0f
			fab.scaleX = 0f
			fab.scaleY = 0f
			fab.translationX = 0f
			fab.translationY = 0f
		}

		// ラベルも初期状態を設定
		listOf(binding.labelNewDay, binding.labelDelDay, binding.labelSetting).forEach { label ->
			label?.let {
				it.visibility = View.VISIBLE
				it.alpha = 0f
				it.translationX = 0f
				it.translationY = 0f
			}
		}

//		// fabSettingのラベルは非表示のまま
//		binding.labelSetting?.visibility = View.GONE

		// メインFABの回転アニメーション
		animateMainFabRotation(45f)

		// サブFABのアニメーション（左）- ラベルはさらに左側
		animateSubFabExpansionWithLabel(
			fab = binding.fabNewDay,
			label = binding.labelNewDay,
			fabTranslationX = -FAB_SPACING,
			fabTranslationY = 0f,
			labelPosition = LabelPosition.LEFT,
			delay = 0L
		)

		// サブFABのアニメーション（左上45度）- ラベルは左側
		animateSubFabExpansionWithLabel(
			fab = binding.fabDelDay,
			label = binding.labelDelDay,
			fabTranslationX = -FAB_SPACING * 0.7f,
			fabTranslationY = -FAB_SPACING * 0.7f,
			labelPosition = LabelPosition.LEFT,
			delay = 50L
		)

		// サブFABのアニメーション（上）- ラベルは左側
		animateSubFabExpansionWithLabel(
			fab = binding.fabSetting,
			label = null,
			fabTranslationX = 0f,
			fabTranslationY = -FAB_SPACING,
			labelPosition = LabelPosition.TOP,
			delay = 100L
		)

		// メインFABのアイコンを変更
		binding.fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
	}

	/**
	 * ラベルの位置パターン
	 */
	private enum class LabelPosition {
		LEFT,           // FABの左側
		TOP             // FABの上
	}

	/**
	 * サブFABとラベルを同時に展開
	 */
	private fun animateSubFabExpansionWithLabel(
		fab: View,
		label: View?,
		fabTranslationX: Float,
		fabTranslationY: Float,
		labelPosition: LabelPosition,
		delay: Long
	) {
		// FABのアニメーション
		val translateX = ObjectAnimator.ofFloat(fab, "translationX", 0f, fabTranslationX)
		val translateY = ObjectAnimator.ofFloat(fab, "translationY", 0f, fabTranslationY)
		val fadeIn = ObjectAnimator.ofFloat(fab, "alpha", 0f, 1f)
		val scaleX = ObjectAnimator.ofFloat(fab, "scaleX", 0f, 1f)
		val scaleY = ObjectAnimator.ofFloat(fab, "scaleY", 0f, 1f)

		AnimatorSet().apply {
			playTogether(translateX, translateY, fadeIn, scaleX, scaleY)
			duration = ANIMATION_DURATION
			startDelay = delay
			interpolator = OvershootInterpolator()
			start()
		}

		// ラベルのアニメーション
		label?.let {
			animateLabelWithPosition(it, fabTranslationX, fabTranslationY, labelPosition, delay)
		}
	}

	/**
	 * ラベルの展開アニメーション（位置指定版）
	 */
	private fun animateLabelWithPosition(
		label: View,
		fabTranslationX: Float,
		fabTranslationY: Float,
		labelPosition: LabelPosition,
		delay: Long
	) {
		val labelMargin = LABEL_MARGIN  // ラベルとFABの間のマージン

		// 位置パターンに応じてラベルの座標を計算
		val (labelX, labelY) = when (labelPosition) {
			LabelPosition.LEFT -> {
				// FABの左側
				Pair(fabTranslationX - labelMargin, fabTranslationY)
			}
			LabelPosition.TOP -> {
				// FABの上
				Pair(fabTranslationX, fabTranslationY - labelMargin)
			}
		}

		val translateX = ObjectAnimator.ofFloat(label, "translationX", 0f, labelX)
		val translateY = ObjectAnimator.ofFloat(label, "translationY", 0f, labelY)
		val fadeIn = ObjectAnimator.ofFloat(label, "alpha", 0f, 1f)

		AnimatorSet().apply {
			playTogether(translateX, translateY, fadeIn)
			duration = ANIMATION_DURATION
			startDelay = delay + 50L  // FABより少し遅れて表示
			start()
		}
	}

	/**
	 * FABメニューをアニメーション付きで収納
	 */
	private fun hideFabMenuWithAnimation() {
		// メインFABの回転アニメーション（逆回転）
		animateMainFabRotation(0f)

		// サブFABの収納アニメーション
		animateSubFabCollapse(binding.fabNewDay, binding.labelNewDay, 0L)
		animateSubFabCollapse(binding.fabDelDay, binding.labelDelDay, 50L)
		animateSubFabCollapse(binding.fabSetting, null, 100L)

		// メインFABのアイコンを元に戻す
		binding.fabMain.setImageResource(android.R.drawable.ic_input_add)
	}

	/**
	 * メインFABの回転アニメーション
	 */
	private fun animateMainFabRotation(targetRotation: Float) {
		ObjectAnimator.ofFloat(binding.fabMain, "rotation", targetRotation).apply {
			duration = ANIMATION_DURATION
			start()
		}
	}

	/**
	 * サブFABの展開アニメーション
	 */
	private fun animateSubFabExpansion(
		fab: View,
		label: View?,
		translationX: Float,
		translationY: Float,
		delay: Long
	) {
		// 位置移動アニメーション
		val translateX = ObjectAnimator.ofFloat(fab, "translationX", 0f, translationX)
		val translateY = ObjectAnimator.ofFloat(fab, "translationY", 0f, translationY)

		// フェードインアニメーション
		val fadeIn = ObjectAnimator.ofFloat(fab, "alpha", 0f, 1f)

		// スケールアニメーション（バネのような効果）
		val scaleX = ObjectAnimator.ofFloat(fab, "scaleX", 0f, 1f)
		val scaleY = ObjectAnimator.ofFloat(fab, "scaleY", 0f, 1f)

		AnimatorSet().apply {
			playTogether(translateX, translateY, fadeIn, scaleX, scaleY)
			duration = ANIMATION_DURATION
			startDelay = delay
			interpolator = OvershootInterpolator()
			start()
		}

		// ラベルのアニメーション
		label?.let { animateLabelExpansion(it, translationX, translationY, delay) }
	}

	/**
	 * サブFABの収納アニメーション
	 */
	private fun animateSubFabCollapse(
		fab: View,
		label: View?,
		delay: Long
	) {
		// 位置移動アニメーション（元の位置に戻す）
		val translateX = ObjectAnimator.ofFloat(fab, "translationX", fab.translationX, 0f)
		val translateY = ObjectAnimator.ofFloat(fab, "translationY", fab.translationY, 0f)

		// フェードアウトアニメーション
		val fadeOut = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0f)

		// スケールアニメーション
		val scaleX = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 0f)
		val scaleY = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 0f)

		AnimatorSet().apply {
			playTogether(translateX, translateY, fadeOut, scaleX, scaleY)
			duration = ANIMATION_DURATION
			startDelay = delay
			start()

			// アニメーション終了後に非表示にする
			addListener(object : android.animation.AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: android.animation.Animator) {
					fab.visibility = View.GONE
				}
			})
		}

		// ラベルのアニメーション
		label?.let { animateLabelCollapse(it, delay) }
	}

	/**
	 * ラベルの展開アニメーション
	 */
	private fun animateLabelExpansion(
		label: View,
		translationX: Float,
		translationY: Float,
		delay: Long
	) {
		// ラベルは少し内側に表示（FABより少し近い位置）
		val labelOffsetX = translationX * 0.8f
		val labelOffsetY = translationY * 0.8f

		val translateX = ObjectAnimator.ofFloat(label, "translationX", 0f, labelOffsetX)
		val translateY = ObjectAnimator.ofFloat(label, "translationY", 0f, labelOffsetY)
		val fadeIn = ObjectAnimator.ofFloat(label, "alpha", 0f, 1f)

		AnimatorSet().apply {
			playTogether(translateX, translateY, fadeIn)
			duration = ANIMATION_DURATION
			startDelay = delay + 50L  // FABより少し遅れて表示
			start()
		}
	}

	/**
	 * ラベルの収納アニメーション
	 */
	private fun animateLabelCollapse(label: View, delay: Long) {
		val translateX = ObjectAnimator.ofFloat(label, "translationX", label.translationX, 0f)
		val translateY = ObjectAnimator.ofFloat(label, "translationY", label.translationY, 0f)
		val fadeOut = ObjectAnimator.ofFloat(label, "alpha", 1f, 0f)

		AnimatorSet().apply {
			playTogether(translateX, translateY, fadeOut)
			duration = ANIMATION_DURATION
			startDelay = delay
			start()

			addListener(object : android.animation.AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: android.animation.Animator) {
					label.visibility = View.GONE
				}
			})
		}
	}


	/**
	 * 新規作成処理（一覧画面と同じ挙動）
	 */
	private fun createNewWorkData() {
		// 現在表示されている日付を取得
		val currentDate = viewModel.date.value?.replace("/", "") ?: run {
			showToast("日付情報が取得できません")
			return
		}

		// デフォルト値で時間を設定
		setDefaultTime()

		// データベースに挿入
		val newWorkInfo = createWorkInfo(currentDate)
		viewModel.insertWorkInfo(newWorkInfo)

		showToast("新規データを作成しました")

		// FABメニューを閉じる
		hideFabMenuWithAnimation()
	}

	/**
	 * デフォルトの時間をViewModelに設定
	 */
	private fun setDefaultTime() {
		val (startHour, startMinute) = DEFAULT_START_TIME.split(":")
		val (endHour, endMinute) = DEFAULT_END_TIME.split(":")

		viewModel.setStartHour(startHour)
		viewModel.setStartMinute(startMinute)
		viewModel.setEndHour(endHour)
		viewModel.setEndMinute(endMinute)
	}

	/**
	 * WorkInfoオブジェクトを作成(最低限のものをセット)
	 */
	private fun createWorkInfo(date: String): WorkInfo {
		return WorkInfo(
			date = date,
			startTime = DEFAULT_START_TIME,
			endTime = DEFAULT_END_TIME,
			workingTime = "",
			breakTime = "",
			isHoliday = false,
			isNationalHoliday = false,
			weekday = ""
		)
	}

	/**
	 * 削除確認ダイアログを表示
	 */
	private fun showDeleteConfirmDialog(date: String) {
		AlertDialog.Builder(fragment.requireContext())
			.setTitle("削除確認")
			.setMessage("${viewModel.date.value} のデータを削除しますか?")
			.setPositiveButton("削除") { _, _ ->
				executeDelete(date)
			}
			.setNegativeButton("キャンセル", null)
			.show()
	}

	/**
	 * 削除を実行
	 */
	private fun executeDelete(date: String) {
//		viewModel.deleteWorkInfoByDate(date)
		showToast("データを削除しました")
		hideFabMenuWithAnimation()
	}

	/**
	 * 設定画面への遷移
	 */
	private fun navigateToSettings() {
		// TODO: 設定画面への遷移処理を実装
		// Navigation Componentを使用する場合:
		// findNavController().navigate(R.id.action_home_to_settings)

		// または、インテントで別のActivityを起動する場合:
		// val intent = Intent(fragment.requireContext(), SettingsActivity::class.java)
		// fragment.startActivity(intent)

		showToast("設定画面への遷移（未実装）")
		hideFabMenuWithAnimation()
	}

	/**
	 * Toastメッセージを表示
	 */
	private fun showToast(message: String) {
		Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()
	}

	/**
	 * FABメニューが開いている場合は閉じる
	 * （戻るボタン押下時などに使用）
	 */
	fun closeFabMenuIfOpen(): Boolean {
		return if (isFabMenuOpen) {
			hideFabMenuWithAnimation()
			true
		} else {
			false
		}
	}
}
