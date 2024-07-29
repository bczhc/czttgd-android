package com.czttgd.android.zhijian.ui

import android.os.Bundle
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.databinding.ActivityManualBinding

class ManualActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ActivityManualBinding.inflate(layoutInflater)
        setContentView(bindings.root)
    }
}
