package com.czttgd.android.zhijian.data

import java.io.Serializable

data class User(
    val id: Int,
    val name: String,
    val enableState: Int,
    val userType: String,
): Serializable
