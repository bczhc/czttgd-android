package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.appHttpClient
import com.czttgd.android.zhijian.utils.withIo
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.io.File

object Log {
    suspend fun uploadLog(logFile: File) {
        withIo {
            appHttpClient.post("$serverAddr/log") {
                setBody(MultiPartFormDataContent(formData {
                    append("log-file", logFile.readBytes(), Headers.build {
                        contentType(ContentType.Text.Plain)
                    })
                }))
            }
        }
    }
}
