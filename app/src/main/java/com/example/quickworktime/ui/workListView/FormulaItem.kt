package com.example.quickworktime.ui.workListView

enum class ItemType {
    START_TIME,
    END_TIME,
    BREAK_TIME,
    PARENTHESIS,
    OPERATOR,
    NUMBER
}

data class FormulaItem(
    val id: String,
    val value: String,
    val type: ItemType,
    var isSelected: Boolean = false
)
