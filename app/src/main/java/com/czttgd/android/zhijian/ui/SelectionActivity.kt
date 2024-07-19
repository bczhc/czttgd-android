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
import com.czttgd.android.zhijian.utils.getTypedSerializableExtra


class SelectionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        bindings.toolbar.setUpBackButton()

        val extra = intent.getTypedSerializableExtra<Array<String>>(EXTRA_LIST_ITEMS)
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
                putExtra(EXTRA_SELECTED_ITEM, listItems[position])
            }
            setResult(0, resultIntent)
            finish()
        }
    }

    class ListAdapter(private val items: List<String>) : AdapterWithClickListener<ListAdapter.MyViewHolder>() {
        class MyViewHolder(bindings: SelectionActivityListItemBinding) : ViewHolder(bindings.root) {
            val textView = bindings.tv
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.textView.text = items[position]
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val bindings = SelectionActivityListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(bindings)
        }
    }

    companion object {
        /**
         * serializable extra
         *
         * type: an [Array] of [String]s
         */
        const val EXTRA_LIST_ITEMS = "items"

        /**
         * string result extra
         */
        const val EXTRA_SELECTED_ITEM = "selected item"
    }

    class ActivityContract : ActivityResultContract<Array<String>, String?>() {
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return Intent(context, SelectionActivity::class.java).apply {
                putExtra(EXTRA_LIST_ITEMS, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return (intent ?: return null).getStringExtra(EXTRA_SELECTED_ITEM)
        }
    }
}
