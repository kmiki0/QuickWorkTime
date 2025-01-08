package com.example.quickworktime.ui.workListView

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quickworktime.R
import com.example.quickworktime.room.WorkInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.ProgressBar
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.quickworktime.room.WorkSetting
import com.example.quickworktime.room.repository.WorkInfoRepository
import com.example.quickworktime.room.repository.WorkSettingRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TimeListAdapter(
	private val items: MutableList<WorkInfo>,
	private val vm: WorkListViewViewModel
) : RecyclerView.Adapter<TimeListAdapter.TimeViewHolder>() {

	inner class TimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		// 日付
		val dateText: TextView = view.findViewById(R.id.dateText)
		// 合計時間
		val calcText: TextView = view.findViewById(R.id.calcText)
		// 開始終了時間
		val timeText: TextView = view.findViewById(R.id.timeText)
		// プログレスバー
		val progressBar: ProgressBar = view.findViewById(R.id.item_progress)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_time, parent, false)
		return TimeViewHolder(view)
	}

	@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
	override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
		val item = items[position]

		// セット項目の文字列
		val day = item.date.substring(6, 8)
		val week = item.weekday.substring(0, 3)
		// 日付
		holder.dateText.text = "$day th ($week)"
		// 合計時間
		holder.calcText.text = item.workingTime.replace(":", ".") + " h"
		// 開始終了時間
		holder.timeText.text = "${item.startTime} ~ ${item.endTime}"
		holder.itemView.setOnClickListener { btnAction(item, holder.itemView) }

	    // 長押し処理
        var isPressing = false
        val handler = Handler(Looper.getMainLooper())
        var progress = 0

        val updateProgressForItemView = object : Runnable {
            override fun run() {
                if (isPressing) {
                    progress += 5
                    holder.progressBar.progress = progress
                    if (progress < 100) {
                        handler.postDelayed(this, 20)
                    } else {
						handler.removeCallbacks(this)
						progress = 0
						isPressing = false
						holder.progressBar.progress = 0
						holder.progressBar.visibility = View.GONE
						// 長押し時にボトムシートを表示
						showBottomSheet(holder.itemView.context, item, holder)
					}
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            isPressing = true
            holder.progressBar.visibility = View.VISIBLE
            handler.post(updateProgressForItemView)
            true
        }

        holder.itemView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isPressing = false
                handler.removeCallbacks(updateProgressForItemView)
                holder.progressBar.visibility = View.GONE
                progress = 0
                holder.progressBar.progress = 0
            }
            false
        }
	}

	fun updateData(newItems: List<WorkInfo>) {
		(items as? MutableList<WorkInfo>)?.apply {
			clear()
			addAll(newItems)
		}
		notifyDataSetChanged()
	}

	private fun btnAction(item: WorkInfo, view: View) {
		val navController = Navigation.findNavController(view)
		navController.navigate(
			R.id.action_navigation_dashboard_to_navigation_home,
			Bundle().apply {
				putString("date", item.date)
			}
		)
	}

	private fun showBottomSheet(context: Context, workInfo: WorkInfo, holder: TimeViewHolder) {
		val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_menu, null)
		val dialog = BottomSheetDialog(context)
		dialog.setContentView(bottomSheetView)

		// メニューのクリックイベントを設定
		bottomSheetView.findViewById<TextView>(R.id.menu_new)?.setOnClickListener {
			// カレンダーピッカーを表示
			val calendar = Calendar.getInstance()
			val year = calendar.get(Calendar.YEAR)
			val month = calendar.get(Calendar.MONTH)
			val day = calendar.get(Calendar.DAY_OF_MONTH)

			val datePickerDialog = DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
				val formattedDate = String.format("%04d%02d%02d", selectedYear, selectedMonth + 1, selectedDay)


					// すでにデータが存在する場合
//				vm.selctedDataByDate(formattedDate).observe(vm, { isExist ->
//					if (isExist){
//						btnAction(workInfo, holder.itemView)
//						// yyyy/mm/dd 形式で表示
//						Toast.makeText(context, "${workInfo.date.substring(0, 4)}/${workInfo.date.substring(4, 6)}/${workInfo.date.substring(6, 8)} のデータを表示します", Toast.LENGTH_SHORT).show()
//					} else {
//						Toast.makeText(context, "${formattedDate.substring(0, 4)}/${formattedDate.substring(4, 6)}/${formattedDate.substring(6, 8)} を新規作成しました", Toast.LENGTH_SHORT).show()
//					}
//				}

			}, year, month, day)

			datePickerDialog.show()
			dialog.dismiss()
		}

		bottomSheetView.findViewById<TextView>(R.id.menu_edit)?.setOnClickListener {
			btnAction(workInfo, holder.itemView)
			dialog.dismiss()
		}

		bottomSheetView.findViewById<TextView>(R.id.menu_delete)?.setOnClickListener {
			Toast.makeText(context, "長押しで、削除", Toast.LENGTH_SHORT).show()
		}



		// 長押し処理
		var isPressing = false
		val handler = Handler(Looper.getMainLooper())
		var progress = 0
		val progressBar = bottomSheetView.findViewById<ProgressBar>(R.id.menu_delete_progress)

		val updateProgressForDelete = object : Runnable {
			override fun run() {
				if (isPressing) {
					progress += 5
					progressBar.progress = progress
					if (progress < 100) {
						handler.postDelayed(this, 30)
					} else {
						handler.removeCallbacks(this)
						progress = 0
						isPressing = false
						progressBar.progress = 0
						progressBar.visibility = View.GONE
						// 削除ダイアログを表示
						showConfirmationDialog(context, workInfo)
					}
				}
			}
		}

		bottomSheetView.findViewById<TextView>(R.id.menu_delete)?.setOnLongClickListener {
			isPressing = true
			progressBar.visibility = View.VISIBLE
			handler.post(updateProgressForDelete)
			true
		}

		dialog.show()
	}

	private fun showConfirmationDialog(context: Context, workInfo: WorkInfo) {
		AlertDialog.Builder(context).apply {
			val  date = workInfo.date
			setTitle("削除")
			setMessage("${date.substring(0, 4)}/${date.substring(4, 6)}/${date.substring(6, 8)} のデータを削除しますか？")
			setPositiveButton("Yes") { _, _ ->
				// Yesが押されたときの処理
//				val dao = DatabaseProvider.getDatabase(context).workInfoDao()
//				val repository = WorkInfoRepository(dao)
//				CoroutineScope(Dispatchers.IO).launch {
//					repository.deleteWorkInfo(workInfo)
//					withContext(Dispatchers.Main) {
//						Toast.makeText(context, "削除しました", Toast.LENGTH_SHORT).show()
//					}
//				}
			}
			setNegativeButton("No") { dialog, _ ->
				// Noが押されたときの処理
				dialog.dismiss() // ダイアログを閉じる
			}
			setCancelable(true) // ダイアログ外のタップで閉じる
		}.show()
	}

	override fun getItemCount(): Int = items.size
}
