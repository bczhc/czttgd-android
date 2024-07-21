package com.czttgd.android.zhijian.utils

import com.czttgd.android.zhijian.GSON
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

object FormDataUtils {
    inline fun <reified T : Any> fromObject(obj: T): FormDataContent {
        return FormDataContent(Parameters.build {
            T::class.memberProperties.filter { it.visibility == KVisibility.PUBLIC }.forEach {
                val name = it.name
                val value = it.getter.call(obj).toString()
                append(name, value)
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
