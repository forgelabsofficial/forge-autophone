package com.forge.autophone.model

import android.view.accessibility.AccessibilityNodeInfo

/**
 * NodeSnapshot — a serialisable snapshot of an [AccessibilityNodeInfo].
 *
 * AccessibilityNodeInfo objects are live references that are recycled by the
 * system. This data class captures the fields the Forge OS agent needs for
 * reasoning and tool dispatch without holding a live reference.
 */
data class NodeSnapshot(
    val viewId: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isEnabled: Boolean,
    val boundsLeft: Int,
    val boundsTop: Int,
    val boundsRight: Int,
    val boundsBottom: Int,
    val childCount: Int,
    val depth: Int = 0
) {
    val centerX: Float get() = (boundsLeft + boundsRight) / 2f
    val centerY: Float get() = (boundsTop + boundsBottom) / 2f

    companion object {
        fun from(node: AccessibilityNodeInfo, depth: Int = 0): NodeSnapshot {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            return NodeSnapshot(
                viewId = node.viewIdResourceName,
                className = node.className?.toString(),
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                isClickable = node.isClickable,
                isEditable = node.isEditable,
                isEnabled = node.isEnabled,
                boundsLeft = bounds.left,
                boundsTop = bounds.top,
                boundsRight = bounds.right,
                boundsBottom = bounds.bottom,
                childCount = node.childCount,
                depth = depth
            )
        }
    }
}
