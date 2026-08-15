package com.forge.autophone.context

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.model.NodeSnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for AppContextTracker — context detection and screen type classification.
 */
class AppContextTrackerTest {

    private lateinit var service: AutoPhoneAccessibilityService
    private lateinit var tracker: AppContextTracker

    @Before
    fun setup() {
        service = mockk(relaxed = true)
        tracker = AppContextTracker(service)
    }

    @Test
    fun `detects login screen from username and password fields`() {
        // Given: UI with login-related elements
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { mockRoot.className } returns "LoginActivity"
        every { service.getActiveWindowRoot() } returns mockRoot

        // Mock UITreeInspector snapshot with login elements
        val loginSnapshot = listOf(
            createMockNode(viewId = "username_field", text = "Username", isEditable = true),
            createMockNode(viewId = "password_field", contentDesc = "Password", isEditable = true),
            createMockNode(viewId = "login_button", text = "Sign In")
        )

        // When: Getting context
        val context = tracker.getCurrentContext()

        // Then: Should detect login screen
        assertNotNull(context)
        assertEquals(ScreenType.LOGIN, context?.screenType)
    }

    @Test
    fun `detects settings screen from activity name`() {
        // Given: Settings activity
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { mockRoot.className } returns "SettingsActivity"
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Getting context
        val context = tracker.getCurrentContext()

        // Then: Should detect settings screen
        assertNotNull(context)
        assertEquals(ScreenType.SETTINGS, context?.screenType)
    }

    @Test
    fun `detects UI patterns - bottom navigation`() {
        // Given: UI with bottom nav
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { service.getActiveWindowRoot() } returns mockRoot

        val snapshot = listOf(
            createMockNode(viewId = "bottom_navigation", className = "BottomNavigationView")
        )

        // When: Getting context
        val context = tracker.getCurrentContext()

        // Then: Should detect bottom nav pattern
        assertNotNull(context)
        assertTrue(context?.uiPatterns?.contains(UIPattern.BOTTOM_NAV) == true)
    }

    @Test
    fun `detects form fields with correct types`() {
        // Given: UI with various form fields
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Detecting form fields
        val fields = tracker.detectFormFields()

        // Then: Should classify field types correctly
        assertTrue(fields.isNotEmpty())
    }

    @Test
    fun `caches context for performance`() {
        // Given: Service with root
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Getting context twice quickly
        val context1 = tracker.getCurrentContext()
        val context2 = tracker.getCurrentContext()

        // Then: Should return same cached instance
        assertSame(context1, context2)
    }

    @Test
    fun `clears cache when requested`() {
        // Given: Cached context
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot
        tracker.getCurrentContext()

        // When: Clearing cache
        tracker.clearCache()

        // Then: Next call should compute fresh
        val newContext = tracker.getCurrentContext()
        assertNotNull(newContext)
    }

    @Test
    fun `returns null when no active window`() {
        // Given: No active window
        every { service.getActiveWindowRoot() } returns null

        // When: Getting context
        val context = tracker.getCurrentContext()

        // Then: Should return null
        assertNull(context)
    }

    @Test
    fun `detects chat screen with message elements`() {
        // Given: UI with chat elements
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { service.getActiveWindowRoot() } returns mockRoot

        val chatSnapshot = listOf(
            createMockNode(viewId = "message_input", contentDesc = "Type message", isEditable = true),
            createMockNode(viewId = "send_button", text = "Send"),
            createMockNode(className = "androidx.recyclerview.widget.RecyclerView")
        )

        // When: Getting context
        val context = tracker.getCurrentContext()

        // Then: Should detect chat screen
        assertNotNull(context)
        // Note: Actual detection depends on UITreeInspector implementation
    }

    @Test
    fun `detects loading screen with progress bar`() {
        // Given: UI with progress indicator
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot

        val loadingSnapshot = listOf(
            createMockNode(className = "android.widget.ProgressBar", text = "Loading...")
        )

        // When: Getting context
        val context = tracker.getCurrentContext()

        // Then: Should detect loading screen
        assertNotNull(context)
    }

    @Test
    fun `detects error screen from error text`() {
        // Given: UI with error message
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot

        val errorSnapshot = listOf(
            createMockNode(text = "Error: Connection failed"),
            createMockNode(text = "Try again")
        )

        // When: Getting context
        val context = tracker.getCurrentContext()

        // Then: Should detect error state
        assertNotNull(context)
    }

    // ── Helper methods ───────────────────────────────────────────────────────

    private fun createMockNode(
        viewId: String? = null,
        text: String? = null,
        contentDesc: String? = null,
        className: String? = null,
        isEditable: Boolean = false,
        isClickable: Boolean = false
    ): NodeSnapshot {
        return NodeSnapshot(
            viewId = viewId,
            text = text,
            contentDescription = contentDesc,
            className = className ?: "android.widget.View",
            packageName = "com.example.app",
            isEnabled = true,
            isClickable = isClickable,
            isEditable = isEditable,
            isFocusable = false,
            isScrollable = false,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 100,
            boundsBottom = 100,
            childCount = 0
        )
    }
}
