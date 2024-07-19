package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.data.Server.fetch

object Options {
    suspend fun machineNumbers(stage: Int): Array<Int> {
        return fetch<Array<Int>>("$SERVER_ADDR/stage/$stage/machines")
    }
}
