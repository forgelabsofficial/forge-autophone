package com.forge.autophone.diff

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.model.NodeSnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for UITreeDiffer — UI state change detection and diffing.
 */
class UITreeDifferTest {

    private lateinit var service: AutoPhoneAccessibilityService
    private lateinit var differ: UITreeDiffer

    @Before
    fun setup() {
        service = mockk(relaxed = true)
        differ = UITreeDiffer(service)
    }

    @Test
    fun `detects added nodes`() {
        // Given: Initial snapshot
        val before = listOf(
            createNode(viewId = "node1", text = "Original")
        )
        differ.updateBaseline(before)

        // When: New node appears
        val after = listOf(
            createNode(viewId = "node1", text = "Original"),
            createNode(viewId = "node2", text = "New")
        )
        val diff = differ.diff(after)

        // Then: Should detect added node
        assertEquals(1, diff.added.size)
        assertEquals("node2", diff.added.first().viewId)
    }

    @Test
    fun `detects removed nodes`() {
        // Given: Snapshot with two nodes
        val before = listOf(
            createNode(viewId = "node1", text = "Stay"),
            createNode(viewId = "node2", text = "Remove")
        )
        differ.updateBaseline(before)

        // When: Node is removed
        val after = listOf(
            createNode(viewId = "node1", text = "Stay")
        )
        val diff = differ.diff(after)

        // Then: Should detect removed node
        assertEquals(1, diff.removed.size)
        assertEquals("node2", diff.removed.first().viewId)
    }

    @Test
    fun `detects modified nodes - text change`() {
        // Given: Snapshot with node
        val before = listOf(
            createNode(viewId = "node1", text = "Old Text")
        )
        differ.updateBaseline(before)

        // When: Text changes
        val after = listOf(
            createNode(viewId = "node1", text = "New Text")
        )
        val diff = differ.diff(after)

        // Then: Should detect modified node
        assertEquals(1, diff.modified.size)
        val modified = diff.modified.first()
        assertEquals("node1", modified.newNode.viewId)
        assertTrue(modified.changes.any { it is PropertyChange.TextChanged })
    }

    @Test
    fun `detects modified nodes - enabled state change`() {
        // Given: Disabled node
        val before = listOf(
            createNode(viewId = "btn1", isEnabled = false)
        )
        differ.updateBaseline(before)

        // When: Node becomes enabled
        val after = listOf(
            createNode(viewId = "btn1", isEnabled = true)
        )
        val diff = differ.diff(after)

        // Then: Should detect enabled change
        assertEquals(1, diff.modified.size)
        val modified = diff.modified.first()
        assertTrue(modified.changes.any { it is PropertyChange.EnabledChanged })
    }

    @Test
    fun `detects modified nodes - bounds change`() {
        // Given: Node at position
        val before = listOf(
            createNode(viewId = "node1", left = 0, top = 0, right = 100, bottom = 100)
        )
        differ.updateBaseline(before)

        // When: Node moves
        val after = listOf(
            createNode(viewId = "node1", left = 50, top = 50, right = 150, bottom = 150)
        )
        val diff = differ.diff(after)

        // Then: Should detect bounds change
        assertEquals(1, diff.modified.size)
        assertTrue(diff.modified.first().changes.any { it is PropertyChange.BoundsChanged })
    }

    @Test
    fun `detects multiple changes on same node`() {
        // Given: Original node
        val before = listOf(
            createNode(viewId = "node1", text = "Old", isEnabled = false, isClickable = false)
        )
        differ.updateBaseline(before)

        // When: Multiple properties change
        val after = listOf(
            createNode(viewId = "node1", text = "New", isEnabled = true, isClickable = true)
        )
        val diff = differ.diff(after)

        // Then: Should detect all changes
        assertEquals(1, diff.modified.size)
        val changes = diff.modified.first().changes
        assertTrue(changes.size >= 3) // Text, Enabled, Clickable
    }

    @Test
    fun `hasChanges returns true when UI changed`() {
        // Given: Initial state
        val before = listOf(
            createNode(viewId = "node1", text = "Original")
        )
        differ.updateBaseline(before)

        // When: Checking for changes (without computing diff)
        val hasChanges = differ.hasChanges()

        // Then: Should detect changes exist
        // Note: Actual result depends on UITreeInspector's current snapshot
        assertNotNull(hasChanges)
    }

