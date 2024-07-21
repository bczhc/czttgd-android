package com.czttgd.android.zhijian.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
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
            val bindings = getValue()
            bindings.hintTv.visibility = View.GONE
            bindings.inputTv.text = (it ?: return@registerForActivityResult)
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
                        fieldBindings.hintTv.visibility = View.GONE
                        fieldBindings.inputTv.text = dialogBindings.et.text.toString()
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

        bindings.fieldBreakpointTime.inputTv.text = dateFormatter.format(Date())

        bindings.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.hintTv.text = getString(R.string.form_please_input_hint)
                    setUpClickEvent(bindings.fieldBreakpointPosition)
                }

                R.id.非拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.hintTv.text = getString(R.string.form_please_select_hint)
                    setUpSelectionFields(bindings.fieldBreakpointPosition, 2) { SelectList.breakPoints() }
                }

                else -> unreachable()
            }
        }


        bindings.submit.setOnClickListener {
            val (record, hasError) = collectForm()
            if (hasError) {
                toast(R.string.form_filling_please_check_fields)
                return@setOnClickListener
            }

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
            fieldBreakSpecs.inputTv.setOnClickListener {
                // mock data for test purposes
                fieldBreakSpecs.inputTv.text = "abc"
                fieldCopperWireNo.inputTv.text = "1"
                fieldCopperStickNo.inputTv.text = "2"
                fieldRepoNo.inputTv.text = "3"
            }
        }
    }

    private fun collectForm(): Pair<InspectionRecord, Boolean> {
        fun FormFillingFieldLayoutBinding.fieldValue(): String {
            return inputTv.text.toString()
        }

        bindings.apply {
            var hasError = false

            fun <T> FormFillingFieldLayoutBinding.checkedField(
                onError: (field: FormFillingFieldLayoutBinding) -> Unit,
                transform: (String) -> T
            ): T? {
                val field = this
                field.labelTv.setTextColor(Color.BLACK)
                val fieldValue = field.fieldValue()
                if (field.required!! && fieldValue.isEmpty()) {
                    onError(field)
                    return null
                }
                // empty non-required fields are valid
                if (!field.required!! && fieldValue.isEmpty()) {
                    return null
                }
                runCatching {
                    return transform(fieldValue)
                }.onFailure {
                    onError(field)
                }
                return null
            }

            val breakType = when (radioGroup.checkedRadioButtonId) {
                R.id.拉丝池内断线_radio -> 0
                R.id.非拉丝池内断线_radio -> 1
                else -> unreachable()
            }

            val onError: (field: FormFillingFieldLayoutBinding) -> Unit = {
                it.labelTv.setTextColor(getColor(R.color.form_filling_error))
                hasError = true
            }
            val dummyZero = 0.toUInt()
            val record = InspectionRecord(
                creator = fieldCreator.checkedField(onError) { it } ?: "",
                machineNumber = fieldMachineNumber.checkedField(onError) { it.toUInt() } ?: dummyZero,
                creationTime = fieldBreakpointTime.checkedField(onError) { it } ?: "",
                productSpecs = fieldProductSpecs.checkedField(onError) { it } ?: "",
                wireNumber = fieldWireNumber.checkedField(onError) { it.toUInt() },
                breakSpecs = fieldBreakSpecs.checkedField(onError) { it } ?: "",
                copperWireNo = fieldCopperWireNo.checkedField(onError) { it.toUInt() },
                copperStickNo = fieldCopperStickNo.checkedField(onError) { it.toUInt() },
                repoNo = fieldRepoNo.checkedField(onError) { it.toUInt() },
                breakType = breakType.toUInt(),
                breakReasonA = fieldBreakpointReason.checkedField(onError) { it } ?: "",
                breakPositionB = if (breakType == 0) {
                    fieldBreakpointPosition.checkedField(onError) { it.toFloat() }
                } else null,
                breakPositionA = if (breakType == 1) {
                    fieldBreakpointPosition.checkedField(onError) { it }
                } else null,
                comments = fieldComments.checkedField(onError) { it },
                machineCategory = fieldMachineCategory.checkedField(onError) { it } ?: "",
            )
            return Pair(record, hasError)
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
