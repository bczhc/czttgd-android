package com.czttgd.android.zhijian

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

open class BaseActivity: AppCompatActivity() {
    protected fun Toolbar.setUpBackButton() {
        this.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
