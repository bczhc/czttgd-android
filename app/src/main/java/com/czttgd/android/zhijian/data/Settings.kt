package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.App
import com.czttgd.android.zhijian.App.Companion.GSON
import com.czttgd.android.zhijian.utils.fromJsonOrNull
import java.io.File

data class Settings(
    var serverAddr: String?,
) {

    companion object {
        private const val JSON_FILENAME = "settings.json"

        private val jsonFile by lazy {
            File(App.appContext.filesDir, JSON_FILENAME).also {
                if (!it.exists()) {
                    it.writeText("{}")
                }
            }
        }

        fun empty(): Settings {
            return Settings(null)
        }

        fun read(): Settings {
            val settings = GSON.fromJsonOrNull<Settings>(jsonFile.readText())
            if (settings == null) {
                write(empty())
            }
            return GSON.fromJsonOrNull(jsonFile.readText())!!
        }

        fun write(settings: Settings) {
            jsonFile.writeText(GSON.toJson(settings))
        }
    }
}
