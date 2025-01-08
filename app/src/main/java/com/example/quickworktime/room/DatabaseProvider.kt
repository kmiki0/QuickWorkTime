package com.example.quickworktime.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//object DatabaseProvider {
//	@Volatile
//	private var INSTANCE: AppRoom? = null
//
//	fun getDatabase(context: Context): AppRoom {
//		return INSTANCE ?: synchronized(this)  {
//			val instance = Room.databaseBuilder(
//				context.applicationContext,
//				AppRoom::class.java,
//				"app_database"
//			).build()
//			INSTANCE = instance
//			instance
//		}
//	}
//}

