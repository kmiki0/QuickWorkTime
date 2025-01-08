package com.example.quickworktime.ui.home


import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Bundle
import android.widget.GridLayout
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.Calendar


class GridLayoutLogic  {


    private fun showGridLayout() {

//        if (binding.gridBtnLayout.visibility == View.GONE) {
//
//            // 表示アニメーション
//            val slideUp = ObjectAnimator.ofFloat(binding.gridBtnLayout, "translationY", binding.gridBtnLayout.height.toFloat(), 0f)
//
//            // アニメーション開始時に表示する
//            slideUp.addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationStart(animation: Animator) {
//                    binding.gridBtnLayout.visibility = View.VISIBLE
//                }
//            })
//            slideUp.duration = 300
//            slideUp.start()
//
//        } else {
//
//            // 非表示アニメーション
//            val slideDown = ObjectAnimator.ofFloat(binding.gridBtnLayout, "translationY", 0f, binding.gridBtnLayout.height.toFloat())
//            slideDown.duration = 300
//            slideDown.start()
//
//            // アニメーション終了後に非表示にする
//            slideDown.addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator) {
//                    binding.gridBtnLayout.visibility = View.GONE
//                }
//            })
//        }
    }


    /** ============================================
     *  GridLayout内のボタンをタップしたときの処理
     *  @param gridLayout グリッドレイアウト
     *  @param textView 変更対象のTextView
     *  ============================================ */
    private fun showTimePickerDialog(gridLayout: GridLayout, textView: TextView) {

        // GridLayout内からボタンを取得する
    }



}
