package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.App.Companion.GSON
import com.czttgd.android.zhijian.utils.setFormDataBody
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LoginRepository {
    private const val SERVER_URL = "http://192.168.4.4:8000"

    data class Form(val username: String, val password: String)

    suspend fun login(username: String, password: String): JsonObject {
        return withContext(Dispatchers.IO) {
            val body = HttpClient().post("$SERVER_URL/demo/login") {
                contentType(ContentType.Application.FormUrlEncoded)
                setFormDataBody(Form(username, password))
            }.bodyAsText()
            GSON.fromJson(body, JsonObject::class.java)
        }
    }
}
