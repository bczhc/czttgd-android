@file:Suppress("NonAsciiCharacters", "EnumEntryName")

package com.czttgd.android.zhijian.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.databinding.FragmentBreakpointRecordsBinding
import com.czttgd.android.zhijian.databinding.InspectionRecordsListItemBinding
import com.czttgd.android.zhijian.dateFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

class InspectionRecordsFragment : Fragment() {
    private val itemData = mutableListOf<ItemData>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = FragmentBreakpointRecordsBinding.inflate(inflater)
        bindings.setUpViews()
        return bindings.root
    }

    private fun FragmentBreakpointRecordsBinding.setUpViews() {
        val date = Date()
        val mockData = listOf(
            ItemData(1.toUInt(), "中央爆裂", "TXR1.8(TTGD)3.0", "TXR8/0.148(JXZY)", "沈立昊", State.已初检, date),
            ItemData(2.toUInt(), "边缘开裂", "TXR1.9(TTGD)3.1", "TXR9/0.149(JXZY)", "李晓明", State.已初检, date),
            ItemData(3.toUInt(), "表面划伤", "TXR2.0(TTGD)3.2", "TXR10/0.150(JXZY)", "张伟", State.已终检, date),
            ItemData(4.toUInt(), "边缘破损", "TXR2.1(TTGD)3.3", "TXR11/0.151(JXZY)", "王芳", State.已初检, date),
            ItemData(5.toUInt(), "中央裂缝", "TXR2.2(TTGD)3.4", "TXR12/0.152(JXZY)", "陈杰", State.已终检, date),
            ItemData(6.toUInt(), "表面磨损", "TXR2.3(TTGD)3.5", "TXR13/0.153(JXZY)", "刘强", State.已初检, date),
            ItemData(7.toUInt(), "中央磨损", "TXR2.4(TTGD)3.6", "TXR14/0.154(JXZY)", "赵敏", State.已初检, date),
            ItemData(8.toUInt(), "边缘磨损", "TXR2.5(TTGD)3.7", "TXR15/0.155(JXZY)", "孙浩", State.已终检, date),
            ItemData(9.toUInt(), "表面破损", "TXR2.6(TTGD)3.8", "TXR16/0.156(JXZY)", "杨华", State.已初检, date),
            ItemData(10.toUInt(), "中央划痕", "TXR2.7(TTGD)3.9", "TXR17/0.157(JXZY)", "吴丽", State.已终检, date),
        )
        itemData.addAll(mockData)

        val listAdapter = ListAdapter(itemData)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
        }

        iv.setOnClickListener {
        }
    }

    data class ItemData(
        val machineNumber: UInt,
        val breakCause: String,
        val breakSpecs: String,
        val productSpecs: String,
        val creator: String,
        val state: State,
        val date: Date,
    )

    enum class State {
        已初检,
        已终检,
    }

    private val dateFormatter by lazy {
        SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    }

    class ListAdapter(private val itemData: List<ItemData>) : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
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
                    subtitleTv.text = item.breakCause
                    bodyLine1.text = getString(R.string.inspection_records_card_body1, item.breakSpecs)
                    bodyLine2.text = getString(R.string.inspection_records_card_body2, item.productSpecs)
                    creatorTv.text = item.creator
                    when (item.state) {
                        State.已初检 -> {
                            inspectTv.text = getString(R.string.inspection_records_已初检)
                            inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_a))
                        }
                        State.已终检 -> {
                            inspectTv.text = getString(R.string.inspection_records_已终检)
                            inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_b))
                        }
                    }
                    dateTv.text = dateFormatter.format(item.date)
                }
            }
        }

        override fun getItemCount(): Int {
            return itemData.size
        }
    }
}
