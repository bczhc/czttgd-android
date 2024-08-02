package com.czttgd.android.zhijian.data

import java.io.Serializable

data class BreakCause(
    val id: Int,
    val type: String?,
    val cause: String?,
    val enableState: Int,
) : Serializable
