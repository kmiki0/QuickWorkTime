package com.example.quickworktime.ui.popup

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_work_list_view, null)
        val dialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)

        alertDialog = dialogBuilder.create()
        alertDialog.show()

        setupUpperButtons(dialogView)
        setupFormulaRecyclerView(dialogView)

        dialogView.findViewById<Button>(R.id.popupButton).setOnClickListener {
            alertDialog.dismiss()
            // TODO: 勤怠計算結果の反映処理をここに追加
        }
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

        // ボタンIDとFormulaItemのマッピング
        val buttonMap = mapOf(
            R.id.btn_start_time to upperButtons[0],
            R.id.btn_end_time to upperButtons[1],
            R.id.btn_break_time to upperButtons[2],
            R.id.btn_number to upperButtons[3],
            R.id.btn_left_parenthesis to upperButtons[4],
            R.id.btn_right_parenthesis to upperButtons[5],
            R.id.btn_plus to upperButtons[6],
            R.id.btn_minus to upperButtons[7]
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
                        button.setBackgroundColor(context.getColor(R.color.button_disabled))
                        button.isEnabled = false
                    } else {
                        button.setBackgroundColor(context.getColor(R.color.button_normal))
                        button.isEnabled = true
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
                formulaItems.add(toPos, formulaItems.removeAt(fromPos))
                formulaAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // スワイプ削除はなし
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
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}
