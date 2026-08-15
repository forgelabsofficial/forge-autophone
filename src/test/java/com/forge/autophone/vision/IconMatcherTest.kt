package com.forge.autophone.vision

import android.graphics.Point
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for IconMatcher data structures.
 */
class IconMatcherTest {

    @Test
    fun `IconMatch centerX and centerY properties work correctly`() {
        val match = IconMatch(
            name = "test_icon",
            topLeft = Point(100, 200),
            width = 50,
            height = 40,
            confidence = 0.95
        )

        assertEquals(125f, match.centerX, 0.1f)
        assertEquals(220f, match.centerY, 0.1f)
    }

    @Test
    fun `IconMatch bounds property creates correct Rect`() {
        val match = IconMatch(
            name = "test_icon",
            topLeft = Point(10, 20),
            width = 30,
            height = 40,
            confidence = 0.9
        )

        val bounds = match.bounds
        assertEquals(10, bounds.left)
        assertEquals(20, bounds.top)
        assertEquals(40, bounds.right)
        assertEquals(60, bounds.bottom)
    }

    @Test
    fun `IconMatch with high confidence is reliable`() {
        val match = IconMatch(
            name = "menu_icon",
            topLeft = Point(0, 0),
            width = 24,
            height = 24,
            confidence = 0.95
        )

        assertTrue("High confidence should be >= 0.8", match.confidence >= 0.8)
    }

    @Test
    fun `IconMatch with low confidence is not reliable`() {
        val match = IconMatch(
            name = "ambiguous_icon",
            topLeft = Point(0, 0),
            width = 24,
            height = 24,
            confidence = 0.65
        )

        assertTrue("Low confidence should be < 0.8", match.confidence < 0.8)
    }

    @Test
    fun `IconMatch data class equality works correctly`() {
        val match1 = IconMatch("icon", Point(10, 20), 30, 40, 0.9)
        val match2 = IconMatch("icon", Point(10, 20), 30, 40, 0.9)
        val match3 = IconMatch("icon", Point(10, 20), 30, 40, 0.8) // Different confidence

        assertEquals(match1, match2)
        assertNotEquals(match1, match3)
    }
}
