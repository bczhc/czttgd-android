package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.data.Server.parseResponse
import com.czttgd.android.zhijian.utils.setFormDataBody
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

data class InspectionRecord(
    val creator: String,
    val machineNumber: UInt,
    val machineCategory: String,
    val creationTime: String,
    val productSpecs: String,
    val wireNumber: UInt?,
    val breakSpecs: String,
    val copperWireNo: UInt?,
    val copperStickNo: UInt?,
    val repoNo: UInt?,
    // 0: 拉丝池内断线
    // 1: 非拉丝池内断线
    val breakType: UInt,
    // 拉丝池
    val breakPositionB: Float?,
    // 非拉丝池
    val breakPositionA: String?,
    // 初检
    val breakReasonA: String,
    val comments: String?,
)

object Inspection {
    suspend fun post(record: InspectionRecord) {
        HttpClient().post("$serverAddr/inspection") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Unit>()
    }
}
