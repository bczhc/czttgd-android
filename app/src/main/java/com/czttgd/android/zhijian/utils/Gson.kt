package com.czttgd.android.zhijian.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

fun <T> Gson.fromJsonOrNull(json: String, classOfT: Class<T>): T? {
    return try {
        this.fromJson(json, classOfT)
    } catch (e: JsonSyntaxException) {
        null
    }
}
