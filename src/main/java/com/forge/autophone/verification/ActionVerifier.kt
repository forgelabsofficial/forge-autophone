package com.forge.autophone.verification

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.inspector.UITreeInspector
import com.forge.autophone.model.NodeSnapshot
import kotlinx.coroutines.delay

/**
 * ActionVerifier — verifies that actions succeeded and enables rollback.
 *
 * Detects when automation fails:
 * - Tap didn't trigger expected outcome
 * - Text entry failed
 * - Navigation led to unexpected screen
 * - Error messages appeared
 *
 * Enables self-correcting automation: "I tapped the button but the expected
 * dialog didn't appear. Let me try a different approach."
 */
class ActionVerifier(private val service: AutoPhoneAccessibilityService) {

    /**
     * Verify that a tap action succeeded.
     *
     * @param x Tap X coordinate
     * @param y Tap Y coordinate
     * @param expectedOutcome What should happen after the tap
     * @param verificationDelayMs How long to wait before checking (default 500ms)
     * @return VerificationResult with success status and details
     */
    suspend fun verifyTap(
        x: Float,
        y: Float,
        expectedOutcome: ExpectedOutcome,
        verificationDelayMs: Long = 500
    ): VerificationResult {
        // Capture state before action
        val beforeSnapshot = UITreeInspector(service).snapshot()
        val beforeWindow = service.getActiveWindowRoot()?.packageName?.toString()

        // Perform tap
        service.gestureHandler.performClick(x, y)

        // Wait for UI to update
        delay(verificationDelayMs)

        // Capture state after action
        val afterSnapshot = UITreeInspector(service).snapshot()
        val afterWindow = service.getActiveWindowRoot()?.packageName?.toString()

        // Verify outcome
        return when (expectedOutcome) {
            is ExpectedOutcome.NodeAppears -> {
                val appeared = afterSnapshot.any { it.viewId == expectedOutcome.viewId }
                VerificationResult(
                    success = appeared,
                    message = if (appeared) "Node ${expectedOutcome.viewId} appeared" 
                             else "Node ${expectedOutcome.viewId} did not appear",
                    beforeSnapshot = beforeSnapshot,
                    afterSnapshot = afterSnapshot
                )
            }

            is ExpectedOutcome.NodeDisappears -> {
                val wasThere = beforeSnapshot.any { it.viewId == expectedOutcome.viewId }
                val isGone = afterSnapshot.none { it.viewId == expectedOutcome.viewId }
                VerificationResult(
                    success = wasThere && isGone,
                    message = if (wasThere && isGone) "Node ${expectedOutcome.viewId} disappeared"
                             else "Node did not disappear as expected",
                    beforeSnapshot = beforeSnapshot,
                    afterSnapshot = afterSnapshot
                )
            }

            is ExpectedOutcome.TextChanges -> {
                val before = beforeSnapshot.find { it.viewId == expectedOutcome.viewId }?.text
                val after = afterSnapshot.find { it.viewId == expectedOutcome.viewId }?.text
                val changed = before != after
                VerificationResult(
                    success = changed,
                    message = if (changed) "Text changed from '$before' to '$after'"
                             else "Text did not change",
                    beforeSnapshot = beforeSnapshot,
                    afterSnapshot = afterSnapshot
                )
            }

            is ExpectedOutcome.WindowChanges -> {
                val changed = beforeWindow != afterWindow
                VerificationResult(
                    success = changed,
                    message = if (changed) "Window changed from $beforeWindow to $afterWindow"
                             else "Window did not change",
                    beforeSnapshot = beforeSnapshot,
                    afterSnapshot = afterSnapshot
                )
            }

            is ExpectedOutcome.TextAppears -> {
                val appeared = afterSnapshot.any { node ->
                    node.text?.contains(expectedOutcome.text, ignoreCase = true) == true ||
                    node.contentDescription?.contains(expectedOutcome.text, ignoreCase = true) == true
                }
                VerificationResult(
                    success = appeared,
                    message = if (appeared) "Text '${expectedOutcome.text}' appeared"
                             else "Text '${expectedOutcome.text}' did not appear",
                    beforeSnapshot = beforeSnapshot,
                    afterSnapshot = afterSnapshot
                )
            }

            is ExpectedOutcome.NoChange -> {
                val unchanged = beforeSnapshot == afterSnapshot
                VerificationResult(
                    success = unchanged,
                    message = if (unchanged) "UI remained unchanged (as expected)"
                             else "UI changed unexpectedly",
                    beforeSnapshot = beforeSnapshot,
                    afterSnapshot = afterSnapshot
                )
            }
        }
    }

