package com.example.quickworktime.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.quickworktime.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TransparentTimePickerActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: TimePickerAdapter
	private lateinit var snapHelper: LinearSnapHelper
	private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
	private var selectedTime = "18:00"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		Log.d("TimePickerActivity", "onCreate called")

		// 透明な背景を設定
		window.setBackgroundDrawableResource(android.R.color.transparent)

		// 背景をタップで閉じられるようにする
		window.setFlags(
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
		)
		window.setFlags(
			WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
			WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
		)

		setContentView(R.layout.activity_transparent_time_picker)

		// Intentからデータを取得
		appWidgetId = intent.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID
		)
		val currentTime = intent.getStringExtra("current_time") ?: "18:00"

		Log.d("TimePickerActivity", "appWidgetId: $appWidgetId, currentTime: $currentTime")

		setupViews(currentTime)
	}

	private fun setupViews(currentTime: String) {
		recyclerView = findViewById(R.id.recyclerView)
		val btnConfirm = findViewById<Button>(R.id.btnConfirm)
		val btnCancel = findViewById<Button>(R.id.btnCancel)
		val backgroundOverlay = findViewById<View>(R.id.backgroundOverlay)

		// 5分刻みの時刻リストを生成 (00:00 - 23:55)
		val items = generateTimeList()

		// レイアウトマネージャーの設定
		val layoutManager = LinearLayoutManager(this)
		recyclerView.layoutManager = layoutManager

		// アダプターの設定
		adapter = TimePickerAdapter(items) { time ->
			selectedTime = time
		}
		recyclerView.adapter = adapter

		// スナップ機能
		snapHelper = LinearSnapHelper()
		snapHelper.attachToRecyclerView(recyclerView)

		// 上下に余白を追加
		recyclerView.addItemDecoration(PickerItemDecoration())

		// スクロールリスナー
		recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)

				if (newState == RecyclerView.SCROLL_STATE_IDLE) {
					val centerView = snapHelper.findSnapView(layoutManager)
					centerView?.let {
						val position = layoutManager.getPosition(it)
						adapter.setSelectedPosition(position)
						selectedTime = items[position]
					}
				}
			}
		})

		// 初期位置を設定
		val initialPosition = items.indexOf(currentTime).takeIf { it >= 0 } ?:
		items.indexOf("18:00").takeIf { it >= 0 } ?: 0
		recyclerView.post {
			recyclerView.scrollToPosition(initialPosition)
			adapter.setSelectedPosition(initialPosition)
		}

		// 確定ボタン
		btnConfirm.setOnClickListener {
			updateWidgetAndFinish()
		}

		// キャンセルボタン
		btnCancel.setOnClickListener {
			finish()
		}

		// 背景をタップで閉じる
		backgroundOverlay.setOnClickListener {
			finish()
		}
	}

	/**
	 * 5分刻みの時刻リストを生成
	 */
	private fun generateTimeList(): List<String> {
		val formatter = DateTimeFormatter.ofPattern("HH:mm")
		val times = mutableListOf<String>()

		var time = LocalTime.of(0, 0)
		repeat(24 * 12) { // 24時間 × 12 (5分刻み)
			times.add(time.format(formatter))
			time = time.plusMinutes(5)
		}

		return times
	}

	private fun updateWidgetAndFinish() {
		// ウィジェットに選択値を送信
		val intent = Intent(this, WorkTimeWidgetProvider::class.java).apply {
			action = WorkTimeWidgetProvider.ACTION_TIME_SELECTED
			putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
			putExtra("selected_time", selectedTime)
		}
		sendBroadcast(intent)

		finish()
	}

	override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
		// ピッカー領域外をタップしたら閉じる
		if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
			finish()
			return true
		}
		return super.onTouchEvent(event)
	}
}