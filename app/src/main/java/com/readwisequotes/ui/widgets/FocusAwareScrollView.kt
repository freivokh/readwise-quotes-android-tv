package com.readwisequotes.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ScrollView

/**
 * Custom ScrollView that properly handles D-pad focus navigation when some
 * child views are hidden (GONE/INVISIBLE).
 *
 * Inspired by SmartTube's approach: instead of dynamically updating focus chains
 * when visibility changes, we override focusSearch() to skip hidden views and
 * find the next visible focusable view in the chain.
 */
class FocusAwareScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FocusAwareScrollView"
        private const val MAX_RECURSION_DEPTH = 10
    }

    override fun focusSearch(focused: View?, direction: Int): View? {
        if (focused == null) {
            return super.focusSearch(focused, direction)
        }

        // Try to find next visible view following the focus chain
        val result = findNextVisibleFocusable(focused, direction, 0)
        if (result != null) {
            Log.d(TAG, "focusSearch: found ${result.javaClass.simpleName} id=${result.id}")
            return result
        }

        // Fall back to default focus search
        Log.d(TAG, "focusSearch: falling back to super")
        return super.focusSearch(focused, direction)
    }

    /**
     * Recursively find the next visible and focusable view in the given direction.
     */
    private fun findNextVisibleFocusable(fromView: View, direction: Int, depth: Int): View? {
        // Prevent infinite recursion
        if (depth > MAX_RECURSION_DEPTH) {
            Log.w(TAG, "Max recursion depth reached")
            return null
        }

        // Get the next focus ID based on direction
        val nextFocusId = when (direction) {
            View.FOCUS_UP -> fromView.nextFocusUpId
            View.FOCUS_DOWN -> fromView.nextFocusDownId
            View.FOCUS_LEFT -> fromView.nextFocusLeftId
            View.FOCUS_RIGHT -> fromView.nextFocusRightId
            else -> View.NO_ID
        }

        if (nextFocusId == View.NO_ID) {
            return null
        }

        val nextView = findViewById<View>(nextFocusId) ?: return null

        // Use isShown() which checks entire view hierarchy visibility
        if (nextView.isShown && nextView.isFocusable) {
            return nextView
        }

        // View is hidden or not focusable, continue searching from it
        Log.d(TAG, "Skipping hidden/unfocusable view, continuing search from depth $depth")
        return findNextVisibleFocusable(nextView, direction, depth + 1)
    }
}
