package com.czttgd.android.zhijian.data

import com.czttgd.android.zhijian.appHttpClient
import com.czttgd.android.zhijian.data.Server.parseResponse
import com.czttgd.android.zhijian.utils.setFormDataBody
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.Serializable

// https://discuss.kotlinlang.org/t/cant-understand-kotlins-manner-that-privatize-members-of-a-data-class
data class InspectionForm(
    @JvmField
    val creator: RefId,
    @JvmField
    val deviceCode: Int,
    @JvmField
    val deviceCategory: String,
    @JvmField
    val creationTime: String,
    @JvmField
    val productSpec: String?,
    @JvmField
    val wireNumber: Int?,
    @JvmField
    val wireType: String?,
    @JvmField
    val breakSpec: String,
    @JvmField
    val wireBatchCode: String?,
    @JvmField
    val stickBatchCode: String?,
    @JvmField
    val warehouse: String?,
    /**
     * 是否拉丝池内断线
     */
    @JvmField
    val breakFlag: Boolean,
    @JvmField
    val breakpointB: String?,
    @JvmField
    val breakpointA: RefId?,
    @JvmField
    val breakCauseA: RefId?,
    @JvmField
    val comments: String?,
    @JvmField
    val productTime: String?,
) : Serializable

data class InspectionDetails(
    val id: Long,
    val deviceCode: Int,
    val deviceCategory: String,
    val creator: User,
    val creationTime: String,
    /**
     * 0: 已初检 1: 已终检 2: 关闭
     */
    val inspectionFlag: Int,
    val productSpec: String?,
    val wireNum: Int?,
    val wireType: String?,
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
    val breakpointA: Breakpoint?,
    val breakCauseA: BreakCause?,
    val breakCauseB: BreakCause?,
    val comments: String?,
    val inspector: User?,
    val inspectionTime: String?
) : Serializable

data class InspectionSummary(
    val id: Long,
    val deviceCode: Int,
    val breakCauseA: BreakCause?,
    val breakCauseB: BreakCause?,
    val breakSpec: String,
    val productSpec: String?,
    val creator: User,
    val creationTime: String,
    val inspectionFlag: Int,
    val breakFlag: Boolean,
) {
    companion object {
        fun fromDetails(id: Long, details: InspectionDetails): InspectionSummary {
            return details.let {
                return@let InspectionSummary(
                    id = id,
                    deviceCode = it.deviceCode,
                    breakCauseA = it.breakCauseA,
                    breakCauseB = it.breakCauseB,
                    breakSpec = it.breakSpec,
                    productSpec = it.productSpec,
                    creator = it.creator,
                    creationTime = it.creationTime,
                    inspectionFlag = it.inspectionFlag,
                    breakFlag = it.breakFlag
                )
            }
        }
    }
}

object Inspection {
    /**
     * returns the last inserted id
     */
    suspend fun post(record: InspectionForm): Long {
        return appHttpClient.post("$serverAddr/inspection") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Long>().data!!
    }

    suspend fun update(record: InspectionForm, id: Long) {
        appHttpClient.put("$serverAddr/inspection/$id") {
            contentType(ContentType.Application.FormUrlEncoded)
            setFormDataBody(record)
        }.parseResponse<Unit>()
    }

    suspend fun querySummary(
        filter: String,
        stage: Int,
        limit: Int = LIST_LIMIT,
        offset: Int = 0
    ): Server.ResponseData<Array<InspectionSummary>> {
        return appHttpClient.get("$serverAddr/inspection/search?filter=${filter.encodeURLPathPart()}" +
                "&stage=$stage&limit=$limit&offset=$offset")
            .parseResponse<Array<InspectionSummary>>()
    }

    const val LIST_LIMIT = 50

    suspend fun queryDetails(id: Long): InspectionDetails {
        return appHttpClient.get("$serverAddr/inspection/$id/details")
            .parseResponse<InspectionDetails>().data!!
    }

    suspend fun fetchStage(deviceCode: Int): Int {
        if (SelectList.machineNumbers(1).contains(deviceCode)) return 1
        if (SelectList.machineNumbers(2).contains(deviceCode)) return 2
        throw RuntimeException("No stage number for device code: $deviceCode")
    }
}
