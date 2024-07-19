package com.czttgd.android.zhijian.ui

import android.os.Bundle
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.databinding.ActivityFormFillingBinding

class FormFillingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ActivityFormFillingBinding.inflate(layoutInflater)

        bindings.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setContentView(bindings.root)
    }

    companion object {
        /**
         * Integer extra
         *
         * 期数（1或2）
         *
         * See [STAGE_ONE] and [STAGE_TWO]
         */
        const val EXTRA_STAGE = "stage"

        const val STAGE_ONE = 1

        const val STAGE_TWO = 2
    }
}
