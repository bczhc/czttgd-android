package com.czttgd.android.zhijian.utils

inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> = this.map(transform).toTypedArray()
