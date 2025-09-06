package com.example.quickworktime.ui.workListView

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.quickworktime.databinding.FragmentDashboardBinding
import com.example.quickworktime.ui.workListView.data.DataDisplayManager
import com.example.quickworktime.ui.workListView.menu.DrawerMenuHandler
import com.example.quickworktime.ui.workListView.navigation.MonthNavigationController

class WorkListViewFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val vm: WorkListViewViewModel by viewModels()
    private val binding get() = _binding!!

    // 月次ナビゲーションコントローラー
    private lateinit var monthNavigationController: MonthNavigationController

    // ドロワーメニューハンドラー
    private lateinit var drawerMenuHandler: DrawerMenuHandler

    // データ表示マネージャー
    private lateinit var dataDisplayManager: DataDisplayManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // データ表示マネージャーを初期化
        dataDisplayManager = DataDisplayManager(
            fragment = this,
            binding = binding,
            viewModel = vm,
            lifecycleOwner = viewLifecycleOwner
        )

        // 月次ナビゲーションコントローラーを初期化
        monthNavigationController = MonthNavigationController(
            fragment = this,
            binding = binding,
            viewModel = vm,
            lifecycleOwner = viewLifecycleOwner,
            onDataLoad = ::loadData
        )

        // ドロワーメニューハンドラーを初期化
        drawerMenuHandler = DrawerMenuHandler(this, binding)

        // 各コンポーネントをセットアップ
        dataDisplayManager.setupDataDisplay()
        monthNavigationController.setupMonthNavigation()
        drawerMenuHandler.setupDrawerMenu()

        // 初期データをロード
        loadData()
        // 履歴ボタンの表示非表示の設定
        monthNavigationController.setHistoryButton()
    }

    /**
     * データロード処理（簡略化版）
     */
    private fun loadData() {
        val yyyyMM = monthNavigationController.getCurrentMonth()
        dataDisplayManager.loadData(yyyyMM)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
