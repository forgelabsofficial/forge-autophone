package com.forge.autophone.service

import com.forge.autophone.service.GlobalAction
import com.forge.autophone.service.NavigationActions
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

/**
 * Unit tests for [NavigationActions] enum coverage.
 * Device-bound dispatchGesture tests live in androidTest.
 */
class NavigationActionsTest {

    @Test
    fun `GlobalAction enum contains all expected values`() {
        val values = GlobalAction.values().map { it.name }
        assertTrue(values.contains("BACK"))
        assertTrue(values.contains("HOME"))
        assertTrue(values.contains("RECENTS"))
        assertTrue(values.contains("NOTIFICATIONS"))
        assertTrue(values.contains("QUICK_SETTINGS"))
        assertTrue(values.contains("LOCK_SCREEN"))
        assertTrue(values.contains("SCREENSHOT"))
    }

    @Test
    fun `GlobalAction has exactly 7 entries`() {
        assertEquals(7, GlobalAction.values().size)
    }
}
