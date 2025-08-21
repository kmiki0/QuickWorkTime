package com.example.quickworktime.widget

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * フリック操作による時間調整を処理するクラス
 */
class TimeAdjustmentHandler {
    
    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }
    
    /**
     * フリック操作を処理して調整された時間を返す
     * 
     * @param baseTime 基準となる時間文字列 (HH:mm形式)
     * @param direction フリック方向
     * @param timeComponent 調整対象の時間コンポーネント
     * @return 調整後の時間文字列 (HH:mm形式)
     */
    fun handleFlickGesture(
        baseTime: String, 
        direction: FlickDirection, 
        timeComponent: TimeComponent
    ): String {
        return try {
            val time = LocalTime.parse(baseTime, TIME_FORMATTER)
            val adjustedTime = when (timeComponent) {
                TimeComponent.HOUR -> adjustHour(time, direction)
                TimeComponent.MINUTE -> adjustMinute(time, direction)
            }
            adjustedTime.format(TIME_FORMATTER)
        } catch (e: Exception) {
            // パースエラーの場合は元の時間を返す
            baseTime
        }
    }
    
    /**
     * 現在時刻を5分単位で切り下げて丸める
     * 
     * @param currentTime 現在時刻
     * @return 5分単位で切り下げられた時間文字列 (HH:mm形式)
     */
    fun calculateRoundedTime(currentTime: LocalTime): String {
        val minutes = currentTime.minute
        val roundedMinutes = (minutes / 5) * 5
        val roundedTime = currentTime
            .withMinute(roundedMinutes)
            .withSecond(0)
            .withNano(0)
        return roundedTime.format(TIME_FORMATTER)
    }
    
    /**
     * 時間文字列を5分単位で切り下げて丸める
     * 
     * @param timeString 時間文字列 (HH:mm形式)
     * @return 5分単位で切り下げられた時間文字列 (HH:mm形式)
     */
    fun calculateRoundedTime(timeString: String): String {
        return try {
            val time = LocalTime.parse(timeString, TIME_FORMATTER)
            calculateRoundedTime(time)
        } catch (e: Exception) {
            timeString
        }
    }
    
    /**
     * 時間を調整する（±1時間）
     */
    private fun adjustHour(time: LocalTime, direction: FlickDirection): LocalTime {
        return when (direction) {
            FlickDirection.UP -> time.plusHours(1)
            FlickDirection.DOWN -> time.minusHours(1)
        }
    }
    
    /**
     * 分を調整する（±5分）
     */
    private fun adjustMinute(time: LocalTime, direction: FlickDirection): LocalTime {
        return when (direction) {
            FlickDirection.UP -> time.plusMinutes(5)
            FlickDirection.DOWN -> time.minusMinutes(5)
        }
    }
}

/**
 * フリック方向を表すenum
 */
enum class FlickDirection {
    UP,    // 上方向フリック
    DOWN   // 下方向フリック
}

/**
 * 時間コンポーネントを表すenum
 */
enum class TimeComponent {
    HOUR,   // 時間部分
    MINUTE  // 分部分
}