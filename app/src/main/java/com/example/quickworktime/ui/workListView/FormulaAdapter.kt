
package com.example.quickworktime.ui.workListView

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quickworktime.R

class FormulaAdapter(
    private val items: MutableList<FormulaItem>,
    private val onItemRemove: (position: Int) -> Unit
) : RecyclerView.Adapter<FormulaAdapter.FormulaViewHolder>() {

    // ドラッグ中のアイテムを管理
    private var isDragging = false
    private var draggingViewHolder: RecyclerView.ViewHolder? = null
    private var shakeAnimator: AnimatorSet? = null
    private var hasMoved = false // ドラッグ中に実際に移動したかのフラグ

    inner class FormulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.formulaItemText)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                // ドラッグ中でない場合のみクリック削除を許可
                if (position != RecyclerView.NO_POSITION && !isDragging) {
                    onItemRemove(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormulaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_formula, parent, false)
        return FormulaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormulaViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.value
    }

    override fun getItemCount(): Int = items.size

    /**
     * ドラッグ開始時に呼び出す
     */
    fun startDragAnimation(viewHolder: RecyclerView.ViewHolder) {
        isDragging = true
        hasMoved = false // 移動フラグをリセット
        draggingViewHolder = viewHolder
        startShakeAnimation(viewHolder.itemView)
    }

    /**
     * ドラッグ終了時に呼び出す
     */
    fun stopDragAnimation() {
        draggingViewHolder?.let { holder ->
            stopShakeAnimation(holder.itemView)
        }
        draggingViewHolder = null

        // 少し遅延してからドラッグ状態を解除（誤クリック防止）
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isDragging = false
            hasMoved = false
        }, 100) // 100ms遅延
    }

    /**
     * アイテムが移動したことを記録
     */
    fun onItemMoved() {
        hasMoved = true
    }

    /**
     * ぶるぶるアニメーションを開始
     */
    private fun startShakeAnimation(view: View) {
        stopShakeAnimation(view) // 既存のアニメーションがあれば停止

        // 回転アニメーション
        val rotateAnimator = ObjectAnimator.ofFloat(view, "rotation", -2f, 2f).apply {
            duration = 100
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        // 軽微なスケールアニメーション
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.98f, 1.02f).apply {
            duration = 150
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.98f, 1.02f).apply {
            duration = 150
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        // 軽微な位置の揺れ
        val translationXAnimator = ObjectAnimator.ofFloat(view, "translationX", -1f, 1f).apply {
            duration = 80
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        val translationYAnimator = ObjectAnimator.ofFloat(view, "translationY", -1f, 1f).apply {
            duration = 120
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        // アニメーションセットを作成
        shakeAnimator = AnimatorSet().apply {
            playTogether(rotateAnimator, scaleXAnimator, scaleYAnimator, translationXAnimator, translationYAnimator)
            start()
        }
    }

    /**
     * ぶるぶるアニメーションを停止
     */
    private fun stopShakeAnimation(view: View) {
        shakeAnimator?.cancel()
        shakeAnimator = null

        // ビューを元の状態に戻す
        view.rotation = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationX = 0f
        view.translationY = 0f
    }
}
