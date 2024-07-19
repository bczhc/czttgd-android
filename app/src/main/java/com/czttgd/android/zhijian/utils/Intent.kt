package com.czttgd.android.zhijian.utils

import android.content.Intent
import android.os.Build
import java.io.Serializable

inline fun <reified T : Serializable> Intent.getTypedSerializableExtra(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getSerializableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        this.getSerializableExtra(name) as T?
    }
}
