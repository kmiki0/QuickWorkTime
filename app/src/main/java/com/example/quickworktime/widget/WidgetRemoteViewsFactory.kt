package com.example.quickworktime.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.quickworktime.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * StackView用のデータプロバイダー
 */
class WidgetRemoteViewsFactory(
	private val context: Context,
	intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

	private val appWidgetId: Int = intent.getIntExtra(
		AppWidgetManager.EXTRA_APPWIDGET_ID,
		AppWidgetManager.INVALID_APPWIDGET_ID
	)

	private val timeList = mutableListOf<String>()
	private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

	override fun onCreate() {
		// 00:00 から 23:55 までの5分刻みの時刻を生成
		generateTimeList()
	}

	override fun onDataSetChanged() {
		// データの更新が必要な場合
		// 現在は固定リストなので特に処理なし
	}

	override fun onDestroy() {
		timeList.clear()
	}

	override fun getCount(): Int = timeList.size

	override fun getViewAt(position: Int): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_time_list_item)
		views.setTextViewText(R.id.widget_time_item_text, timeList[position])

		// アイテムクリック時のIntent設定
		val fillInIntent = Intent()
		fillInIntent.putExtra("selected_time", timeList[position])
		fillInIntent.putExtra("position", position)
		views.setOnClickFillInIntent(R.id.widget_time_item_text, fillInIntent)

		return views
	}

	override fun getLoadingView(): RemoteViews? = null

	override fun getViewTypeCount(): Int = 1

	override fun getItemId(position: Int): Long = position.toLong()

	override fun hasStableIds(): Boolean = true

	/**
	 * 5分刻みの時刻リストを生成
	 */
	private fun generateTimeList() {
		timeList.clear()
		for (hour in 0..23) {
			for (minute in 0..55 step 5) {
				val time = LocalTime.of(hour, minute)
				timeList.add(time.format(timeFormatter))
			}
		}
	}
}