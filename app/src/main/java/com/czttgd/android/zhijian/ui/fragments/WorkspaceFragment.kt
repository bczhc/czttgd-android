package com.czttgd.android.zhijian.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.czttgd.android.zhijian.databinding.FragmentWorkspaceBinding
import com.czttgd.android.zhijian.ui.FormFillingActivity

class WorkspaceFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = FragmentWorkspaceBinding.inflate(inflater)

        val context = inflater.context
        bindings.card2.button.setOnClickListener {
            startActivity(Intent(context, FormFillingActivity::class.java).apply {
                putExtra(FormFillingActivity.EXTRA_STAGE, FormFillingActivity.STAGE_ONE)
            })
        }
        bindings.card3.button.setOnClickListener {
            startActivity(Intent(context, FormFillingActivity::class.java).apply {
                putExtra(FormFillingActivity.EXTRA_STAGE, FormFillingActivity.STAGE_TWO)
            })
        }

        return bindings.root
    }
}
