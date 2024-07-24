package com.czttgd.android.zhijian.utils

import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.broadcast.BarcodeBroadcastReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object Barcode {
    fun Context.requestBarcodeWithDialog(onScan: (String) -> Unit) {
        var receiver: BarcodeBroadcastReceiver? = null

        val tryUnregisterBroadcast = {
            receiver?.let {
                unregisterReceiver(it)
                receiver = null
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setNegativeAction { _, _ ->
                tryUnregisterBroadcast()
            }
            .setOnCancelListener {
                tryUnregisterBroadcast()
            }
            .setTitle(R.string.scan_dialog_title)
            .setMessage(R.string.scan_dialog_message)
            .create().apply { setCanceledOnTouchOutside(false) }.also { it.show() }

        receiver = BarcodeBroadcastReceiver { _, content ->
            tryUnregisterBroadcast()
            dialog.dismiss()
            content?.let(onScan)
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(BarcodeBroadcastReceiver.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
