package com.czttgd.android.zhijian

import android.app.Application
import android.content.Context
import com.google.gson.Gson

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
