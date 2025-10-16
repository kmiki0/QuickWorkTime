package com.example.quickworktime.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class PickerItemDecoration : RecyclerView.ItemDecoration() {
	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		super.getItemOffsets(outRect, view, parent, state)

		val position = parent.getChildAdapterPosition(view)
		val itemCount = state.itemCount
		val parentHeight = parent.height
		val itemHeight = view.layoutParams.height
		val padding = (parentHeight - itemHeight) / 2

		// 最初と最後のアイテムに余白を追加して中央に来るようにする
		if (position == 0) {
			outRect.top = padding
		}
		if (position == itemCount - 1) {
			outRect.bottom = padding
		}
	}
}