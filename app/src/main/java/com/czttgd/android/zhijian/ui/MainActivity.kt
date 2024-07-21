package com.czttgd.android.zhijian.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.Ping
import com.czttgd.android.zhijian.data.Settings
import com.czttgd.android.zhijian.databinding.ActivityMainBinding
import com.czttgd.android.zhijian.databinding.SettingsDialogBinding
import com.czttgd.android.zhijian.ui.fragments.BreakpointRecordsFragment
import com.czttgd.android.zhijian.ui.fragments.WorkspaceFragment
import com.czttgd.android.zhijian.utils.defaultNegativeButton
import com.czttgd.android.zhijian.utils.setPositiveAction
import com.czttgd.android.zhijian.utils.toast
import com.czttgd.android.zhijian.utils.unreachable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val updateFragment = { fragment: Fragment ->
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.container, fragment)
            }
        }.also { it(WorkspaceFragment()) }
        bindings.bottomNavBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.workspace -> updateFragment(WorkspaceFragment())
                R.id.records -> updateFragment(BreakpointRecordsFragment())
                else -> unreachable()
            }
            true
        }
    }
}
