package com.czttgd.android.zhijian.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes

private var toast: Toast? = null

fun Context.toast(text: String) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        return uiThreadToast(text)
    }
    Handler(Looper.getMainLooper()).post {
        uiThreadToast(text)
    }
}

fun Context.toast(@StringRes textRes: Int) {
    this.toast(this.getString(textRes))
}

private fun Context.uiThreadToast(text: String) {
    toast?.cancel()
    toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).also { it.show() }
}
