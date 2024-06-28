package com.czttgd.android.zhijian

import android.app.Application
import android.content.Context

class App : Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    init {
        appContext = this
    }
}
