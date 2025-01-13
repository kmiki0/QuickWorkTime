package com.example.quickworktime.ui.workListView

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.InvalidationTracker
import com.example.quickworktime.databinding.FragmentDashboardBinding
import com.example.quickworktime.room.repository.WorkInfoRepository
import java.util.Calendar

class WorkListViewFragment : Fragment() {

    private lateinit var myAdapter: TimeListAdapter

    private var _binding: FragmentDashboardBinding? = null
    private val vm: WorkListViewViewModel by viewModels()

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = binding.timeList

        // 空のリストで Adapter を初期化
        myAdapter = TimeListAdapter(mutableListOf(), vm)
        recyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        recyclerView.adapter = myAdapter

        // 画面のmonthTextにvmのmonthTextをバインド
        vm.monthText.observe(viewLifecycleOwner, { monthText ->
            binding.monthText.text = monthText
        })

        // データをロードして表示
        loadData()
        // 履歴ボタンの表示非表示の設定
        setHistoryButton()

        binding.btnBackMonth.setOnClickListener { view ->
            if (binding.btnBackMonth.alpha != 1.0f) return@setOnClickListener
            val yyyyMM = vm.monthText.value.toString().replace("/", "")
            // 表示する月を変更
            vm.setMonthText(changeMonth(yyyyMM, -1).substring(0, 4) + "/" + changeMonth(yyyyMM, -1).substring(4, 6))
            // データをロードして表示
            loadData()
            // 履歴ボタンの表示非表示の設定
            setHistoryButton()
        }

        binding.btnNextMonth.setOnClickListener { view ->
            if (binding.btnNextMonth.alpha != 1.0f) return@setOnClickListener
            val yyyyMM = vm.monthText.value.toString().replace("/", "")
            // 表示する月を変更
            vm.setMonthText(changeMonth(yyyyMM, 1).substring(0, 4) + "/" + changeMonth(yyyyMM, 1).substring(4, 6))
            // データをロードして表示
            loadData()
            // 履歴ボタンの表示非表示の設定
            setHistoryButton()
        }

        // リスナーを設定
        binding.btnMenu.setOnClickListener { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 履歴ボタンの表示非表示の設定
    private fun setHistoryButton() {

        // LiveDataの監視
        vm.monthText.observe(viewLifecycleOwner, { monthText ->

            // yyyyMM を取得
            val yyyyMM = monthText.replace("/", "")

            // 前月
            val backMonth = changeMonth(yyyyMM, -1)

            vm.getMonthDataCount(backMonth).observe(viewLifecycleOwner, { count ->
                if (count > 0) {
                    binding.btnBackMonth.alpha = 1.0f
                } else {
                    binding.btnBackMonth.alpha = 0.2f
                }
            })

            // 次の月
            val nextMonth = changeMonth(yyyyMM, 1)
            vm.getMonthDataCount(nextMonth).observe(viewLifecycleOwner, { count ->
                if (count > 0) {
                    binding.btnNextMonth.alpha = 1.0f
                } else {
                    binding.btnNextMonth.alpha = 0.2f
                }
            })

            // 表示する月を変更
            binding.monthText.text = monthText
        })
    }

    // 引数によって、月を変更する
    private fun changeMonth(yyyyMM: String,  month: Int): String {
        // yyyyMM をDate型に変換
        val date = java.text.SimpleDateFormat("yyyyMM").parse(yyyyMM)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MONTH, month)

        return java.text.SimpleDateFormat("yyyyMM").format(calendar.time)
    }

    private fun loadData() {

        // yyyyMM を取得
        val yyyyMM = vm.monthText.value.toString().replace("/", "")

        vm.getListData(yyyyMM)

        // Roomの変更を検知し、変更時にtextViewの内容を変更する
        vm.listData.observe(viewLifecycleOwner, { value ->
            value?.let {
                myAdapter.updateData(it)   // Adapter にデータを更新

                // data の中のworkingTimeを合計して表示
                var sum: Float = 0.0f
                for (i in it) {
                    // workingTime は "HH:mm" 形式なので、":" を取り除いて Float に変換
                    val time = i.workingTime.split(":")
                    // 下２桁で四捨五入
                    sum += (time[0] + "." + time[1]).toFloat()
                }
                binding.totalTimeText.text = sum.toString() + " h"
            }
        })

//        vm.getListData(yyyyMM).observe(viewLifecycleOwner, { listData ->
//            // データを取得
//            myAdapter.updateData(listData)   // Adapter にデータを更新
//
//            // data の中のworkingTimeを合計して表示
//            var sum: Float = 0.0f
//            for (i in listData) {
//                // workingTime は "HH:mm" 形式なので、":" を取り除いて Float に変換
//                val time = i.workingTime.split(":")
//                // 下２桁で四捨五入
//                sum += (time[0] + "." + time[1]).toFloat()
//            }
//            binding.totalTimeText.text = sum.toString() + " h"
//        })

        if (vm.listData.value != null) {
            // データを取得
            myAdapter.updateData(vm.listData.value!!)   // Adapter にデータを更新

            // data の中のworkingTimeを合計して表示
            var sum: Float = 0.0f
            for (i in vm.listData.value!!) {
                // workingTime は "HH:mm" 形式なので、":" を取り除いて Float に変換
                val time = i.workingTime.split(":")
                // 下２桁で四捨五入
                sum += (time[0] + "." + time[1]).toFloat()
            }
            binding.totalTimeText.text = sum.toString() + " h"
        }
    }
}

