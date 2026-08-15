package com.forge.autophone.diff

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.inspector.UITreeInspector
import com.forge.autophone.model.NodeSnapshot

/**
 * UITreeDiffer — tracks UI state changes between snapshots.
 *
 * Enables agents to understand "what changed":
 * - Which nodes appeared
 * - Which nodes disappeared
 * - Which nodes were modified (text, enabled state, etc.)
 *
 * Powers event-driven automation: "When the submit button becomes enabled,
 * tap it automatically."
 */
class UITreeDiffer(private val service: AutoPhoneAccessibilityService) {

    private var lastSnapshot: List<NodeSnapshot> = emptyList()
    private var lastSnapshotTimestamp: Long = 0

    /**
     * Compute diff between current UI and last snapshot.
     * Automatically captures current snapshot.
     */
    fun diff(): UIDiff {
        val current = UITreeInspector(service).snapshot()
        return diff(current)
    }

    /**
     * Compute diff between provided snapshot and last snapshot.
     */
    fun diff(current: List<NodeSnapshot>): UIDiff {
        val added = findAddedNodes(lastSnapshot, current)
        val removed = findRemovedNodes(lastSnapshot, current)
        val modified = findModifiedNodes(lastSnapshot, current)

        val result = UIDiff(
            added = added,
            removed = removed,
            modified = modified,
            timestamp = System.currentTimeMillis(),
            totalNodesBefore = lastSnapshot.size,
            totalNodesAfter = current.size
        )

        // Update state
        lastSnapshot = current
        lastSnapshotTimestamp = result.timestamp

        return result
    }

    /**
     * Check if any changes occurred since last snapshot.
     */
    fun hasChanges(): Boolean {
        val current = UITreeInspector(service).snapshot()
        return current != lastSnapshot
    }

    /**
     * Get the current snapshot without computing a diff.
     */
    fun getCurrentSnapshot(): List<NodeSnapshot> {
        return UITreeInspector(service).snapshot()
    }

    /**
     * Manually update the baseline snapshot.
     */
    fun updateBaseline(snapshot: List<NodeSnapshot>) {
        lastSnapshot = snapshot
        lastSnapshotTimestamp = System.currentTimeMillis()
    }

    /**
     * Reset the differ (clear baseline).
     */
    fun reset() {
        lastSnapshot = emptyList()
        lastSnapshotTimestamp = 0
    }

    /**
     * Find nodes that appeared in the new snapshot.
     */
    private fun findAddedNodes(
        before: List<NodeSnapshot>,
        after: List<NodeSnapshot>
    ): List<NodeSnapshot> {
        return after.filter { newNode ->
            // Consider a node "added" if its viewId didn't exist before
            // OR if viewId is null but the text/position combination is new
            newNode.viewId?.let { id ->
                before.none { it.viewId == id }
            } ?: run {
                // For nodes without IDs, use text + position heuristic
                before.none {
                    it.text == newNode.text &&
                    it.boundsLeft == newNode.boundsLeft &&
                    it.boundsTop == newNode.boundsTop
                }
            }
        }
    }

    /**
     * Find nodes that disappeared from the old snapshot.
     */
    private fun findRemovedNodes(
        before: List<NodeSnapshot>,
        after: List<NodeSnapshot>
    ): List<NodeSnapshot> {
        return before.filter { oldNode ->
            oldNode.viewId?.let { id ->
                after.none { it.viewId == id }
            } ?: run {
                after.none {
                    it.text == oldNode.text &&
                    it.boundsLeft == oldNode.boundsLeft &&
                    it.boundsTop == oldNode.boundsTop
                }
            }
        }
    }

