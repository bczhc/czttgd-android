package com.czttgd.android.zhijian.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import com.czttgd.android.zhijian.BaseActivity
import com.czttgd.android.zhijian.R
import com.czttgd.android.zhijian.databinding.ActivityLoginBinding

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val bindings = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bindings.root)



        super.onCreate(savedInstanceState)

        val captcha = resources.openRawResource(R.raw.captcha_demo).use {
            BitmapFactory.decodeStream(it)
        }
        bindings.captchaView.updateImage(captcha!!)

        bindings.loginButton.setOnClickListener {
            startActivity(Intent(this, MainPageActivity::class.java))
        }
    }
}
