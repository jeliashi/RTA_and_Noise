package com.jeliav.android.rtaandnoise.view

import android.animation.Animator
import android.app.ActionBar
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.Animation

/**
 * Created by jeliashiv on 3/9/18.
 */
val Float.dp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)
val Float.px : Float
    get() = (this /Resources.getSystem().displayMetrics.density)

val textPaint: () -> Paint = {
    Paint().apply {
        color = Color.parseColor("#AAFFFFFF")
        style = Paint.Style.FILL
        textSize = 12f.px
        typeface = Typeface.MONOSPACE
    }
}

val errTextPaint: () -> Paint = {
    Paint().apply {
        color = Color.parseColor("#BBFF0000")
        style = Paint.Style.FILL
        textSize = 12f.px
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
}

fun ViewPropertyAnimator.onEnd(then : () -> Unit):ViewPropertyAnimator {
    this.setListener(object : Animator.AnimatorListener{
        override fun onAnimationRepeat(p0: Animator?) {
        }

        override fun onAnimationEnd(p0: Animator?) {
            then()
        }

        override fun onAnimationCancel(p0: Animator?) {
        }

        override fun onAnimationStart(p0: Animator?) {
        }

    })
    return this
}

fun ViewPropertyAnimator.onTerminate(then: () -> Unit): ViewPropertyAnimator {
    this.setListener(object : Animator.AnimatorListener{
        override fun onAnimationCancel(p0: Animator?) {
            then()
        }

        override fun onAnimationEnd(p0: Animator?) {
            then()
        }

        override fun onAnimationRepeat(p0: Animator?) {
        }

        override fun onAnimationStart(p0: Animator?) {
        }
    })
    return this
}

fun Animation.onTerminate(then: () -> Unit): Animation {
    this.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationEnd(p0: Animation?) {
            then()
        }

        override fun onAnimationRepeat(p0: Animation?) {
        }

        override fun onAnimationStart(p0: Animation?) {
        }

    })
    return this
}

fun View.Padding(i: Int){
    setPadding(i,i,i,i)
}

fun ActionBar.view(): ViewGroup = customView.parent.parent as ViewGroup
