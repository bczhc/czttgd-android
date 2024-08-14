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
import androidx.activity.addCallback
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.*


class FormFillingActivity : BaseActivity() {
    private lateinit var bindings: ActivityFormFillingBinding
    private var updateMode: Boolean = false
    private var updateId: Long? = null
    private val barcodeBroadcastReceiver = BarcodeBroadcastReceiver barcode@{ _, content ->
        onBarcodeScanned(content)
    }
    private var stage: Int? = null

    private val selectedIds = object {
        var creator: Int? = null
        var breakpointA: Int? = null
        var breakCauseA: Int? = null
    }

    private val zxingLauncher = registerForActivityResult(ScanContract()) a@{
        onBarcodeScanned((it ?: return@a).contents)
    }

    private var modified = false

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
        registerSelectionLauncher({ bindings.fieldWireType }) {},
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
            { fieldBindings: FormFillingFieldLayoutBinding, inputType: Int?, check: (String) -> FieldCheckResult ->
                if (fieldBindings.inputTv.text.isNotEmpty()) {
                    fieldBindings.hintTv.visibility = View.GONE
                } else {
                    fieldBindings.hintTv.text = getString(R.string.form_please_input_hint)
                }

                fieldBindings.rl.setOnClickListener {
                    modified = true
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
                                    val fieldCheckResult = check(dialogBindings.et.text.toString())
                                    if (fieldCheckResult == FieldCheckResult.Success) {
                                        dialogBindings.et.error = null
                                        fieldBindings.hintTv.visibility = View.GONE
                                        fieldBindings.inputTv.text = dialogBindings.et.text.toString()
                                        d.dismiss()
                                    } else {
                                        dialogBindings.et.error = fieldCheckResult.error
                                    }
                                }
                            }
                        }
                        .show()

                    dialogBindings.et.requestFocus()
                }
            }

        setUpClickEvent(bindings.fieldProductSpecs, null) {
            val match = it.matches(Regex("""^[0-9]{2}/[0-9]\.[0-9]{3}$"""))
            FieldCheckResult.from(match, "格式示例：10/0.100")
        }
        setUpClickEvent(bindings.fieldComments, null) { FieldCheckResult.Success }
        setUpClickEvent(bindings.fieldWireNumber, InputType.TYPE_CLASS_NUMBER) {
            FieldCheckResult.from(it.matches(Regex("^[0-9]{1,2}$")), "最多两位数字")
        }
        setUpClickEvent(bindings.fieldWireSpeed, InputType.TYPE_CLASS_NUMBER) { FieldCheckResult.Success }

        val setUpSelectionFields =
            a@{ fieldBindings: FormFillingFieldLayoutBinding, launcherIndex: Int, getItems: suspend () -> Array<SelectionActivity.Item> ->
                if (fieldBindings.inputTv.text.isNotEmpty()) {
                    fieldBindings.hintTv.visibility = View.GONE
                } else {
                    fieldBindings.hintTv.text = getString(R.string.form_please_select_hint)
                }

                fieldBindings.rl.setOnClickListener {
                    modified = true
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
            SelectList.allUsers().filter {
                it.enableState == 2 /* 已启用 */ && it.userType == "2" /* App用户 */
            }.map { SelectionActivity.Item(it.id, it.name) }.toTypedArray()
        }
        setUpSelectionFields(bindings.fieldBreakpointReason, 3) {
            SelectList.breakCauses().filter {
                it.enableState == 2 /* 已启用 */
            }.map {
                SelectionActivity.Item(it.id, "${it.type ?: ""}/${it.cause ?: ""}")
            }.toTypedArray()
        }

        setUpClickEvent(bindings.fieldMachineNumber, InputType.TYPE_CLASS_NUMBER) {
            FieldCheckResult.from(it.matches(Regex("^[0-9]{1,2}$")), "最多两位数字")
        }

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
                        ) { FieldCheckResult.Success }
                    }
                }

                R.id.非拉丝池内断线_radio -> {
                    bindings.fieldBreakpointPosition.apply {
                        hintTv.text = getString(R.string.form_please_select_hint)
                        inputTv.text = ""
                        setUpSelectionFields(this, 2) {
                            SelectList.breakPoints()
                                .filter { it.enableState == 2 }
                                .map { SelectionActivity.Item(it.id, it.breakpoint ?: "") }
                                .toTypedArray()
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
            fieldMachineNumber.hintTv.text = "请输入或扫码"

            // this is hard-coded
            fieldMachineCategory.inputTv.text = "DT"

            // for camera barcode scanning
            listOf(
                bindings.fieldBreakSpecs,
                bindings.fieldCopperStickNo,
                bindings.fieldRepoNo,
                bindings.fieldProductTime,
            ).forEach {
                it.hintTv.setOnLongClickListener {
                    zxingLauncher.launch(ScanOptions())
                    true
                }
            }
        }

        if (updateMode) {
            setUpClickEvent(bindings.fieldBreakpointTime, null) {
                val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
                if (!it.matches(dateRegex)) {
                    return@setUpClickEvent FieldCheckResult.Failure.apply { error = "格式有误。示例：2024-02-03" }
                }

                FieldCheckResult.Success
            }
        }

        onBackPressedDispatcher.addCallback {
            if (modified) {
                MaterialAlertDialogBuilder(this@FormFillingActivity)
                    .setTitle(R.string.ask_for_save_dialog_title)
                    .setNegativeAction { _, _ ->
                        finish()
                    }
                    .setPositiveAction { _, _ ->
                        bindings.bottomButton.performClick()
                    }
                    .show()
            } else finish()
        }

        bindings.fieldBreakSpecs.hintTv.text = "请扫码"

        setUpSelectionFields(bindings.fieldWireType, 5) {
            arrayOf("裸铜", "镀锡").mapToArray { SelectionActivity.Item(0, it) }
        }
    }

    /**
     * Fills the fields on "modification" mode.
     */
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
//                fieldWireSpeed.fill(it.wireSpeed?.toString() ?: "")
                fieldWireNumber.fill(it.wireNum?.toString() ?: "")
                fieldBreakSpecs.fill(it.breakSpec)
                fieldCopperStickNo.fill(it.stickBatchCode ?: "")
                fieldRepoNo.fill(it.warehouse ?: "")
                fieldProductTime.fill(it.productTime ?: "")

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
                fieldWireType.fill(it.wireType ?: "")
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
                                    .setOnCancelListener {
                                        finish()
                                    }
                                    .create().apply {
                                        setCanceledOnTouchOutside(false)
                                    }.also { it.show() }
                            }
                        }.onFailure {
                            val error = it.toString()
                            // display user-friendly toasts for specific errors
                            if (error.contains("Out of range value for column 'breakpointb'")) {
                                toast(getString(R.string.breakpoint_b_out_of_range_toast))
                            } else if (error.contains("network")) {
                                toast(getString(R.string.network_error_toast, error))
                            } else {
                                toast(getString(R.string.request_error_toast_with_message, error))
                            }
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
                productSpec = fieldProductSpecs.checkedField(onError) { it },
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
                productTime = fieldProductTime.checkedField(onError) { it },
                wireType = fieldWireType.checkedField(onError) { it },
            )
            return Pair(record, hasError)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(barcodeBroadcastReceiver)
        super.onDestroy()
    }

    private fun onBarcodeScanned(content: String?) {
        Log.d(tag, "Scanned: $content")
        content ?: return
        if (Regex("^[0-9]{1,2}$").matches(content)) {
            // for device code
            bindings.fieldMachineNumber.apply {
                inputTv.text = content
                hintTv.visibility = View.GONE
            }
            return
        }

        runCatching {
            val split = content.split(",")
            bindings.apply {
                val stickBatchCode = split[split.lastIndex - 1]
                fieldBreakSpecs.inputTv.text = split[1]
                fieldCopperStickNo.inputTv.text = stickBatchCode
                fieldRepoNo.inputTv.text = split.last()
                val productTime = stickBatchCode.substring(1 until (1 + 6)).let {
                    // just assume the date is in 20xx
                    "20${it.substring(0 until 2)}-${it.substring(2 until 4)}-${it.substring(4 until 6)}"
                }
                fieldProductTime.inputTv.text = productTime
            }
        }.onFailure {
            toast(R.string.invalid_qr_toast)
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

    enum class FieldCheckResult {
        Success,
        Failure;

        var error: String? = null

        companion object {
            fun from(check: Boolean, error: String? = null): FieldCheckResult {
                return when (check) {
                    true -> Success
                    false -> Failure.apply { this.error = error }
                }
            }
        }
    }
}
