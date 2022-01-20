//https://stackoverflow.com/a/60961447
package com.protectednet.utilizr.ViewExtenions

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class KotlinConstraintSet : ConstraintSet() {

    companion object {
        inline fun buildConstraintSet(block:KotlinConstraintSet.()->Unit) =
            KotlinConstraintSet().apply(block)
    }
    //add this if you plan on using the margin param in ConstraintSet.connect
    var margin: Int? = null
        get() {
            val result = field
            margin = null //reset it to work with other constraints
            return result
        }

    inline infix fun Unit.and(other: Int) = other // just to join two functions

    inline infix fun Int.topToBottomOf(bottom: Int) =
        margin?.let {
            connect(this, TOP, bottom, BOTTOM, it)
        } ?: connect(this, TOP, bottom, BOTTOM)

    inline fun margin(margin: Int) {
        this.margin = margin
    }

    inline infix fun Int.bottomToBottomOf(bottom: Int) =
        margin?.let {
            connect(this, BOTTOM, bottom, BOTTOM, it)
        } ?: connect(this, BOTTOM, bottom, BOTTOM)

    inline infix fun Int.topToTopOf(top: Int) =
        margin?.let {
            connect(this, TOP, top, TOP, it)
        } ?: connect(this, TOP, top, TOP)

    inline infix fun Int.startToEndOf(end: Int) =
        margin?.let {
            connect(this, START, end, END, it)
        } ?: connect(this, START, end, END)


    //TODO generate other functions depending on your needs

    infix fun Int.clear(constraint: Constraints) =
        when (constraint) {
            Constraints.TOP -> clear(this, TOP)
            Constraints.BOTTOM -> clear(this, BOTTOM)
            Constraints.END -> clear(this, END)
            Constraints.START -> clear(this, START)
        }

    //inline infix fun clearTopCon
    inline infix fun appliesTo(constraintLayout: ConstraintLayout) =
        applyTo(constraintLayout)

    inline infix fun clones(constraintLayout: ConstraintLayout) =
        clone(constraintLayout)

    inline fun constraint(view: Int, block: Int.() -> Unit) =
        view.apply(block)
}

enum class Constraints {
    TOP, BOTTOM, START, END //you could add other values to use with the clear fun like LEFT
}