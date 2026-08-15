package com.forge.autophone.extensions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path

/**
 * Extension functions for cleaner [GestureDescription] construction
 * and [AccessibilityService.dispatchGesture] calls.
 */

/**
 * Build a simple single-stroke [GestureDescription] from a [Path].
 *
 * @param startMs  delay before the stroke starts (default 0 ms)
 * @param durationMs  duration of the stroke in milliseconds
 */
fun Path.toGesture(startMs: Long = 0, durationMs: Long = 100): GestureDescription =
    GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(this, startMs, durationMs))
        .build()

/**
 * Dispatch a tap gesture at the given (x, y) screen coordinates.
 * Convenience wrapper around [AccessibilityService.dispatchGesture].
 */
fun AccessibilityService.dispatchTap(
    x: Float,
    y: Float,
    callback: AccessibilityService.GestureResultCallback? = null
) {
    val path = Path().apply { moveTo(x, y) }
    dispatchGesture(path.toGesture(durationMs = 1), callback, null)
}

/**
 * Dispatch a swipe gesture from (x1, y1) to (x2, y2).
 */
fun AccessibilityService.dispatchSwipe(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    durationMs: Long = 300,
    callback: AccessibilityService.GestureResultCallback? = null
) {
    val path = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }
    dispatchGesture(path.toGesture(durationMs = durationMs), callback, null)
}

/**
 * Dispatch a long-press gesture at (x, y).
 */
fun AccessibilityService.dispatchLongPress(
    x: Float,
    y: Float,
    durationMs: Long = 600,
    callback: AccessibilityService.GestureResultCallback? = null
) {
    val path = Path().apply { moveTo(x, y) }
    dispatchGesture(path.toGesture(durationMs = durationMs), callback, null)
}
