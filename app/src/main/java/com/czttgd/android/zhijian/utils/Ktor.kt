package com.czttgd.android.zhijian.utils

import com.czttgd.android.zhijian.GSON
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.reflect.full.memberProperties

object FormDataUtils {
    inline fun <reified T : Any> fromObject(obj: T): FormDataContent {
        return FormDataContent(Parameters.build {
            T::class.memberProperties.filter {
                // all property visibilities will become PRIVATE when the app is built in release mode
                // kinda weird, and I don't know why, plus I haven't found any relevant issue on the Internet
                // Just comment this out at this moment.
                // it.visibility == KVisibility.PUBLIC
                true
            }.forEach {
                val name = it.name
                val value = it.getter.call(obj)
                value?.let { v ->
                    append(name, v.toString())
                }
            }
        })
    }
}

inline fun <reified T : Any> HttpRequestBuilder.setFormDataBody(src: T) {
    this.setBody(FormDataUtils.fromObject(src))
}

suspend inline fun <reified T : Any> HttpResponse.bodyAsJson(): T? {
    return GSON.fromJsonOrNull<T>(this.bodyAsText())
}
