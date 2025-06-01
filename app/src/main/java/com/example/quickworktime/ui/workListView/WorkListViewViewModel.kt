package com.example.quickworktime.ui.workListView

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.quickworktime.room.AppDatabase
import com.example.quickworktime.room.WorkInfo
import com.example.quickworktime.room.WorkSetting
import com.example.quickworktime.room.repository.WorkInfoRepository
import com.example.quickworktime.room.repository.WorkSettingRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class WorkListViewViewModel(application: Application) : AndroidViewModel(application) {

    private val _monthText = MutableLiveData<String>().apply {
        // 現在の年月を取得
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        // month は00 で表示するため、2桁にする
        value = "${year}/${month.toString().padStart(2, '0')}"
    }
    val monthText: LiveData<String> = _monthText

    // データベース
    private val db = AppDatabase.getDatabase(application)
    private val infoRepo: WorkInfoRepository = WorkInfoRepository(db.workInfo())
    private val settingRepo: WorkSettingRepository = WorkSettingRepository(db.workSetting())

    // 画面データ
    private val _displayData = MutableLiveData<WorkInfo?>()
    val displayData: LiveData<WorkInfo?> get() = _displayData

    // 初期化
    init {


    }

    private val _listData = MutableLiveData<List<WorkInfo>>()
    var listData: LiveData<List<WorkInfo>> = _listData

    private val _workInfoList = MutableLiveData<List<WorkInfo>>()
    val workInfoList: LiveData<List<WorkInfo>> = _workInfoList

    fun setMonthText(text: String) {
        _monthText.postValue(text)
    }

    fun getListData(date: String) {
        val result = MutableLiveData<List<WorkInfo>>()
        viewModelScope.launch {
            result.value = infoRepo.getMonthWorkInfo(date)
            _listData.postValue(result.value)
        }
    }


    fun getMonthDataCount(date: String): LiveData<Int> {
        val result = MutableLiveData<Int>()
        viewModelScope.launch {
            result.value = infoRepo.getMonthWorkInfo(date).size
        }
        return result
    }


    fun selectedDataByDate(date: String):LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            // 日付で検索して、データがなければ新規作成
            val workInfo: WorkInfo? = infoRepo.getWorkInfoByDate(date)

            // すでにデータが存在する場合
            if (workInfo != null){
                result.value = true
            } else {
                // デフォルトの設定値を取得
                var workSetting = settingRepo.getWorkSetting()

                if (workSetting == null) {
                    // デフォルトの設定値を登録
                    val defaultData = WorkSetting(0, "09:00", "18:00", "01:00", "0111110")
                    settingRepo.insertWorkSetting(defaultData)
                    workSetting = defaultData
                }

                // WorkInfoにデータを登録
                infoRepo.insertWorkInfo(
                    WorkInfo(
                        date,
                        workSetting.defaultStartTime,
                        workSetting.defaultEndTime,
                        "",
                        "",
                        false,
                        false,
                        ""
                    )
                )
                result.value = false
            }
        }
        return result
    }


    fun deleteWorkInfo(workInfo: WorkInfo) {
        viewModelScope.launch {
            infoRepo.deleteWorkInfo(workInfo)
        }
    }
}