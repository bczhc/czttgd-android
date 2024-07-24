package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.data.Server.parseResponse
import com.czttgd.android.zhijian.utils.setFormDataBody
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.Serializable

data class InspectionForm(
    val creator: String,
    val machineNumber: Int,
    val machineCategory: String,
    val creationTime: String,
    val productSpecs: String?,
    val wireNumber: Int?,
    val breakSpecs: String,
    val copperWireNo: Int?,
    val copperStickNo: Int?,
    val repoNo: Int?,
    // 0: 拉丝池内断线
    // 1: 非拉丝池内断线
    val breakType: Int,
    // 拉丝池 BigDecimal
    val breakPositionB: String?,
    // 非拉丝池
    val breakPositionA: String?,
    // 初检
    val breakReasonA: String,
    val comments: String?,
): Serializable {
    companion object {
        fun fromDetails(details: InspectionDetails) {
            return details.let {
//                InspectionForm (
//                    creator = it.creator,
//                    machineNumber = it.deviceCode,
//                    machineCategory = TODO(),
//                )
                TODO()
            }
        }
    }
}

data class InspectionDetails(
    val deviceCode: Int,
    val creator: String,
    val creationTime: String,
    /**
     * 0: 已初检 1: 已终检 2: 关闭
     */
    val inspectionFlag: Int,
    val productSpec: String?,
    val wireNum: Int?,
    val breakSpec: String,
    val wireBatchCode: String?,
    val stickBatchCode: String?,
    val warehouse: String?,
    val productTime: String?,
    /**
     * 是否拉丝池内断线
     */
    val breakFlag: Boolean,
    /**
     * actually BigDecimal
     */
    val breakpointB: String?,
    val breakpointA: String?,
    val causeType: String?,
    val breakCauseA: String?,
    val comments: String?,
    val inspector: String?,
    val inspectionTime: String?,
    val breakCauseB: String?,
)

data class InspectionSummary(
    val id: Int,
    val machineNumber: Int,
    val cause: String?,
    val breakSpec: String,
    val productSpec: String?,
    val creator: String,
    val creationTime: String,
    val checkingState: Int,
)

object Inspection {
    suspend fun post(record: InspectionForm) {
        HttpClient().post("$serverAddr/inspection") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Unit>()
    }

    suspend fun update(record: InspectionForm, id: Int) {
        HttpClient().put("$serverAddr/inspection/$id") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Unit>()
    }

    suspend fun querySummary(filter: String, stage: Int): Server.ResponseData<Array<InspectionSummary>> {
        return HttpClient().get("$serverAddr/inspections?filter=${filter.encodeURLPathPart()}&stage=$stage")
            .parseResponse<Array<InspectionSummary>>()
    }

    suspend fun queryDetails(id: Int): Server.ResponseData<InspectionDetails> {
        return HttpClient().get("$serverAddr/inspection/$id/details")
            .parseResponse<InspectionDetails>()
    }

    suspend fun fetchStage(deviceCode: Int): Int {
        if (SelectList.machineNumbers(1).contains(deviceCode)) return 1
        if (SelectList.machineNumbers(2).contains(deviceCode)) return 2
        throw RuntimeException("No stage number for device code: $deviceCode")
    }
}
