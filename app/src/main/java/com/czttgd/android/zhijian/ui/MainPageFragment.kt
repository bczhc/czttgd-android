package com.czttgd.android.zhijian.ui

import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.czttgd.android.zhijian.databinding.FragmentMainPageBinding
import com.czttgd.android.zhijian.utils.toastShow
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions


class MainPageFragment: Fragment() {
    private val launcher = object {
        val barcode = registerForActivityResult(ScanContract()) {
            // cancelled
            val content = it.contents
            content ?: return@registerForActivityResult
            toastShow("Scanned content: $content")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = FragmentMainPageBinding.inflate(inflater, container, false)

        bindings.scanButton.setOnClickListener {
            launcher.barcode.launch(ScanOptions())
        }
        bindings.exitSystem.setOnClickListener {
            requireActivity().finishAffinity()
            val pid = Process.myPid()
            Process.killProcess(pid)
        }

        return bindings.root
    }
}
