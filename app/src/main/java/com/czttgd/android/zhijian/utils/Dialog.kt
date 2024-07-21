package com.czttgd.android.zhijian.utils

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.databinding.DialogProgressIndeterminateBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


fun MaterialAlertDialogBuilder.setPositiveAction(action: ((dialog: DialogInterface, which: Int) -> Unit)? = null): MaterialAlertDialogBuilder {
    return this.setPositiveButton(R.string.confirm_button, action)
}

fun MaterialAlertDialogBuilder.defaultNegativeButton(): MaterialAlertDialogBuilder {
    return this.setNegativeAction(null)
}

fun MaterialAlertDialogBuilder.setNegativeAction(action: ((dialog: DialogInterface, which: Int) -> Unit)? = null): MaterialAlertDialogBuilder {
    return this.setNegativeButton(R.string.cancel_button, action)
}

//val setUpSelectionFields =
//            { fieldBindings: FormFillingFieldLayoutBinding, launcherIndex: Int, getItems: suspend () -> Array<String> ->
//                fieldBindings.rl.setOnClickListener {
//                    val dialogBindings = DialogFetchingInfoBinding.inflate(layoutInflater)
//                    val dialog = MaterialAlertDialogBuilder(this)
//                        .setView(dialogBindings.root)
//                        .create().apply {
//                            setCanceledOnTouchOutside(false)
//                        }.also { it.show() }
//
//                    lifecycleScope.launch {
//                        val result = runCatching { getItems() }
//                        withContext(Dispatchers.Main) {
//                            dialog.dismiss()
//                            result.onSuccess {
//                                selectionLaunchers[launcherIndex].launch(it)
//                            }.onFailure {
//                                toast(it.toString())
//                            }
//                        }
//                    }
//                }
//            }

fun Context.buildProgressDialog(title: String, task: (dialog: AlertDialog) -> Unit): AlertDialog {
    val context = this
    val viewBindings = DialogProgressIndeterminateBinding.inflate(LayoutInflater.from(context))
    val dialog = MaterialAlertDialogBuilder(context)
        .setView(viewBindings.root)
        .setTitle(title)
        .create().apply {
            setCanceledOnTouchOutside(false)
        }

    task(dialog)

    return dialog
}
