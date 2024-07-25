package com.czttgd.android.zhijian.utils

import com.czttgd.android.zhijian.App
import com.czttgd.android.zhijian.data.InspectionDetails
import com.czttgd.android.zhijian.dbDateFormatter
import com.czttgd.android.zhijian.dottedDateFormatter
import com.czttgd.android.zhijian.ui.InspectionDetailsActivity
import com.example.lc_print_sdk.PrintUtil
import com.example.lc_print_sdk.PrintUtil.PrinterBinderListener

private fun preparePrinter() {
    PrintUtil.getInstance(App.appContext)
    PrintUtil.printConcentration(15)
    PrintUtil.printEnableMark(true)
}

object PrintUtils {
    fun printInspection(details: InspectionDetails, stage: Int, onPrintCallback: (Int) -> Unit) {
        preparePrinter()
        val stageChar = when (stage) {
            1 -> "一"
            2 -> "二"
            else -> ""
        }
        val inspectionFlag = when (details.inspectionFlag) {
            0 -> "已初检"
            1 -> "已终检"
            2 -> "关闭"
            else -> ""
        }
        PrintUtil.setPrintEventListener(object : PrinterBinderListener {
            override fun onPrintCallback(p0: Int) {
                onPrintCallback(p0)
            }

            override fun onVersion(p0: String?) {
            }
        })
        PrintUtil.printText("${details.deviceCode}号机台 铜线${stageChar}期                $inspectionFlag")
        PrintUtil.printLine(1)
        PrintUtil.printText("${details.creator} ${dottedDateFormatter.format(dbDateFormatter.tryParse(details.creationTime) ?: "")} 提交")
        PrintUtil.printLine(2)

        PrintUtil.printText(
            "${
                when (details.inspectionFlag) {
                    0 -> "初检信息"
                    1 -> "终检信息"
                    else -> ""
                }
            }："
        )
        PrintUtil.printLine(1)

        InspectionDetailsActivity.buildFieldsMap(details).forEach {
            PrintUtil.printText("${it.first}：${it.second}")
            PrintUtil.printLine(1)
        }

        PrintUtil.setUnwindPaperLen(50)
        PrintUtil.setFeedPaperSpace(200)
        PrintUtil.start()
    }
}
