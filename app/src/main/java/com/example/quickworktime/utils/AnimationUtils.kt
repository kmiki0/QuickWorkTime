package com.example.quickworktime.utils

import android.animation.ValueAnimator
import android.util.TypedValue
import android.view.View
import android.widget.TextView

// アニメーション関連のユーティリティ関数をまとめたオブジェクト
// シングルトンオブジェクト (new, create キーワードが不要)
object AnimationUtils {

	/**
	 * フォントサイズを徐々に変更するアニメーション
	 * @param textView 操作対象の TextView
	 * @param fromSize 初期倍率
	 * @param toSize 最終倍率
	 * @param duration アニメーション時間（ミリ秒）
	 */
	fun animateFullFontSize(
		textView: TextView,
		fromSize: Float,
		toSize: Float,
		duration: Long = 300
	) {
		val animator = ValueAnimator.ofFloat(fromSize, toSize)
		animator.duration = duration
		animator.addUpdateListener { animation ->
			val animatedValue = animation.animatedValue as Float
			textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue)
		}
		animator.start()
	}
	// Wrapper for animateFullFontSize
	fun animateFullFontSizeWrapper(
		textViewList: List<TextView>,
		toSize: Float,
		duration: Long = 300
	) {
		textViewList.forEach { textView ->
			animateFullFontSize(textView, textView.textSize, toSize, duration)
		}
	}

	/**
	 * テキストのアルファ値（透明度）を変更するアニメーション
	 * @param textView 操作対象の TextView
	 * @param startAlpha 初期アルファ値
	 * @param endAlpha 最終アルファ値
	 */
    fun animateTextAlpha(textView: TextView, startAlpha: Float, endAlpha: Float) {
        val animator = ValueAnimator.ofFloat(startAlpha, endAlpha)
        animator.duration = 500 // アニメーションの時間（ミリ秒）
        animator.addUpdateListener { valueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
			textView.alpha = alpha
        }
        animator.start()
    }
	// Wrapper for animateTextAlpha
	fun animateTextAlphaWrapper(
		textViewList: List<TextView>,
		endAlpha: Float
	) {
		textViewList.forEach { textView ->
			animateTextAlpha(textView, textView.alpha, endAlpha)
		}
	}

	// ビューのフェードインアニメーション
	fun fadeIn(view: View, duration: Long = 300) {
		view.alpha = 0f
		view.visibility = View.VISIBLE
		view.animate()
			.alpha(1f)
			.setDuration(duration)
			.start()
	}

	// ビューのフェードアウトアニメーション
	fun fadeOut(view: View, duration: Long = 300) {
		view.animate()
			.alpha(0f)
			.setDuration(duration)
			.withEndAction { view.visibility = View.GONE }
			.start()
	}

	// ビューをスライドアップさせるアニメーション
	fun slideUp(view: View, duration: Long = 300) {
		view.animate()
			.translationY(0f)
			.setDuration(duration)
			.start()
	}

	// ビューをスライドダウンさせるアニメーション
	fun slideDown(view: View, targetHeight: Float, duration: Long = 300) {
		view.animate()
			.translationY(targetHeight)
			.setDuration(duration)
			.start()
	}
}
