package com.example.hydrohomie.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.hydrohomie.R

/**
 * A circular indicator that shows how much of the daily goal was reached.
 */
class WaterProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val arcBounds = RectF()

    private var progress: Float = 0f
    private var animator: ValueAnimator? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaterProgressView, defStyleAttr, 0)
        try {
            trackPaint.color = typedArray.getColor(
                R.styleable.WaterProgressView_trackColor,
                ContextCompat.getColor(context, R.color.progress_track),
            )
            progressPaint.color = typedArray.getColor(
                R.styleable.WaterProgressView_progressColor,
                ContextCompat.getColor(context, R.color.progress_fill),
            )
            val strokeWidth = typedArray.getDimension(
                R.styleable.WaterProgressView_strokeWidth,
                resources.getDimension(R.dimen.progress_stroke_width),
            )
            trackPaint.strokeWidth = strokeWidth
            progressPaint.strokeWidth = strokeWidth
        } finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = minOf(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val inset = progressPaint.strokeWidth / 2f + paddingLeft
        arcBounds.set(inset, inset, width - inset, height - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(arcBounds, START_ANGLE, FULL_SWEEP, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(arcBounds, START_ANGLE, FULL_SWEEP * progress, false, progressPaint)
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    /**
     * Sets the progress of the indicator.
     *
     * @param progress the progress between `0f` and `1f`.
     * @param animate whether the change is animated.
     */
    fun setProgress(progress: Float, animate: Boolean = true) {
        val clamped = progress.coerceIn(0f, 1f)
        val from = this.progress
        if (clamped == from) return

        animator?.cancel()
        if (!animate) {
            this.progress = clamped
            invalidate()
            return
        }

        animator = ValueAnimator.ofFloat(from, clamped).apply {
            duration = ANIMATION_MILLIS
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                this@WaterProgressView.progress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private companion object {
        const val START_ANGLE = -90f
        const val FULL_SWEEP = 360f
        const val ANIMATION_MILLIS = 400L
    }
}
