package com.ost.application.ui.core.util

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.activity.BackEventCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.animation.PathInterpolatorCompat
import com.google.android.material.R
import com.google.android.material.motion.MotionUtils
import com.tribalfs.stargazers.ui.core.util.setWindowTransparent
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.invokeOnBack
import androidx.appcompat.R as appcompatR

//TODO (move this to  design lib?)
@SuppressLint("RestrictedApi")
class ActivityBackAnimationDelegate private constructor() {
    private lateinit var activity: AppCompatActivity
    private lateinit var rootView: ViewGroup

    private lateinit var mOutlineProvider: ActivityRoundedCorner
    private var predictiveBackMargin : Int = 0
    private var initialTouchY = -1f

    private lateinit var gestureInterpolator: TimeInterpolator

    companion object{

        fun init(
            activity: AppCompatActivity,
            rootView: ViewGroup
        ): ActivityBackAnimationDelegate {
            return ActivityBackAnimationDelegate().apply {
                this.activity = activity
                this.rootView = rootView
                activity.setWindowTransparent(true)
                gestureInterpolator = MotionUtils.resolveThemeInterpolator(activity,
                    R.attr.motionEasingStandardDecelerateInterpolator,
                    PathInterpolatorCompat.create(0f, 0f, 0f, 1f))
                predictiveBackMargin = 8.dpToPx(activity.resources)

                setupPredictiveBackAnimation()
                animateCornerRadiusToZero()
            }
        }
    }

    var onBackInvoked: (() -> Unit)? = null
    var onBackProgressed: ((BackEventCompat) -> Unit)? = null
    var onBackStarted: ((BackEventCompat) -> Unit)? = null
    var onBackCancelled: (() -> Unit)? = null

    private fun setupPredictiveBackAnimation(){
        rootView.apply {
            outlineProvider = ActivityRoundedCorner().also { mOutlineProvider = it }
            clipToOutline = true
        }
        activity.invokeOnBack(
            onBackPressed = {
                onBackInvoked?.invoke()
                activity.finishAfterTransition()
            },
            onBackStarted = {
                onBackStarted?.invoke(it)
            },
            onBackCancelled = {
                initialTouchY = -1f
                rootView.apply {
                    translationX = 0f
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
                onBackCancelled?.invoke()
            },
            onBackProgressed = {backEvent ->
                if (initialTouchY < 0f) initialTouchY = backEvent.touchY

                val progress = gestureInterpolator.getInterpolation(backEvent.progress)
                val scale = 1f - (0.1f * progress)
                val maxTranslationX = ((rootView.width / 20) - predictiveBackMargin) *
                        (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1)

                val progressY = gestureInterpolator.getInterpolation(
                    (backEvent.touchY - initialTouchY) / rootView.height)
                val maxTranslationY = (rootView.height / 20) - predictiveBackMargin

                rootView.apply {
                    translationX = progress * maxTranslationX
                    translationY = progressY * maxTranslationY
                    scaleX = scale
                    scaleY = scale
                }
                mOutlineProvider.setProgress(progress)
                onBackProgressed?.invoke(backEvent)
            }
        )
    }

    private fun animateCornerRadiusToZero() {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 160
            startDelay = 160
            addUpdateListener { mOutlineProvider.setProgress(it.animatedValue as Float) }
            start()
        }
    }

    private inner class ActivityRoundedCorner : ViewOutlineProvider() {
        private val maxCornerRadius = activity.resources.getDimensionPixelSize(appcompatR.dimen.sesl_rounded_corner_radius)
        private var currentRadius = 0f

        fun setProgress(progress: Float) {
            this.currentRadius = maxCornerRadius * progress
            rootView.invalidateOutline()
        }

        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, currentRadius)
        }
    }

}