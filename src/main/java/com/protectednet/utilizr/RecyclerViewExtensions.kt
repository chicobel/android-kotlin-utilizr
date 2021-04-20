package com.protectednet.utilizr

import android.graphics.*
import android.graphics.drawable.InsetDrawable
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.addItemSpacing(sides:Double=0.0, vertical:Double=10.0){
    val ATTRS = intArrayOf(android.R.attr.listDivider)
    val a = context!!.obtainStyledAttributes(ATTRS)
    val divider = a.getDrawable(0)
    val sideInset = (resources.displayMetrics.density * sides).toInt()
    val verticalInset = (resources.displayMetrics.density * vertical).toInt()
    val insetDivider = InsetDrawable(divider, sideInset, verticalInset, sideInset, verticalInset)
    a.recycle()
    insetDivider.alpha=0
    val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    itemDecoration.setDrawable(insetDivider)
    this.addItemDecoration(itemDecoration)
}

fun RecyclerView.addSeparatorLines(color:Int, sides:Double=0.0, vertical:Double=0.0){
    val ATTRS = intArrayOf(android.R.attr.listDivider)
    val a = context!!.obtainStyledAttributes(ATTRS)
    val divider = a.getDrawable(0)
    val p = PorterDuffColorFilter(color,PorterDuff.Mode.MULTIPLY)
    if(divider != null) {
        divider.colorFilter = p
    }
    val sideInset = (resources.displayMetrics.density * sides).toInt()
    val verticalInset = (resources.displayMetrics.density * vertical).toInt()
    val insetDivider = InsetDrawable(divider, sideInset, verticalInset, sideInset, verticalInset)
    a.recycle()
    val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    itemDecoration.setDrawable(insetDivider)
    this.addItemDecoration(itemDecoration)
}