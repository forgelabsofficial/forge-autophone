package com.forge.autophone.extensions

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Extension functions for typing text via the Accessibility API.
 *
 * ACTION_SET_TEXT requires passing a Bundle — these extensions handle that
 * boilerplate so call sites are a single line.
 */

/**
 * Type [text] into this node using [AccessibilityNodeInfo.ACTION_SET_TEXT].
 * Focuses the node first. Returns true if both actions succeeded.
 */
fun AccessibilityNodeInfo.typeText(text: String): Boolean {
    performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    val args = Bundle().apply {
        putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
    }
    return performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
}

/** Clear the text in this node (sets empty string). */
fun AccessibilityNodeInfo.clearText(): Boolean = typeText("")

/**
 * Append [text] to the current text in this node.
 * Reads the existing text, appends, then sets the result.
 */
fun AccessibilityNodeInfo.appendText(text: String): Boolean {
    val current = this.text?.toString() ?: ""
    return typeText(current + text)
}
