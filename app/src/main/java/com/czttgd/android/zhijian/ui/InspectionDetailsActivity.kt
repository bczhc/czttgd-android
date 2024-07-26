package com.czttgd.android.zhijian.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import com.czttgd.android.zhijian.*
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.data.InspectionDetails
import com.czttgd.android.zhijian.databinding.ActivityInspectionDetailsBinding
import com.czttgd.android.zhijian.databinding.InspectionDetailsFieldBinding
import com.czttgd.android.zhijian.utils.*

class InspectionDetailsActivity : BaseActivity() {
    private lateinit var bindings: ActivityInspectionDetailsBinding
    private var inspectionId: Int? = null
    private var stage: Int? = null
    private var inspection: InspectionDetails? = null
    private val updateLauncher = registerForActivityResult(FormFillingActivity.UpdateActivityContract()) { id ->
        id ?: return@registerForActivityResult
        inspectionId!!
        fetchAndUpdateUiWithDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityInspectionDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        inspectionId = intent.getId()
        Intent().apply {
            putExtra(EXTRA_ID, inspectionId!!)
            setResult(0, this)
        }

        bindings.toolbar.apply {
            setUpBackButton()
            setOnMenuItemClickListener l@{
                val inspection = inspection
                if (it.itemId != R.id.print || inspection == null) return@l false

                buildProgressDialog(getString(R.string.printing_dialog_title)) { d ->
                    runCatching {
                        PrintUtils.printInspection(inspection, stage!!) {
                            d.dismiss()
                        }
                    }.onFailure { e ->
                        toast(R.string.print_error_toast)
                        e.printStackTrace()
                        d.dismiss()
                    }
                }.show()

                true
            }
        }
        fetchAndUpdateUiWithDialog()
    }

    private fun fetchAndUpdateUiWithDialog() {
        buildProgressDialog(getString(R.string.fetching_dialog_title)) {
            coroutineLaunchIo {
                fetchAndUpdateUi()
            }
        }
    }

    private suspend fun fetchAndUpdateUi() {
        val result = runCatching {
            val inspection = Inspection.queryDetails(inspectionId!!)
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

    private fun setUpButton() {
        val inspection = this.inspection ?: return
        // 只有终检能修改
        if (inspection.inspectionFlag != 0) {
            return
        }

        bindings.bottomRl.visibility = View.VISIBLE
        bindings.bottomButton.setOnClickListener {
            updateLauncher.launch(
                FormFillingActivity.UpdateActivityContract.Input(
                    id = intent.getId(),
                    details = inspection,
                    stage = this.stage!!,
                )
            )
        }
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
        bindings.creatorTv.text = inspection.creator.name
        bindings.timeTv.text = toDottedDateTime(inspection.creationTime)

        bindings.fieldsLl.removeAllViews()
        buildFieldsMap(inspection).forEach {
            val fieldBindings = InspectionDetailsFieldBinding.inflate(layoutInflater).apply {
                labelTv.text = it.first
                fieldTv.text = it.second
            }
            bindings.fieldsLl.addView(fieldBindings.root)
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

    private fun Intent.getId(): Int {
        androidAssertion(intent.hasExtra(EXTRA_ID))
        return intent.getIntExtra(EXTRA_ID, -1)
    }

    companion object {
        /**
         * int extra
         */
        const val EXTRA_ID = "id"

        private fun toDottedDate(date: String): String {
            return dottedDateFormatter.format(dbDateFormatter.tryParse(date) ?: return "")
        }

        private fun toDottedDateTime(date: String): String {
            return dottedDateTimeFormatter.format(dbDateFormatter.tryParse(date) ?: return "")
        }

        private fun Boolean.toText(): String {
            return when (this) {
                true -> App.appContext.getString(R.string.yes)
                false -> App.appContext.getString(R.string.no)
            }
        }

        fun buildFieldsMap(inspection: InspectionDetails): List<Pair<CharSequence, String>> {
            val breakpointText = when (inspection.breakFlag) {
                true -> {
                    inspection.breakpointB
                }

                false -> {
                    inspection.breakpointA?.breakpoint
                }
            }

            return when (inspection.inspectionFlag) {
                0 -> {
                    App.appContext.resources.getTextArray(R.array.inspection_a_field_labels).zip(inspection.let {
                        listOf(
                            it.creator.name,
                            "${it.deviceCode}",
                            toDottedDateTime(it.creationTime),
                            it.productSpec ?: "",
                            it.wireSpeed?.toString() ?: "",
                            it.wireNum?.toString() ?: "",
                            it.breakSpec,
                            it.wireBatchCode ?: "",
                            it.stickBatchCode ?: "",
                            it.warehouse ?: "",
                            toDottedDate(it.productTime ?: ""),
                            it.breakFlag.toText(),
                            breakpointText ?: "",
                            it.breakCauseA?.type ?: "",
                            it.breakCauseA?.cause ?: "",
                            it.comments ?: "",
                        )
                    })
                }

                1 -> {
                    App.appContext.resources.getTextArray(R.array.inspection_b_field_labels).zip(inspection.let {
                        listOf(
                            it.inspector?.name ?: "",
                            toDottedDateTime(it.inspectionTime ?: ""),
                            it.breakCauseB?.type ?: "",
                            it.breakCauseB?.cause ?: "",
                            it.comments ?: "",
                        )
                    })
                }

                else -> {
                    throw RuntimeException("Unexpected value")
                }
            }
        }
    }
}
