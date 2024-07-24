package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.appHttpClient
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
    val wireSpeed: Int?,
    val breakSpecs: String,
    val copperWireNo: String?,
    val copperStickNo: String?,
    val repoNo: String?,
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
) : Serializable {
    companion object {
        fun fromDetails(details: InspectionDetails): InspectionForm {
            if (details.inspectionFlag != 0) {
                throw RuntimeException("require: 初检")
            }
            return details.let {
                return@let InspectionForm(
                    creator = it.creator,
                    machineNumber = it.deviceCode,
                    machineCategory = it.deviceCategory,
                    creationTime = it.creationTime,
                    productSpecs = it.productSpec,
                    wireNumber = it.wireNum,
                    wireSpeed = it.wireSpeed,
                    breakSpecs = it.breakSpec,
                    copperWireNo = it.wireBatchCode,
                    copperStickNo = it.stickBatchCode,
                    repoNo = it.warehouse,
                    breakType = when (it.breakFlag) {
                        true -> 0
                        false -> 1
                    },
                    breakPositionB = it.breakpointB,
                    breakPositionA = it.breakpointA,
                    breakReasonA = it.breakCauseA!!,
                    comments = it.comments
                )
            }
        }
    }
}

data class InspectionDetails(
    val deviceCode: Int,
    val deviceCategory: String,
    val creator: String,
    val creationTime: String,
    /**
     * 0: 已初检 1: 已终检 2: 关闭
     */
    val inspectionFlag: Int,
    val productSpec: String?,
    val wireSpeed: Int?,
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
) {
    companion object {
        fun fromDetails(id: Int, details: InspectionDetails): InspectionSummary {
            return details.let {
                return@let InspectionSummary(
                    id = id,
                    machineNumber = it.deviceCode,
                    cause = when (details.inspectionFlag) {
                        0 -> it.breakCauseA
                        1 -> it.breakCauseB
                        2 -> ""
                        else -> throw RuntimeException("Unexpected value")
                    },
                    breakSpec = it.breakSpec,
                    productSpec = it.productSpec,
                    creator = it.creator,
                    creationTime = it.creationTime,
                    checkingState = it.inspectionFlag,
                )
            }
        }
    }
}

object Inspection {
    suspend fun post(record: InspectionForm) {
        appHttpClient.post("$serverAddr/inspection") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Unit>()
    }

    suspend fun update(record: InspectionForm, id: Int) {
        appHttpClient.put("$serverAddr/inspection/$id") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Unit>()
    }

    suspend fun querySummary(filter: String, stage: Int): Server.ResponseData<Array<InspectionSummary>> {
        return appHttpClient.get("$serverAddr/inspections?filter=${filter.encodeURLPathPart()}&stage=$stage")
            .parseResponse<Array<InspectionSummary>>()
    }

    suspend fun queryDetails(id: Int): InspectionDetails {
        return appHttpClient.get("$serverAddr/inspection/$id/details")
            .parseResponse<InspectionDetails>().data!!
    }

    suspend fun fetchStage(deviceCode: Int): Int {
        if (SelectList.machineNumbers(1).contains(deviceCode)) return 1
        if (SelectList.machineNumbers(2).contains(deviceCode)) return 2
        throw RuntimeException("No stage number for device code: $deviceCode")
    }
}
