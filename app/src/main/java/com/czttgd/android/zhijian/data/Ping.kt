package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.appHttpClient
import com.czttgd.android.zhijian.data.Server.parseResponse
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Ping {
    suspend fun pingTest(serverAddr: String) {
        val text = System.currentTimeMillis().toString()
        withContext(Dispatchers.IO) {
            data class Pong(val text: String)

            val pong = appHttpClient.get("$serverAddr/ping?text=$text").parseResponse<Pong>()
            if (text != pong.data?.text) {
                throw RuntimeException("Unexpected pong!")
            }
        }
    }
}
