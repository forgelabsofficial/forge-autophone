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

    // ── Multi-touch gestures (NEW - Phase 2) ─────────────────────────────────

    /**
     * Pinch zoom in (two fingers moving apart).
     * @param centerX Center X coordinate of the pinch
     * @param centerY Center Y coordinate of the pinch
     * @param spreadStart Initial distance between fingers (pixels)
     * @param spreadEnd Final distance between fingers (pixels)
     * @param durationMs Duration of the gesture
     */
    fun performPinchZoomIn(
        centerX: Float,
        centerY: Float,
        spreadStart: Float = 50f,
        spreadEnd: Float = 200f,
        durationMs: Long = 300
    ) {
        val path1 = Path().apply {
            moveTo(centerX - spreadStart, centerY)
            lineTo(centerX - spreadEnd, centerY)
        }
        val path2 = Path().apply {
            moveTo(centerX + spreadStart, centerY)
            lineTo(centerX + spreadEnd, centerY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    /**
     * Pinch zoom out (two fingers moving together).
     */
    fun performPinchZoomOut(
        centerX: Float,
        centerY: Float,
        spreadStart: Float = 200f,
        spreadEnd: Float = 50f,
        durationMs: Long = 300
    ) {
        performPinchZoomIn(centerX, centerY, spreadStart, spreadEnd, durationMs)
    }

    /**
     * Two-finger rotation gesture.
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param radius Distance of fingers from center
     * @param degrees Rotation angle in degrees (positive = clockwise)
     * @param durationMs Duration of the gesture
     */
    fun performRotate(
        centerX: Float,
        centerY: Float,
        radius: Float = 100f,
        degrees: Float,
        durationMs: Long = 300
    ) {
        // Convert degrees to radians
        val radians = Math.toRadians(degrees.toDouble())
        
        // First finger path (starts at 0 degrees)
        val path1 = Path().apply {
            moveTo(centerX + radius, centerY)
            // Create arc
            val steps = 20
            for (i in 1..steps) {
                val angle = radians * i / steps
                val x = centerX + radius * Math.cos(angle).toFloat()
                val y = centerY + radius * Math.sin(angle).toFloat()
                lineTo(x, y)
            }
        }
        
        // Second finger path (starts at 180 degrees)
        val path2 = Path().apply {
            moveTo(centerX - radius, centerY)
            val steps = 20
            for (i in 1..steps) {
                val angle = Math.PI + radians * i / steps
                val x = centerX + radius * Math.cos(angle).toFloat()
                val y = centerY + radius * Math.sin(angle).toFloat()
                lineTo(x, y)
            }
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    /**
     * Two-finger tap (simultaneous tap at two points).
     * Useful for accessibility zoom or special gestures.
     */
    fun performTwoFingerTap(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Long = 100
    ) {
        val path1 = Path().apply { moveTo(x1, y1) }
        val path2 = Path().apply { moveTo(x2, y2) }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    /**
     * Three-finger swipe (useful for system gestures like app switching).
     */
    fun performThreeFingerSwipe(
        startY: Float,
        endY: Float,
        screenWidth: Float,
        durationMs: Long = 300
    ) {
        val spacing = screenWidth / 4
        val centerX = screenWidth / 2
        
        val path1 = Path().apply {
            moveTo(centerX - spacing, startY)
            lineTo(centerX - spacing, endY)
        }
        val path2 = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val path3 = Path().apply {
            moveTo(centerX + spacing, startY)
            lineTo(centerX + spacing, endY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path3, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    /**
     * Double tap (two quick taps at the same location).
     */
    fun performDoubleTap(x: Float, y: Float, delayBetweenTapsMs: Long = 100) {
        performClick(x, y)
        Thread.sleep(delayBetweenTapsMs)
        performClick(x, y)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun dispatch(path: Path, durationMs: Long) {
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
    }
}