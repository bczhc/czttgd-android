package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.data.Server.fetch

object SelectList {
    suspend fun machineNumbers(stage: Int): Array<Int> {
        return fetch<Array<Int>>("$serverAddr/stage/$stage/machines")
    }

    suspend fun allUsers(): Array<String> {
        data class Obj(
            val name: String,
        )

        return fetch<Array<Obj>>("$serverAddr/users").map { it.name }.toTypedArray()
    }

    suspend fun breakReasons(): Array<String> {
        return fetch<Array<String>>("$serverAddr/break/reasons")
    }

    suspend fun breakPoints(): Array<String> {
        return fetch<Array<String>>("$serverAddr/break/points")
    }
}
