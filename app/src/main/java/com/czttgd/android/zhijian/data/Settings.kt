package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.App
import com.czttgd.android.zhijian.App.Companion.GSON
import com.czttgd.android.zhijian.utils.fromJsonOrNull
import java.io.File

data class Settings(
    var server: Server?,
    var database: Database?,
) {
    data class Server(
        var ip: String?,
    )

    data class Database(
        var username: String?,
        var password: String?,
    )

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
            return Settings(null, null)
        }

        fun read(): Settings {
            val settings = GSON.fromJsonOrNull(jsonFile.readText(), Settings::class.java)
            if (settings == null) {
                write(empty())
            }
            return GSON.fromJsonOrNull(jsonFile.readText(), Settings::class.java)!!
        }

        fun write(settings: Settings) {
            jsonFile.writeText(GSON.toJson(settings))
        }
    }
}
