package com.czttgd.android.zhijian.ui

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
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

            val ipEt = dialogBindings.ipEt
            val passwordEt = dialogBindings.passwordEt
            val usernameEt = dialogBindings.usernameEt

            val settings = Settings.read()

            ipEt.setText(settings.server?.ip ?: "")
            usernameEt.setText(settings.database?.username ?: "")
            passwordEt.setText(settings.database?.password ?: "")

            val dialog = MaterialAlertDialogBuilder(this)
                .defaultNegativeButton()
                .setPositiveAction { _, _ ->
                    settings.server = Settings.Server(ipEt.text.toString())
                    settings.database = Settings.Database(
                        username = usernameEt.text.toString(),
                        password = passwordEt.text.toString(),
                    )
                    Settings.write(settings)
                    toast(R.string.saved_toast)
                }
                .setTitle(R.string.settings_dialog_title)
                .setView(dialogBindings.root)
                .create().also { it.show() }

            // disable dialog canceled-on-touch-outside after text changed for UX improvement
            listOf(ipEt, usernameEt, passwordEt).forEach {
                it.doAfterTextChanged {
                    dialog.setCanceledOnTouchOutside(false)
                }
            }
        }
    }
}
