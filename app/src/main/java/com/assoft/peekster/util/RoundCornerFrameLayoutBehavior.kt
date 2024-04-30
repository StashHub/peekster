package com.assoft.peekster.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView

/**
 * Layout Behavior to show/hide a RoundCornerFrameLayoutBehavior anchored to a AppBarLayout
 */
class RoundCornerFrameLayoutBehavior @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CoordinatorLayout.Behavior<RoundCornerFrameLayout>(context, attrs) {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: RoundCornerFrameLayout,
        dependency: View
    ): Boolean {
        // check that our dependency is the AppBarLayout
        return dependency is NestedScrollView
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout, child: RoundCornerFrameLayout,
        dependency: View
    ): Boolean {
        updateImageViewVisibility(dependency, child)
        return true
    }

    private fun updateImageViewVisibility(
        dependency: View, child: RoundCornerFrameLayout
    ): Boolean {
        if (dependency.y < -670) {
            child.visibility = View.GONE
        } else {
            child.visibility = View.VISIBLE
        }
        return true
    }
}