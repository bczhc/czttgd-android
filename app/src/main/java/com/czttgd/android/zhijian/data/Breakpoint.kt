package com.czttgd.android.zhijian.data

import java.io.Serializable

data class Breakpoint(
    val id: Int,
    val breakpoint: String?,
    val enableState: Int,
) : Serializable
