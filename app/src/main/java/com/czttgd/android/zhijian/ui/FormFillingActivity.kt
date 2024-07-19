package com.czttgd.android.zhijian.ui

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.lifecycleScope
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.SelectList
import com.czttgd.android.zhijian.databinding.ActivityFormFillingBinding
import com.czttgd.android.zhijian.databinding.DialogFetchingInfoBinding
import com.czttgd.android.zhijian.databinding.DialogInputTextBinding
import com.czttgd.android.zhijian.databinding.FormFillingFieldLayoutBinding
import com.czttgd.android.zhijian.utils.defaultNegativeButton
import com.czttgd.android.zhijian.utils.setPositiveAction
import com.czttgd.android.zhijian.utils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        listOf(bindings.fieldProductSpecs, bindings.fieldWireNumber, bindings.fieldComments).forEach(setUpClickEvent)

        val setUpSelectionFields =
            { fieldBindings: FormFillingFieldLayoutBinding, launcherIndex: Int, getItems: suspend () -> Array<String> ->
                fieldBindings.rl.setOnClickListener {
                    val dialogBindings = DialogFetchingInfoBinding.inflate(layoutInflater)
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setView(dialogBindings.root)
                        .create().apply {
                            setCanceledOnTouchOutside(false)
                        }.also { it.show() }

                    lifecycleScope.launch {
                        val result = runCatching { getItems() }
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            result.onSuccess {
                                selectionLaunchers[launcherIndex].launch(it)
                            }.onFailure {
                                toast(it.toString())
                            }
                        }
                    }
                }
            }

        setUpSelectionFields(bindings.fieldCreator, 0) { SelectList.allUsers().toTypedArray() }
        setUpSelectionFields(bindings.fieldMachineNumber, 1) {
            val stage = when (val stageExtra = intent.getIntExtra(EXTRA_STAGE, 0)) {
                STAGE_ONE -> 1
                STAGE_TWO -> 2
                else -> throw RuntimeException("Unexpected stage: $stageExtra")
            }
            SelectList.machineNumbers(stage).map { it.toString() }.toTypedArray()
        }
        setUpSelectionFields(bindings.fieldBreakpointPosition, 2) { arrayOf() }
        setUpSelectionFields(bindings.fieldBreakpointReason, 3) { arrayOf() }
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
