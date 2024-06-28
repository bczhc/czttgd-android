package com.czttgd.android.zhijian.ui

import android.os.Bundle
import androidx.fragment.app.commit
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.databinding.ActivityMainPageBinding

class MainPageActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.container, MainPageFragment())
        }
    }
}
