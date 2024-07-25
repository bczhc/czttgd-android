package com.czttgd.android.zhijian.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.broadcast.BarcodeBroadcastReceiver
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.data.InspectionForm
import com.czttgd.android.zhijian.data.SelectList
import com.czttgd.android.zhijian.databinding.ActivityFormFillingBinding
import com.czttgd.android.zhijian.databinding.DialogInputTextBinding
import com.czttgd.android.zhijian.databinding.FormFillingFieldLayoutBinding
import com.czttgd.android.zhijian.dbDateFormatter
import com.czttgd.android.zhijian.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.*


class FormFillingActivity : BaseActivity() {
    private lateinit var bindings: ActivityFormFillingBinding
    private var updateMode: Boolean = false
    private var updateId: Int? = null
    private val barcodeBroadcastReceiver = BarcodeBroadcastReceiver { _, content ->
        toast("Scanned: $content")
        // TODO
        bindings.fieldBreakSpecs.inputTv.text = content
    }
    private var stage: Int? = null

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

        val updateMode = intent.hasExtra(EXTRA_UPDATE_FORM_DATA)
        if (updateMode) {
            bindings.bottomButton.text = getString(R.string.modify_button)
            fillFields(intent.getTypedSerializableExtra<InspectionForm>(EXTRA_UPDATE_FORM_DATA)!!)
            androidAssertion(intent.hasExtra(EXTRA_UPDATE_ID))
            this.updateId = intent.getIntExtra(EXTRA_UPDATE_ID, 0)
        } else {
            bindings.bottomButton.text = getString(R.string.submit_button)
        }
        this.updateMode = updateMode