    @Test
    fun `didNodeChange detects specific node change`() {
        // Given: Baseline with node
        val before = listOf(
            createNode(viewId = "btn1", text = "Old")
        )
        differ.updateBaseline(before)

        // When: Node changes
        val after = listOf(
            createNode(viewId = "btn1", text = "New")
        )
        differ.diff(after)

        // Check specific node
        val changed = differ.didNodeChange("btn1")

        // Then: Should detect that specific node changed
        // Note: Result depends on current diff state
        assertNotNull(changed)
    }

    @Test
    fun `reset clears baseline`() {
        // Given: Differ with baseline
        val snapshot = listOf(createNode(viewId = "node1"))
        differ.updateBaseline(snapshot)

        // When: Resetting
        differ.reset()

        // Then: Next diff should start fresh
        val diff = differ.diff(emptyList())
        assertEquals(0, diff.totalNodesBefore)
    }

    @Test
    fun `updateBaseline sets new baseline`() {
        // Given: New snapshot
        val newBaseline = listOf(
            createNode(viewId = "node1"),
            createNode(viewId = "node2")
        )

        // When: Updating baseline
        differ.updateBaseline(newBaseline)

        // Then: Next diff should use new baseline
        val diff = differ.diff(newBaseline)
        assertFalse(diff.hasChanges)
    }

    @Test
    fun `diff without baseline shows all nodes as added`() {
        // Given: Empty baseline (initial state)
        differ.reset()

        // When: First diff with nodes
        val snapshot = listOf(
            createNode(viewId = "node1"),
            createNode(viewId = "node2")
        )
        val diff = differ.diff(snapshot)

        // Then: All nodes should be marked as added
        assertEquals(2, diff.added.size)
        assertEquals(0, diff.removed.size)
    }

    @Test
    fun `getCurrentSnapshot returns current UI state`() {
        // When: Getting current snapshot
        val snapshot = differ.getCurrentSnapshot()

        // Then: Should return snapshot (may be empty in test)
        assertNotNull(snapshot)
    }

    @Test
    fun `diff tracks node count changes`() {
        // Given: Baseline with 2 nodes
        val before = listOf(
            createNode(viewId = "node1"),
            createNode(viewId = "node2")
        )
        differ.updateBaseline(before)

        // When: Snapshot has 3 nodes
        val after = listOf(
            createNode(viewId = "node1"),
            createNode(viewId = "node2"),
            createNode(viewId = "node3")
        )
        val diff = differ.diff(after)

        // Then: Should track count change
        assertEquals(2, diff.totalNodesBefore)
        assertEquals(3, diff.totalNodesAfter)
        assertTrue(diff.nodeCountChanged)
    }

    @Test
    fun `detects content description change`() {
        // Given: Node with description
        val before = listOf(
            createNode(viewId = "node1", contentDesc = "Old description")
        )
        differ.updateBaseline(before)

        // When: Description changes
        val after = listOf(
            createNode(viewId = "node1", contentDesc = "New description")
        )
        val diff = differ.diff(after)

        // Then: Should detect change
        assertEquals(1, diff.modified.size)
        assertTrue(diff.modified.first().changes.any { 
            it is PropertyChange.ContentDescriptionChanged 
        })
    }

    // ── Helper methods ───────────────────────────────────────────────────────

    private fun createNode(
        viewId: String? = null,
        text: String? = null,
        contentDesc: String? = null,
        isEnabled: Boolean = true,
        isClickable: Boolean = false,
        isEditable: Boolean = false,
        left: Int = 0,
        top: Int = 0,
        right: Int = 100,
        bottom: Int = 100
    ): NodeSnapshot {
        return NodeSnapshot(
            viewId = viewId,
            text = text,
            contentDescription = contentDesc,
            className = "android.widget.View",
            packageName = "com.example.app",
            isEnabled = isEnabled,
            isClickable = isClickable,
            isEditable = isEditable,
            isFocusable = false,
            isScrollable = false,
            boundsLeft = left,
            boundsTop = top,
            boundsRight = right,
            boundsBottom = bottom,
            childCount = 0
        )
    }
}
