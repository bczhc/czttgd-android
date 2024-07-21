@file:Suppress("NAME_SHADOWING")

package com.czttgd.android.zhijian.ui

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.data.InspectionRecord
import com.czttgd.android.zhijian.data.SelectList
import com.czttgd.android.zhijian.databinding.ActivityFormFillingBinding
import com.czttgd.android.zhijian.databinding.DialogInputTextBinding
import com.czttgd.android.zhijian.databinding.FormFillingFieldLayoutBinding
import com.czttgd.android.zhijian.dateFormatter
import com.czttgd.android.zhijian.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class FormFillingActivity : BaseActivity() {
    private lateinit var bindings: ActivityFormFillingBinding

    private fun registerSelectionLauncher(getValue: () -> FormFillingFieldLayoutBinding): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(SelectionActivity.ActivityContract()) {
            getValue().tv.text = (it ?: return@registerForActivityResult)
        }
    }

    private val selectionLaunchers = arrayOf(
        registerSelectionLauncher { bindings.fieldCreator },
        registerSelectionLauncher { bindings.fieldMachineNumber },
        registerSelectionLauncher { bindings.fieldBreakpointPosition },
        registerSelectionLauncher { bindings.fieldBreakpointReason },
        registerSelectionLauncher { bindings.fieldMachineCategory },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityFormFillingBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        bindings.toolbar.setUpBackButton()

        val setUpClickEvent = { fieldBindings: FormFillingFieldLayoutBinding ->
            fieldBindings.rl.setOnClickListener {
                val dialogBindings = DialogInputTextBinding.inflate(layoutInflater)
                MaterialAlertDialogBuilder(this)
                    .defaultNegativeButton()
                    .setPositiveAction { _, _ ->
                        fieldBindings.tv.text = dialogBindings.et.text.toString()
                    }
                    .setTitle(getString(R.string.please_enter_text_dialog_title, fieldBindings.labelTv.text))
                    .setView(dialogBindings.root)
                    .create().apply {
                        setCanceledOnTouchOutside(false)
                    }
                    .show()
            }
        }

        listOf(
            bindings.fieldProductSpecs,
            bindings.fieldWireNumber,
            bindings.fieldComments,
            bindings.fieldBreakpointPosition
        ).forEach(setUpClickEvent)

        val setUpSelectionFields =
            { fieldBindings: FormFillingFieldLayoutBinding, launcherIndex: Int, getItems: suspend () -> Array<String> ->
                fieldBindings.rl.setOnClickListener {
                    buildProgressDialog(
                        title = getString(R.string.fetching_info_dialog_title)
                    ) { d ->
                        coroutineLaunchIo {
                            val result = runCatching { getItems() }
                            withContext(Dispatchers.Main) {
                                d.dismiss()
                                result.onSuccess {
                                    selectionLaunchers[launcherIndex].launch(it)
                                }.onFailure {
                                    toast(it.toString())
                                    it.printStackTrace()
                                }
                            }
                        }
                    }.show()
                }
            }

        setUpSelectionFields(bindings.fieldCreator, 0) { SelectList.allUsers() }
        setUpSelectionFields(bindings.fieldMachineNumber, 1) {
            val stage = when (val stageExtra = intent.getIntExtra(EXTRA_STAGE, 0)) {
                STAGE_ONE -> 1
                STAGE_TWO -> 2
                else -> throw RuntimeException("Unexpected stage: $stageExtra")
            }
            SelectList.machineNumbers(stage).map { it.toString() }.toTypedArray()
        }
        setUpSelectionFields(bindings.fieldBreakpointReason, 3) { SelectList.breakReasons() }
        setUpSelectionFields(bindings.fieldMachineCategory, 4) { arrayOf("DL", "DT", "JX") }

        bindings.fieldBreakpointTime.tv.text = dateFormatter.format(Date())

        bindings.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.tv.text = getString(R.string.form_please_input_hint)
                    setUpClickEvent(bindings.fieldBreakpointPosition)
                }

                R.id.非拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.tv.text = getString(R.string.form_please_select_hint)
                    setUpSelectionFields(bindings.fieldBreakpointPosition, 2) { SelectList.breakPoints() }
                }

                else -> unreachable()
            }
        }

        fun FormFillingFieldLayoutBinding.fieldValue(): String {
            return tv.text.toString()
        }
        bindings.submit.setOnClickListener {
            val result = runCatching {
                val record: InspectionRecord
                bindings.apply {
                    val breakType = when (radioGroup.checkedRadioButtonId) {
                        R.id.拉丝池内断线_radio -> 0
                        R.id.非拉丝池内断线_radio -> 1
                        else -> unreachable()
                    }
                    val breakPositionInput = fieldBreakpointPosition.fieldValue()
                    record = InspectionRecord(
                        creator = fieldCreator.fieldValue(),
                        machineNumber = fieldMachineNumber.fieldValue().toUInt(),
                        creationTime = fieldBreakpointTime.fieldValue(),
                        productSpecs = fieldProductSpecs.fieldValue(),
                        wireNumber = fieldWireNumber.fieldValue().toUInt(),
                        breakSpecs = fieldBreakSpecs.fieldValue(),
                        copperWireNo = fieldCopperWireNo.fieldValue().toUInt(),
                        copperStickNo = fieldCopperStickNo.fieldValue().toUInt(),
                        repoNo = fieldRepoNo.fieldValue().toUInt(),
                        breakType = breakType.toUInt(),
                        breakReasonA = fieldBreakpointReason.fieldValue(),
                        breakPositionB = if (breakType == 0) {
                            breakPositionInput.toFloat()
                        } else null,
                        breakPositionA = if (breakType == 1) {
                            breakPositionInput
                        } else null,
                        comments = fieldComments.fieldValue(),
                        machineCategory = fieldMachineCategory.fieldValue(),
                    )
                }
                record
            }
            result.onFailure {
                toast(it.toString())
                return@setOnClickListener
            }

            val record = result.getOrNull()!!
            buildProgressDialog(
                title = getString(R.string.submitting_dialog_title)
            ) {
                coroutineLaunchIo {
                    val result = runCatching {
                        Inspection.post(record)
                    }
                    withMain {
                        it.dismiss()
                        result.onSuccess {
                            toast(R.string.submit_succeeded_toast)
                            finish()
                        }.onFailure {
                            toast(R.string.submit_error_toast)
                            it.printStackTrace()
                        }
                    }
                }
            }
        }

        bindings.apply {
            fieldBreakSpecs.tv.setOnClickListener {
                // mock data for test purposes
                fieldBreakSpecs.tv.text = "abc"
                fieldCopperWireNo.tv.text = "1"
                fieldCopperStickNo.tv.text = "2"
                fieldRepoNo.tv.text = "3"
            }
        }
    }

    companion object {
        /**
         * Integer extra
         *
         * 期数（1或2）
         *
         * See [STAGE_ONE] and [STAGE_TWO]
         */
        const val EXTRA_STAGE = "stage"

        const val STAGE_ONE = 1

        const val STAGE_TWO = 2
    }
}
