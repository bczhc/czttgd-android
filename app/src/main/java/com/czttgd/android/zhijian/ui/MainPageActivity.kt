package com.czttgd.android.zhijian.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.databinding.ActivityMainPageBinding
import com.czttgd.android.zhijian.ui.fragments.MainPageFragment
import com.czttgd.android.zhijian.ui.fragments.PersonnelInfoFragment
import com.czttgd.android.zhijian.ui.fragments.断线管理
import com.czttgd.android.zhijian.ui.fragments.断线记录

class MainPageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.container, MainPageFragment())
        }

        val updateFragment = { fragment: Fragment ->
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.container, fragment)
            }
        }

        val bottomButtonGroup = bindings.bottomNavigator
        bottomButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.main_page_bottom_item_首页 -> updateFragment(MainPageFragment())
                R.id.main_page_bottom_item_断线记录 -> updateFragment(断线记录())
                R.id.main_page_bottom_item_断线管理 -> updateFragment(断线管理())
                R.id.main_page_bottom_item_我的 -> updateFragment(PersonnelInfoFragment())
            }
        }
    }
}
