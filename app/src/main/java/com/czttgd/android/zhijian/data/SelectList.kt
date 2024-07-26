package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.data.Server.fetch

object SelectList {
    suspend fun machineNumbers(stage: Int): Array<Int> {
        return fetch<Array<Int>>("$serverAddr/stage/$stage/machines")
    }

    suspend fun allUsers(): Array<User> {
        return fetch<Array<User>>("$serverAddr/users")
    }

    suspend fun breakCauses(): Array<BreakCause> {
        return fetch<Array<BreakCause>>("$serverAddr/break/reasons")
    }

    suspend fun breakPoints(): Array<Breakpoint> {
        return fetch<Array<Breakpoint>>("$serverAddr/break/points")
    }
}
