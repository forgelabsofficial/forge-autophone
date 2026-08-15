package com.forge.autophone.inspector

import android.view.accessibility.AccessibilityNodeInfo
import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.model.NodeSnapshot

/**
 * UITreeInspector — walks the live accessibility node tree and produces a
 * structured snapshot the Forge OS agent can reason about.
 *
 * This is the "eyes" of AutoPhone: it converts Android's live, recycled
 * [AccessibilityNodeInfo] tree into stable [NodeSnapshot] objects.
 */
class UITreeInspector(private val service: AutoPhoneAccessibilityService) {

    /**
     * Returns a flat list of all [NodeSnapshot]s in the active window,
     * depth-first. Filters out invisible or empty nodes by default.
     */
    fun snapshot(onlyClickable: Boolean = false): List<NodeSnapshot> {
        val root = service.getActiveWindowRoot() ?: return emptyList()
        return walk(root, depth = 0, onlyClickable = onlyClickable)
    }

    /**
     * Returns only nodes that are clickable — useful for giving the agent
     * a minimal action space.
     */
    fun clickableNodes(): List<NodeSnapshot> = snapshot(onlyClickable = true)

    /**
     * Find the node whose text or content description matches [query] (case-insensitive).
     */
    fun findByLabel(query: String): NodeSnapshot? {
        val q = query.lowercase()
        return snapshot().firstOrNull { node ->
            node.text?.lowercase()?.contains(q) == true ||
            node.contentDescription?.lowercase()?.contains(q) == true
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun walk(
        node: AccessibilityNodeInfo,
        depth: Int,
        onlyClickable: Boolean
    ): List<NodeSnapshot> {
        val results = mutableListOf<NodeSnapshot>()
        val snap = NodeSnapshot.from(node, depth)

        if (!onlyClickable || snap.isClickable) {
            results.add(snap)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                results.addAll(walk(child, depth + 1, onlyClickable))
                child.recycle()
            }
        }
        return results
    }
}
