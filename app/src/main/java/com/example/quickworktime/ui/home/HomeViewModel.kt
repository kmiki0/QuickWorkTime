package com.example.quickworktime.ui.home

import android.app.Application
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.quickworktime.room.AppDatabase
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.WorkInfoDao
import com.example.quickworktime.room.WorkSetting
import com.example.quickworktime.room.repository.WorkInfoRepository
import com.example.quickworktime.room.repository.WorkSettingRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // アクティブ状態のテキストフィールド
    enum class ActiveText {
        DEFAULT, // 初期状態 (DEFAULT)
        START,   // 開始時間 (START)
        END,     // 終了時間 (END)
    }

    // データベース
    private val db = AppDatabase.getDatabase(application)
    private val workInfoDao: WorkInfoDao = db.workInfo()
    private val repo: WorkInfoRepository = WorkInfoRepository(workInfoDao)
    private val settingRepo: WorkSettingRepository = WorkSettingRepository(db.workSetting())

    // 画面データ
    private val _displayData = MutableLiveData<WorkInfo?>()
    val displayData: LiveData<WorkInfo?> get() = _displayData

    // アクティブ状態を保持するLiveData
    private val _activeTextState = MutableLiveData<ActiveText>()
    val activeTextState: LiveData<ActiveText> = _activeTextState
    // 状態を更新する関数
    fun updateActiveTextState(activeText: ActiveText) {
        _activeTextState.value = activeText
    }

    // アクティブ状態のTextView
    private val _activeTextView = MutableLiveData<TextView>()
    val activeTextView: LiveData<TextView> = _activeTextView
    // 更新関数
    fun setActiveTextView(activeTextView: TextView) {
        _activeTextView.value = activeTextView
    }


    // ----------------------------------------
    // 画面項目 (Data Binding)
    // ----------------------------------------

    // 日付 (yyyy/MM/dd)
    private val _date = MutableLiveData<String?>()
    val date: LiveData<String?> get() = _date

    // 曜日
    private val _week = MutableLiveData<String?>()
    val week: LiveData<String?> get() = _week

    // 開始時間（HH）
    private val _startHour = MutableLiveData<String?>()
    val startHour: LiveData<String?> get() = _startHour

    // 開始時間（mm）
    private val _startMinute = MutableLiveData<String?>()
    val startMinute: LiveData<String?> get() = _startMinute

    // 終了時間（HH）
    private val _endHour = MutableLiveData<String?>()
    val endHour: LiveData<String?> get() = _endHour

    // 終了時間（mm）
    private val _endMinute = MutableLiveData<String?>()
    val endMinute: LiveData<String?> get() = _endMinute


    // ----------------------------------------
    // データバインディング用の関数
    // ----------------------------------------
    fun setDate(date: String) {
        _date.value = date.substring(0, 4) + "/" + date.substring(4, 6) + "/" + date.substring(6, 8)
    }
    fun setWeek(week: String) {
        _week.value = "($week)"
    }
    fun setStartHour(hour: String) {
        _startHour.value = hour.padStart(2, '0')
    }
    fun setStartMinute(minute: String) {
        _startMinute.value = minute.padStart(2, '0')
    }
    fun setEndHour(hour: String) {
        _endHour.value = hour.padStart(2, '0')
    }
    fun setEndMinute(minute: String) {
        _endMinute.value = minute.padStart(2, '0')
    }

    fun setDisplayDate(workInfo: WorkInfo) {
        setDate(workInfo.date)
        setWeek(workInfo.weekday)
        setStartHour(workInfo.startTime.split(":")[0])
        setStartMinute(workInfo.startTime.split(":")[1])
        setEndHour(workInfo.endTime.split(":")[0])
        setEndMinute(workInfo.endTime.split(":")[1])
    }


    // 初期化
    init {
        // 初期表示
        getDisplayData("")
    }


    /** ============================================
     *  PKに沿ったデータを取得（引数がNULL）
     *  @param date String?
     *  @return LiveData<WorkInfo>
     *  ============================================ */
    fun getDisplayData(date: String?) {
        viewModelScope.launch {
            try {
                var searchDate = date
                var data: WorkInfo? = null

                if (searchDate.isNullOrEmpty()) {
                    // 引数がnullまたは空の場合、最新のデータを取得
                    val latestDate = repo.getLatestDate()
                    if (latestDate.isNotEmpty()) {
                        searchDate = latestDate
                        data = repo.getWorkInfoByDate(searchDate)
                    }
                } else {
                    // 指定された日付のデータを取得
                    data = repo.getWorkInfoByDate(searchDate)
                }

                if (data != null) {
                    // データが存在する場合
                    _displayData.postValue(data)
                } else {
                    // データが存在しない場合、新規作成
                    val newData = createDefaultWorkInfo(searchDate)
                    if (newData != null) {
                        _displayData.postValue(newData)
                    }
                }
            } catch (e: Exception) {
                // エラーの場合、今日の日付でデフォルトデータを作成
                val todayDate = getCurrentDateString()
                val defaultData = createDefaultWorkInfo(todayDate)
                if (defaultData != null) {
                    _displayData.postValue(defaultData)
                }
            }
        }
    }

    /**
     * デフォルトのWorkInfoを作成（データベースには登録しない）
     */
    private suspend fun createDefaultWorkInfo(targetDate: String?): WorkInfo? {
        return try {
            // 使用する日付を決定
            val useDate = if (targetDate.isNullOrEmpty()) {
                getCurrentDateString()
            } else {
                targetDate
            }

            // デフォルトの設定値を取得または作成
            var workSetting = settingRepo.getWorkSetting()
            if (workSetting == null) {
                // デフォルトの設定値を登録（WorkSettingのみ永続化）
                val defaultSetting = WorkSetting(0, "09:00", "18:00", "01:00", "0111110")
                settingRepo.insertWorkSetting(defaultSetting)
                workSetting = defaultSetting
            }

            // 曜日を取得
            val weekday = getWeekdayFromDate(useDate)

            // 新しいWorkInfoを作成（データベースには登録しない）
            val newWorkInfo = WorkInfo(
                date = useDate,
                startTime = workSetting.defaultStartTime,
                endTime = workSetting.defaultEndTime,
                workingTime = "",
                breakTime = "",
                isHoliday = false,
                isNationalHoliday = false,
                weekday = weekday
            )

            // データベースには登録せず、オブジェクトのみ返す
            newWorkInfo
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 現在の日付をyyyyMMdd形式で取得
     */
    private fun getCurrentDateString(): String {
        val today = LocalDate.now()
        return today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

    /**
     * 日付文字列から曜日を取得
     */
    private fun getWeekdayFromDate(dateString: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val date = LocalDate.parse(dateString, formatter)
            date.dayOfWeek.toString()
        } catch (e: Exception) {
            "MONDAY" // デフォルト値
        }
    }

    // データ登録
    fun insertWorkInfo(workInfo: WorkInfo) {
        viewModelScope.launch {
            repo.insertWorkInfo(workInfo)
        }
    }


}