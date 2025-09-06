package com.example.quickworktime.ui.home
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import com.example.quickworktime.databinding.FragmentHomeBinding
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.utils.AnimationUtils
import com.example.quickworktime.view.ParticleAnimationView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val txtSizeMin: Float = 200f
    val txtSizeNormal: Float = 300f
    val txtSizeMax: Float = 400f

    // アクティブ状態のテキストフィールド

    // ViewModelを生成
    private val vm: HomeViewModel by viewModels()

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

        binding.constraintLayout.setOnClickListener{
            vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
        }

        /**
         * StartTime (HH) タップ イベント
         */
        binding.textTimeStartHH.setOnClickListener {
            val state = vm.activeTextState.value
            val txtView = binding.textTimeStartHH
            vm.setActiveTextView(binding.textTimeStartHH)

            if (state == HomeViewModel.ActiveText.START && txtView.textSize == txtSizeMax) {
                /**
                 * 状態 = START かつ テキストサイズが最大の場合
                 */
                // StateをDefaultに変更
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.START && txtView.textSize == txtSizeNormal) {
                /**
                 * 状態 = START かつ テキストサイズが通常の場合
                 */
                // textTimeStartHH のみ大きく + 透過度を1.0に
                AnimationUtils.animateFullFontSize(binding.textTimeStartHH, binding.textTimeStartHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartHH, binding.textTimeStartHH.alpha, 1.0f)
                // textTimeStartMM のみ通常に + 透過度を0.5に
                AnimationUtils.animateFullFontSize(binding.textTimeStartMM, binding.textTimeStartMM.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeStartMM, binding.textTimeStartMM.alpha, 0.5f)
                // ボタンのラベルを設定
                setGridBtnLabel(0)
                vm.setActiveTextView(binding.textTimeStartHH)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                /**
                 * 状態 = Default の場合
                 */
                // StateをSTARTに変更
//                binding.textTimeStartHH.layoutParams.height = (binding.textTimeStartHH.height * 1.5).toInt()
                vm.updateActiveTextState(HomeViewModel.ActiveText.START)
                // layoutHeight * 1.3 に変更
                AnimationUtils.animateFullFontSize(binding.textTimeStartHH, binding.textTimeStartHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartHH, binding.textTimeStartHH.alpha, 1.0f)
                // ボタンのラベルを設定
                setGridBtnLabel(0)
                vm.setActiveTextView(binding.textTimeStartHH)
            } else if ( state == HomeViewModel.ActiveText.END) {
                /**
                 * 状態 = END の場合
                 */
                // StateをDEFAULTに変更
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
                /**
                 * 状態 = START かつ テキストサイズが最大の場合
                 */
                 // StateをDefaultに変更
                 vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.START && txtView.textSize == txtSizeNormal) {
                /**
                 * 状態 = START かつ テキストサイズが通常の場合
                 */
                // textTimeStartMM のみ大きく + 透過度を1.0に
                AnimationUtils.animateFullFontSize(binding.textTimeStartMM, binding.textTimeStartMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartMM, binding.textTimeStartMM.alpha, 1.0f)
                // textTimeStartHH のみ通常に + 透過度を0.5に
                AnimationUtils.animateFullFontSize(binding.textTimeStartHH, binding.textTimeStartHH.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeStartHH, binding.textTimeStartHH.alpha, 0.5f)
                // ボタンのラベルを設定
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeStartMM)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                /**
                 * 状態 = Default の場合
                 */
                // StateをSTARTに変更
                vm.updateActiveTextState(HomeViewModel.ActiveText.START)
                AnimationUtils.animateFullFontSize(binding.textTimeStartMM, binding.textTimeStartMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeStartMM, binding.textTimeStartMM.alpha, 1.0f)
                // ボタンのラベルを設定
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeStartMM)
            } else if ( state == HomeViewModel.ActiveText.END) {
                /**
                 * 状態 = END の場合
                 */
                // StateをDEFAULTに変更
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
                /**
                 * 状態 = END かつ テキストサイズが最大の場合
                 */
                // StateをDefaultに変更
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.END && txtView.textSize == txtSizeNormal) {
                /**
                 * 状態 = END かつ テキストサイズが通常の場合
                 */
                // textTimeEndHH のみ大きく + 透過度を1.0に
                AnimationUtils.animateFullFontSize(binding.textTimeEndHH, binding.textTimeEndHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndHH, binding.textTimeEndHH.alpha, 1.0f)
                // textTimeEndMM のみ通常に + 透過度を0.5に
                AnimationUtils.animateFullFontSize(binding.textTimeEndMM, binding.textTimeEndMM.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeEndMM, binding.textTimeEndMM.alpha, 0.5f)
                // ボタンのラベルを設定
                setGridBtnLabel(1)
                vm.setActiveTextView(binding.textTimeEndHH)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                /**
                 * 状態 = Default の場合
                 */
                // StateをENDに変更
                vm.updateActiveTextState(HomeViewModel.ActiveText.END)
                AnimationUtils.animateFullFontSize(binding.textTimeEndHH, binding.textTimeEndHH.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndHH, binding.textTimeEndHH.alpha, 1.0f)
                // ボタンのラベルを設定
                setGridBtnLabel(1)
                vm.setActiveTextView(binding.textTimeEndHH)
            } else if ( state == HomeViewModel.ActiveText.START) {
                /**
                 * 状態 = START の場合
                 */
                // StateをDEFAULTに変更
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
                /**
                 * 状態 = END かつ テキストサイズが最大の場合
                 */
                // StateをDefaultに変更
                vm.updateActiveTextState(HomeViewModel.ActiveText.DEFAULT)
            } else if (state == HomeViewModel.ActiveText.END && txtView.textSize == txtSizeNormal) {
                /**
                 * 状態 = END かつ テキストサイズが通常の場合
                 */
                // textTimeEndMM のみ大きく + 透過度を1.0に
                AnimationUtils.animateFullFontSize(binding.textTimeEndMM, binding.textTimeEndMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndMM, binding.textTimeEndMM.alpha, 1.0f)
                // textTimeEndHH のみ通常に + 透過度を0.5に
                AnimationUtils.animateFullFontSize(binding.textTimeEndHH, binding.textTimeEndHH.textSize, txtSizeNormal)
                AnimationUtils.animateTextAlpha(binding.textTimeEndHH, binding.textTimeEndHH.alpha, 0.5f)
                // ボタンのラベルを設定
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeEndMM)
            }else if (state == HomeViewModel.ActiveText.DEFAULT) {
                /**
                 * 状態 = Default の場合
                 */
                // StateをENDに変更
                vm.updateActiveTextState(HomeViewModel.ActiveText.END)
                AnimationUtils.animateFullFontSize(binding.textTimeEndMM, binding.textTimeEndMM.textSize, txtSizeMax)
                AnimationUtils.animateTextAlpha(binding.textTimeEndMM, binding.textTimeEndMM.alpha, 1.0f)
                // ボタンのラベルを設定
                setGridBtnLabel(2)
                vm.setActiveTextView(binding.textTimeEndMM)
            } else if ( state == HomeViewModel.ActiveText.START) {
                /**
                 * 状態 = START の場合
                 */
                // StateをDEFAULTに変更
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

        vm.getDisplayData(date ?: "")
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