    /**
     * Verify text entry succeeded.
     */
    suspend fun verifyTextEntry(
        viewId: String,
        expectedText: String,
        verificationDelayMs: Long = 300
    ): VerificationResult {
        delay(verificationDelayMs)
        
        val snapshot = UITreeInspector(service).snapshot()
        val node = snapshot.find { it.viewId == viewId }
        val actualText = node?.text ?: ""
        
        val success = actualText == expectedText
        
        return VerificationResult(
            success = success,
            message = if (success) "Text '$expectedText' entered successfully"
                     else "Expected '$expectedText' but found '$actualText'",
            beforeSnapshot = emptyList(),
            afterSnapshot = snapshot
        )
    }

    /**
     * Verify scroll action succeeded (content changed).
     */
    suspend fun verifyScroll(
        scrollableId: String,
        verificationDelayMs: Long = 300
    ): VerificationResult {
        val beforeSnapshot = UITreeInspector(service).snapshot()
        val beforeScrollable = beforeSnapshot.find { it.viewId == scrollableId }
        
        // Perform scroll
        val scrollable = service.findById(scrollableId).firstOrNull()
        val scrolled = scrollable?.performAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        ) ?: false
        
        delay(verificationDelayMs)
        
        val afterSnapshot = UITreeInspector(service).snapshot()
        val afterScrollable = afterSnapshot.find { it.viewId == scrollableId }
        
        // Check if content changed (simple heuristic: child count or text changed)
        val contentChanged = beforeScrollable?.childCount != afterScrollable?.childCount ||
                            beforeSnapshot.size != afterSnapshot.size
        
        return VerificationResult(
            success = scrolled && contentChanged,
            message = if (scrolled && contentChanged) "Scroll succeeded, content updated"
                     else if (!scrolled) "Scroll action failed"
                     else "Scrolled but no new content appeared",
            beforeSnapshot = beforeSnapshot,
            afterSnapshot = afterSnapshot
        )
    }

    /**
     * Detect if an error occurred after an action.
     * Looks for error indicators in the UI.
     */
    suspend fun detectError(delayMs: Long = 500): ErrorDetection {
        delay(delayMs)
        
        val snapshot = UITreeInspector(service).snapshot()
        val errorKeywords = listOf("error", "failed", "unable", "cannot", "invalid", "wrong")
        
        val errorNodes = snapshot.filter { node ->
            val text = node.text?.lowercase() ?: ""
            val desc = node.contentDescription?.lowercase() ?: ""
            errorKeywords.any { text.contains(it) || desc.contains(it) }
        }
        
        return if (errorNodes.isNotEmpty()) {
            ErrorDetection(
                hasError = true,
                errorMessage = errorNodes.firstOrNull()?.text ?: "Error detected",
                errorNodes = errorNodes
            )
        } else {
            ErrorDetection(hasError = false, errorMessage = null, errorNodes = emptyList())
        }
    }

    /**
     * Create a state checkpoint that can be used for rollback.
     */
    fun createCheckpoint(): StateCheckpoint {
        val snapshot = UITreeInspector(service).snapshot()
        val windowInfo = service.getActiveWindowRoot()
        
        return StateCheckpoint(
            timestamp = System.currentTimeMillis(),
            packageName = windowInfo?.packageName?.toString(),
            snapshot = snapshot
        )
    }

    /**
     * Compare current state with a checkpoint to see if rollback is needed.
     */
    fun shouldRollback(checkpoint: StateCheckpoint): Boolean {
        val current = createCheckpoint()
        
        // If we're in a different app, definitely need rollback
        if (current.packageName != checkpoint.packageName) return true
        
        // If error appeared, need rollback
        val errorDetected = current.snapshot.any { node ->
            val text = node.text?.lowercase() ?: ""
            text.contains("error") || text.contains("failed")
        }
        
        return errorDetected
    }
}

/**
 * Expected outcome after an action.
 */
sealed class ExpectedOutcome {
    data class NodeAppears(val viewId: String) : ExpectedOutcome()
    data class NodeDisappears(val viewId: String) : ExpectedOutcome()
    data class TextChanges(val viewId: String) : ExpectedOutcome()
    data class TextAppears(val text: String) : ExpectedOutcome()
    object WindowChanges : ExpectedOutcome()
    object NoChange : ExpectedOutcome()
}

/**
 * Result of action verification.
 */
data class VerificationResult(
    val success: Boolean,
    val message: String,
    val beforeSnapshot: List<NodeSnapshot>,
    val afterSnapshot: List<NodeSnapshot>
)

/**
 * Error detection result.
 */
data class ErrorDetection(
    val hasError: Boolean,
    val errorMessage: String?,
    val errorNodes: List<NodeSnapshot>
)

/**
 * State checkpoint for rollback.
 */
data class StateCheckpoint(
    val timestamp: Long,
    val packageName: String?,
    val snapshot: List<NodeSnapshot>
)
