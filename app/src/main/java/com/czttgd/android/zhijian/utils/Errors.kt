package com.czttgd.android.zhijian.utils

class UnimplementedError : Error()

fun <T> unimplemented(): T {
    throw UnimplementedError()
}
