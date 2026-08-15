package com.forge.autophone.extensions

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.forge.autophone.model.NodeSnapshot

/**
 * Kotlin extension functions for [AccessibilityNodeInfo].
 *
 * Makes working with the accessibility node tree idiomatic from Kotlin,
 * and avoids the boilerplate of bounds/bundle operations scattered across the codebase.
 */

/** Snapshot this node into a stable [NodeSnapshot] (safe to hold after recycling). */
fun AccessibilityNodeInfo.snapshot(depth: Int = 0): NodeSnapshot =
    NodeSnapshot.from(this, depth)

/** Returns the centre X coordinate of this node's screen bounds. */
val AccessibilityNodeInfo.centerX: Float
    get() {
        val r = Rect()
        getBoundsInScreen(r)
        return (r.left + r.right) / 2f
    }

/** Returns the centre Y coordinate of this node's screen bounds. */
val AccessibilityNodeInfo.centerY: Float
    get() {
        val r = Rect()
        getBoundsInScreen(r)
        return (r.top + r.bottom) / 2f
    }

/** Returns screen bounds as a [Rect]. */
val AccessibilityNodeInfo.screenBounds: Rect
    get() = Rect().also { getBoundsInScreen(it) }

/** Walk child nodes and collect all matching [predicate]. */
fun AccessibilityNodeInfo.findAll(predicate: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
    val result = mutableListOf<AccessibilityNodeInfo>()
    if (predicate(this)) result.add(this)
    for (i in 0 until childCount) {
        getChild(i)?.let { child ->
            result.addAll(child.findAll(predicate))
        }
    }
    return result
}

/** Find first child node (depth-first) matching [predicate]. */
fun AccessibilityNodeInfo.findFirst(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    if (predicate(this)) return this
    for (i in 0 until childCount) {
        getChild(i)?.findFirst(predicate)?.let { return it }
    }
    return null
}

/** Returns true if the node's text or contentDescription contains [query] (case-insensitive). */
fun AccessibilityNodeInfo.hasLabel(query: String): Boolean {
    val q = query.lowercase()
    return text?.toString()?.lowercase()?.contains(q) == true ||
           contentDescription?.toString()?.lowercase()?.contains(q) == true
}

/** Perform ACTION_CLICK on this node. Returns true if the action succeeded. */
fun AccessibilityNodeInfo.click(): Boolean =
    performAction(AccessibilityNodeInfo.ACTION_CLICK)

/** Perform ACTION_LONG_CLICK on this node. Returns true if the action succeeded. */
fun AccessibilityNodeInfo.longClick(): Boolean =
    performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)

/** Perform ACTION_SCROLL_FORWARD on this node. */
fun AccessibilityNodeInfo.scrollForward(): Boolean =
    performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

/** Perform ACTION_SCROLL_BACKWARD on this node. */
fun AccessibilityNodeInfo.scrollBackward(): Boolean =
    performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
