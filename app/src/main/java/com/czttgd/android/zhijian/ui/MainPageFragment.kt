package com.czttgd.android.zhijian.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.czttgd.android.zhijian.databinding.FragmentMainPageBinding

class MainPageFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = FragmentMainPageBinding.inflate(inflater, container, false)
        return bindings.root
    }
}
