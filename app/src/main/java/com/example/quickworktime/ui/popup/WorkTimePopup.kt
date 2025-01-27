package com.example.quickworktime.ui.popup

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import com.example.quickworktime.R
import java.util.Calendar

class WorkTimePopup(private val context: Context) {

	fun show() {
		val dialogView = LayoutInflater.from(context).inflate(R.layout.popup_worktime, null)
		val dialogBuilder = AlertDialog.Builder(context)
			.setView(dialogView)

		val alertDialog = dialogBuilder.create()
		alertDialog.show()

		dialogView.findViewById<Button>(R.id.popupButton).setOnClickListener {
			alertDialog.dismiss()
		}

		val startTimeEditText: EditText = dialogView.findViewById(R.id.startTime)
		startTimeEditText.setOnClickListener {
			showTimePickerDialog(startTimeEditText)
		}

		val endTimeEditText: EditText = dialogView.findViewById(R.id.endTime)
		endTimeEditText.setOnClickListener {
			showTimePickerDialog(endTimeEditText)
		}
	}

	private fun showTimePickerDialog(editText: EditText) {
		val calendar = Calendar.getInstance()
		val hour = calendar.get(Calendar.HOUR_OF_DAY)
		val minute = calendar.get(Calendar.MINUTE)

		val timePickerDialog = TimePickerDialog(
			context,
			{ _, selectedHour, selectedMinute ->
				editText.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
			},
			hour,
			minute,
			true
		)
		timePickerDialog.show()
	}
}