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
 * ウィジェットの更新操作をバックグラウンドで処理するサービス
 * このサービスは退勤記録とウィジェットの更新操作を処理します
 */
class WidgetUpdateService : Service() {
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)


    companion object {
        const val ACTION_RECORD_CLOCK_OUT = "com.example.quickworktime.widget.ACTION_RECORD_CLOCK_OUT"
        const val ACTION_UPDATE_WIDGET = "com.example.quickworktime.widget.ACTION_UPDATE_WIDGET"
        const val EXTRA_CLOCK_OUT_TIME = "clock_out_time"

        // 重複実行防止用フラグ
        @Volatile
        private var isRecordingClockOut = false

        /**
         * 退勤記録を開始するヘルパーメソッド（重複実行防止付き）
         */
        fun startClockOutRecording(context: Context, clockOutTime: String? = null) {
            if (isRecordingClockOut) {
                Log.w("WidgetUpdateService", "既に退勤記録処理中のため、リクエストをスキップ")
                return
            }

            Log.i("WidgetUpdateService", "退勤記録サービス開始")
            val intent = Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_RECORD_CLOCK_OUT
                // nullでない場合、clockOutTime 追加
                clockOutTime?.let { putExtra(EXTRA_CLOCK_OUT_TIME, it) }
            }
            context.startService(intent)
        }

        /**
         * ウィジェット更新をトリガーするヘルパーメソッド
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
        Log.i("WidgetUpdateService", "onStartCommand() 呼び出し - action: ${intent?.action}")

        when (intent?.action) {
            ACTION_RECORD_CLOCK_OUT -> {
                if (isRecordingClockOut) {
                    Log.w("WidgetUpdateService", "既に退勤記録処理中のため、処理をスキップ")
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                handleClockOutRecording(intent, startId)
            }
            ACTION_UPDATE_WIDGET -> {
                handleWidgetUpdate(startId)
            }
            else -> {
                Log.w("WidgetUpdateService", "不明なアクション: ${intent?.action}")
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
     * バックグラウンドスレッドで退勤記録処理を行う（重複実行防止付き）
     */
    private fun handleClockOutRecording(intent: Intent, startId: Int) {
        Log.i("WidgetUpdateService", "handleClockOutRecording() 開始")
        isRecordingClockOut = true

        serviceScope.launch {
            try {
                val repository = WidgetRepository.create(this@WidgetUpdateService)
                repository.initialize(this@WidgetUpdateService)
                val errorHandler = WidgetErrorHandler()

                // Intentから退勤時間を取得、なければ最適時間を計算
                val clockOutTime = intent.getStringExtra(EXTRA_CLOCK_OUT_TIME)
                    ?: when (val timeResult = repository.calculateOptimalClockOutTime()) {
                        is WidgetErrorHandler.WidgetResult.Success -> timeResult.data
                        is WidgetErrorHandler.WidgetResult.Error -> {
                            Log.e("WidgetUpdateService", "最適時間計算失敗: ${timeResult.error.message}")
                            "18:00" // フォールバック
                        }
                    }

                Log.i("WidgetUpdateService", "退勤記録処理開始 - 時間: $clockOutTime")

                // 退勤時間を記録
                when (val recordResult = repository.recordClockOut(clockOutTime)) {
                    is WidgetErrorHandler.WidgetResult.Success -> {
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
                        Log.e("WidgetUpdateService", "退勤記録失敗: ${recordResult.error.message}")
                        errorHandler.logError(this@WidgetUpdateService, recordResult.error, "recordClockOut")

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
                Log.e("WidgetUpdateService", "退勤記録中に予期しないエラー", e)
            } finally {
                isRecordingClockOut = false
                stopSelf(startId)
                Log.i("WidgetUpdateService", "handleClockOutRecording() 終了")
            }
        }
    }

    /**
     * バックグラウンドスレッドでウィジェットを更新し、エラー処理を行います
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