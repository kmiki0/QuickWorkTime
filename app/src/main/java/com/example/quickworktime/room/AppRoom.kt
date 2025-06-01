package com.example.quickworktime.room

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
	entities = [WorkInfo::class, WorkSetting::class, HolidayInfo::class],
	version = 2,
	exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

	abstract fun workInfo(): WorkInfoDao
	abstract fun workSetting(): WorkSettingDao
	abstract fun holidayInfo(): HolidayInfoDao

	companion object {
		@Volatile
		private var INSTANCE: AppDatabase? = null

		fun getDatabase(context: Context): AppDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					AppDatabase::class.java,
					"database"
				)
				.addMigrations(MIGRATION_1_2)
				.build()
				INSTANCE = instance
				instance
			}
		}

		private val MIGRATION_1_2 = object : Migration(1, 2) {
			override fun migrate(database: SupportSQLiteDatabase) {
				// 必要ならスキーマ変更のSQLを記述
				// 例: テーブルに新しいカラムを追加
//				 database.execSQL("ALTER TABLE work_info ADD COLUMN new_column INTEGER DEFAULT 0 NOT NULL")
			}
		}
	}
}