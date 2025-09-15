package com.example.quickworktime.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.quickworktime.databinding.FragmentHomeBinding
import com.example.quickworktime.ui.home.animation.HomeAnimationController
import com.example.quickworktime.ui.home.input.TimeInputHandler
import com.example.quickworktime.ui.home.test.Test
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModelを生成
    private val vm: HomeViewModel by viewModels()

    // アニメーションコントローラー
    private lateinit var animationController: HomeAnimationController

    // 時間入力ハンドラー
    private lateinit var timeInputHandler: TimeInputHandler

    private val testClass: Test by lazy {
        Test()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // ViewModelをバインド
        binding.viewModel = vm
        // ライフサイクル所有者を設定
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // アニメーションコントローラーを初期化
        animationController = HomeAnimationController(binding, vm)

        // 時間入力ハンドラーを初期化
        timeInputHandler = TimeInputHandler(this, binding, vm, animationController)

        // Safe Args で引数を取得
        setDisplayByArgument()

        // GridLayout 非表示
        vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)

        // 状態変化によって、発火するメソッドを設定
        animationController.observeActiveTextStateChanges()

        // GridLayoutのボタンにラベルを設定する
        animationController.showGridLayout()

        binding.constraintLayout.setOnClickListener {
            vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
        }

        // 時間テキストのクリックリスナーを設定
        timeInputHandler.setupTimeTextClickListeners()

        // テストコード
//        executeStep4Tests()
    }

    /**
     * Phase 2 Step 4 テスト実行メソッド
     */
    private fun executeStep4Tests() {
        lifecycleScope.launch {
            try {
                val testClass = Test()

                // 1. UseCase個別テスト（Step 3と同じ、動作確認）
                testClass.quickUseCaseTest()

                // 2. Repository統合テスト（Step 3と同じ、既存機能確認）
                testClass.testRepositoryIntegrationWithContext(requireContext())

                Log.d("Step4Test", "=== Phase 2 Step 4 HomeViewModel動作確認完了 ===")

            } catch (e: Exception) {
                Log.e("Step4Test", "テストエラー: ${e.message}", e)
            }
        }
    }

    private fun setDisplayByArgument() {
        // 値が取れない場合、Null で初期化
        val date: String? = arguments?.getString("date")

        vm.displayData.observe(viewLifecycleOwner) { workInfo ->
            if (workInfo != null) {
                // vm にデータをセット
                vm.setDisplayDate(workInfo)
            }
        }

        vm.getDisplayData(date ?: "")
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment resumes to ensure latest data is displayed
        // This is especially important when app is launched from widget
        val date: String? = arguments?.getString("date")
        vm.getDisplayData(date ?: "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}