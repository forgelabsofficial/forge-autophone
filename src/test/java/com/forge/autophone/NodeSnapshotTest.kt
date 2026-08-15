package com.forge.autophone

import com.forge.autophone.model.NodeSnapshot
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [NodeSnapshot] — runnable on the JVM without an Android device.
 *
 * Accessibility integration tests (requiring a real service connection) live in
 * the androidTest source set.
 */
class NodeSnapshotTest {

    @Test
    fun `centerX is midpoint of left and right bounds`() {
        val snap = NodeSnapshot(
            viewId = "com.forge.test:id/btn",
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = null,
            isClickable = true,
            isEditable = false,
            isEnabled = true,
            boundsLeft = 100,
            boundsTop = 200,
            boundsRight = 300,
            boundsBottom = 250
        )
        assertEquals(200f, snap.centerX)
    }

    @Test
    fun `centerY is midpoint of top and bottom bounds`() {
        val snap = NodeSnapshot(
            viewId = null,
            className = "android.widget.TextView",
            text = "Hello",
            contentDescription = null,
            isClickable = false,
            isEditable = false,
            isEnabled = true,
            boundsLeft = 0,
            boundsTop = 400,
            boundsRight = 100,
            boundsBottom = 450
        )
        assertEquals(425f, snap.centerY)
    }

    @Test
    fun `NodeSnapshot from zero bounds returns zero center`() {
        val snap = NodeSnapshot(
            viewId = null,
            className = null,
            text = null,
            contentDescription = null,
            isClickable = false,
            isEditable = false,
            isEnabled = false,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 0,
            boundsBottom = 0
        )
        assertEquals(0f, snap.centerX)
        assertEquals(0f, snap.centerY)
    }
}
