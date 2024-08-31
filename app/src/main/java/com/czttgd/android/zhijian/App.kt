package com.czttgd.android.zhijian

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.plugins.*
import java.io.File
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

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}

val dbDateFormatter by lazy {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
}

val dottedDateFormatter by lazy {
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
}

val GSON by lazy { App.GSON }

val httpLogFile by lazy {
    File(App.appContext.filesDir, "http.log").also {
        if (!it.exists()) {
            it.writeText("")
        }
    }
}

val appHttpClient: HttpClient
    get() = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
        }
    }
