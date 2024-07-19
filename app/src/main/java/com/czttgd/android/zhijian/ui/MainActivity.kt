package com.czttgd.android.zhijian.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.databinding.ActivityMainBinding
import com.czttgd.android.zhijian.databinding.SettingsDialogBinding
import com.czttgd.android.zhijian.ui.fragments.BreakpointRecordsFragment
import com.czttgd.android.zhijian.ui.fragments.WorkspaceFragment
import com.czttgd.android.zhijian.utils.defaultNegativeButton
import com.czttgd.android.zhijian.utils.setPositiveAction
import com.czttgd.android.zhijian.utils.unimplemented
import com.czttgd.android.zhijian.utils.unreachable
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        bindings.settingsIv.setOnClickListener {
            val dialogBindings = SettingsDialogBinding.inflate(layoutInflater)

            MaterialAlertDialogBuilder(this)
                .defaultNegativeButton()
                .setPositiveAction { dialog, which ->

                }
                .setTitle(R.string.settings_dialog_title)
                .setView(dialogBindings.root)
                .show()
        }
    }
}
