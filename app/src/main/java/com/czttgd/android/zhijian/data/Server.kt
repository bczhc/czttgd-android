package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.App
import com.czttgd.android.zhijian.utils.fromJsonOrNull
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val serverAddr: String
    get() = Settings.read().serverAddr ?: ""

object Server {
    suspend inline fun <reified T> fetch(url: String): T {
        return withContext(Dispatchers.IO) {
            val res = HttpClient().get(url)
            if (res.status.value != 200) {
                throw RuntimeException("Non-200 status code")
            }
            val body = res.bodyAsText()
            val json =
                App.GSON.fromJsonOrNull<JsonObject>(body) ?: throw RuntimeException("Response JSON parsing error")
            if (json["code"].asInt != 0) {
                throw RuntimeException("Non-zero response code")
            }
            App.GSON.fromJson(json["data"], T::class.java)
        }
    }
}
