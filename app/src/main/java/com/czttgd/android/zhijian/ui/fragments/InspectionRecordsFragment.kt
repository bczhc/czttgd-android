@file:Suppress("NonAsciiCharacters", "EnumEntryName")

package com.czttgd.android.zhijian.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.data.InspectionSummary
import com.czttgd.android.zhijian.databinding.FragmentBreakpointRecordsBinding
import com.czttgd.android.zhijian.databinding.InspectionRecordsListItemBinding
import com.czttgd.android.zhijian.dbDateFormatter
import com.czttgd.android.zhijian.utils.buildProgressDialog
import com.czttgd.android.zhijian.utils.coroutineLaunchIo
import com.czttgd.android.zhijian.utils.toast
import com.czttgd.android.zhijian.utils.withMain
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class InspectionRecordsFragment : Fragment() {
    private val itemData = mutableListOf<InspectionSummary>()
    private lateinit var bindings: FragmentBreakpointRecordsBinding
    private lateinit var listAdapter: ListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindings = FragmentBreakpointRecordsBinding.inflate(inflater)
        bindings.setUpViews()
        return bindings.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun FragmentBreakpointRecordsBinding.setUpViews() {
        requireContext().apply contextScope@{
            listAdapter = ListAdapter(itemData)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@contextScope)
                adapter = listAdapter
            }

            iv.setOnClickListener {
                queryList()
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                queryList()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun queryList() {
        requireContext().apply {
            buildProgressDialog(getString(R.string.fetching_dialog_title)) {
                coroutineLaunchIo {
                    val result = runCatching {
                        Inspection.querySummary(bindings.searchView.query.toString(), currentStage())
                    }
                    withMain {
                        result.onSuccess {
                            val items = it.data ?: arrayOf()
                            items.sortByDescending { x ->
                                dbDateFormatter.parse(x.creationTime)
                            }
                            itemData.clear()
                            itemData.addAll(it.data ?: arrayOf())
                            listAdapter.notifyDataSetChanged()
                        }.onFailure {
                            toast(R.string.request_failed_dialog)
                            it.printStackTrace()
                        }
                        it.dismiss()
                    }
                }
            }.show()
        }
    }

    private fun currentStage(): Int {
        return when (bindings.tabLayout.getTabAt(0)!!.isSelected) {
            true -> 1
            false -> 2
        }
    }

    enum class State {
        已初检,
        已终检;

        companion object {
            fun from(int: UInt): State {
                return when (int.toInt()) {
                    0 -> 已初检
                    1 -> 已终检
                    else -> {
                        throw RuntimeException("Unknown state number: $int")
                    }
                }
            }
        }
    }

    class ListAdapter(private val itemData: List<InspectionSummary>) :
        RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
        class MyViewHolder(val bindings: InspectionRecordsListItemBinding) : RecyclerView.ViewHolder(bindings.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val bindings = InspectionRecordsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(bindings)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val item = itemData[position]
            val context = holder.bindings.inspectTv.context
            context.apply {
                holder.bindings.apply {
                    titleTv.text = getString(R.string.inspection_records_card_title, item.machineNumber.toInt())
                    subtitleTv.text = item.cause
                    bodyLine1.text = getString(R.string.inspection_records_card_body1, item.breakSpec)
                    bodyLine2.text = getString(R.string.inspection_records_card_body2, item.productSpec)
                    creatorTv.text = item.creator
                    when (State.from(item.checkingState)) {
                        State.已初检 -> {
                            inspectTv.text = getString(R.string.inspection_records_已初检)
                            inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_a))
                        }

                        State.已终检 -> {
                            inspectTv.text = getString(R.string.inspection_records_已终检)
                            inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_b))
                        }
                    }

                    val date = dbDateFormatter.parse(item.creationTime)
                    date?.let {
                        dateTv.text = cardDateFormatter.format(it)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return itemData.size
        }
    }

    companion object {
        val cardDateFormatter by lazy {
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
        }
    }
}