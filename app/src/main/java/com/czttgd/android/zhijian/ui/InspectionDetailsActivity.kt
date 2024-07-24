package com.czttgd.android.zhijian.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import com.czttgd.android.zhijian.*
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.data.InspectionDetails
import com.czttgd.android.zhijian.data.InspectionForm
import com.czttgd.android.zhijian.data.SelectList
import com.czttgd.android.zhijian.databinding.ActivityInspectionDetailsBinding
import com.czttgd.android.zhijian.databinding.InspectionDetailsFieldBinding
import com.czttgd.android.zhijian.utils.*

class InspectionDetailsActivity : BaseActivity() {
    private lateinit var bindings: ActivityInspectionDetailsBinding
    private var stage: Int? = null
    private var inspection: InspectionDetails? = null
    private val updateLauncher = registerForActivityResult(FormFillingActivity.UpdateActivityContract()) {id->
        id ?: return@registerForActivityResult
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityInspectionDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val intent = intent
        androidAssertion(intent.hasExtra(EXTRA_ID))
        val id = intent.getIntExtra(EXTRA_ID, -1)
        Intent().apply {
            putExtra(EXTRA_ID, id)
            setResult(0, this)
        }

        bindings.toolbar.setUpBackButton()

        buildProgressDialog(getString(R.string.fetching_dialog_title)) {
            coroutineLaunchIo {
                val result = runCatching {
                    val inspection = Inspection.queryDetails(id).data!!
                    this@InspectionDetailsActivity.stage = Inspection.fetchStage(inspection.deviceCode)
                    this@InspectionDetailsActivity.inspection = inspection
                }

                withMain {
                    result.onFailure {
                        toast(R.string.request_failed_dialog)
                        it.printStackTrace()
                    }.onSuccess {
                        setUpFieldsUi()
                        setUpButton()
                    }
                }
            }
        }
    }

    private fun setUpButton() {
        val inspection = this.inspection ?: return



        updateLauncher.launch(FormFillingActivity.UpdateActivityContract.Input(
            id = TODO(),
            form = TODO(),
            stage = TODO(),
        ))
    }

    private fun setUpFieldsUi() {
        val inspection = this.inspection ?: return
        when (inspection.inspectionFlag) {
            0 -> {
                bindings.inspectTv.text = getString(R.string.inspection_records_已初检)
                bindings.inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_a))
                bindings.inspectionCardTitle.text = getString(R.string.inspection_a_info_title)
            }

            1 -> {
                bindings.inspectTv.text = getString(R.string.inspection_records_已终检)
                bindings.inspectTv.setTextColor(getColor(R.color.inspection_records_inspect_b))
                bindings.inspectionCardTitle.text = getString(R.string.inspection_b_info_title)
            }

            else -> return
        }

        bindings.deviceTv.text = getString(R.string.inspection_device_code, inspection.deviceCode)
        bindings.stageTv.text = when (stage) {
            1 -> getString(R.string.stage_one)
            2 -> getString(R.string.stage_two)
            else -> "?"
        }
        bindings.creatorTv.text = inspection.creator
        bindings.timeTv.text = toDottedDateTime(inspection.creationTime)

        val breakpointText = when (inspection.breakFlag) {
            true -> {
                "${inspection.breakpointB}"
            }

            false -> {
                "${inspection.breakpointA}"
            }
        }

        val fieldValues = inspection.let {
            listOf(
                it.creator,
                "${it.deviceCode}",
                toDottedDateTime(it.creationTime),
                it.productSpec ?: "",
                it.wireNum?.toString() ?: "",
                it.breakSpec,
                it.wireBatchCode ?: "",
                it.stickBatchCode ?: "",
                it.warehouse ?: "",
                toDottedDate(it.productTime ?: ""),
                it.breakFlag.toText(),
                breakpointText,
                it.causeType ?: "",
                it.breakCauseA ?: "",
                it.comments ?: "",
            )
        }
        bindings.fieldsLl.removeAllViews()
        fieldValues.zip(resources.getTextArray(R.array.inspection_a_field_labels)).forEach {
            val fieldBindings = InspectionDetailsFieldBinding.inflate(layoutInflater).apply {
                labelTv.text = it.second
                fieldTv.text = it.first
            }
            bindings.fieldsLl.addView(fieldBindings.root)
        }

        // 初检
        if (inspection.inspectionFlag == 0) {
            bindings.bottomRl.visibility = View.VISIBLE
        }
    }

    class ActivityContract : ActivityResultContract<Int, Int>() {
        override fun createIntent(context: Context, input: Int): Intent {
            return Intent(context, InspectionDetailsActivity::class.java).apply {
                putExtra(EXTRA_ID, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            return intent!!.getIntExtra(EXTRA_ID, -1)
        }
    }

    private fun Boolean.toText(): String {
        return when (this) {
            true -> getString(R.string.yes)
            false -> getString(R.string.no)
        }
    }

    private fun toDottedDate(date: String): String {
        return dottedDateFormatter.format(dbDateFormatter.tryParse(date) ?: return "")
    }

    private fun toDottedDateTime(date: String): String {
        return dottedDateTimeFormatter.format(dbDateFormatter.tryParse(date) ?: return "")
    }

    companion object {
        /**
         * int extra
         */
        const val EXTRA_ID = "id"
    }
}
