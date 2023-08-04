package com.storyteller_f.rui_library

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.minus
import androidx.core.graphics.withTranslation
import kotlin.math.ceil
import kotlin.math.floor

class Rui @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) :
    View(context, attributeSet) {
    private val starSpace: Float
    private val isIndicator: Boolean
    private val starCount: Int
    private val starDirection: Int
    private val starDrawable: Drawable
    private val backgroundDrawable: Drawable?
    private val clippedDrawable: Drawable

    object Direction {
        const val left = 1
        const val right = 2
    }

    private var starProgress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var listener: RatingChangedListener? = null

    init {
        val obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.Rui)
        starSpace = obtainStyledAttributes.getDimension(R.styleable.Rui_starSpace, 0f)
        isIndicator = obtainStyledAttributes.getBoolean(R.styleable.Rui_isIndicator, false)
        starCount = obtainStyledAttributes.getInteger(R.styleable.Rui_starCount, 5)
        val drawable = obtainStyledAttributes.getDrawable(R.styleable.Rui_ratingBarStyle)
        drawable as LayerDrawable
        starDrawable = drawable.getDrawable(drawable.findIndexByLayerId(android.R.id.progress))
        val backgroundIndex = drawable.findIndexByLayerId(android.R.id.background)
        backgroundDrawable =
            if (backgroundIndex >= 0) drawable.getDrawable(backgroundIndex) else null
        clippedDrawable = ClipDrawable(starDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
        starProgress = obtainStyledAttributes.getFloat(R.styleable.Rui_starProgress, 0f)
        starDirection =
            obtainStyledAttributes.getInteger(R.styleable.Rui_starDirection, Direction.left)
        obtainStyledAttributes.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val inheritedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val starWidth = getStarWidth(inheritedWidth)
        setMeasuredDimension(inheritedWidth, starWidth.toInt())
    }

    /**
     * @return 返回一个星星的宽
     */
    private fun getStarWidth(viewWidth: Int) =
        (viewWidth - starSpace * (starCount - 1)) / starCount

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val starWidth = getStarWidth(measuredWidth).toInt()

        val currentProgress = if (progressWhenMoving >= 0f) progressWhenMoving else starProgress
        val fullStarCount = floor(currentProgress).toInt()
        for (i in fullStarCount until starCount) {
            val start = starStartAt(i, starWidth)
            canvas.withTranslation(x = start) {
                drawStar(backgroundDrawable, starWidth, canvas)
            }
        }
        repeat(fullStarCount) {
            val start = starStartAt(it, starWidth)
            canvas.withTranslation(x = start) {
                drawStar(starDrawable, starWidth, canvas)
            }
        }
        val restStarPercent = currentProgress - fullStarCount
        if (restStarPercent > 0) {
            val start = starStartAt(fullStarCount, starWidth)
            canvas.withTranslation(x = start) {
                clippedDrawable.let { clipDrawable ->
                    clipDrawable.level = (10000 * restStarPercent).toInt()
                    drawStar(clipDrawable, starWidth, canvas)
                }
            }
        }
    }

    private fun starStartAt(i: Int, starWidth: Int) =
        (if (starDirection == Direction.right) starCount - 1 - i else i) * (starWidth + starSpace)

    private fun drawStar(
        drawable: Drawable?,
        starWidth: Int,
        canvas: Canvas
    ) {
        drawable?.setBounds(0, 0, starWidth, starWidth)
        drawable?.draw(canvas)
    }

    private var positionWhenTouchDown = PointF(-1f, -1f)

    /**
     * 大于0 代表正在触摸滑动
     */
    private var progressWhenMoving: Float = -1f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || isIndicator) return super.onTouchEvent(event)
        val currentWidth = measuredWidth
        val validX = event.x.coerceIn(0f, currentWidth.toFloat())
        val x = if (starDirection == Direction.left) validX else currentWidth - validX
        val currentPoint = PointF(x, event.y)
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                progressWhenMoving = (x / currentWidth) * starCount
                invalidate()
            }

            MotionEvent.ACTION_DOWN -> {
                progressWhenMoving = -1f
                positionWhenTouchDown = currentPoint
            }

            MotionEvent.ACTION_UP -> {
                val currentMovingProgress = progressWhenMoving
                val starWidth = getStarWidth(currentWidth)
                val starWidthAndSpace = starWidth + starSpace
                val newStarProgress =
                    if (currentMovingProgress >= 0 && (positionWhenTouchDown.minus(currentPoint).sumOfSquares > 20)) {
                        //检查是否超过1半的星星
                        if (x % starWidthAndSpace < starWidth / 2)
                            floor(currentMovingProgress) + 0.5f
                        else
                            ceil(currentMovingProgress)
                    } else {
                        // 触摸位置在第几个星星
                        val position = ceil(x / starWidthAndSpace).toInt()
                        val oldPosition = ceil(starProgress).toInt()
                        val oldContainHalfStar = oldPosition - starProgress > 0
                        when {
                            position != oldPosition -> position.toFloat() - 0.5f
                            oldContainHalfStar -> {
                                position.toFloat()
                            }

                            else -> position - 0.5f
                        }
                    }
                progressWhenMoving = -1f

                val currentListener = listener
                if (currentListener == null) {
                    starProgress = newStarProgress
                } else if (currentListener.onChanged(newStarProgress, starCount, true)) {
                    starProgress = newStarProgress
                } else invalidate()
            }
        }
        return true
    }

    @FunctionalInterface
    fun interface RatingChangedListener {
        fun onChanged(progress: Float, max: Int, fromUser: Boolean): Boolean
    }
}

val PointF.sumOfSquares: Float
    get() {
        return x * x + y * y
    }