package com.example.quickworktime.widget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Service for handling widget update operations in the background
 * This service handles clock-out recording and widget refresh operations
 */
class WidgetUpdateService : Service() {
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    companion object {
        const val ACTION_RECORD_CLOCK_OUT = "com.example.quickworktime.widget.ACTION_RECORD_CLOCK_OUT"
        const val ACTION_UPDATE_WIDGET = "com.example.quickworktime.widget.ACTION_UPDATE_WIDGET"
        const val EXTRA_CLOCK_OUT_TIME = "clock_out_time"
        
        /**
         * Helper method to start clock-out recording
         */
        fun startClockOutRecording(context: Context, clockOutTime: String? = null) {
            val intent = Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_RECORD_CLOCK_OUT
                clockOutTime?.let { putExtra(EXTRA_CLOCK_OUT_TIME, it) }
            }
            context.startService(intent)
        }
        
        /**
         * Helper method to trigger widget update
         */
        fun startWidgetUpdate(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RECORD_CLOCK_OUT -> {
                handleClockOutRecording(intent, startId)
            }
            ACTION_UPDATE_WIDGET -> {
                handleWidgetUpdate(startId)
            }
            else -> {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    /**
     * バックグラウンドスレッドで退勤記録処理を行う
     */
    private fun handleClockOutRecording(intent: Intent, startId: Int) {
        serviceScope.launch {
            try {
                val repository = WidgetRepository.create(this@WidgetUpdateService)
                repository.initialize(this@WidgetUpdateService) // 重要: 初期化を確実に実行
                val errorHandler = WidgetErrorHandler()

                // Intentから退勤時間を取得、なければ最適時間を計算
                val clockOutTime = intent.getStringExtra(EXTRA_CLOCK_OUT_TIME)
                    ?: repository.calculateOptimalClockOutTime()

                // 今日のレコードが既に存在するかをチェック - 適切なエラーハンドリング付き
                val todayRecordResult = repository.getTodayWorkInfo()
                when (todayRecordResult) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
                        val existingRecord = todayRecordResult.data

                        if (existingRecord?.endTime != null) {
                            // 既に終了時間が記録済み、ウィジェットの現在状態を更新するだけ
                            when (val updateResult = repository.updateWidget(this@WidgetUpdateService)) {
                                is WidgetErrorHandler.WidgetResult.Error -> {
                                    errorHandler.logError(this@WidgetUpdateService, updateResult.error, "重複記録チェック後のウィジェット更新")
                                }
                                is WidgetErrorHandler.WidgetResult.Success -> {
                                    Log.i("WidgetUpdateService", "重複チェック後ウィジェット更新成功")
                                }
                            }
                            stopSelf(startId)
                            return@launch
                        } else {
                            // レコードは存在するが終了時間なし、記録処理を続行
                            Log.i("WidgetUpdateService", "終了時間なしの既存レコード発見、退勤記録を続行")
                        }
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        // 既存レコードチェックでエラー、記録処理は続行
                        Log.w("WidgetUpdateService", "既存レコードチェックでエラー、記録処理続行: ${todayRecordResult.error.message}")
                    }
                }

                // エラーハンドリング付きで退勤時間を記録
                Log.i("WidgetUpdateService", "退勤時間を記録中: $clockOutTime")
                when (val recordResult = repository.recordClockOut(clockOutTime.toString())) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
                        // 記録成功、通知とウィジェット更新
                        Log.i("WidgetUpdateService", "退勤記録成功")
                        repository.notifyDataUpdated(this@WidgetUpdateService)

                        when (val updateResult = repository.updateWidget(this@WidgetUpdateService)) {
                            is WidgetErrorHandler.WidgetResult.Error -> {
                                errorHandler.logError(this@WidgetUpdateService, updateResult.error, "記録成功後のウィジェット更新")
                            }
                            is WidgetErrorHandler.WidgetResult.Success -> {
                                Log.i("WidgetUpdateService", "記録後ウィジェット更新成功")
                            }
                        }
                    }
                    is WidgetErrorHandler.WidgetResult.Error -> {
                        // 記録失敗、エラーログを出力してもウィジェット更新は試行
                        Log.e("WidgetUpdateService", "退勤記録失敗: ${recordResult.error.message}")
                        errorHandler.logError(this@WidgetUpdateService, recordResult.error, "recordClockOut")

                        // 現在の状態を表示するためウィジェット更新を試行（エラー状態を表示する可能性）
                        when (val updateResult = repository.updateWidget(this@WidgetUpdateService)) {
                            is WidgetErrorHandler.WidgetResult.Error -> {
                                errorHandler.logError(this@WidgetUpdateService, updateResult.error, "記録失敗後のウィジェット更新")
                            }
                            is WidgetErrorHandler.WidgetResult.Success -> {
                                Log.i("WidgetUpdateService", "記録失敗後ウィジェット更新")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                // 予期しないエラー、エラーを作成してログ出力
                Log.e("WidgetUpdateService", "退勤記録中に予期しないエラー", e)
                val unexpectedError = WidgetErrorHandler.WidgetError.UnknownError("退勤記録中に予期しないエラー", e)
                val errorHandler = WidgetErrorHandler()
                errorHandler.logError(this@WidgetUpdateService, unexpectedError, "handleClockOutRecording")

                // それでもウィジェット更新は試行
                try {
                    val repository = WidgetRepository.create(this@WidgetUpdateService)
                    repository.initialize(this@WidgetUpdateService)
                    repository.updateWidget(this@WidgetUpdateService)
                } catch (updateException: Exception) {
                    Log.e("WidgetUpdateService", "エラー後のウィジェット更新失敗", updateException)
                }
            } finally {
                stopSelf(startId)
            }
        }
    }


    /**
     * Handles widget update in background thread with error handling
     */
    private fun handleWidgetUpdate(startId: Int) {
        serviceScope.launch {
            val repository = WidgetRepository.create(this@WidgetUpdateService)
            val errorHandler = WidgetErrorHandler()
            
            when (val result = repository.updateWidget(this@WidgetUpdateService)) {
                is WidgetErrorHandler.WidgetResult.Success -> {
                    // Update successful
                }
                is WidgetErrorHandler.WidgetResult.Error -> {
                    errorHandler.logError(this@WidgetUpdateService, result.error, "handleWidgetUpdate")
                }
            }
            
            stopSelf(startId)
        }
    }
}