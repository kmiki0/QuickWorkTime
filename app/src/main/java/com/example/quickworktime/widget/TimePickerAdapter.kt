package com.example.quickworktime.widget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quickworktime.R

class TimePickerAdapter(
	private val items: List<String>,
	private val onItemSelected: (String) -> Unit
) : RecyclerView.Adapter<TimePickerAdapter.ViewHolder>() {

	private var selectedPosition = RecyclerView.NO_POSITION

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val textView: TextView = view.findViewById(R.id.widget_time_text)  // 既存のIDを使用
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.widget_time_list_item, parent, false)  // 既存のレイアウトを使用
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = items[position]
		holder.textView.text = item

		// 選択されたアイテムを強調表示
		if (position == selectedPosition) {
			holder.textView.textSize = 32f
			holder.textView.setTextColor(Color.parseColor("#3F51B5"))
			holder.textView.alpha = 1.0f
		} else {
			holder.textView.textSize = 24f
			holder.textView.setTextColor(Color.GRAY)
			holder.textView.alpha = 0.5f
		}
	}

	override fun getItemCount() = items.size

	fun setSelectedPosition(position: Int) {
		val previousPosition = selectedPosition
		selectedPosition = position

		// 変更があった部分のみ更新
		notifyItemChanged(previousPosition)
		notifyItemChanged(selectedPosition)

		if (position != RecyclerView.NO_POSITION) {
			onItemSelected(items[position])
		}
	}
}