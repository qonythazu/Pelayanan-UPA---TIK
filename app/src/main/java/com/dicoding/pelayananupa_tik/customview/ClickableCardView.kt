package com.dicoding.pelayananupa_tik.customview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.dicoding.pelayananupa_tik.R

class ClickableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CardView(context, attrs) {

    private var originalBackground: GradientDrawable? = null

    init {
        originalBackground = background as? GradientDrawable

        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val pressedBackground = GradientDrawable().apply {
                        setColor(Color.parseColor("#80FFFFFF"))
                        cornerRadius = originalBackground?.cornerRadius ?: 0f
                        setStroke(5, ContextCompat.getColor(context, R.color.primary_blue))
                    }
                    v.background = pressedBackground
                    v.scaleX = 1.1f
                    v.scaleY = 1.1f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = originalBackground
                    v.scaleX = 1f
                    v.scaleY = 1f
                    performClick()
                }
            }
            true
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
