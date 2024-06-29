package com.czttgd.android.zhijian.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.czttgd.android.zhijian.App

private var toast: Toast? = null

fun Context.toastShow(text: String) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        return uiThreadToastShow(text)
    }
    Handler(Looper.getMainLooper()).post {
        uiThreadToastShow(text)
    }
}

fun toastShow(text: String) {
    App.appContext.toastShow(text)
}

private fun Context.uiThreadToastShow(text: String) {
    toast?.cancel()
    toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).also { it.show() }
}
