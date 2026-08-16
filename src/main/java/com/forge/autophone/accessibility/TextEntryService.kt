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
        val root = service.rootInActiveWindow ?: return
        // Find EditText nodes by traversing the tree
        val editTextNodes = findNodesByClassName(root, "android.widget.EditText")
        editTextNodes.firstOrNull()?.let { 
            setTextOnNode(it, text)
            it.recycle()
        }
        editTextNodes.forEach { if (it != editTextNodes.firstOrNull()) it.recycle() }
    }

    /**
     * Type [text] into a node found by its visible text label.
     * Handy for hint-text matching (e.g. "Search…").
     */
    fun typeIntoFieldWithHint(text: String, hint: String) {
        val nodes = service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText(hint)
            ?: return
        nodes.firstOrNull()?.let { 
            setTextOnNode(it, text)
        }
        nodes.forEach { it.recycle() }
    }
    
    /**
     * Helper to find nodes by class name (replacement for deprecated method)
     */
    private fun findNodesByClassName(root: android.view.accessibility.AccessibilityNodeInfo, className: String): List<android.view.accessibility.AccessibilityNodeInfo> {
        val result = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
        
        fun traverse(node: android.view.accessibility.AccessibilityNodeInfo) {
            if (node.className?.toString() == className) {
                result.add(node)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverse(it) }
            }
        }
        
        traverse(root)
        return result
    }

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