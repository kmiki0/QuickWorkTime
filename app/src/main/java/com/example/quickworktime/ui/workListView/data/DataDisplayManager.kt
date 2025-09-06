package com.example.quickworktime.ui.workListView.data

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quickworktime.databinding.FragmentDashboardBinding
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.ui.workListView.TimeListAdapter
import com.example.quickworktime.ui.workListView.WorkListViewViewModel

/**
 * WorkListViewFragment のデータロード・表示処理を専門に行うクラス
 * 責任: RecyclerView管理、データロード、合計時間計算、LiveData観測
 */
class DataDisplayManager(
	private val fragment: Fragment,
	private val binding: FragmentDashboardBinding,
	private val viewModel: WorkListViewViewModel,
	private val lifecycleOwner: LifecycleOwner
) {

	private lateinit var timeListAdapter: TimeListAdapter

	/**
	 * データ表示の初期セットアップ
	 */
	fun setupDataDisplay() {
		setupRecyclerView()
		observeDataChanges()
	}

	/**
	 * RecyclerViewとAdapterの初期化
	 */
	private fun setupRecyclerView() {
		val recyclerView: RecyclerView = binding.timeList

		// 空のリストで Adapter を初期化
		timeListAdapter = TimeListAdapter(mutableListOf(), viewModel)
		recyclerView.layoutManager = LinearLayoutManager(fragment.requireContext())
		recyclerView.adapter = timeListAdapter
	}

	/**
	 * ViewModelのLiveDataを観測してデータ変更に対応
	 */
	private fun observeDataChanges() {
		// Roomの変更を検知し、変更時にUI内容を変更する
		viewModel.listData.observe(lifecycleOwner) { workInfoList ->
			workInfoList?.let { dataList ->
				updateDisplayWithData(dataList)
			}
		}
	}

	/**
	 * 指定した年月のデータをロード
	 * @param yyyyMM 年月（yyyyMM形式）
	 */
	fun loadData(yyyyMM: String) {
		viewModel.getListData(yyyyMM)

		// 既存データがある場合は即座に表示更新
		viewModel.listData.value?.let { existingData ->
			updateDisplayWithData(existingData)
		}
	}

	/**
	 * データを受け取ってUI表示を更新
	 * @param workInfoList 勤務情報リスト
	 */
	private fun updateDisplayWithData(workInfoList: List<WorkInfo>) {
		// Adapter にデータを更新
		timeListAdapter.updateData(workInfoList)

		// 合計時間を計算して表示
		val totalHours = calculateTotalWorkingTime(workInfoList)
		binding.totalTimeText.text = "${totalHours} h"
	}

	/**
	 * 勤務時間の合計を計算
	 * @param workInfoList 勤務情報リスト
	 * @return 合計時間（小数点形式の文字列）
	 */
	private fun calculateTotalWorkingTime(workInfoList: List<WorkInfo>): String {
		var totalSum = 0.0f

		for (workInfo in workInfoList) {
			try {
				// workingTime は "HH:mm" 形式なので、":" で分割して Float に変換
				val timeParts = workInfo.workingTime.split(":")
				if (timeParts.size >= 2) {
					val hours = timeParts[0].toFloatOrNull() ?: 0f
					val minutes = timeParts[1].toFloatOrNull() ?: 0f

					// "HH:mm" を "HH.mm" 形式として計算
					totalSum += hours + (minutes / 100f)
				}
			} catch (e: Exception) {
				// エラーの場合は該当項目をスキップ
				continue
			}
		}

		return String.format("%.2f", totalSum)
	}

	/**
	 * 現在表示中のデータを取得
	 * @return 現在のワークリスト
	 */
	fun getCurrentWorkInfoList(): List<WorkInfo>? {
		return viewModel.listData.value
	}

	/**
	 * アダプターを取得（外部からのアクセス用）
	 * @return TimeListAdapter
	 */
	fun getAdapter(): TimeListAdapter {
		return timeListAdapter
	}

	/**
	 * RecyclerViewを取得（外部からのアクセス用）
	 * @return RecyclerView
	 */
	fun getRecyclerView(): RecyclerView {
		return binding.timeList
	}

	/**
	 * 表示をリフレッシュ
	 */
	fun refreshDisplay() {
		viewModel.listData.value?.let { currentData ->
			updateDisplayWithData(currentData)
		}
	}

	/**
	 * データ表示をクリア
	 */
	fun clearDisplay() {
		timeListAdapter.updateData(emptyList())
		binding.totalTimeText.text = "0.00 h"
	}
}