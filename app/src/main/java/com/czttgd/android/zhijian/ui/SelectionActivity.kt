package com.czttgd.android.zhijian.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContract
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.databinding.ActivitySelectionBinding
import com.czttgd.android.zhijian.databinding.SelectionActivityListItemBinding
import com.czttgd.android.zhijian.utils.AdapterWithClickListener
import com.czttgd.android.zhijian.utils.androidAssertion
import com.czttgd.android.zhijian.utils.getTypedSerializableExtra
import java.io.Serializable


class SelectionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        bindings.toolbar.setUpBackButton()

        val extra = intent.getTypedSerializableExtra<Array<Item>>(EXTRA_LIST_ITEMS)
        val listItems = (extra ?: arrayOf()).toList()

        val listAdapter = ListAdapter(listItems)
        bindings.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SelectionActivity)
            adapter = listAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@SelectionActivity,
                    LinearLayout.VERTICAL
                )
            )
        }

        listAdapter.setOnItemClickListener { position, _ ->
            val resultIntent = Intent().apply {
                putExtra(EXTRA_SELECTED_INDEX, position)
                putExtra(EXTRA_LIST_ITEMS, listItems.toTypedArray())
            }
            setResult(0, resultIntent)
            finish()
        }
    }

    class ListAdapter(private val items: List<Item>) : AdapterWithClickListener<ListAdapter.MyViewHolder>() {
        class MyViewHolder(bindings: SelectionActivityListItemBinding) : ViewHolder(bindings.root) {
            val textView = bindings.tv
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.textView.text = items[position].text
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val bindings = SelectionActivityListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(bindings)
        }
    }

    data class Item(
        val id: Int,
        val text: String,
    ): Serializable

    companion object {
        /**
         * serializable extra
         *
         * type: an [Array] of [Item]s
         */
        const val EXTRA_LIST_ITEMS = "items"

        /**
         * int result extra
         *
         * returns the selected index
         */
        const val EXTRA_SELECTED_INDEX = "selected"
    }

    class ActivityContract : ActivityResultContract<Array<Item>, ActivityContract.Output?>() {
        class Output(
            val items: Array<Item>,
            val selected: Int,
        )

        override fun createIntent(context: Context, input: Array<Item>): Intent {
            return Intent(context, SelectionActivity::class.java).apply {
                putExtra(EXTRA_LIST_ITEMS, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Output? {
            intent ?: return null
            androidAssertion(intent.hasExtra(EXTRA_SELECTED_INDEX))
            val index = intent.getIntExtra(EXTRA_SELECTED_INDEX, -1)
            val items = intent.getTypedSerializableExtra<Array<Item>>(EXTRA_LIST_ITEMS)!!
            return Output(items, index)
        }
    }
}
