package com.example.quickworktime.ui.popup

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quickworktime.R
import com.example.quickworktime.ui.workListView.FormulaAdapter
import com.example.quickworktime.ui.workListView.FormulaItem
import com.example.quickworktime.ui.workListView.ItemType
import java.util.Calendar
import java.util.UUID

class AttendanceCalculationPopup(private val context: Context) {

    private val upperButtons = mutableListOf<FormulaItem>()
    private val formulaItems = mutableListOf<FormulaItem>()
    private lateinit var formulaAdapter: FormulaAdapter
    private lateinit var alertDialog: AlertDialog
    private lateinit var calculationPreview: TextView
    private lateinit var formulaPreview: TextView

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_work_list_view, null)
        val dialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)

        alertDialog = dialogBuilder.create()
        alertDialog.show()

        // プレビューTextViewを取得
        calculationPreview = dialogView.findViewById(R.id.calculation_preview)
        formulaPreview = dialogView.findViewById(R.id.formula_preview)

        setupUpperButtons(dialogView)
        setupFormulaRecyclerView(dialogView)

        dialogView.findViewById<Button>(R.id.popupButton).setOnClickListener {
            alertDialog.dismiss()
            // TODO: 勤怠計算結果の反映処理をここに追加
        }

        // 初期計算結果を表示
        updateCalculationPreview()
    }

    private fun setupUpperButtons(dialogView: View) {
        upperButtons.clear()
        upperButtons.add(FormulaItem("start_time", "開始時間", ItemType.START_TIME))
        upperButtons.add(FormulaItem("end_time", "終了時間", ItemType.END_TIME))
        upperButtons.add(FormulaItem("break_time", "休憩", ItemType.BREAK_TIME))
        upperButtons.add(FormulaItem("number", "数値", ItemType.NUMBER))
        upperButtons.add(FormulaItem("left_parenthesis", "(", ItemType.PARENTHESIS))
        upperButtons.add(FormulaItem("right_parenthesis", ")", ItemType.PARENTHESIS))
        upperButtons.add(FormulaItem("plus", "+", ItemType.OPERATOR))
        upperButtons.add(FormulaItem("minus", "-", ItemType.OPERATOR))
        upperButtons.add(FormulaItem("multiply", "×", ItemType.OPERATOR))
        upperButtons.add(FormulaItem("divide", "÷", ItemType.OPERATOR))

        // ボタンIDとFormulaItemのマッピング
        val buttonMap = mapOf(
            R.id.btn_start_time to upperButtons[0],
            R.id.btn_end_time to upperButtons[1],
            R.id.btn_break_time to upperButtons[2],
            R.id.btn_number to upperButtons[3],
            R.id.btn_left_parenthesis to upperButtons[4],
            R.id.btn_right_parenthesis to upperButtons[5],
            R.id.btn_plus to upperButtons[6],
            R.id.btn_minus to upperButtons[7],
            R.id.btn_multiply to upperButtons[8],
            R.id.btn_divide to upperButtons[9]
        )

        for ((id, item) in buttonMap) {
            val button = dialogView.findViewById<Button>(id)
            button.setOnClickListener {
                if (item.type == ItemType.NUMBER) {
                    showNumberInputDialog()
                } else {
                    addFormulaItem(item)
                }
            }
        }

        updateButtonStates(dialogView)
    }

    private fun addFormulaItem(item: FormulaItem) {
        if (item.type == ItemType.START_TIME || item.type == ItemType.END_TIME || item.type == ItemType.BREAK_TIME) {
            if (formulaItems.any { it.type == item.type }) {
                return
            }
            upperButtons.find { it.id == item.id }?.isSelected = true
        }
        val newItem = item.copy(id = UUID.randomUUID().toString())
        formulaItems.add(newItem)
        formulaAdapter.notifyItemInserted(formulaItems.size - 1)
        updateButtonStates(alertDialog.window!!.decorView)
        updateCalculationPreview() // 計算結果を更新
    }

    private fun updateButtonStates(rootView: View) {
        for (buttonItem in upperButtons) {
            if (buttonItem.type == ItemType.START_TIME || buttonItem.type == ItemType.END_TIME || buttonItem.type == ItemType.BREAK_TIME) {
                val buttonId = when (buttonItem.type) {
                    ItemType.START_TIME -> R.id.btn_start_time
                    ItemType.END_TIME -> R.id.btn_end_time
                    ItemType.BREAK_TIME -> R.id.btn_break_time
                    else -> 0
                }
                if (buttonId != 0) {
                    val button = rootView.findViewById<Button>(buttonId)
                    if (buttonItem.isSelected) {
                        // 選択済みの場合は無効化（背景は変更せずステートのみ変更）
                        button.isEnabled = false
                        button.alpha = 0.5f // 透明度で無効状態を表現
                    } else {
                        // 通常状態に戻す
                        button.isEnabled = true
                        button.alpha = 1.0f // 透明度を元に戻す
                    }
                }
            }
        }
    }

    private fun setupFormulaRecyclerView(dialogView: View) {
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.formula_recycler_view)
        formulaAdapter = FormulaAdapter(formulaItems) { position ->
            val removedItem = formulaItems.removeAt(position)
            if (removedItem.type == ItemType.START_TIME || removedItem.type == ItemType.END_TIME || removedItem.type == ItemType.BREAK_TIME) {
                upperButtons.find { it.type == removedItem.type }?.isSelected = false
                updateButtonStates(alertDialog.window!!.decorView)
            }
            formulaAdapter.notifyItemRemoved(position)
            updateCalculationPreview() // 計算結果を更新
        }
        recyclerView.adapter = formulaAdapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                // 実際に移動が発生したことを記録
                formulaAdapter.onItemMoved()

                // 元のコードと全く同じ処理
                formulaItems.add(toPos, formulaItems.removeAt(fromPos))
                formulaAdapter.notifyItemMoved(fromPos, toPos)
                updateCalculationPreview() // 計算結果を更新
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // スワイプ削除はなし
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                when (actionState) {
                    ItemTouchHelper.ACTION_STATE_DRAG -> {
                        // ドラッグ開始時にぶるぶるアニメーションを開始
                        viewHolder?.let {
                            formulaAdapter.startDragAnimation(it)
                        }
                    }
                    ItemTouchHelper.ACTION_STATE_IDLE -> {
                        // ドラッグ終了時にアニメーションを停止
                        formulaAdapter.stopDragAnimation()
                    }
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // ドラッグが完全に終了した時点でアニメーションを停止
                formulaAdapter.stopDragAnimation()
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true // 長押しでドラッグを有効にする
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showNumberInputDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_number_input, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_number)

        AlertDialog.Builder(context)
            .setTitle("数値を入力")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val number = editText.text.toString()
                if (number.isNotEmpty()) {
                    val newItem = FormulaItem(
                        id = UUID.randomUUID().toString(),
                        value = number,
                        type = ItemType.NUMBER
                    )
                    formulaItems.add(newItem)
                    formulaAdapter.notifyItemInserted(formulaItems.size - 1)
                    updateCalculationPreview() // 計算結果を更新
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    /**
     * 計算結果のプレビューを更新
     */
    private fun updateCalculationPreview() {
        try {
            if (formulaItems.isEmpty()) {
                formulaPreview.text = "数式を入力してください"
                calculationPreview.text = "---"
                return
            }

            // 数式文字列を作成
            val displayFormula = buildDisplayFormulaString()
            formulaPreview.text = displayFormula

            // 計算結果を算出
            val result = calculateFormula()
            if (result != null) {
                calculationPreview.text = formatTimeResult(result)
            } else {
                calculationPreview.text = "N/A"
            }
        } catch (e: Exception) {
            formulaPreview.text = "エラー"
            calculationPreview.text = "N/A"
        }
    }

    /**
     * 数式を計算
     */
    private fun calculateFormula(): Double? {
        try {
            // FormulaItemsを数式文字列に変換
            val formulaString = buildFormulaString()
            if (formulaString.isEmpty()) return null

            // 簡単な数式評価器で計算
            return evaluateFormula(formulaString)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * FormulaItemsから数式文字列を構築
     */
    private fun buildFormulaString(): String {
        val formula = StringBuilder()

        for (item in formulaItems) {
            when (item.type) {
                ItemType.START_TIME -> formula.append("9.0") // 仮の開始時間 9:00
                ItemType.END_TIME -> formula.append("18.0") // 仮の終了時間 18:00
                ItemType.BREAK_TIME -> formula.append("1.0") // 仮の休憩時間 1:00
                ItemType.NUMBER -> formula.append(item.value)
                ItemType.OPERATOR -> {
                    when (item.value) {
                        "×" -> formula.append(" * ")
                        "÷" -> formula.append(" / ")
                        else -> formula.append(" ${item.value} ")
                    }
                }
                ItemType.PARENTHESIS -> formula.append(item.value)
            }
        }

        return formula.toString()
    }

    /**
     * 表示用の数式文字列を構築
     */
    private fun buildDisplayFormulaString(): String {
        val formula = StringBuilder()

        for (item in formulaItems) {
            when (item.type) {
                ItemType.START_TIME -> formula.append("開始(09:00)")
                ItemType.END_TIME -> formula.append("終了(18:00)")
                ItemType.BREAK_TIME -> formula.append("休憩(01:00)")
                ItemType.NUMBER -> formula.append(item.value)
                ItemType.OPERATOR -> formula.append(" ${item.value} ")
                ItemType.PARENTHESIS -> formula.append(item.value)
            }
        }

        return formula.toString()
    }

    /**
     * 簡単な数式評価器
     */
    private fun evaluateFormula(formula: String): Double? {
        try {
            // 空白を除去
            val cleanFormula = formula.replace(" ", "")

            // 基本的な計算のみサポート（括弧、加減算）
            return evaluateExpression(cleanFormula)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 数式を評価（再帰的に括弧を処理）
     */
    private fun evaluateExpression(expression: String): Double? {
        var expr = expression

        // 括弧を再帰的に処理
        while (expr.contains('(')) {
            val lastOpenParen = expr.lastIndexOf('(')
            val closeParen = expr.indexOf(')', lastOpenParen)
            if (closeParen == -1) return null // 括弧が閉じられていない

            val innerExpr = expr.substring(lastOpenParen + 1, closeParen)
            val innerResult = evaluateSimpleExpression(innerExpr) ?: return null

            expr = expr.substring(0, lastOpenParen) + innerResult + expr.substring(closeParen + 1)
        }

        return evaluateSimpleExpression(expr)
    }

    /**
     * 括弧のない単純な数式を評価
     */
    private fun evaluateSimpleExpression(expression: String): Double? {
        try {
            if (expression.isEmpty()) return null

            // トークンに分割
            val tokens = mutableListOf<String>()
            var current = ""
            var i = 0

            while (i < expression.length) {
                val char = expression[i]
                when {
                    char == '+' || char == '-' || char == '*' || char == '/' -> {
                        if (current.isNotEmpty()) {
                            tokens.add(current)
                            current = ""
                        }
                        tokens.add(char.toString())
                    }
                    char.isDigit() || char == '.' -> {
                        current += char
                    }
                }
                i++
            }

            if (current.isNotEmpty()) {
                tokens.add(current)
            }

            if (tokens.isEmpty()) return null

            // 先に乗算と除算を処理
            i = 1
            while (i < tokens.size - 1) {
                val operator = tokens[i]
                if (operator == "*" || operator == "/") {
                    val left = tokens[i - 1].toDoubleOrNull() ?: return null
                    val right = tokens[i + 1].toDoubleOrNull() ?: return null

                    val result = when (operator) {
                        "*" -> left * right
                        "/" -> {
                            if (right == 0.0) return null // ゼロ除算エラー
                            left / right
                        }
                        else -> return null
                    }

                    // 結果で置き換え
                    tokens[i - 1] = result.toString()
                    tokens.removeAt(i + 1)
                    tokens.removeAt(i)
                    i -= 1
                } else {
                    i += 2
                }
            }

            // 次に加算と減算を処理
            var result = tokens[0].toDoubleOrNull() ?: return null

            i = 1
            while (i < tokens.size - 1) {
                val operator = tokens[i]
                val operand = tokens[i + 1].toDoubleOrNull() ?: return null

                when (operator) {
                    "+" -> result += operand
                    "-" -> result -= operand
                    else -> return null
                }
                i += 2
            }

            return result
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 時間結果をフォーマット
     */
    private fun formatTimeResult(hours: Double): String {
        return if (hours < 0) {
            "N/A"
        } else {
            val totalMinutes = (hours * 60).toInt()
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            "${h}:${m.toString().padStart(2, '0')}"
        }
    }
}
