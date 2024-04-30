package com.assoft.peekster.util

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout
import com.assoft.peekster.R

/**
 * Frame layout that has rounded corners (it clips content too).
 */
class RoundCornerFrameLayout(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {

    private val stencilPath: Path = Path()
    private var cornerRadius = 0f

    constructor(context: Context) : this(context, null) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // compute the path
        stencilPath.reset()
        stencilPath.addRoundRect(0F, 0F, w.toFloat(), h.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
        stencilPath.close()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save: Int = canvas.save()
        canvas.clipPath(stencilPath)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }

    init {
        val attrArray: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.RoundCornerFrameLayout, 0, 0)
        cornerRadius = try {
            attrArray.getDimension(R.styleable.RoundCornerFrameLayout_corner_radius, 0f)
        } finally {
            attrArray.recycle()
        }
    }
}