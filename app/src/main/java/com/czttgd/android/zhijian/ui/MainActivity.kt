package com.czttgd.android.zhijian.ui

import android.content.Intent
import android.os.Bundle
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.card2.button.setOnClickListener {
            startActivity(Intent(this, FormFillingActivity::class.java))
        }
    }
}
