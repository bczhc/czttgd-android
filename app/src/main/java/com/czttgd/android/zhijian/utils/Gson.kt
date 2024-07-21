package com.czttgd.android.zhijian.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException

inline fun <reified T> Gson.fromJsonOrNull(json: String): T? {
    return try {
        this.fromJson(json, T::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }
}

inline fun <reified T> Gson.fromJsonOrNull(json: JsonElement): T? {
    return try {
        this.fromJson(json, T::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }
}
