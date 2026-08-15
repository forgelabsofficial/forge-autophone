package com.forge.autophone.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * TextEntryService — types text into UI fields via the Accessibility API.
 *
 * Uses [AccessibilityNodeInfo.ACTION_SET_TEXT] (API 21+) with a [Bundle] arg,
 * which is the correct way — direct assignment to `node.text` is read-only.
 */
class TextEntryService(private val service: AccessibilityService) {

    /**
     * Type [text] into a node identified by its view resource ID.
     * e.g. viewId = "com.example.app:id/username_field"
     */
    fun typeIntoViewId(text: String, viewId: String) {
        val nodes = service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId(viewId)
            ?: return
        nodes.firstOrNull()?.let { setTextOnNode(it, text) }
    }

    /**
     * Type [text] into the first EditText visible in the active window.
     * Useful when the view ID is unknown.
     */
    fun typeIntoFocusedField(text: String) {
        val nodes = service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByClassName("android.widget.EditText")
            ?: return
        nodes.firstOrNull()?.let { setTextOnNode(it, text) }
    }

    /**
     * Type [text] into a node found by its visible text label.
     * Handy for hint-text matching (e.g. "Search…").
     */
    fun typeIntoFieldWithHint(text: String, hint: String) {
        val nodes = service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText(hint)
            ?: return
        nodes.firstOrNull()?.let { setTextOnNode(it, text) }
    }

    /** Clear text in a node identified by view resource ID. */
    fun clearField(viewId: String) {
        typeIntoViewId("", viewId)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun setTextOnNode(node: AccessibilityNodeInfo, text: String) {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}