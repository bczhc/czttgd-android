package com.czttgd.android.zhijian.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.czttgd.android.zhijian.BuildConfig
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.Log
import com.czttgd.android.zhijian.data.Ping
import com.czttgd.android.zhijian.data.Settings
import com.czttgd.android.zhijian.databinding.FragmentWorkspaceBinding
import com.czttgd.android.zhijian.databinding.SettingsDialogBinding
import com.czttgd.android.zhijian.httpLogFile
import com.czttgd.android.zhijian.ui.FormFillingActivity
import com.czttgd.android.zhijian.ui.ManualActivity
import com.czttgd.android.zhijian.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = FragmentWorkspaceBinding.inflate(inflater)

        val context = inflater.context
        bindings.card2.button.setOnClickListener {
            startActivity(Intent(context, FormFillingActivity::class.java).apply {
                putExtra(FormFillingActivity.EXTRA_STAGE, FormFillingActivity.STAGE_ONE)
            })
        }
        bindings.card3.button.setOnClickListener {
            startActivity(Intent(context, FormFillingActivity::class.java).apply {
                putExtra(FormFillingActivity.EXTRA_STAGE, FormFillingActivity.STAGE_TWO)
            })
        }
        bindings.card1.button.setOnClickListener {
            startActivity(Intent(context, ManualActivity::class.java))
        }

        bindings.settingsIv.setOnClickListener {
            val dialogBindings = SettingsDialogBinding.inflate(layoutInflater)

            dialogBindings.apply {
                versionNameTv.text = getString(R.string.app_version_name_tv, BuildConfig.VERSION_NAME)
            }

            val addrEt = dialogBindings.addrEt

            val settings = Settings.read()

            addrEt.setText(settings.serverAddr ?: "")
            if (addrEt.text.toString().isEmpty()) {
                @Suppress("HttpUrlsUsage", "SetTextI18n")
                addrEt.setText("http://")
            }

            var onConnectionTesting = false
            val dialog = MaterialAlertDialogBuilder(context)
                .defaultNegativeButton()
                .setPositiveAction { _, _ ->
                    settings.serverAddr = addrEt.text.toString()
                    Settings.write(settings)
                    context.toast(R.string.saved_toast)
                }
                .setNeutralButton(R.string.button_test_connection, null)
                .setTitle(R.string.settings_dialog_title)
                .setView(dialogBindings.root)
                .create().apply {
                    // this way makes the dialog not close on the neutral button clicked
                    setOnShowListener {
                        (it as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener OnClick@{
                            if (onConnectionTesting) return@OnClick
                            onConnectionTesting = true
                            dialogBindings.progressBar.visibility = View.VISIBLE
                            lifecycleScope.launch {
                                val result = runCatching {
                                    Ping.pingTest(addrEt.text.toString())
                                }
                                withContext(Dispatchers.Main) {
                                    result.onSuccess {
                                        context.toast(R.string.connection_succeeded_toast)
                                    }.onFailure {
                                        context.toast(R.string.connection_failed_toast)
                                    }
                                    dialogBindings.progressBar.visibility = View.GONE
                                    onConnectionTesting = false
                                }
                            }
                        }
                    }
                }.also { it.show() }

            // disable dialog canceled-on-touch-outside after text changed for UX improvement
            listOf(addrEt).forEach {
                it.doAfterTextChanged {
                    dialog.setCanceledOnTouchOutside(false)
                }
            }
        }

        bindings.settingsIv.setOnLongClickListener {
            context.buildProgressDialog(getString(R.string.uploading_log_dialog_title)) { d ->
                coroutineLaunchIo {
                    val result = runCatching {
                        Log.uploadLog(httpLogFile)
                    }
                    withMain {
                        result.onSuccess {
                            context.toast(R.string.uploading_succeeded_toast)
                        }.onFailure {
                            context.toast(R.string.uploading_failed_toast)
                        }
                        d.dismiss()
                    }
                }
            }.show()
            true
        }

        return bindings.root
    }
}
