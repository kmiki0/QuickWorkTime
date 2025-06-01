package com.example.quickworktime.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.transition.ChangeBounds
import android.transition.TransitionManager
import androidx.constraintlayout.widget.ConstraintSet
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.quickworktime.databinding.FragmentHomeBinding
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.repository.WorkInfoRepository
import com.example.quickworktime.utils.AnimationUtils
import com.example.quickworktime.view.ParticleAnimationView
import kotlinx.coroutines.CoroutineScope

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val txtSizeMin: Float = 200f
    val txtSizeNormal: Float = 300f
    val txtSizeMax: Float = 400f

    // ViewModelを生成
    private val vm: HomeViewModel by viewModels()

    // FAB関連のフラグ
    private var isFabOpen = false

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

        // Safe Args で引数を取得
        setDisplayByArgument()

        // GridLayout 非表示
        vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
        // 状態変化によって、発火するメソッド
        onActiveTextStateChanged()
        // GridLayoutのボタンにラベルを設定する
        showGridLayout()

        // ★ FAB関連のセットアップを追加
        setupFloatingActionButtons()

        binding.constraintLayout.setOnClickListener{
            vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            // FABメニューが開いている場合は閉じる
            if (isFabOpen) {
                closeFabMenu()
            }
        }

        // 既存のTextViewクリックイベント（省略 - 元のコードと同じ）
        setupTextViewClickEvents()
    }

    /**
     * ★ フローティングボタンのセットアップ
     */
    private fun setupFloatingActionButtons() {
        // メインFABのクリックイベント
        binding.fabMain.setOnClickListener {
            if (isFabOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

        // サブFABのクリックイベント
        binding.fabAddPerson.setOnClickListener {
            handleFabClick("Person")
            closeFabMenu()
        }

        binding.fabAddAlarm.setOnClickListener {
            handleFabClick("Alarm")
            closeFabMenu()
        }

        binding.fabAddItem.setOnClickListener {
            handleFabClick("Item")
            closeFabMenu()
        }

        // ラベルのクリックイベント（FABと同じ動作）
        binding.labelAddPerson.setOnClickListener {
            handleFabClick("Person")
            closeFabMenu()
        }

        binding.labelAddAlarm.setOnClickListener {
            handleFabClick("Alarm")
            closeFabMenu()
        }

        binding.labelAddItem.setOnClickListener {
            handleFabClick("Item")
            closeFabMenu()
        }
    }

    companion object {
        private const val FAB_DISTANCE = 180f  // FABからの距離
        private const val LABEL_OFFSET = 150f  // ラベルのオフセット
    }

    private fun openFabMenu() {
        isFabOpen = true

        // メインFABを回転させる
        val mainFabRotation = ObjectAnimator.ofFloat(binding.fabMain, "rotation", 0f, 45f)
        mainFabRotation.duration = 300

        // 角度と距離からサブFABの位置を計算
        val fabConfigs = listOf(
            // 左 (180度)
            createFabConfig(
                fab = binding.fabAddItem,
                label = binding.labelAddItem,
                angle = 180f,
                distance = FAB_DISTANCE
            ),
            // 左上45度 (135度)
            createFabConfig(
                fab = binding.fabAddAlarm,
                label = binding.labelAddAlarm,
                angle = 135f,
                distance = FAB_DISTANCE
            ),
            // 上 (90度)
            createFabConfig(
                fab = binding.fabAddPerson,
                label = binding.labelAddPerson,
                angle = 90f,
                distance = FAB_DISTANCE
            )
        )

        // アニメーション実行
        fabConfigs.forEachIndexed { index, config ->
            animateFabOpen(config, index * 80L)
        }

        mainFabRotation.start()
    }

    /**
     * 角度と距離からFAB設定を作成
     */
    private fun createFabConfig(
        fab: com.google.android.material.floatingactionbutton.FloatingActionButton,
        label: TextView,
        angle: Float,
        distance: Float
    ): FabAnimationConfig {
        val radians = Math.toRadians(angle.toDouble())
        val deltaX = (distance * Math.cos(radians)).toFloat()
        val deltaY = -(distance * Math.sin(radians)).toFloat() // Y軸は上が負

        // ラベルのオフセット計算（FABの左側に配置）
        val labelOffsetX = when {
            angle > 90f && angle < 270f -> -LABEL_OFFSET // 左側の場合
            angle < 90f || angle > 270f -> LABEL_OFFSET  // 右側の場合
            else -> 0f // 真上・真下の場合
        }

        return FabAnimationConfig(
            fab = fab,
            label = label,
            fabX = deltaX,
            fabY = deltaY,
            labelX = deltaX + labelOffsetX,
            labelY = deltaY
        )
    }

    /**
     * FAB展開アニメーション
     */
    private fun animateFabOpen(config: FabAnimationConfig, delay: Long) {
        // 表示
        config.fab.visibility = View.VISIBLE
        config.label.visibility = View.VISIBLE

        // 初期位置設定
        config.fab.translationX = 0f
        config.fab.translationY = 0f
        config.fab.alpha = 0f
        config.fab.scaleX = 0f
        config.fab.scaleY = 0f

        config.label.translationX = 0f
        config.label.translationY = 0f
        config.label.alpha = 0f
        config.label.scaleX = 0f
        config.label.scaleY = 0f

        // FABのアニメーション
        val fabTranslateX = ObjectAnimator.ofFloat(config.fab, "translationX", 0f, config.fabX)
        val fabTranslateY = ObjectAnimator.ofFloat(config.fab, "translationY", 0f, config.fabY)
        val fabAlpha = ObjectAnimator.ofFloat(config.fab, "alpha", 0f, 1f)
        val fabScaleX = ObjectAnimator.ofFloat(config.fab, "scaleX", 0f, 1f)
        val fabScaleY = ObjectAnimator.ofFloat(config.fab, "scaleY", 0f, 1f)

        val fabAnimatorSet = AnimatorSet()
        fabAnimatorSet.playTogether(fabTranslateX, fabTranslateY, fabAlpha, fabScaleX, fabScaleY)
        fabAnimatorSet.duration = 300
        fabAnimatorSet.startDelay = delay

        // ラベルのアニメーション
        val labelTranslateX = ObjectAnimator.ofFloat(config.label, "translationX", 0f, config.labelX)
        val labelTranslateY = ObjectAnimator.ofFloat(config.label, "translationY", 0f, config.labelY)
        val labelAlpha = ObjectAnimator.ofFloat(config.label, "alpha", 0f, 1f)
        val labelScaleX = ObjectAnimator.ofFloat(config.label, "scaleX", 0f, 1f)
        val labelScaleY = ObjectAnimator.ofFloat(config.label, "scaleY", 0f, 1f)

        val labelAnimatorSet = AnimatorSet()
        labelAnimatorSet.playTogether(labelTranslateX, labelTranslateY, labelAlpha, labelScaleX, labelScaleY)
        labelAnimatorSet.duration = 300
        labelAnimatorSet.startDelay = delay + 100

        fabAnimatorSet.start()
        labelAnimatorSet.start()
    }

    /**
     * FAB設定用のデータクラス
     */
    private data class FabAnimationConfig(
        val fab: com.google.android.material.floatingactionbutton.FloatingActionButton,
        val label: TextView,
        val fabX: Float,
        val fabY: Float,
        val labelX: Float,
        val labelY: Float
    )

    /**
     * FABメニューを閉じる
     */
    private fun closeFabMenu() {
        isFabOpen = false

        val mainFabRotation = ObjectAnimator.ofFloat(binding.fabMain, "rotation", 45f, 0f)
        mainFabRotation.duration = 300

        val fabData = listOf(
            Triple(binding.fabAddItem, binding.labelAddItem, Pair(-80f, 0f)),
            Triple(binding.fabAddAlarm, binding.labelAddAlarm, Pair(-56.6f, -56.6f)),
            Triple(binding.fabAddPerson, binding.labelAddPerson, Pair(0f, -80f))
        )

        fabData.forEachIndexed { index, (fab, label, position) ->
            // ★ 重要: 逆順のdelayを計算
            val delay = (fabData.size - 1 - index) * 50L

            // FABのアニメーション
            val fabTranslateX = ObjectAnimator.ofFloat(fab, "translationX", fab.translationX, 0f)
            val fabTranslateY = ObjectAnimator.ofFloat(fab, "translationY", fab.translationY, 0f)
            val fabAlpha = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0f)
            val fabScaleX = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 0f)
            val fabScaleY = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 0f)

            val fabAnimatorSet = AnimatorSet()
            fabAnimatorSet.playTogether(fabTranslateX, fabTranslateY, fabAlpha, fabScaleX, fabScaleY)
            fabAnimatorSet.duration = 200
            fabAnimatorSet.startDelay = delay

            // ラベルのアニメーション
            val labelTranslateX = ObjectAnimator.ofFloat(label, "translationX", label.translationX, 0f)
            val labelTranslateY = ObjectAnimator.ofFloat(label, "translationY", label.translationY, 0f)
            val labelAlpha = ObjectAnimator.ofFloat(label, "alpha", 1f, 0f)
            val labelScaleX = ObjectAnimator.ofFloat(label, "scaleX", 1f, 0f)
            val labelScaleY = ObjectAnimator.ofFloat(label, "scaleY", 1f, 0f)

            val labelAnimatorSet = AnimatorSet()
            labelAnimatorSet.playTogether(labelTranslateX, labelTranslateY, labelAlpha, labelScaleX, labelScaleY)
            labelAnimatorSet.duration = 200
            labelAnimatorSet.startDelay = delay

            // アニメーション終了後に非表示
            fabAnimatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fab.visibility = View.GONE
                }
            })

            labelAnimatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    label.visibility = View.GONE
                }
            })

            fabAnimatorSet.start()
            labelAnimatorSet.start()
        }

        mainFabRotation.start()
    }

    /**
     * FABクリック時の処理
     */
    private fun handleFabClick(action: String) {
        // 振動フィードバック
        binding.fabMain.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        when (action) {
            "Person" -> {
                Toast.makeText(requireContext(), "Add Person clicked", Toast.LENGTH_SHORT).show()
                // ここに実際の処理を追加
            }
            "Alarm" -> {
                Toast.makeText(requireContext(), "Add Alarm clicked", Toast.LENGTH_SHORT).show()
                // ここに実際の処理を追加
            }
            "Item" -> {
                Toast.makeText(requireContext(), "Add Item clicked", Toast.LENGTH_SHORT).show()
                // ここに実際の処理を追加
            }
        }
    }

    /**
     * 既存のTextViewクリックイベント処理をまとめたメソッド
     */
    private fun setupTextViewClickEvents() {
        /**
         * StartTime (HH) タップ イベント
         */
        binding.textTimeStartHH.setOnClickListener {
            val state = vm.activeTextState.value
            val txtView = binding.textTimeStartHH
            vm.setActiveTextView(binding.textTimeStartHH)

            if (state == HomeViewModel.ActiveText.START && txtView.textSize == txtSizeMax) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.START && txtView.textSize == txtSizeNormal) {
                AnimationUtils.animateFullFontSize(binding.textTimeStartHH, binding.textTimeStartHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartHH, binding.textTimeStartHH.alpha, 1.0f)
                AnimationUtils.animateFullFontSize(binding.textTimeStartMM, binding.textTimeStartMM.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeStartMM, binding.textTimeStartMM.alpha, 0.5f)
                setGridBtnLabel(0)
                vm.setActiveTextView(binding.textTimeStartHH)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.START)
                AnimationUtils.animateFullFontSize(binding.textTimeStartHH, binding.textTimeStartHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartHH, binding.textTimeStartHH.alpha, 1.0f)
                setGridBtnLabel(0)
                vm.setActiveTextView(binding.textTimeStartHH)
            } else if ( state == HomeViewModel.ActiveText.END) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            }
        }

        /**
         * StartTime (MM) タップ イベント
         */
        binding.textTimeStartMM.setOnClickListener {
            val state = vm.activeTextState.value
            val txtView = binding.textTimeStartMM
            vm.setActiveTextView(binding.textTimeStartMM)

            if (state == HomeViewModel.ActiveText.START && txtView.textSize == txtSizeMax) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.START && txtView.textSize == txtSizeNormal) {
                AnimationUtils.animateFullFontSize(binding.textTimeStartMM, binding.textTimeStartMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartMM, binding.textTimeStartMM.alpha, 1.0f)
                AnimationUtils.animateFullFontSize(binding.textTimeStartHH, binding.textTimeStartHH.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeStartHH, binding.textTimeStartHH.alpha, 0.5f)
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeStartMM)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.START)
                AnimationUtils.animateFullFontSize(binding.textTimeStartMM, binding.textTimeStartMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartMM, binding.textTimeStartMM.alpha, 1.0f)
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeStartMM)
            } else if ( state == HomeViewModel.ActiveText.END) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            }
        }

        /**
         * EndTime (HH) タップ イベント
         */
        binding.textTimeEndHH.setOnClickListener {
            val state = vm.activeTextState.value
            val txtView = binding.textTimeEndHH
            vm.setActiveTextView(binding.textTimeEndHH)

            if (state == HomeViewModel.ActiveText.END && txtView.textSize == txtSizeMax) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.END && txtView.textSize == txtSizeNormal) {
                AnimationUtils.animateFullFontSize(binding.textTimeEndHH, binding.textTimeEndHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndHH, binding.textTimeEndHH.alpha, 1.0f)
                AnimationUtils.animateFullFontSize(binding.textTimeEndMM, binding.textTimeEndMM.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeEndMM, binding.textTimeEndMM.alpha, 0.5f)
                setGridBtnLabel(1)
                vm.setActiveTextView(binding.textTimeEndHH)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.END)
                AnimationUtils.animateFullFontSize(binding.textTimeEndHH, binding.textTimeEndHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndHH, binding.textTimeEndHH.alpha, 1.0f)
                setGridBtnLabel(1)
                vm.setActiveTextView(binding.textTimeEndHH)
            } else if ( state == HomeViewModel.ActiveText.START) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            }
        }

        /**
         * EndTime (MM) タップ イベント
         */
        binding.textTimeEndMM.setOnClickListener {
            val state = vm.activeTextState.value
            val txtView = binding.textTimeEndMM
            vm.setActiveTextView(binding.textTimeEndMM)

            if (state == HomeViewModel.ActiveText.END && txtView.textSize == txtSizeMax) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.END && txtView.textSize == txtSizeNormal) {
                AnimationUtils.animateFullFontSize(binding.textTimeEndMM, binding.textTimeEndMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndMM, binding.textTimeEndMM.alpha, 1.0f)
                AnimationUtils.animateFullFontSize(binding.textTimeEndHH, binding.textTimeEndHH.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeEndHH, binding.textTimeEndHH.alpha, 0.5f)
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeEndMM)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.END)
                AnimationUtils.animateFullFontSize(binding.textTimeEndMM, binding.textTimeEndMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndMM, binding.textTimeEndMM.alpha, 1.0f)
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeEndMM)
            } else if ( state == HomeViewModel.ActiveText.START) {
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            }
        }
    }

    private fun showGridLayout() {

        if (vm.activeTextState.value == HomeViewModel.ActiveText.DEFAULT) {
            // 非表示アニメーション
            val slideDown = ObjectAnimator.ofFloat(binding.gridBtnLayout, "translationY", 0f, binding.gridBtnLayout.height.toFloat())
            slideDown.duration = 300
            slideDown.start()

            // アニメーション終了後に非表示にする
            slideDown.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.gridBtnLayout.visibility = View.GONE
                }
            })
        } else {
            // 表示アニメーション
            val slideUp = ObjectAnimator.ofFloat(binding.gridBtnLayout, "translationY", binding.gridBtnLayout.height.toFloat(), 0f)

            // アニメーション開始時に表示する
            slideUp.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    binding.gridBtnLayout.visibility = View.VISIBLE
                }
            })
            slideUp.duration = 300
            slideUp.start()
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

        vm.getDisplayData(date)
    }


    // アニメーション開始処理
    private fun startParticleAnimationFromTextView(particleView: ParticleAnimationView, textView: TextView) {
        textView.visibility = View.INVISIBLE // アニメーション後に非表示
        particleView.startAnimation(textView, textView.x, textView.y, textView.text.toString())
    }

    /**
     * 状態変化によって、発火するメソッド
     */
    private fun onActiveTextStateChanged() {
        val minAlpha = 0.5f
        val maxAlpha = 1.0f

        // Active状態のテキストを監視
        vm.activeTextState.observe(viewLifecycleOwner) { activeText ->
            if (activeText == HomeViewModel.ActiveText.START) {
                // [StartTime Active]
                // EndTime の パーティクルアニメーションを開始
                startParticleAnimationFromTextView(binding.particleView1, binding.labelTimeEnd)
                startParticleAnimationFromTextView(binding.particleView2, binding.textTimeEndHH)
                startParticleAnimationFromTextView(binding.particleView3, binding.labelTimeEndDelimiter)
                startParticleAnimationFromTextView(binding.particleView4, binding.textTimeEndMM)
                // StartTime サイズを通常に
                AnimationUtils.animateFullFontSizeWrapper(
                    listOf(binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
                    txtSizeNormal
                )
                // StartTime 透過度 > 0.5
                AnimationUtils.animateTextAlphaWrapper(
                    listOf(binding.labelTimeStart, binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
                    minAlpha
                )
                // StartTimeの位置を中央に移動
//                setVerticalBias()
                setConstraintBottom(binding.labelTimeStartDelimiter, true)
                showGridLayout()
            } else if (activeText == HomeViewModel.ActiveText.END) {
                // [EndTime Active]
                // StartTime の パーティクルアニメーションを開始
                startParticleAnimationFromTextView(binding.particleView1, binding.labelTimeStart)
                startParticleAnimationFromTextView(binding.particleView2, binding.textTimeStartHH)
                startParticleAnimationFromTextView(binding.particleView3, binding.labelTimeStartDelimiter)
                startParticleAnimationFromTextView(binding.particleView4, binding.textTimeStartMM)
                // EndTime サイズを通常に
                AnimationUtils.animateFullFontSizeWrapper(
                    listOf(binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
                    txtSizeNormal
                )
                // EndTime 透過度 > 0.5
                AnimationUtils.animateTextAlphaWrapper(
                    listOf(binding.labelTimeEnd, binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
                    minAlpha
                )
                // EndTimeの位置を中央に移動
//                setVerticalBias()
                setConstraintBottom(binding.labelTimeEndDelimiter, true)
                showGridLayout()
            }else {
                // Default

                if (binding.textTimeStartHH.visibility == View.INVISIBLE) {

                    binding.labelTimeStart.alpha = 0.0f
                    binding.textTimeStartHH.alpha = 0.0f
                    binding.textTimeStartMM.alpha = 0.0f
                    binding.labelTimeStartDelimiter.alpha = 0.0f

                    binding.labelTimeStart.visibility = View.VISIBLE
                    binding.textTimeStartHH.visibility = View.VISIBLE
                    binding.textTimeStartMM.visibility = View.VISIBLE
                    binding.labelTimeStartDelimiter.visibility = View.VISIBLE
                }
                // StartTime 透過度 0.0 > 1.0
                AnimationUtils.animateTextAlphaWrapper(
                    listOf(binding.labelTimeStart, binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
                    maxAlpha
                )

                if (binding.textTimeEndHH.visibility == View.INVISIBLE) {

                    binding.labelTimeEnd.alpha = 0.0f
                    binding.textTimeEndHH.alpha = 0.0f
                    binding.textTimeEndMM.alpha = 0.0f
                    binding.labelTimeEndDelimiter.alpha = 0.0f

                    binding.labelTimeEnd.visibility = View.VISIBLE
                    binding.textTimeEndHH.visibility = View.VISIBLE
                    binding.textTimeEndMM.visibility = View.VISIBLE
                    binding.labelTimeEndDelimiter.visibility = View.VISIBLE
                }
                // EndTime 透過度 0.0 > 1.0
                AnimationUtils.animateTextAlphaWrapper(
                    listOf(binding.labelTimeEnd, binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
                    maxAlpha
                )

                // StartTime サイズを通常に
                AnimationUtils.animateFullFontSizeWrapper(
                    listOf(binding.textTimeStartHH, binding.textTimeStartMM, binding.labelTimeStartDelimiter),
                    txtSizeNormal
                )
                // EndTime サイズを通常に
                AnimationUtils.animateFullFontSizeWrapper(
                    listOf(binding.textTimeEndHH, binding.textTimeEndMM, binding.labelTimeEndDelimiter),
                    txtSizeNormal
                )

                // すべてlabelの位置を中央に移動
                setConstraintBottom(binding.labelTimeStartDelimiter, false)
                setConstraintBottom(binding.labelTimeEndDelimiter, false)
                // 非表示
                showGridLayout()
            }
        }
    }

    private fun insertWorkInfo() {
        val data = WorkInfo(
            vm.date.value!!.replace("/", ""),
            vm.startHour.value + ":" + vm.startMinute.value,
            vm.endHour.value + ":" + vm.endMinute.value,
            "",
            "",
            false,
            false,
            vm.week.value!!.replace("(", "").replace(")", "")
        )

        vm.insertWorkInfo(data)
        Toast.makeText(requireContext(), "${binding.textDate.text} を登録しました", Toast.LENGTH_SHORT).show()
    }

    private fun setConstraintBottom(textView: TextView, bln: Boolean) {

        var bias: Float = 0f
        var changeTarget: Int = 0

        // TextActive が START, END の場合 True
        if (bln){
            bias = 0.6f
            changeTarget = binding.gridBtnLayout.id
        }else{
            bias = 0.3f
            changeTarget = binding.guideline.id
        }

        val rootLayout = binding.constraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout) // 現在のレイアウトをコピー

        if (textView.id == binding.labelTimeEndDelimiter.id) {
            if (bln) {
                // TextView の上方向の制約
                constraintSet.connect(
                    textView.id, // 対象のビューID
                    ConstraintSet.TOP, // 変更する制約の側 (Top)
                    binding.constraintLayout.id, // 接続先のビューID
                    ConstraintSet.TOP, // 接続先の側 (Top)
                )
            } else {
                // TextView の上方向の制約
                constraintSet.connect(
                    textView.id, // 対象のビューID
                    ConstraintSet.TOP, // 変更する制約の側 (Top)
                    binding.labelTimeStartDelimiter.id, // 接続先のビューID
                    ConstraintSet.BOTTOM, // 接続先の側 (Bottom)
                )
            }
        }

        // TextView の下方向の制約
        constraintSet.connect(
            textView.id, // 対象のビューID
            ConstraintSet.BOTTOM, // 変更する制約の側 (Bottom)
            changeTarget, // 接続先のビューID
            ConstraintSet.TOP, // 接続先の側 (Top)
        )

        // layout_constraintVertical_bias を変更
        constraintSet.setVerticalBias(textView.id, bias) // 垂直方向バイアス 設定

        // アニメーションを適用
        val transition = ChangeBounds()
        transition.duration = 500
        TransitionManager.beginDelayedTransition(rootLayout, transition)
        constraintSet.applyTo(rootLayout)
    }

    /** ============================================
     *  GridLayoutのボタンにラベルを設定する
     *  @param lblTimeNum ラベルの種類 [0: AM, 1: PM, 2: min]
     *  ============================================ */
    private fun setGridBtnLabel(lblTimeNum: Int) {
        val lblTime = listOf(
            listOf( "01 h", "02 h", "03 h", "04 h", "05 h", "06 h", "07 h", "08 h", "09 h", "10 h", "11 h", "12 h" ),
            listOf( "13 h", "14 h", "15 h", "16 h", "17 h", "18 h", "19 h", "20 h", "21 h", "22 h", "23 h", "00 h" ),
            listOf( "00 m", "05 m", "10 m", "15 m", "20 m", "25 m", "30 m", "35 m", "40 m", "45 m", "50 m", "55 m" )
        )

        // gridLayoutの要素を取得
        for ((viewBtnCnt, viewBtn) in binding.gridBtnLayout.touchables.withIndex()) {
            // Type が Button の場合
            if (viewBtn is Button) {
                // ボタンのテキストを変更
                viewBtn.text = lblTime[lblTimeNum][viewBtnCnt]
                viewBtn.setOnClickListener{
                    // ボタンをタップした際の振動
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    // テキストを変更
//                    vm.activeTextView.value?.text = viewBtn.text.substring(0, 2)
                    if (isChangedActiveTextValue(viewBtn.text.substring(0, 2))){
                        insertWorkInfo()
                    }
                }
            }
        }
    }

    // value : 変更しようとしている値
    private fun isChangedActiveTextValue(value: String): Boolean {
        // case　文
        when (vm.activeTextView.value?.id) {
            binding.textTimeStartHH.id -> {
                if (value != vm.startHour.value) {
                    vm.setStartHour(value)
                    return true
                }else{
                    return false
                }
            }
            binding.textTimeStartMM.id -> {
                if (value != vm.startMinute.value) {
                    vm.setStartMinute(value)
                    return true
                }else{
                    return false
                }
            }
            binding.textTimeEndHH.id -> {
                if (value != vm.endHour.value) {
                    vm.setEndHour(value)
                    return true
                }else{
                    return false
                }
            }
            binding.textTimeEndMM.id -> {
                if (value != vm.endMinute.value) {
                    vm.setEndMinute(value)
                    return true
                }else{
                    return false
                }
            }
            else -> {
                return false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
