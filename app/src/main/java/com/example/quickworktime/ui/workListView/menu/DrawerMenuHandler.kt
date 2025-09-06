package com.example.quickworktime.ui.workListView.menu

import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.quickworktime.R
import com.example.quickworktime.databinding.FragmentDashboardBinding
import com.example.quickworktime.ui.popup.AttendanceCalculationPopup
import com.example.quickworktime.ui.popup.WorkTimePopup
import com.google.android.material.navigation.NavigationView

/**
 * WorkListViewFragment のドロワーメニュー処理を専門に行うクラス
 * 責任: ドロワーメニューの開閉、メニューアイテム選択、ポップアップ表示
 */
class DrawerMenuHandler(
	private val fragment: Fragment,
	private val binding: FragmentDashboardBinding
) {

	/**
	 * ドロワーメニューのセットアップ
	 */
	fun setupDrawerMenu() {
		val drawerLayout: DrawerLayout = binding.drawerLayout
		val navView: NavigationView = binding.navSideView

		// メニューボタンのクリックリスナー設定
		binding.btnMenu.setOnClickListener {
			drawerLayout.openDrawer(navView)
		}

		// ナビゲーションアイテムの選択リスナー設定
		navView.setNavigationItemSelectedListener { menuItem ->
			handleMenuItemSelection(menuItem.itemId)
		}
	}

	/**
	 * メニューアイテムの選択処理
	 * @param itemId 選択されたメニューアイテムのID
	 * @return 処理が完了した場合true
	 */
	private fun handleMenuItemSelection(itemId: Int): Boolean {
		return when (itemId) {
			R.id.settingWorkTime -> {
				showWorkTimePopup()
				true
			}
			R.id.attendanceCalculation -> {
				showAttendanceCalculationPopup()
				true
			}
			R.id.settingWeekday -> {
				showWeekdaySettingPopup()
				true
			}
			R.id.calBreakTime -> {
				showBreakTimeCalculationPopup()
				true
			}
			R.id.exportCSV -> {
				showCSVExportPopup()
				true
			}
			else -> false
		}
	}

	/**
	 * 勤務時間設定ポップアップを表示
	 */
	private fun showWorkTimePopup() {
		WorkTimePopup(fragment.requireContext()).show()
	}

	/**
	 * 勤怠計算ポップアップを表示
	 */
	private fun showAttendanceCalculationPopup() {
		AttendanceCalculationPopup(fragment.requireContext()).show()
	}

	/**
	 * 出勤曜日設定ポップアップを表示
	 */
	private fun showWeekdaySettingPopup() {
		showCustomPopup(
			layoutResId = R.layout.custom_popup,
			title = "出勤曜日設定",
			message = "出勤曜日を設定してください"
		)
	}

	/**
	 * 休憩時間計算ポップアップを表示
	 */
	private fun showBreakTimeCalculationPopup() {
		showCustomPopup(
			layoutResId = R.layout.custom_popup,
			title = "休憩時間計算",
			message = "休憩時間の計算方法を設定してください"
		)
	}

	/**
	 * CSV出力ポップアップを表示
	 */
	private fun showCSVExportPopup() {
		showCustomPopup(
			layoutResId = R.layout.custom_popup,
			title = "CSV出力",
			message = "勤務データをCSV形式で出力します"
		)
	}

	/**
	 * カスタムポップアップを表示
	 * @param layoutResId レイアウトリソースID
	 * @param title ポップアップタイトル
	 * @param message ポップアップメッセージ
	 */
	private fun showCustomPopup(layoutResId: Int, title: String, message: String = "") {
		val dialogView = LayoutInflater.from(fragment.requireContext()).inflate(layoutResId, null)
		val dialogBuilder = AlertDialog.Builder(fragment.requireContext())
			.setView(dialogView)

		val alertDialog = dialogBuilder.create()
		alertDialog.show()

		// タイトルとメッセージを設定（レイアウトにこれらのViewが存在する場合）
		try {
			dialogView.findViewById<Button>(R.id.popupTitle)?.text = title
			if (message.isNotEmpty()) {
				dialogView.findViewById<Button>(R.id.popupMessage)?.text = message
			}
		} catch (e: Exception) {
			// Viewがレイアウトに存在しない場合は無視
		}

		// OKボタンの処理
		dialogView.findViewById<Button>(R.id.popupButton)?.setOnClickListener {
			alertDialog.dismiss()
		}
	}

	/**
	 * ドロワーメニューを閉じる
	 */
	fun closeDrawer() {
		val drawerLayout: DrawerLayout = binding.drawerLayout
		val navView: NavigationView = binding.navSideView
		if (drawerLayout.isDrawerOpen(navView)) {
			drawerLayout.closeDrawer(navView)
		}
	}

	/**
	 * ドロワーメニューが開いているかチェック
	 */
	fun isDrawerOpen(): Boolean {
		val drawerLayout: DrawerLayout = binding.drawerLayout
		val navView: NavigationView = binding.navSideView
		return drawerLayout.isDrawerOpen(navView)
	}
}