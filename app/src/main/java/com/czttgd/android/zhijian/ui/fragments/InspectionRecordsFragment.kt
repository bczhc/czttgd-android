@file:Suppress("NonAsciiCharacters", "EnumEntryName")

package com.czttgd.android.zhijian.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.data.Inspection.LIST_LIMIT
import com.czttgd.android.zhijian.data.InspectionSummary
import com.czttgd.android.zhijian.databinding.FragmentBreakpointRecordsBinding
import com.czttgd.android.zhijian.databinding.InspectionRecordsListItemBinding
import com.czttgd.android.zhijian.dbDateFormatter
import com.czttgd.android.zhijian.dottedDateFormatter
import com.czttgd.android.zhijian.ui.InspectionDetailsActivity
import com.czttgd.android.zhijian.utils.*
import com.google.android.material.tabs.TabLayout

class InspectionRecordsFragment : Fragment() {
    private val itemData = mutableListOf<InspectionSummary>()
    private lateinit var bindings: FragmentBreakpointRecordsBinding
    private lateinit var listAdapter: ListAdapter

    private var detailsLauncher: ActivityResultLauncher<Long>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        detailsLauncher = registerForActivityResult(InspectionDetailsActivity.ActivityContract()) {
            val id = it ?: return@registerForActivityResult

            requireContext().apply {
                buildProgressDialog(getString(R.string.updating_dialog_title)) {
                    coroutineLaunchIo {
                        val result = runCatching {
                            Inspection.queryDetails(id)
                        }
                        withMain {
                            result.onFailure {
                                toast(R.string.request_failed_toast)
                            }.onSuccess { details ->
                                val summary = InspectionSummary.fromDetails(id, details)
                                val index = itemData.indexOfFirst { x -> x.id == id }
                                itemData[index] = summary
                                listAdapter.notifyItemChanged(index)
                            }
                            it.dismiss()
                        }
                    }
                }.show()
            }
        }

        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        detailsLauncher = null
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        bindings = FragmentBreakpointRecordsBinding.inflate(inflater)
        bindings.setUpViews()
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindings.tabLayout.selectIndex(SharedStates.tabIndex)
        bindings.searchView.setQuery(SharedStates.searchQuery, false)
        queryAndUpdateList()
    }

    override fun onDestroyView() {
        SharedStates.tabIndex = bindings.tabLayout.selectedIndex()
        SharedStates.searchQuery = bindings.searchView.query.toString()
        super.onDestroyView()
    }

    private fun RecyclerView.setUpUi() {
        var page = 0
        var loading = false
        val progressBar = bindings.loadingPi

        val context = requireContext()
        listAdapter = ListAdapter(itemData)
        layoutManager = LinearLayoutManager(context)
        adapter = listAdapter
        addOnScrollListener(object : OnScrollListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    if (loading) return
                    loading = true
                    page += 1
                    progressBar.visibility = View.VISIBLE
                    coroutineLaunchIo {
                        val result = runCatching {
                            Inspection.querySummary(
                                bindings.searchView.query.toString(), currentStage(),
                                limit = LIST_LIMIT, offset = page * LIST_LIMIT
                            ).data!!
                        }
                        withMain {
                            progressBar.visibility = View.GONE
                            result.onSuccess {
                                itemData.addAll(it)
                                itemData.distinctBy { x -> x.id }
                                adapter!!.notifyDataSetChanged()
                            }.onFailure {
                                requireContext().toast(R.string.request_failed_toast)
                            }
                            loading = false
                        }
                    }
                }
            }
        })

        listAdapter.setOnItemClickListener { position, _ ->
            val id = itemData[position].id
            detailsLauncher!!.launch(id)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun FragmentBreakpointRecordsBinding.setUpViews() {
        requireContext().apply contextScope@{
            recyclerView.setUpUi()

            iv.setOnClickListener {
                queryAndUpdateList()
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                queryAndUpdateList()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun queryAndUpdateList() {
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
                                dbDateFormatter.parse(x.creationTime)!!
                            }
                            itemData.clear()
                            itemData.addAll(it.data ?: arrayOf())
                            itemData.distinctBy { x -> x.id }
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
        return when (bindings.tabLayout.selectedIndex()) {
            0 -> 1
            1 -> 2
            else -> unreachable()
        }
    }

    enum class State {
        已初检,
        已终检;

        companion object {
            fun from(int: Int): State {
                return when (int) {
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
        AdapterWithClickListener<ListAdapter.MyViewHolder>() {
        class MyViewHolder(val bindings: InspectionRecordsListItemBinding) : RecyclerView.ViewHolder(bindings.root)

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val bindings = InspectionRecordsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(bindings)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val item = itemData[position]
            val context = holder.bindings.inspectTv.context
            context.apply {
                holder.bindings.apply {
                    val cause = when (item.inspectionFlag) {
                        0 -> {
                            // 初检
                            item.breakCauseA?.cause
                        }

                        1 -> {
                            // 终检
                            item.breakCauseB?.cause
                        }

                        2 -> {
                            ""
                        }

                        else -> throw RuntimeException("Unexpected value")
                    }

                    titleTv.text = getString(R.string.inspection_records_card_title, item.deviceCode)
                    subtitleTv.text = cause
                    bodyLine1.text = getString(R.string.inspection_records_card_body1, item.breakSpec)
                    bodyLine2.text = getString(R.string.inspection_records_card_body2, item.productSpec ?: "")
                    creatorTv.text = item.creator.name
                    when (State.from(item.inspectionFlag)) {
                        State.已初检 -> {
                            inspectTv.text = getString(R.string.inspection_records_未终检)
                            inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_a))
                        }

                        State.已终检 -> {
                            inspectTv.text = getString(R.string.inspection_records_已终检)
                            inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_b))
                        }
                    }

                    val date = dbDateFormatter.tryParse(item.creationTime)
                    date?.let {
                        dateTv.text = dottedDateFormatter.format(it)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return itemData.size
        }
    }

    companion object {
        /**
         * workaround for fragment not saving view states
         */
        object SharedStates {
            var tabIndex: Int = 0
            var searchQuery: String = ""
        }
    }

    private fun TabLayout.selectedIndex(): Int {
        if (getTabAt(0)!!.isSelected) return 0
        return 1
    }

    private fun TabLayout.selectIndex(i: Int) {
        when (i) {
            0 -> {
                getTabAt(0)!!.select()
            }

            1 -> {
                getTabAt(1)!!.select()
            }
        }
    }
}
