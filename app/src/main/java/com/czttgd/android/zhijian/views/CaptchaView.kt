package com.czttgd.android.zhijian.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CaptchaView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var image: Bitmap? = null
    private val imagePaint = Paint()
    private var viewHeight: Int? = null
    private var viewWidth: Int? = null

    fun updateImage(image: Bitmap) {
        this.image = image
        invalidate()
        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        image?.let {
            val scale = (measuredHeight.toDouble() / it.height.toDouble()).toFloat()
            canvas.scale(scale, scale)
            canvas.drawBitmap(it, 0F, 0F, imagePaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val noDisplay = {
            setMeasuredDimension(0, 0)
        }

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            viewHeight = MeasureSpec.getSize(heightMeasureSpec)
            (image ?: return noDisplay()).let {
                val fixedHeight = MeasureSpec.getSize(heightMeasureSpec)
                val aspectRatio = it.width.toDouble() / it.height.toDouble()
                val newWidth = (aspectRatio * fixedHeight.toDouble()).toInt()
                viewWidth = newWidth
            }
        }
        setMeasuredDimension(viewWidth ?: 0, viewHeight ?: 0)
    }
}
