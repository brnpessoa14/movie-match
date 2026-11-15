package br.com.cinematch.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import br.com.cinematch.R

/** Evita esticar formulários e cartazes em telas maiores. */
class MaxWidthLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maximum = resources.getDimensionPixelSize(if (tag == "wide") R.dimen.list_max_width else R.dimen.content_max_width)
        val constrained = MeasureSpec.makeMeasureSpec(minOf(MeasureSpec.getSize(widthMeasureSpec), maximum), MeasureSpec.getMode(widthMeasureSpec))
        super.onMeasure(constrained, heightMeasureSpec)
    }
}
