package com.example.quickworktime.ui.workListView

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

    inner class FormulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.formulaItemText)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
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
}
