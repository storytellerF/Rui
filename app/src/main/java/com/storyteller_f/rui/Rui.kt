package com.storyteller_f.rui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withTranslation
import kotlin.math.ceil
import kotlin.math.floor

class Rui @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {
    private val starSpace: Float
    private val isIndicator: Boolean
    private val starCount: Int
    private val starDrawable: Drawable
    private val backgroundDrawable: Drawable
    private val clippedDrawable: Drawable
    private var starProgress: Float = 2.5f
        set(value) {
            field = value
            invalidate()
        }

    var listener: RatingChangedListener? = null

    init {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attributeSet, R.styleable.Rui)
        starSpace =
            obtainStyledAttributes.getDimension(R.styleable.Rui_starSpace, 0f)
        isIndicator =
            obtainStyledAttributes.getBoolean(R.styleable.Rui_isIndicator, false)
        starCount = obtainStyledAttributes.getInteger(R.styleable.Rui_starCount, 5)
        val drawable =
            obtainStyledAttributes.getDrawable(R.styleable.Rui_ratingBarStyle)
        drawable as LayerDrawable
        starDrawable = drawable.getDrawable(drawable.findIndexByLayerId(android.R.id.progress))
        backgroundDrawable =
            drawable.getDrawable(drawable.findIndexByLayerId(android.R.id.background))
        clippedDrawable = ClipDrawable(starDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
        obtainStyledAttributes.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val inheritedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val starWidth = getStarWidth(inheritedWidth)
        setMeasuredDimension(inheritedWidth, starWidth.toInt())
    }

    private fun getStarWidth(inheritedWidth: Int) =
        (inheritedWidth - starSpace * (starCount - 1)) / starCount

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val starWidth = getStarWidth(measuredWidth).toInt()

        val down = floor(starProgress).toInt()
        for (i in down until starCount + 1) {
            val start = i * (starWidth + starSpace)
            canvas.withTranslation(x = start) {
                backgroundDrawable.let { drawable ->
                    drawable.setBounds(0, 0, starWidth, starWidth)
                    drawable.draw(canvas)
                }
            }
        }
        repeat(down) {
            val start = it * (starWidth + starSpace)
            canvas.withTranslation(x = start) {
                starDrawable.let { drawable ->
                    drawable.setBounds(0, 0, starWidth, starWidth)
                    drawable.draw(canvas)
                }
            }
        }
        val fl = starProgress - down
        if (fl > 0) {
            val start = down * (starWidth + starSpace)
            canvas.withTranslation(x = start) {
                clippedDrawable.let { clipDrawable ->
                    clipDrawable.level = (10000 * fl).toInt()
                    clipDrawable.setBounds(0, 0, starWidth, starWidth)
                    clipDrawable.draw(canvas)
                }
            }
        }
    }

    private var moved = false
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || isIndicator) return super.onTouchEvent(event)
        val x = event.x
        val currentWidth = measuredWidth

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                moved = true
                starProgress = (x / currentWidth) * starCount
            }

            MotionEvent.ACTION_DOWN -> {
                moved = false
            }

            MotionEvent.ACTION_UP -> {
                starProgress = if (moved) {
                    ceil(starProgress)
                } else {
                    val starWidth = getStarWidth(currentWidth)
                    val starAndSpace = starWidth + starSpace
                    //触摸位置在第几个星星
                    val position = ceil(x / starAndSpace).toInt()
                    val oldPosition = ceil(starProgress).toInt()
                    val oldSplitStar = oldPosition - starProgress > 0
                    when {
                        position != oldPosition -> position.toFloat() - 0.5f
                        oldSplitStar -> {
                            position.toFloat()
                        }

                        else -> position - 0.5f
                    }
                }
                listener?.onChanged(starProgress, starCount, true)
            }
        }
        return true
    }

    @FunctionalInterface
    interface RatingChangedListener {
        fun onChanged(progress: Float, max: Int, fromUser: Boolean)
    }
}