        val setUpClickEvent = { fieldBindings: FormFillingFieldLayoutBinding, inputType: Int? ->
            if (fieldBindings.inputTv.text.isNotEmpty()) {
                fieldBindings.hintTv.visibility = View.GONE
            }

            fieldBindings.rl.setOnClickListener {
                val dialogBindings = DialogInputTextBinding.inflate(layoutInflater).apply {
                    inputType?.let { et.inputType = it }
                    et.setOnFocusChangeListener { _, hasFocus ->
                        et.post {
                            if (hasFocus) {
                                val imm: InputMethodManager =
                                    this@FormFillingActivity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                    }
                }
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

                dialogBindings.et.requestFocus()
            }
        }

        setUpClickEvent(bindings.fieldProductSpecs, null)
        setUpClickEvent(bindings.fieldComments, null)
        setUpClickEvent(bindings.fieldWireNumber, InputType.TYPE_CLASS_NUMBER)
        setUpClickEvent(bindings.fieldWireSpeed, InputType.TYPE_CLASS_NUMBER)
        setUpClickEvent(
            bindings.fieldBreakpointPosition,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        val setUpSelectionFields =
            a@{ fieldBindings: FormFillingFieldLayoutBinding, launcherIndex: Int, getItems: suspend () -> Array<String> ->
                if (fieldBindings.inputTv.text.isNotEmpty()) {
                    fieldBindings.hintTv.visibility = View.GONE
                }

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
        setUpSelectionFields(bindings.fieldBreakpointReason, 3) { SelectList.breakReasons() }
        setUpSelectionFields(bindings.fieldMachineCategory, 4) { arrayOf("DL", "DT", "JX") }

        setUpClickEvent(bindings.fieldMachineNumber, InputType.TYPE_CLASS_NUMBER)

        if (!updateMode) {
            bindings.fieldBreakpointTime.inputTv.text = dbDateFormatter.format(Date())
        }

        bindings.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.apply {
                        hintTv.text = getString(R.string.form_please_input_hint)
                        inputTv.text = ""
                        setUpClickEvent(this, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                    }
                }

                R.id.非拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.apply {
                        hintTv.text = getString(R.string.form_please_select_hint)
                        inputTv.text = ""
                        setUpSelectionFields(this, 2) { SelectList.breakPoints() }
                    }
                }

                else -> unreachable()
            }
        }

        setUpButton()

        ContextCompat.registerReceiver(
            this,
            barcodeBroadcastReceiver,
            IntentFilter(BarcodeBroadcastReceiver.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun fillFields(form: InspectionForm) {
        bindings.apply {
            form.let {
                fieldCreator.inputTv.text = it.creator
                fieldMachineNumber.inputTv.text = it.machineNumber.toString()
                fieldMachineCategory.inputTv.text = it.machineCategory
                fieldBreakpointTime.inputTv.text = it.creationTime
                fieldProductSpecs.inputTv.text = it.productSpecs ?: ""
                fieldWireNumber.inputTv.text = it.wireNumber?.toString() ?: ""
                fieldBreakSpecs.inputTv.text = it.breakSpecs
                fieldCopperWireNo.inputTv.text = it.copperWireNo ?: ""
                fieldCopperStickNo.inputTv.text = it.copperStickNo ?: ""

                radioGroup.check(
                    when (it.breakType) {
                        0 -> R.id.拉丝池内断线_radio
                        1 -> R.id.非拉丝池内断线_radio
                        else -> throw RuntimeException("Unknown breakpoint type")
                    }
                )
                fieldBreakpointPosition.inputTv.text = when (it.breakType) {
                    0 -> it.breakPositionB
                    1 -> it.breakPositionA
                    else -> unreachable()
                }

                fieldBreakpointReason.inputTv.text = it.breakReasonA
                fieldComments.inputTv.text = it.comments
            }
        }
    }

    private fun setUpButton() {
        bindings.bottomButton.setOnClickListener {
            val (record, hasError) = collectForm()
            if (hasError) {
                toast(R.string.form_filling_please_check_fields)
                return@setOnClickListener
            }

            var insertedId: Int? = null
            buildProgressDialog(
                title = getString(R.string.submitting_dialog_title)
            ) {
                coroutineLaunchIo {
                    val result = runCatching {
                        if (updateMode) {
                            Inspection.update(record, id = updateId!!)
                        } else {
                            insertedId = Inspection.post(record)
                        }
                    }
                    withMain {
                        it.dismiss()
                        result.onSuccess {
                            if (updateMode) {
                                toast(R.string.modification_succeeded_toast)
                                Intent().apply {
                                    putExtra(EXTRA_UPDATE_ID, updateId!!)
                                    setResult(0, this)
                                }
                                finish()
                            } else {
                                MaterialAlertDialogBuilder(this@FormFillingActivity)
                                    .setTitle(R.string.submit_succeeded_print_dialog_title)
                                    .setNegativeAction { _, _ ->
                                        finish()
                                    }
                                    .setPositiveAction { d, _ ->
                                        d.dismiss()

                                        buildProgressDialog(getString(R.string.printint_dialog_title)) { pd ->
                                            coroutineLaunchIo {
                                                val r = runCatching {
                                                    Inspection.queryDetails(insertedId!!)
                                                }
                                                r.onFailure {
                                                    toast(R.string.print_error_toast)
                                                    finish()
                                                }.onSuccess { details ->
                                                    withMain {
                                                        runCatching {
                                                            PrintUtils.printInspection(details, stage!!) {
                                                                pd.dismiss()
                                                                finish()
                                                            }
                                                        }.onFailure {
                                                            toast(R.string.print_error_toast)
                                                            finish()
                                                        }
                                                    }
                                                }
                                            }
                                        }.show()
                                    }
                                    .create().apply {
                                        setCanceledOnTouchOutside(false)
                                    }.also { it.show() }
                            }
                        }.onFailure {
                            toast(getString(R.string.request_error_toast_with_message, it.toString()))
                            it.printStackTrace()
                        }
                    }
                }
            }.show()
        }
    }

    private fun collectForm(): Pair<InspectionForm, Boolean> {
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
            val dummyZero = 0
            val record = InspectionForm(
                creator = fieldCreator.checkedField(onError) { it } ?: "",
                machineNumber = fieldMachineNumber.checkedField(onError) { it.toInt() } ?: dummyZero,
                creationTime = fieldBreakpointTime.checkedField(onError) { it } ?: "",
                productSpecs = fieldProductSpecs.checkedField(onError) { it } ?: "",
                wireNumber = fieldWireNumber.checkedField(onError) { it.toInt() },
                breakSpecs = fieldBreakSpecs.checkedField(onError) { it } ?: "",
                copperWireNo = fieldCopperWireNo.checkedField(onError) { it },
                copperStickNo = fieldCopperStickNo.checkedField(onError) { it },
                repoNo = fieldRepoNo.checkedField(onError) { it },
                breakType = breakType,
                breakReasonA = fieldBreakpointReason.checkedField(onError) { it } ?: "",
                breakPositionB = if (breakType == 0) {
                    fieldBreakpointPosition.checkedField(onError) { BigDecimal(it); it }
                } else null,
                breakPositionA = if (breakType == 1) {
                    fieldBreakpointPosition.checkedField(onError) { it }
                } else null,
                comments = fieldComments.checkedField(onError) { it },
                machineCategory = fieldMachineCategory.checkedField(onError) { it } ?: "",
                wireSpeed = fieldWireSpeed.checkedField(onError) { it.toInt() },
            )
            return Pair(record, hasError)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(barcodeBroadcastReceiver)
        super.onDestroy()
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

        /**
         * Serializable extra; type: [InspectionForm]
         *
         * When this is present, the bottom button will turn to "modify" instead of "submit", and
         * it will update the database
         */
        const val EXTRA_UPDATE_FORM_DATA = "update form data"

        /**
         * Int extra
         *
         * ID of the inspection database record
         *
         * This must be present along with [EXTRA_UPDATE_FORM_DATA]
         */
        const val EXTRA_UPDATE_ID = "inspection id"
    }

    class UpdateActivityContract : ActivityResultContract<UpdateActivityContract.Input, Int?>() {
        data class Input(
            val form: InspectionForm,
            val id: Int,
            val stage: Int,
        )

        override fun createIntent(context: Context, input: Input): Intent {
            return Intent(context, FormFillingActivity::class.java).apply {
                putExtra(EXTRA_STAGE, input.stage)
                putExtra(EXTRA_UPDATE_ID, input.id)
                putExtra(EXTRA_UPDATE_FORM_DATA, input.form)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int? {
            intent ?: return null
            androidAssertion(intent.hasExtra(EXTRA_UPDATE_ID))
            return intent.getIntExtra(EXTRA_UPDATE_ID, -1)
        }
    }
}
