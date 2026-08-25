package com.rover.control.ui.drive

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Joystick circular customizado com animação de spring-back.
 * Retorna valores normalizados em [-1, 1] para X e Y via [onMove].
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((x: Float, y: Float) -> Unit)? = null

    // Base circle paint with gradient
    private val paintBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintBaseBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3FB950")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // Stick paint with gradient
    private val paintStick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintStickBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3FB950")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Glow effect paint
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#403FB950")
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var stickRadius = 0f

    private var stickX = 0f
    private var stickY = 0f

    // Animation
    private var springAnimator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        baseRadius  = min(w, h) / 2f * 0.9f
        stickRadius = baseRadius * 0.38f
        stickX = centerX
        stickY = centerY

        // Create gradients
        paintBase.shader = RadialGradient(
            centerX, centerY, baseRadius,
            intArrayOf(Color.parseColor("#1C2333"), Color.parseColor("#161B22")),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        paintStick.shader = RadialGradient(
            centerX, centerY, stickRadius,
            intArrayOf(Color.parseColor("#4ADE80"), Color.parseColor("#3FB950")),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        // Draw base with glow effect
        canvas.drawCircle(centerX, centerY, baseRadius + 4f, paintGlow)
        canvas.drawCircle(centerX, centerY, baseRadius, paintBase)
        canvas.drawCircle(centerX, centerY, baseRadius, paintBaseBorder)

        // Draw crosshair lines (subtle)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#21262D")
            strokeWidth = 1f
        }
        canvas.drawLine(centerX - baseRadius, centerY, centerX + baseRadius, centerY, linePaint)
        canvas.drawLine(centerX, centerY - baseRadius, centerX, centerY + baseRadius, linePaint)

        // Draw stick with border
        canvas.drawCircle(stickX, stickY, stickRadius + 2f, paintStickBorder)
        canvas.drawCircle(stickX, stickY, stickRadius, paintStick)

        // Draw center dot on stick
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(stickX, stickY, stickRadius * 0.2f, dotPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Cancel any running spring animation
                springAnimator?.cancel()

                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = sqrt(dx * dx + dy * dy)
                val maxDist = baseRadius - stickRadius

                if (dist <= maxDist) {
                    stickX = event.x
                    stickY = event.y
                } else {
                    val ratio = maxDist / dist
                    stickX = centerX + dx * ratio
                    stickY = centerY + dy * ratio
                }
                if (maxDist > 0f) {
                    onMove?.invoke(
                        (stickX - centerX) / maxDist,
                        (stickY - centerY) / maxDist
                    )
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Spring-back animation
                animateSpringBack()
            }
        }
        invalidate()
        return true
    }

    private fun animateSpringBack() {
        val startX = stickX
        val startY = stickY
        val duration = 350L

        springAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                stickX = startX + (centerX - startX) * progress
                stickY = startY + (centerY - startY) * progress
                invalidate()

                // Invoke callback with interpolated values
                val maxDist = baseRadius - stickRadius
                if (maxDist > 0f) {
                    onMove?.invoke(
                        (stickX - centerX) / maxDist,
                        (stickY - centerY) / maxDist
                    )
                }
            }
            start()
        }
    }
}
