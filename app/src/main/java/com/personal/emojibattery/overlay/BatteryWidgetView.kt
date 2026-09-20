package com.personal.emojibattery.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.personal.emojibattery.R
import com.personal.emojibattery.data.StickerId
import com.personal.emojibattery.data.WidgetSize
import kotlin.math.roundToInt

/**
 * Custom view: sticker perched on a battery capsule with centered percentage text.
 */
class BatteryWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var level: Int = 67
    private var stickerId: StickerId = StickerId.TREECKO
    private var sizeScale: Float = 1f
    private var stickerDrawable: Drawable? = null

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = ContextCompat.getColor(context, R.color.battery_outline)
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.battery_bg)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.battery_fill_high)
    }
    private val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.battery_outline)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.battery_outline)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val capsuleRect = RectF()
    private val fillRect = RectF()
    private val tipRect = RectF()

    fun setBatteryLevel(percent: Int) {
        level = percent.coerceIn(0, 100)
        updateFillColor()
        invalidate()
    }

    fun setSticker(id: StickerId) {
        stickerId = id
        stickerDrawable = ContextCompat.getDrawable(context, id.drawableRes)
        requestLayout()
        invalidate()
    }

    fun setWidgetSize(size: WidgetSize) {
        sizeScale = size.scale
        requestLayout()
        invalidate()
    }

    fun applyConfig(sticker: StickerId, size: WidgetSize, percent: Int) {
        sizeScale = size.scale
        stickerId = sticker
        stickerDrawable = ContextCompat.getDrawable(context, sticker.drawableRes)
        level = percent.coerceIn(0, 100)
        updateFillColor()
        requestLayout()
        invalidate()
    }

    private fun updateFillColor() {
        val colorRes = when {
            level <= 15 -> R.color.battery_fill_low
            level <= 40 -> R.color.battery_fill_mid
            else -> R.color.battery_fill_high
        }
        fillPaint.color = ContextCompat.getColor(context, colorRes)
    }

    private fun dp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private fun baseCapsuleWidth(): Float = dp(72f) * sizeScale
    private fun baseCapsuleHeight(): Float = dp(26f) * sizeScale
    private fun stickerHeight(): Float = dp(40f) * sizeScale
    private fun tipWidth(): Float = dp(5f) * sizeScale
    private fun overlap(): Float = dp(10f) * sizeScale

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val tip = tipWidth()
        val w = (baseCapsuleWidth() + tip + dp(8f)).roundToInt()
        val h = (stickerHeight() + baseCapsuleHeight() - overlap() + dp(4f)).roundToInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cw = baseCapsuleWidth()
        val ch = baseCapsuleHeight()
        val tip = tipWidth()
        val stickerH = stickerHeight()
        val ov = overlap()

        val left = (width - cw - tip) / 2f
        val top = stickerH - ov

        // Capsule background
        capsuleRect.set(left, top, left + cw, top + ch)
        val radius = ch / 2f
        canvas.drawRoundRect(capsuleRect, radius, radius, bgPaint)

        // Fill level
        val inset = dp(2.5f) * sizeScale
        val fillMaxW = cw - inset * 2
        val fillW = fillMaxW * (level / 100f)
        fillRect.set(
            left + inset,
            top + inset,
            left + inset + fillW,
            top + ch - inset
        )
        val fillRadius = (ch - inset * 2) / 2f
        if (fillW > 0) {
            canvas.drawRoundRect(fillRect, fillRadius, fillRadius, fillPaint)
        }

        // Outline
        outlinePaint.strokeWidth = dp(1.8f) * sizeScale
        canvas.drawRoundRect(capsuleRect, radius, radius, outlinePaint)

        // Battery tip
        val tipH = ch * 0.45f
        tipRect.set(
            left + cw - dp(1f),
            top + (ch - tipH) / 2f,
            left + cw + tip,
            top + (ch + tipH) / 2f
        )
        canvas.drawRoundRect(tipRect, dp(2f), dp(2f), tipPaint)

        // Percentage text
        val label = "$level%"
        textPaint.textSize = dp(13f) * sizeScale
        val textY = capsuleRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, capsuleRect.centerX(), textY, textPaint)

        // Sticker sitting on top of capsule
        val drawable = stickerDrawable
            ?: ContextCompat.getDrawable(context, stickerId.drawableRes)?.also { stickerDrawable = it }
        if (drawable != null) {
            val sw = (cw * 0.95f).roundToInt()
            val sh = stickerH.roundToInt()
            val sx = ((width - sw) / 2f).roundToInt()
            val sy = 0
            drawable.setBounds(sx, sy, sx + sw, sy + sh)
            drawable.draw(canvas)
        }
    }
}
