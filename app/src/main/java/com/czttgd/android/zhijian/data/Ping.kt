package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.App.Companion.GSON
import com.czttgd.android.zhijian.appHttpClient
import com.czttgd.android.zhijian.utils.fromJsonOrNull
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Ping {
    suspend fun pingTest(serverAddr: String) {
        val text = System.currentTimeMillis().toString()
        withContext(Dispatchers.IO) {
            val body = appHttpClient.get("$serverAddr/echo?text=$text").bodyAsText()
            val json = GSON.fromJsonOrNull<JsonObject>(body) ?: throw RuntimeException("Invalid body")
            if (json.get("text").asString != text) {
                throw RuntimeException("Unexpected response")
            }
        }
    }
}
