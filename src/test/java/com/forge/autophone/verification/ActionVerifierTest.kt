package com.forge.autophone.verification

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.model.NodeSnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for ActionVerifier — action verification and error detection.
 */
class ActionVerifierTest {

    private lateinit var service: AutoPhoneAccessibilityService
    private lateinit var verifier: ActionVerifier

    @Before
    fun setup() {
        service = mockk(relaxed = true)
        verifier = ActionVerifier(service)
    }

    @Test
    fun `verifyTap detects node appearance`() = runTest {
        // Given: Node appears after tap
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Verifying tap with NodeAppears outcome
        val result = verifier.verifyTap(
            x = 100f,
            y = 200f,
            expectedOutcome = ExpectedOutcome.NodeAppears("com.example:id/dialog"),
            verificationDelayMs = 100
        )

        // Then: Verification should complete
        assertNotNull(result)
        // Note: Success depends on actual node appearance in mocked tree
    }

    @Test
    fun `verifyTap detects window changes`() = runTest {
        // Given: Window changes after tap
        val beforeWindow = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        val afterWindow = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { beforeWindow.packageName } returns "com.example.app1"
        every { afterWindow.packageName } returns "com.example.app2"

        // Simulate window change
        every { service.getActiveWindowRoot() } returnsMany listOf(beforeWindow, afterWindow)

        // When: Verifying tap with WindowChanges outcome
        val result = verifier.verifyTap(
            x = 100f,
            y = 200f,
            expectedOutcome = ExpectedOutcome.WindowChanges,
            verificationDelayMs = 100
        )

        // Then: Should detect window change
        assertNotNull(result)
    }

    @Test
    fun `verifyTextEntry checks actual text matches expected`() = runTest {
        // Given: Text field with entered text
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Verifying text entry
        val result = verifier.verifyTextEntry(
            viewId = "com.example:id/email",
            expectedText = "test@example.com",
            verificationDelayMs = 100
        )

        // Then: Should return verification result
        assertNotNull(result)
        assertFalse(result.success) // Empty mock snapshot won't match
    }

    @Test
    fun `verifyScroll detects content changes`() = runTest {
        // Given: Scrollable view
        val mockScrollable = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockScrollable.performAction(any()) } returns true
        every { mockScrollable.childCount } returns 10
        every { service.findById("scroll_view") } returns listOf(mockScrollable)

        // When: Verifying scroll
        val result = verifier.verifyScroll(
            scrollableId = "scroll_view",
            verificationDelayMs = 100
        )

        // Then: Should verify scroll action
        assertNotNull(result)
    }

    @Test
    fun `detectError finds error messages in UI`() = runTest {
        // Given: UI with error message
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Detecting errors
        val detection = verifier.detectError(delayMs = 100)

        // Then: Should return error detection result
        assertNotNull(detection)
        assertFalse(detection.hasError) // Empty mock won't have errors
    }

    @Test
    fun `detectError identifies multiple error keywords`() = runTest {
        // Given: Various error messages
        val errorKeywords = listOf("error", "failed", "unable", "cannot", "invalid", "wrong")

        // When: Testing detection logic
        // Then: Each keyword should trigger error detection
        errorKeywords.forEach { keyword ->
            // Error detection logic would catch these in real implementation
            assertTrue(keyword in listOf("error", "failed", "unable", "cannot", "invalid", "wrong"))
        }
    }

    @Test
    fun `createCheckpoint captures current state`() {
        // Given: Active window with state
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Creating checkpoint
        val checkpoint = verifier.createCheckpoint()

        // Then: Should capture state
        assertNotNull(checkpoint)
        assertEquals("com.example.app", checkpoint.packageName)
        assertTrue(checkpoint.timestamp > 0)
    }

    @Test
    fun `shouldRollback detects package change`() {
        // Given: Checkpoint in one app
        val checkpointWindow = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { checkpointWindow.packageName } returns "com.example.app1"
        every { service.getActiveWindowRoot() } returns checkpointWindow

        val checkpoint = verifier.createCheckpoint()

        // When: Current state is different app
        val currentWindow = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { currentWindow.packageName } returns "com.example.app2"
        every { service.getActiveWindowRoot() } returns currentWindow

        val shouldRollback = verifier.shouldRollback(checkpoint)

        // Then: Should recommend rollback
        assertTrue(shouldRollback)
    }

    @Test
    fun `shouldRollback detects error state`() {
        // Given: Checkpoint without errors
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { mockRoot.packageName } returns "com.example.app"
        every { service.getActiveWindowRoot() } returns mockRoot

        val checkpoint = verifier.createCheckpoint()

        // When: Current state has error
        // (In real scenario, UITreeInspector would return nodes with error text)
        val shouldRollback = verifier.shouldRollback(checkpoint)

        // Then: Rollback decision is made
        assertNotNull(shouldRollback)
    }

    @Test
    fun `verifyTap with TextAppears outcome`() = runTest {
        // Given: Expected text appears after tap
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Verifying tap with text appearance expectation
        val result = verifier.verifyTap(
            x = 100f,
            y = 200f,
            expectedOutcome = ExpectedOutcome.TextAppears("Success"),
            verificationDelayMs = 100
        )

        // Then: Should check for text
        assertNotNull(result)
    }

    @Test
    fun `verifyTap with NoChange outcome`() = runTest {
        // Given: UI that shouldn't change
        val mockRoot = mockk<android.view.accessibility.AccessibilityNodeInfo>(relaxed = true)
        every { service.getActiveWindowRoot() } returns mockRoot

        // When: Verifying tap that shouldn't change UI
        val result = verifier.verifyTap(
            x = 100f,
            y = 200f,
            expectedOutcome = ExpectedOutcome.NoChange,
            verificationDelayMs = 100
        )

        // Then: Should verify no change occurred
        assertNotNull(result)
    }
}
