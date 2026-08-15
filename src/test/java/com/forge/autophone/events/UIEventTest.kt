package com.forge.autophone.events

import com.forge.autophone.model.NodeSnapshot
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for UIEvent hierarchy.
 */
class UIEventTest {

    @Test
    fun `WindowChanged event has correct properties`() {
        val event = UIEvent.WindowChanged(
            packageName = "com.example.app",
            className = "MainActivity"
        )

        assertEquals("com.example.app", event.packageName)
        assertEquals("MainActivity", event.className)
        assertTrue(event.timestamp > 0)
    }

    @Test
    fun `ToastShown event captures message`() {
        val event = UIEvent.ToastShown(
            message = "Settings saved"
        )

        assertEquals("Settings saved", event.message)
    }

    @Test
    fun `TextChanged event tracks old and new values`() {
        val event = UIEvent.TextChanged(
            viewId = "com.example:id/username",
            oldText = "john",
            newText = "john_doe"
        )

        assertEquals("com.example:id/username", event.viewId)
        assertEquals("john", event.oldText)
        assertEquals("john_doe", event.newText)
    }

    @Test
    fun `ViewClicked event has coordinates`() {
        val event = UIEvent.ViewClicked(
            viewId = "com.example:id/button",
            x = 540f,
            y = 1200f
        )

        assertEquals(540f, event.x, 0.1f)
        assertEquals(1200f, event.y, 0.1f)
    }

    @Test
    fun `all events have timestamps`() {
        val events = listOf(
            UIEvent.WindowChanged("pkg", "cls"),
            UIEvent.ToastShown("msg"),
            UIEvent.ViewClicked("id", 0f, 0f),
            UIEvent.ViewFocused("id"),
            UIEvent.ViewScrolled("id", 0, 0)
        )

        events.forEach { event ->
            assertTrue("Event ${event::class.simpleName} should have timestamp", 
                event.timestamp > 0)
        }
    }
}
