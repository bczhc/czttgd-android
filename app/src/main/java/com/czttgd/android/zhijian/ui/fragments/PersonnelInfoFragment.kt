package com.czttgd.android.zhijian.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.czttgd.android.zhijian.databinding.FragmentPersonnelInfoBinding
import com.czttgd.android.zhijian.utils.androidAssertion

class PersonnelInfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = FragmentPersonnelInfoBinding.inflate(inflater, container, false)

        val evenlySpaceOut = {tv: TextView->
            tv.text.forEach {
                androidAssertion(!Character.isSurrogate(it), "we do not handle supplementary Han characters here")
            }
            val widgetWidth = tv.width
            tv.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            val textWidth = tv.measuredWidth

            tv.letterSpacing = (widgetWidth - textWidth).toFloat() / (tv.text.length.toFloat() - 1F) / tv.textSize
        }

        for (child in bindings.tableLayout.children) {
            val labelTextView = ((child as TableRow).children.first() as TextView)
            labelTextView.post {
                evenlySpaceOut(labelTextView)
            }
        }

        return bindings.root
    }
}
