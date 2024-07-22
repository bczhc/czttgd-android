package com.czttgd.android.zhijian.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.data.Inspection
import com.czttgd.android.zhijian.utils.androidAssertion
import com.czttgd.android.zhijian.utils.buildProgressDialog
import com.czttgd.android.zhijian.utils.coroutineLaunchIo

class InspectionDetailsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        androidAssertion(intent.hasExtra(EXTRA_ID))
        val id = intent.getIntExtra(EXTRA_ID, -1).toUInt()
        Intent().apply {
            putExtra(EXTRA_ID, id.toInt())
            setResult(0, this)
        }

        buildProgressDialog(getString(R.string.fetching_dialog_title)) {
            coroutineLaunchIo {
                val inspection = Inspection.queryDetails(id)
            }
        }

        super.onCreate(savedInstanceState)
    }

    class ActivityContract : ActivityResultContract<UInt, UInt>() {
        override fun createIntent(context: Context, input: UInt): Intent {
            return Intent(context, InspectionDetailsActivity::class.java).apply {
                putExtra(EXTRA_ID, input.toInt())
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): UInt {
            return intent!!.getIntExtra(EXTRA_ID, -1).toUInt()
        }
    }

    companion object {
        /**
         * int extra
         */
        const val EXTRA_ID = "id"
    }
}