    /**
     * Find nodes that were modified (same ID, different properties).
     */
    private fun findModifiedNodes(
        before: List<NodeSnapshot>,
        after: List<NodeSnapshot>
    ): List<ModifiedNode> {
        val modified = mutableListOf<ModifiedNode>()

        before.forEach { oldNode ->
            oldNode.viewId?.let { id ->
                val newNode = after.find { it.viewId == id }
                if (newNode != null && newNode != oldNode) {
                    // Detect what changed
                    val changes = detectChanges(oldNode, newNode)
                    if (changes.isNotEmpty()) {
                        modified.add(ModifiedNode(
                            oldNode = oldNode,
                            newNode = newNode,
                            changes = changes
                        ))
                    }
                }
            }
        }

        return modified
    }

    /**
     * Detect specific property changes between two nodes.
     */
    private fun detectChanges(old: NodeSnapshot, new: NodeSnapshot): List<PropertyChange> {
        val changes = mutableListOf<PropertyChange>()

        if (old.text != new.text) {
            changes.add(PropertyChange.TextChanged(old.text, new.text))
        }

        if (old.contentDescription != new.contentDescription) {
            changes.add(PropertyChange.ContentDescriptionChanged(
                old.contentDescription,
                new.contentDescription
            ))
        }

        if (old.isEnabled != new.isEnabled) {
            changes.add(PropertyChange.EnabledChanged(old.isEnabled, new.isEnabled))
        }

        if (old.isClickable != new.isClickable) {
            changes.add(PropertyChange.ClickableChanged(old.isClickable, new.isClickable))
        }

        if (old.isEditable != new.isEditable) {
            changes.add(PropertyChange.EditableChanged(old.isEditable, new.isEditable))
        }

        if (old.boundsLeft != new.boundsLeft || old.boundsTop != new.boundsTop ||
            old.boundsRight != new.boundsRight || old.boundsBottom != new.boundsBottom) {
            changes.add(PropertyChange.BoundsChanged(
                oldBounds = android.graphics.Rect(
                    old.boundsLeft, old.boundsTop, old.boundsRight, old.boundsBottom
                ),
                newBounds = android.graphics.Rect(
                    new.boundsLeft, new.boundsTop, new.boundsRight, new.boundsBottom
                )
            ))
        }

        return changes
    }

    /**
     * Find specific changes matching a predicate.
     */
    fun findChanges(predicate: (ModifiedNode) -> Boolean): List<ModifiedNode> {
        val current = UITreeInspector(service).snapshot()
        val diff = diff(current)
        return diff.modified.filter(predicate)
    }

    /**
     * Check if a specific node changed.
     */
    fun didNodeChange(viewId: String): Boolean {
        val current = UITreeInspector(service).snapshot()
        val diff = diff(current)
        return diff.modified.any { it.newNode.viewId == viewId }
    }
}

/**
 * UI diff result showing what changed.
 */
data class UIDiff(
    val added: List<NodeSnapshot>,
    val removed: List<NodeSnapshot>,
    val modified: List<ModifiedNode>,
    val timestamp: Long,
    val totalNodesBefore: Int,
    val totalNodesAfter: Int
) {
    val hasChanges: Boolean get() = added.isNotEmpty() || removed.isNotEmpty() || modified.isNotEmpty()
    val nodeCountChanged: Boolean get() = totalNodesBefore != totalNodesAfter
}

/**
 * A node that was modified between snapshots.
 */
data class ModifiedNode(
    val oldNode: NodeSnapshot,
    val newNode: NodeSnapshot,
    val changes: List<PropertyChange>
)

/**
 * Specific property changes detected.
 */
sealed class PropertyChange {
    data class TextChanged(val oldText: String?, val newText: String?) : PropertyChange()
    data class ContentDescriptionChanged(val oldDesc: String?, val newDesc: String?) : PropertyChange()
    data class EnabledChanged(val wasEnabled: Boolean, val isEnabled: Boolean) : PropertyChange()
    data class ClickableChanged(val wasClickable: Boolean, val isClickable: Boolean) : PropertyChange()
    data class EditableChanged(val wasEditable: Boolean, val isEditable: Boolean) : PropertyChange()
    data class BoundsChanged(val oldBounds: android.graphics.Rect, val newBounds: android.graphics.Rect) : PropertyChange()
}
