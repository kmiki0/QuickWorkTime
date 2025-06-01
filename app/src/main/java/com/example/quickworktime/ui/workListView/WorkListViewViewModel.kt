package com.example.quickworktime.ui.workListView

import android.app.Application
import android.util.Log
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

    // リストデータ
    private val _listData = MutableLiveData<List<WorkInfo>>()
    val listData: LiveData<List<WorkInfo>> = _listData

    fun setMonthText(text: String) {
        _monthText.value = text
    }

    /**
     * 修正版: 不要なMutableLiveDataを削除し、直接_listDataを更新
     */
    fun getListData(date: String) {
        viewModelScope.launch {
            try {
                val data = infoRepo.getMonthWorkInfo(date)
                _listData.postValue(data) // 直接設定
            } catch (e: Exception) {
                Log.e("WorkListViewViewModel", "Error loading data for $date", e)
                _listData.postValue(emptyList()) // エラー時は空リストを設定
            }
        }
    }

    /**
     * 月のデータ件数を取得
     */
    fun getMonthDataCount(date: String): LiveData<Int> {
        val result = MutableLiveData<Int>()
        viewModelScope.launch {
            try {
                val count = infoRepo.getMonthWorkInfo(date).size
                result.postValue(count)
            } catch (e: Exception) {
                Log.e("WorkListViewViewModel", "Error getting count for $date", e)
                result.postValue(0)
            }
        }
        return result
    }

    /**
     * 指定日付のデータ存在確認と新規作成
     */
    fun selectedDataByDate(date: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                // 日付で検索して、データがなければ新規作成
                val workInfo: WorkInfo? = infoRepo.getWorkInfoByDate(date)

                if (workInfo != null) {
                    // すでにデータが存在する場合
                    result.postValue(true)
                } else {
                    // データが存在しない場合、新規作成
                    createNewWorkInfo(date)
                    result.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("WorkListViewViewModel", "Error checking data for $date", e)
                result.postValue(false)
            }
        }
        return result
    }

    /**
     * 新しいWorkInfoを作成
     */
    private suspend fun createNewWorkInfo(date: String) {
        try {
            // デフォルトの設定値を取得
            var workSetting = settingRepo.getWorkSetting()

            if (workSetting == null) {
                // デフォルトの設定値を登録
                val defaultData = WorkSetting(0, "09:00", "18:00", "01:00", "0111110")
                settingRepo.insertWorkSetting(defaultData)
                workSetting = defaultData
            }

            // WorkInfoにデータを登録
            val newWorkInfo = WorkInfo(
                date = date,
                startTime = workSetting.defaultStartTime,
                endTime = workSetting.defaultEndTime,
                workingTime = "",
                breakTime = "",
                isHoliday = false,
                isNationalHoliday = false,
                weekday = ""
            )

            infoRepo.insertWorkInfo(newWorkInfo)
        } catch (e: Exception) {
            Log.e("WorkListViewViewModel", "Error creating new work info for $date", e)
        }
    }

    /**
     * WorkInfoを削除
     */
    fun deleteWorkInfo(workInfo: WorkInfo) {
        viewModelScope.launch {
            try {
                infoRepo.deleteWorkInfo(workInfo)
            } catch (e: Exception) {
                Log.e("WorkListViewViewModel", "Error deleting work info", e)
            }
        }
    }
}