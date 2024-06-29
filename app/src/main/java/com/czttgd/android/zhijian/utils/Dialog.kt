package com.czttgd.android.zhijian.utils

import android.content.DialogInterface
import com.czttgd.android.zhijian.R
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
