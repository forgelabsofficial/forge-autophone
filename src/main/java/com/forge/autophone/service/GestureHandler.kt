package com.forge.autophone.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * GestureHandler — programmatic touch input for the Forge OS accessibility layer.
 *
 * All gestures are dispatched via [AccessibilityService.dispatchGesture], which
 * requires canPerformGestures=true in the accessibility config XML.
 */
class GestureHandler(private val service: AccessibilityService) {

    /** Single tap at absolute screen coordinates (x, y). */
    fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatch(path, durationMs = 1)
    }

    /** Long-press at absolute screen coordinates. */
    fun performLongClick(x: Float, y: Float, durationMs: Long = 600) {
        val path = Path().apply { moveTo(x, y) }
        dispatch(path, durationMs)
    }

    /** Swipe from (startX, startY) to (endX, endY) over [durationMs] ms. */
    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        dispatch(path, durationMs)
    }

    /**
     * Tap the centre of an [AccessibilityNodeInfo] bounding rect.
     * Prefer this over raw-coordinate clicks when you have the node reference.
     */
    fun performTap(node: AccessibilityNodeInfo) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        performClick(
            x = (bounds.left + bounds.right) / 2f,
            y = (bounds.top + bounds.bottom) / 2f
        )
    }

    /** Two-finger pinch zoom centred on (cx, cy). */
    fun performPinch(cx: Float, cy: Float, spread: Float, durationMs: Long = 300) {
        val path1 = Path().apply {
            moveTo(cx - spread, cy)
            lineTo(cx, cy)
        }
        val path2 = Path().apply {
            moveTo(cx + spread, cy)
            lineTo(cx, cy)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun dispatch(path: Path, durationMs: Long) {
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
    }
}