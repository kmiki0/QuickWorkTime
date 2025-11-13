package com.example.quickworktime.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViewsService

/**
 * StackView用のRemoteViewsService
 */
class WidgetRemoteViewsService : RemoteViewsService() {
	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return WidgetRemoteViewsFactory(this.applicationContext, intent)
	}
}