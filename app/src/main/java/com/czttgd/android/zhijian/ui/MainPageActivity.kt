package com.czttgd.android.zhijian.ui

import android.os.Bundle
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.LoginRepository
import com.czttgd.android.zhijian.databinding.ActivityMainPageBinding
import kotlinx.coroutines.launch

class MainPageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            println(LoginRepository.login("abc", "123"))
        }

        val bindings = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.container, MainPageFragment())
        }
    }
}
