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
    fun updateActiveTextView(activeTextView: TextView) {
        _activeTextView.value = activeTextView
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