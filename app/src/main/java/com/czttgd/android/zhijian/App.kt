package com.czttgd.android.zhijian

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.plugins.*
import java.text.SimpleDateFormat
import java.util.*

class App : Application() {
    companion object {
        lateinit var appContext: Context
            private set

        val GSON = Gson()
    }

    init {
        appContext = this
    }
}

val dbDateFormatter by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
}

val dottedDateTimeFormatter by lazy {
    SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
}

val dottedDateFormatter by lazy {
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
}

val GSON by lazy { App.GSON }

val appHttpClient: HttpClient
    get() = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }
