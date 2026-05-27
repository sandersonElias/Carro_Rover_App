package com.rover.control.ui.drive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Joystick circular customizado.
 * Retorna valores normalizados em [-1, 1] para X e Y via [onMove].
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((x: Float, y: Float) -> Unit)? = null

    private val paintBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2C")
        style = Paint.Style.FILL
    }
    private val paintBaseBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val paintStick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var stickRadius = 0f

    private var stickX = 0f
    private var stickY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        baseRadius  = min(w, h) / 2f * 0.9f
        stickRadius = baseRadius * 0.38f
        stickX = centerX
        stickY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, paintBase)
        canvas.drawCircle(centerX, centerY, baseRadius, paintBaseBorder)
        canvas.drawCircle(stickX, stickY, stickRadius, paintStick)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
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
                onMove?.invoke(
                    (stickX - centerX) / maxDist,
                    (stickY - centerY) / maxDist
                )
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stickX = centerX
                stickY = centerY
                onMove?.invoke(0f, 0f)
            }
        }
        invalidate()
        return true
    }
}
