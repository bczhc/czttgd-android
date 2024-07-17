package com.czttgd.android.zhijian.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.czttgd.android.zhijian.databinding.FragmentBreakpointRecordsBinding

class BreakpointRecordsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = FragmentBreakpointRecordsBinding.inflate(inflater)

        return bindings.root
    }
}
