package com.czttgd.android.zhijian.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.broadcast.BarcodeBroadcastReceiver
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.data.InspectionDetails
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
    private var updateId: Long? = null
    private val barcodeBroadcastReceiver = BarcodeBroadcastReceiver barcode@{ _, content ->
        Log.d(tag, "Scanned: $content")
        content ?: return@barcode
        if (Regex("^[0-9]+$").matches(content)) {
            // for device code
            bindings.fieldMachineNumber.apply {
                inputTv.text = content
                hintTv.visibility = View.GONE
            }
            return@barcode
        }

        val regex = Regex("""^.*?:.*?,([0-9]+),([0-9]+),([A-Z]+),.*$""")
        runCatching {
            if (!regex.matches(content)) throw RuntimeException("Invalid QR")

            val captures = regex.find(content)!!
            val groups = captures.groupValues
            bindings.apply {
                fieldBreakSpecs.inputTv.text = groups[1]
                fieldCopperStickNo.inputTv.text = groups[2]
                fieldRepoNo.inputTv.text = groups[3]
            }
        }.onFailure {
            toast(R.string.invalid_qr_toast)
        }
    }
    private var stage: Int? = null

    private val selectedIds = object {
        var creator: Int? = null
        var breakpointA: Int? = null
        var breakCauseA: Int? = null
    }

    private fun registerSelectionLauncher(
        getValue: () -> FormFillingFieldLayoutBinding,
        onIdReturn: (Int) -> Unit
    ): ActivityResultLauncher<Array<SelectionActivity.Item>> {
        return registerForActivityResult(SelectionActivity.ActivityContract()) {
            it ?: return@registerForActivityResult
            val bindings = getValue()
            bindings.hintTv.visibility = View.GONE
            bindings.inputTv.text = it.items[it.selected].text
            onIdReturn(it.items[it.selected].id)
        }
    }

    private val selectionLaunchers = arrayOf(
        registerSelectionLauncher({ bindings.fieldCreator }) { selectedIds.creator = it },
        // deprecated
        registerSelectionLauncher({ bindings.fieldMachineNumber }) {},
        registerSelectionLauncher({ bindings.fieldBreakpointPosition }) { selectedIds.breakpointA = it },
        registerSelectionLauncher({ bindings.fieldBreakpointReason }) { selectedIds.breakCauseA = it },
        // deprecated
        registerSelectionLauncher({ bindings.fieldMachineCategory }) {},
    )

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityFormFillingBinding.inflate(layoutInflater)

        setContentView(bindings.root)
        this.stage = when (intent.getIntExtra(EXTRA_STAGE, -1)) {
            STAGE_ONE -> 1
            STAGE_TWO -> 2
            else -> throw RuntimeException("Unexpected stage number")
        }

        bindings.toolbar.setUpBackButton()

        updateMode = intent.hasExtra(EXTRA_UPDATE_FORM_DATA)

        val setUpClickEvent =
            { fieldBindings: FormFillingFieldLayoutBinding, inputType: Int?, check: (String) -> Boolean ->
                if (fieldBindings.inputTv.text.isNotEmpty()) {
                    fieldBindings.hintTv.visibility = View.GONE
                } else {
                    fieldBindings.hintTv.text = getString(R.string.form_please_input_hint)
                }

                fieldBindings.rl.setOnClickListener {
                    val dialogBindings = DialogInputTextBinding.inflate(layoutInflater).apply {
                        et.setText(fieldBindings.inputTv.text)
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
                        .setPositiveButton(R.string.confirm_button, null)
                        .setTitle(getString(R.string.please_enter_text_dialog_title, fieldBindings.labelTv.text))
                        .setView(dialogBindings.root)
                        .create().apply {
                            setCanceledOnTouchOutside(false)

                            setOnShowListener { d ->
                                val button = (d as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                                button.setOnClickListener {
                                    if (check(dialogBindings.et.text.toString())) {
                                        dialogBindings.et.error = null
                                        fieldBindings.hintTv.visibility = View.GONE
                                        fieldBindings.inputTv.text = dialogBindings.et.text.toString()
                                        d.dismiss()
                                    } else {
                                        dialogBindings.et.error = "格式示例：10/0.100"
                                    }
                                }
                            }
                        }
                        .show()

                    dialogBindings.et.requestFocus()
                }
            }

        setUpClickEvent(bindings.fieldProductSpecs, null) {
            it.matches(Regex("""^[0-9]{2}/[0-9]\.[0-9]{3}$"""))
        }
        setUpClickEvent(bindings.fieldComments, null) { true }
        setUpClickEvent(bindings.fieldWireNumber, InputType.TYPE_CLASS_NUMBER) { true }
        setUpClickEvent(bindings.fieldWireSpeed, InputType.TYPE_CLASS_NUMBER) { true }

        val setUpSelectionFields =
            a@{ fieldBindings: FormFillingFieldLayoutBinding, launcherIndex: Int, getItems: suspend () -> Array<SelectionActivity.Item> ->
                if (fieldBindings.inputTv.text.isNotEmpty()) {
                    fieldBindings.hintTv.visibility = View.GONE
                } else {
                    fieldBindings.hintTv.text = getString(R.string.form_please_select_hint)
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
                                    it.printStackTrace()
                                    if (it.toString().contains("unreachable")) {
                                        toast(R.string.no_network_toast)
                                    } else {
                                        toast(R.string.request_failed_toast)
                                    }
                                }
                            }
                        }
                    }.show()
                }
            }

        setUpSelectionFields(bindings.fieldCreator, 0) {
            SelectList.allUsers().mapToArray { SelectionActivity.Item(it.id, it.name) }
        }
        setUpSelectionFields(bindings.fieldBreakpointReason, 3) {
            SelectList.breakCauses().mapToArray { SelectionActivity.Item(it.id, "${it.type ?: ""}/${it.cause ?: ""}") }
        }

        setUpClickEvent(bindings.fieldMachineNumber, InputType.TYPE_CLASS_NUMBER) { true }

        if (!updateMode) {
            bindings.fieldBreakpointTime.inputTv.text = dbDateFormatter.format(Date())
        }

        val radioGroupOnChecked = { checkedId: Int ->
            selectedIds.breakpointA = null
            when (checkedId) {
                R.id.拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.apply {
                        hintTv.text = getString(R.string.form_please_input_hint)
                        inputTv.text = ""
                        setUpClickEvent(
                            this,
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        ) { true }
                    }
                }

                R.id.非拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.apply {
                        hintTv.text = getString(R.string.form_please_select_hint)
                        inputTv.text = ""
                        setUpSelectionFields(this, 2) {
                            SelectList.breakPoints().mapToArray { SelectionActivity.Item(it.id, it.breakpoint ?: "") }
                        }
                    }
                }

                else -> unreachable()
            }
        }
        bindings.radioGroup.setOnCheckedChangeListener { _, id -> radioGroupOnChecked(id) }
        radioGroupOnChecked(bindings.radioGroup.checkedRadioButtonId)

        setUpButton()

        ContextCompat.registerReceiver(
            this,
            barcodeBroadcastReceiver,
            IntentFilter(BarcodeBroadcastReceiver.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val mock = false
        if (mock) {
            bindings.apply {
//                fieldProductSpecs.inputTv.text = "?"
//                fieldCreator.inputTv.text = "李四"
//                fieldMachineNumber.inputTv.text = "2"
//                fieldMachineCategory.inputTv.text = "DT"
                fieldBreakSpecs.inputTv.text = "123"
//                fieldBreakpointReason.inputTv.text = "?"
            }
        }

        if (updateMode) {
            bindings.bottomButton.text = getString(R.string.modify_button)
            fillFields(intent.getTypedSerializableExtra<InspectionDetails>(EXTRA_UPDATE_FORM_DATA)!!)
            androidAssertion(intent.hasExtra(EXTRA_UPDATE_ID))
            this.updateId = intent.getLongExtra(EXTRA_UPDATE_ID, 0)
        } else {
            bindings.bottomButton.text = getString(R.string.submit_button)
        }

        bindings.apply {
            fieldMachineNumber.rl.updatePadding(right = 0)
            fieldMachineNumber.hintTv.text = "请输入"

            // this is hard-coded
            fieldMachineCategory.inputTv.text = "DT"
        }
    }

    private fun fillFields(form: InspectionDetails) {
        fun FormFillingFieldLayoutBinding.fill(text: String) {
            if (text.isEmpty()) return
            inputTv.text = text
            hintTv.visibility = View.GONE
        }

        bindings.apply {
            form.let {
                it.creator.apply {
                    fieldCreator.fill(name)
                    selectedIds.creator = id
                }
                fieldMachineNumber.fill(it.deviceCode.toString())
                fieldMachineCategory.fill(it.deviceCategory)
                fieldBreakpointTime.fill(it.creationTime)
                fieldProductSpecs.fill(it.productSpec ?: "")
                fieldWireSpeed.fill(it.wireSpeed?.toString() ?: "")
                fieldWireNumber.fill(it.wireNum?.toString() ?: "")
                fieldBreakSpecs.fill(it.breakSpec)
                fieldCopperStickNo.fill(it.stickBatchCode ?: "")
                fieldRepoNo.fill(it.warehouse ?: "")

                when (it.breakFlag) {
                    true -> {
                        radioGroup.check(R.id.拉丝池内断线_radio)
                        fieldBreakpointPosition.labelTv.text = getString(R.string.form_please_input_hint)
                        fieldBreakpointPosition.fill(it.breakpointB ?: "")
                    }

                    false -> {
                        radioGroup.check(R.id.非拉丝池内断线_radio)
                        fieldBreakpointPosition.labelTv.text = getString(R.string.form_please_select_hint)
                        it.breakpointA?.id?.let { id -> selectedIds.breakpointA = id }
                        fieldBreakpointPosition.fill(it.breakpointA?.breakpoint ?: "")
                    }
                }

                it.breakCauseA?.apply {
                    fieldBreakpointReason.fill(cause ?: "")
                    selectedIds.breakCauseA = id
                }
                fieldComments.fill(it.comments ?: "")
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

            var insertedId: Long? = null
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
                                    .setPositiveAction { _, _ ->

                                        buildProgressDialog(getString(R.string.printing_dialog_title)) { pd ->
                                            coroutineLaunchIo {
                                                val r = runCatching {
                                                    Inspection.queryDetails(insertedId!!)
                                                }
                                                withMain {
                                                    r.onFailure {
                                                        toast(R.string.request_failed_toast)
                                                        it.printStackTrace()
                                                        pd.dismiss()
                                                        finish()
                                                    }.onSuccess { details ->
                                                        runCatching {
                                                            runOnUiThread {
                                                                PrintUtils.printInspection(details, stage!!) {
                                                                    pd.dismiss()
                                                                    finish()
                                                                }
                                                            }
                                                        }.onFailure {
                                                            toast(R.string.print_error_toast)
                                                            it.printStackTrace()
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

            val breakFlag = when (radioGroup.checkedRadioButtonId) {
                R.id.拉丝池内断线_radio -> true
                R.id.非拉丝池内断线_radio -> false
                else -> unreachable()
            }

            val onError: (field: FormFillingFieldLayoutBinding) -> Unit = {
                it.labelTv.setTextColor(getColor(R.color.form_filling_error))
                hasError = true
            }
            val dummyZero = 0
            val record = InspectionForm(
                creator = run { fieldCreator.checkedField(onError) { it } ?: ""; selectedIds.creator ?: dummyZero },
                deviceCode = fieldMachineNumber.checkedField(onError) { it.toInt() } ?: dummyZero,
                creationTime = fieldBreakpointTime.checkedField(onError) { it } ?: "",
                productSpec = fieldProductSpecs.checkedField(onError) { it } ?: "",
                wireNumber = fieldWireNumber.checkedField(onError) { it.toInt() },
                breakSpec = fieldBreakSpecs.checkedField(onError) { it } ?: "",
                wireBatchCode = null,
                stickBatchCode = fieldCopperStickNo.checkedField(onError) { it },
                warehouse = fieldRepoNo.checkedField(onError) { it },
                breakFlag = breakFlag,
                breakCauseA = run {
                    fieldBreakpointReason.checkedField(onError) { it } ?: ""; selectedIds.breakCauseA ?: dummyZero
                },
                breakpointB = if (breakFlag) {
                    fieldBreakpointPosition.checkedField(onError) { BigDecimal(it); it }
                } else null,
                breakpointA = if (!breakFlag) {
                    run { fieldBreakpointPosition.checkedField(onError) { it }; selectedIds.breakpointA }
                } else null,
                comments = fieldComments.checkedField(onError) { it },
                deviceCategory = fieldMachineCategory.checkedField(onError) { it } ?: "",
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
         * Serializable extra; type: [InspectionDetails]
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
            val details: InspectionDetails,
            val id: Long,
            val stage: Int,
        )

        override fun createIntent(context: Context, input: Input): Intent {
            return Intent(context, FormFillingActivity::class.java).apply {
                putExtra(EXTRA_STAGE, input.stage)
                putExtra(EXTRA_UPDATE_ID, input.id)
                putExtra(EXTRA_UPDATE_FORM_DATA, input.details)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int? {
            intent ?: return null
            androidAssertion(intent.hasExtra(EXTRA_UPDATE_ID))
            return intent.getIntExtra(EXTRA_UPDATE_ID, -1)
        }
    }
}
