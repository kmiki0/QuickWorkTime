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
import com.example.quickworktime.room.repository.WorkInfoRepository
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // アクティブ状態のテキストフィールド
    enum class ActiveText {
        DEFAULT, // 初期状態 (DEFAULT)
        START,   // 開始時間 (START)
        END,     // 終了時間 (END)
    }

    // データベース
    private val workInfoDao: WorkInfoDao = AppDatabase.getDatabase(application).workInfo()
    private val repo: WorkInfoRepository = WorkInfoRepository(workInfoDao)

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
            // date が NULL の場合、最新のデータを取得
            val data = repo.getWorkInfoByDate(date)
            if (data == null) {
                return@launch
            } else {
                _displayData.postValue(data)
            }
        }
    }

    // データ登録
    fun insertWorkInfo(workInfo: WorkInfo) {
        viewModelScope.launch {
            repo.insertWorkInfo(workInfo)
        }
    }


}