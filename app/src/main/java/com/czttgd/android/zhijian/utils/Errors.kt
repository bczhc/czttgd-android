package com.czttgd.android.zhijian.utils

class UnimplementedError : Error()

class UnreachableError : Error()

class AssertionError : Error {
    constructor() : super()
    constructor(message: String) : super(message)
}

fun <T> unimplemented(): T {
    throw UnimplementedError()
}

fun <T> unreachable(): T {
    throw UnreachableError()
}

fun androidAssertion(condition: Boolean, message: String? = null) {
    if (!condition) {
        throw AssertionError((message ?: throw AssertionError()))
    }
}
