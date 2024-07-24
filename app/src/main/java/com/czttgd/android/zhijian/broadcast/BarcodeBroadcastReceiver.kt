package com.czttgd.android.zhijian.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BarcodeBroadcastReceiver(private val onBarcodeReceive: (self: BarcodeBroadcastReceiver, content: String?) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return onBarcodeReceive(this, null)
        onBarcodeReceive(this, intent.getStringExtra(KEY))
    }

    companion object {
        const val ACTION = "scan.rcv.message"
        const val KEY = "barcodeData"
    }
}
