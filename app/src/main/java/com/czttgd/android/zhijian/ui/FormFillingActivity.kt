package com.czttgd.android.zhijian.ui

import android.os.Bundle
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.databinding.ActivityFormFillingBinding
import com.czttgd.android.zhijian.databinding.DialogInputTextBinding
import com.czttgd.android.zhijian.databinding.FormFillingFieldLayoutBinding
import com.czttgd.android.zhijian.utils.defaultNegativeButton
import com.czttgd.android.zhijian.utils.setPositiveAction
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FormFillingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ActivityFormFillingBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        bindings.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val setUpClickEvent = { fieldBindings: FormFillingFieldLayoutBinding ->
            fieldBindings.tv.setOnClickListener {
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
