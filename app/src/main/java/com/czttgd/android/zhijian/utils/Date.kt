package com.czttgd.android.zhijian.utils

import java.text.SimpleDateFormat
import java.util.*

fun SimpleDateFormat.tryParse(date: String): Date? {
    return runCatching {
        this.parse(date)
    }.getOrElse { null }
}
