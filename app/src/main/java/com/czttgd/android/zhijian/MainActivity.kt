package com.czttgd.android.zhijian

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.czttgd.android.zhijian.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val bindings = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        super.onCreate(savedInstanceState)

        val captcha = resources.openRawResource(R.raw.captcha_demo).use {
            BitmapFactory.decodeStream(it)
        }
        bindings.captchaView.updateImage(captcha!!)
    }
}
