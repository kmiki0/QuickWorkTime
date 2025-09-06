package com.example.quickworktime.ui.home.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import com.example.quickworktime.databinding.FragmentHomeBinding
import com.example.quickworktime.ui.home.HomeViewModel
import com.example.quickworktime.utils.AnimationUtils
import com.example.quickworktime.view.ParticleAnimationView

/**
 * HomeFragment のアニメーション制御を専門に行うクラス
 * 責任: UI アニメーション、制約レイアウト変更、パーティクルエフェクト
 */
class HomeAnimationController(
	private val binding: FragmentHomeBinding,
	private val viewModel: HomeViewModel
) {

	companion object {
		const val TXT_SIZE_MIN = 200f
		const val TXT_SIZE_NORMAL = 300f
		const val TXT_SIZE_MAX = 400f
	}

	/**
	 * GridLayout の表示/非表示アニメーション
	 */
	fun showGridLayout() {
		if (viewModel.activeTextState.value == HomeViewModel.ActiveText.DEFAULT) {
			// 非表示アニメーション
			val slideDown = ObjectAnimator.ofFloat(
				binding.gridBtnLayout,
				"translationY",
				0f,
				binding.gridBtnLayout.height.toFloat()
			)
			slideDown.duration = 300
			slideDown.start()

			// アニメーション終了後に非表示にする
			slideDown.addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					binding.gridBtnLayout.visibility = View.GONE
				}
			})
		} else {
			// 表示アニメーション
			val slideUp = ObjectAnimator.ofFloat(
				binding.gridBtnLayout,
				"translationY",
				binding.gridBtnLayout.height.toFloat(),
				0f
			)

			// アニメーション開始時に表示する
			slideUp.addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationStart(animation: Animator) {
					binding.gridBtnLayout.visibility = View.VISIBLE
				}
			})
			slideUp.duration = 300
			slideUp.start()
		}
	}

	/**
	 * TextViewからパーティクルアニメーションを開始
	 */
	fun startParticleAnimationFromTextView(particleView: ParticleAnimationView, textView: TextView) {
		textView.visibility = View.INVISIBLE // アニメーション後に非表示
		particleView.startAnimation(textView, textView.x, textView.y, textView.text.toString())
	}

	/**
	 * アクティブテキスト状態変化時のアニメーション制御
	 */
	fun observeActiveTextStateChanges() {
		val minAlpha = 0.5f
		val maxAlpha = 1.0f

		// Active状態のテキストを監視
		viewModel.activeTextState.observe(binding.lifecycleOwner!!) { activeText ->
			when (activeText) {
				HomeViewModel.ActiveText.START -> {
					handleStartTimeActive(minAlpha)
				}
				HomeViewModel.ActiveText.END -> {
					handleEndTimeActive(minAlpha)
				}
				else -> { // DEFAULT
					handleDefaultState(maxAlpha)
				}
			}
		}
	}

	/**
	 * START時間がアクティブな場合の処理
	 */
	private fun handleStartTimeActive(minAlpha: Float) {
		// EndTime の パーティクルアニメーションを開始
		startParticleAnimationFromTextView(binding.particleView1, binding.labelTimeEnd)
		startParticleAnimationFromTextView(binding.particleView2, binding.textTimeEndHH)
		startParticleAnimationFromTextView(binding.particleView3, binding.labelTimeEndDelimiter)
		startParticleAnimationFromTextView(binding.particleView4, binding.textTimeEndMM)

		// StartTime サイズを通常に
		AnimationUtils.animateFullFontSizeWrapper(
			listOf(binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
			TXT_SIZE_NORMAL
		)

		// StartTime 透過度 > 0.5
		AnimationUtils.animateTextAlphaWrapper(
			listOf(binding.labelTimeStart, binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
			minAlpha
		)

		// StartTimeの位置を中央に移動
		setConstraintBottom(binding.labelTimeStartDelimiter, true)
		showGridLayout()
	}

	/**
	 * END時間がアクティブな場合の処理
	 */
	private fun handleEndTimeActive(minAlpha: Float) {
		// StartTime の パーティクルアニメーションを開始
		startParticleAnimationFromTextView(binding.particleView1, binding.labelTimeStart)
		startParticleAnimationFromTextView(binding.particleView2, binding.textTimeStartHH)
		startParticleAnimationFromTextView(binding.particleView3, binding.labelTimeStartDelimiter)
		startParticleAnimationFromTextView(binding.particleView4, binding.textTimeStartMM)

		// EndTime サイズを通常に
		AnimationUtils.animateFullFontSizeWrapper(
			listOf(binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
			TXT_SIZE_NORMAL
		)

		// EndTime 透過度 > 0.5
		AnimationUtils.animateTextAlphaWrapper(
			listOf(binding.labelTimeEnd, binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
			minAlpha
		)

		// EndTimeの位置を中央に移動
		setConstraintBottom(binding.labelTimeEndDelimiter, true)
		showGridLayout()
	}

	/**
	 * デフォルト状態の処理
	 */
	private fun handleDefaultState(maxAlpha: Float) {
		// StartTime の復元
		restoreTextViewsIfInvisible(
			listOf(binding.labelTimeStart, binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
			maxAlpha
		)

		// EndTime の復元
		restoreTextViewsIfInvisible(
			listOf(binding.labelTimeEnd, binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
			maxAlpha
		)

		// サイズを通常に戻す
		AnimationUtils.animateFullFontSizeWrapper(
			listOf(binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
			TXT_SIZE_NORMAL
		)
		AnimationUtils.animateFullFontSizeWrapper(
			listOf(binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
			TXT_SIZE_NORMAL
		)

		// レイアウト位置を元に戻す
		setConstraintBottom(binding.labelTimeStartDelimiter, false)
		setConstraintBottom(binding.labelTimeEndDelimiter, false)
		showGridLayout()
	}

	/**
	 * TextViewが非表示の場合に復元する
	 */
	private fun restoreTextViewsIfInvisible(textViews: List<TextView>, targetAlpha: Float) {
		textViews.forEach { textView ->
			if (textView.visibility == View.INVISIBLE) {
				textView.alpha = 0.0f
				textView.visibility = View.VISIBLE
			}
		}
		AnimationUtils.animateTextAlphaWrapper(textViews, targetAlpha)
	}

	/**
	 * 制約レイアウトの下部制約を変更
	 */
	fun setConstraintBottom(textView: TextView, bln: Boolean) {
		var bias: Float = 0f
		var changeTarget: Int = 0

		// TextActive が START, END の場合 True
		if (bln) {
			bias = 0.6f
			changeTarget = binding.gridBtnLayout.id
		} else {
			bias = 0.3f
			changeTarget = binding.guideline.id
		}

		val rootLayout = binding.constraintLayout
		val constraintSet = ConstraintSet()
		constraintSet.clone(rootLayout) // 現在のレイアウトをコピー

		if (textView.id == binding.labelTimeEndDelimiter.id) {
			if (bln) {
				// TextView の上方向の制約
				constraintSet.connect(
					textView.id, // 対象のビューID
					ConstraintSet.TOP, // 変更する制約の側 (Top)
					binding.constraintLayout.id, // 接続先のビューID
					ConstraintSet.TOP, // 接続先の側 (Top)
				)
			} else {
				// TextView の上方向の制約
				constraintSet.connect(
					textView.id, // 対象のビューID
					ConstraintSet.TOP, // 変更する制約の側 (Top)
					binding.labelTimeStartDelimiter.id, // 接続先のビューID
					ConstraintSet.BOTTOM, // 接続先の側 (Bottom)
				)
			}
		}

		// TextView の下方向の制約
		constraintSet.connect(
			textView.id, // 対象のビューID
			ConstraintSet.BOTTOM, // 変更する制約の側 (Bottom)
			changeTarget, // 接続先のビューID
			ConstraintSet.TOP, // 接続先の側 (Top)
		)

		// layout_constraintVertical_bias を変更
		constraintSet.setVerticalBias(textView.id, bias) // 垂直方向バイアス 設定

		// アニメーションを適用
		val transition = ChangeBounds()
		transition.duration = 500
		TransitionManager.beginDelayedTransition(rootLayout, transition)
		constraintSet.applyTo(rootLayout)
	}

	/**
	 * 時間テキストのアニメーション（START時間用）
	 */
	fun animateStartTimeText(targetView: TextView, isMaxSize: Boolean) {
		if (isMaxSize) {
			// 対象のテキストを最大サイズに
			AnimationUtils.animateFullFontSize(targetView, targetView.textSize, TXT_SIZE_MAX)
			AnimationUtils.animateTextAlpha(targetView, targetView.alpha, 1.0f)

			// 他方を通常サイズに
			val otherView = if (targetView.id == binding.textTimeStartHH.id)
				binding.textTimeStartMM else binding.textTimeStartHH
			AnimationUtils.animateFullFontSize(otherView, otherView.textSize, TXT_SIZE_NORMAL)
			AnimationUtils.animateTextAlpha(otherView, otherView.alpha, 0.5f)
		} else {
			// 対象のテキストを最大サイズに
			AnimationUtils.animateFullFontSize(targetView, targetView.textSize, TXT_SIZE_MAX)
			AnimationUtils.animateTextAlpha(targetView, targetView.alpha, 1.0f)
		}
	}

	/**
	 * 時間テキストのアニメーション（END時間用）
	 */
	fun animateEndTimeText(targetView: TextView, isMaxSize: Boolean) {
		if (isMaxSize) {
			// 対象のテキストを最大サイズに
			AnimationUtils.animateFullFontSize(targetView, targetView.textSize, TXT_SIZE_MAX)
			AnimationUtils.animateTextAlpha(targetView, targetView.alpha, 1.0f)

			// 他方を通常サイズに
			val otherView = if (targetView.id == binding.textTimeEndHH.id)
				binding.textTimeEndMM else binding.textTimeEndHH
			AnimationUtils.animateFullFontSize(otherView, otherView.textSize, TXT_SIZE_NORMAL)
			AnimationUtils.animateTextAlpha(otherView, otherView.alpha, 0.5f)
		} else {
			// 対象のテキストを最大サイズに
			AnimationUtils.animateFullFontSize(targetView, targetView.textSize, TXT_SIZE_MAX)
			AnimationUtils.animateTextAlpha(targetView, targetView.alpha, 1.0f)
		}
	}
}
