package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.GSON
import com.czttgd.android.zhijian.appHttpClient
import com.czttgd.android.zhijian.utils.bodyAsJson
import com.czttgd.android.zhijian.utils.fromJsonOrNull
import com.czttgd.android.zhijian.utils.withIo
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

typealias RefId = Int

val serverAddr: String
    get() = Settings.read().serverAddr ?: ""

object Server {
    suspend inline fun <reified T : Any> fetch(url: String): T {
        return withIo {
            val res = appHttpClient.get(url)
            if (res.status.value != 200) {
                throw RuntimeException("Non-200 status code")
            }
            res.parseResponse<T>().data ?: throw RuntimeException("Null response data")
        }
    }

    data class ResponseData<T>(
        val data: T?,
        val code: Int,
        val message: String?,
    )

    suspend inline fun <reified T : Any> HttpResponse.parseResponse(): ResponseData<T> {
        println(this.bodyAsText())
        val json = this.bodyAsJson<JsonObject>() ?: throw RuntimeException("Response JSON parsing error")
        val code = json["code"].asInt
        val element = json["message"]
        val message = if (element.isJsonNull) {
            null
        } else {
            element.asString
        }
        if (code != 0) {
            throw RuntimeException("Non-zero response code; message: $message")
        }
        val data = GSON.fromJsonOrNull<T>(json["data"])
        return ResponseData(
            data = data,
            code = 0,
            message = message
        )
    }
}
