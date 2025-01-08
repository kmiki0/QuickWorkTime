package com.example.quickworktime.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import org.w3c.dom.Text
import kotlin.random.Random

class ParticleAnimationView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : View(context, attrs) {

	private val particles = mutableListOf<Particle>()
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private var onAnimationEnd: (() -> Unit)? = null

	private var animationProgress = 0f // アニメーション進行度 (0-1)
	private val duration = 10000000 // アニメーションの時間（ミリ秒）
	private var startTime: Long = 0 // アニメーション開始時刻

	private var spreadFactor = 0f  // 粒子の初期速度
	private var speedFactor = 60f  // 粒子の広がり具合
	private var friction = 0.95f    // 減速率
//	private var initialSize = 2f  // 粒子の初期サイズ

	private data class Particle(
		var x: Float,
		var y: Float,
		var vx: Float,
		var vy: Float,
		var color: Int,
		var alpha: Int
	)

	// アニメーション終了時のコールバックを設定
	fun setOnAnimationEndListener(listener: () -> Unit) {
		onAnimationEnd = listener
	}

	// EaseInBack イージング関数
	private fun easeInBack(x: Float): Float {
		val c1 = 1.70158f
		val c3 = c1 + 1

		return c3 * x * x * x - c1 * x * x
	}

	// テキストをもとにしたアニメーション開始
	fun startAnimation(textView: TextView, touchX: Float, touchY: Float, text: String, skip: Int = 2) {
		particles.clear()
		val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			textSize = textView.textSize
			color = textView.currentHintTextColor
		}

		// テキストのビットマップを生成
		val textBitmap = createTextBitmap(textView)

		for (px in 0 until textBitmap.width step skip) {
			for (py in 0 until textBitmap.height step skip) {
				val color = textBitmap.getPixel(px, py)
				if (Color.alpha(color) != 0) {
					// ランダムに広がりを持たせて初期位置を設定
					val initialX = touchX + px + (Random.nextFloat() - 0.5f) * spreadFactor
					val initialY = touchY + py + (Random.nextFloat() - 0.5f) * spreadFactor
//					val initialX = touchX + (Random.nextFloat() - 0.5f)
//					val initialY = touchY + (Random.nextFloat() - 0.5f)

					// ランダムな速度を設定
					val initialVx = (Random.nextFloat() * speedFactor - speedFactor / 2)
					val initialVy = (Random.nextFloat() * speedFactor - speedFactor / 2)

					particles.add(
						Particle(
							initialX,
							initialY,
//							Random.nextFloat() * 10 - 5,
//							Random.nextFloat() * 10 - 5,
							initialVx,
							initialVy,
							color,
							255
						)
					)
				}
			}
		}
		invalidate()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		if (particles.isEmpty()) return

		val elapsed = System.currentTimeMillis()
		animationProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

		// イージング適用後の進行度を取得
		val easedProgress = easeInBack(animationProgress)

		val iterator = particles.iterator()
		while (iterator.hasNext()) {
			val particle = iterator.next()

			// 粒子の色をアルファ値に基づいて変化させる
			paint.color = Color.argb(
				particle.alpha,
				Color.red(particle.color),
				Color.green(particle.color),
				Color.blue(particle.color)
			)

			// 粒子を描画（透明度に応じてサイズを調整）
			val size = particle.alpha / 255f // 初期サイズに基づく
			canvas.drawCircle(
				particle.x + (particle.vx * easedProgress),
				particle.y + (particle.vy * easedProgress),
//				particle.x + easedProgress,
//				particle.y + easedProgress,
				size,
				paint
			)

			// 粒子の位置を更新
			particle.x += particle.vx
			particle.y += particle.vy

			// 粒子の速度を徐々に減速
			particle.vx *= friction // X方向の減速
			particle.vy *= friction // Y方向の減速

			// 粒子の透明度を減少させる
			val fadeSpeed = 3 // ここで透明度の減少速度を調整
			particle.alpha = (particle.alpha - fadeSpeed).coerceAtLeast(0)


			// 粒子が完全に透明になったら削除
			if (particle.alpha <= 0.4) {
				iterator.remove()
			}
		}

		// 粒子がまだ残っている場合は再描画
		if (particles.isNotEmpty()) {
			invalidate() // 次のフレームを要求
		} else {
			onAnimationEnd?.invoke() // 全粒子が消えた後にコールバック
		}
	}

	private fun createTextBitmap(textView: TextView): Bitmap //		canvas.drawText(text, 0f, height.toFloat(), paint)
	{
		val width = textView.width
		val height = textView.height
		val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)
		textView.draw(canvas)
		return bitmap
	}
}

