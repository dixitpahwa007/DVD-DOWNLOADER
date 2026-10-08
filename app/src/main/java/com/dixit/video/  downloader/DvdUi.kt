package com.dixit.video.downloader

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object DvdUi {
    val bg = Color.rgb(4, 13, 29)
    val surface = Color.rgb(8, 20, 39)
    val card = Color.rgb(12, 29, 52)
    val card2 = Color.rgb(17, 36, 65)
    val brand = Color.rgb(105, 82, 255)
    val cyan = Color.rgb(64, 213, 255)
    val text = Color.WHITE
    val secondary = Color.rgb(167, 184, 207)
    val divider = Color.rgb(39, 61, 91)

    fun dp(c: Context, value: Int): Int = (value * c.resources.displayMetrics.density).toInt()

    fun background(color: Int, radius: Int = 18, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (stroke != null) setStroke(dpRadius(radius), stroke)
        }

    private fun dpRadius(value: Int): Int = value

    fun text(c: Context, value: String, size: Float, color: Int = text, bold: Boolean = false): TextView =
        TextView(c).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    fun title(c: Context, value: String): TextView = text(c, value, 20f, text, true).apply {
        setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 8))
    }

    fun card(c: Context, child: View, marginTop: Int = 10): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12))
        background = DvdUi.background(DvdUi.card, 18, DvdUi.divider)
        val p = LinearLayout.LayoutParams(-1, -2)
        p.topMargin = dp(c, marginTop)
        addView(child, LinearLayout.LayoutParams(-1, -2))
        layoutParams = p
    }

    fun pill(c: Context, label: String, active: Boolean = false): TextView = text(c, label, 11f, if (active) Color.WHITE else secondary, active).apply {
        gravity = Gravity.CENTER
        setPadding(dp(c, 16), dp(c, 9), dp(c, 16), dp(c, 9))
        background = background(if (active) brand else card2, 40)
    }

    fun row(c: Context, weightSum: Float = 1f): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        this.weightSum = weightSum
        gravity = Gravity.CENTER_VERTICAL
    }
}
