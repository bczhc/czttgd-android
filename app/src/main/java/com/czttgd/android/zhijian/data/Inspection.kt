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
    val productSpecs: String?,
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

data class InspectionSummary(
    val id: UInt,
    val machineNumber: UInt,
    val cause: String?,
    val breakSpec: String,
    val productSpec: String?,
    val creator: String,
    val creationTime: String,
    val checkingState: UInt,
)

object Inspection {
    suspend fun post(record: InspectionRecord) {
        HttpClient().post("$serverAddr/inspection") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Unit>()
    }

    suspend fun querySummary(filter: String, stage: Int): Server.ResponseData<Array<InspectionSummary>> {
        return HttpClient().get("$serverAddr/inspections?filter=${filter.encodeURLPathPart()}&stage=$stage")
            .parseResponse<Array<InspectionSummary>>()
    }
}
