// app/src/main/java/com/readwisequotes/ui/GradientBackgroundView.kt
package com.readwisequotes.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.readwisequotes.R

class GradientBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var gradientOffset = 0f
    private var animator: ValueAnimator? = null

    private val colorStart = ContextCompat.getColor(context, R.color.gradient_start)
    private val colorMid = ContextCompat.getColor(context, R.color.gradient_mid)
    private val colorEnd = ContextCompat.getColor(context, R.color.gradient_end)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        if (width == 0 || height == 0) return

        val diagonal = Math.hypot(width.toDouble(), height.toDouble()).toFloat()
        val offsetX = (gradientOffset * diagonal * 0.5f)

        paint.shader = LinearGradient(
            -diagonal / 2 + offsetX,
            -diagonal / 2,
            diagonal / 2 + offsetX,
            diagonal / 2,
            intArrayOf(colorStart, colorMid, colorEnd, colorStart),
            floatArrayOf(0f, 0.33f, 0.66f, 1f),
            Shader.TileMode.REPEAT
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 30000L // 30 seconds for full cycle
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                gradientOffset = animation.animatedValue as Float
                updateGradient()
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
