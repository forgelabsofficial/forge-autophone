package com.forge.autophone.wait

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.events.UIEvent
import com.forge.autophone.inspector.UITreeInspector
import com.forge.autophone.model.NodeSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull

/**
 * SmartWaiter — intelligent wait strategies for AutoPhone.
 *
 * Eliminates brittle hardcoded delays in agent scripts. Instead of `delay(2000)`,
 * agents can use semantic waits like `waitForNode()` or `waitUntilIdle()`.
 */
class SmartWaiter(private val service: AutoPhoneAccessibilityService) {

    /**
     * Wait until a condition is true, with timeout.
     * Polls the condition at regular intervals.
     *
     * @param timeoutMs Maximum time to wait (default 10 seconds)
     * @param pollIntervalMs How often to check the condition (default 100ms)
     * @param condition The condition to wait for
     * @return true if condition became true, false if timeout
     */
    suspend fun waitUntil(
        timeoutMs: Long = 10000,
        pollIntervalMs: Long = 100,
        condition: suspend () -> Boolean
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) return true
            delay(pollIntervalMs)
        }
        return false
    }

    /**
     * Wait until the UI is idle (no events for a specified duration).
     * Useful after performing an action that might trigger animations.
     *
     * @param timeoutMs Maximum time to wait
     * @param idleDurationMs How long the UI must be idle (default 500ms)
     * @return true if UI became idle, false if timeout
     */
    suspend fun waitUntilIdle(
        timeoutMs: Long = 5000,
        idleDurationMs: Long = 500
    ): Boolean {
        var lastEventTime = System.currentTimeMillis()
        
        // Launch event listener in background
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            service.eventBus.events
                .onEach { lastEventTime = System.currentTimeMillis() }
                .collect {}
        }

        val result = waitUntil(timeoutMs) {
            System.currentTimeMillis() - lastEventTime > idleDurationMs
        }

        job.cancel()
        return result
    }

    /**
     * Wait for a specific window (app/activity) to appear.
     *
     * @param packageName Package name of the target app
     * @param timeoutMs Maximum time to wait
     * @return true if window appeared, false if timeout
     */
    suspend fun waitForWindow(
        packageName: String,
        timeoutMs: Long = 5000
    ): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            service.eventBus.events
                .first { event ->
                    event is UIEvent.WindowChanged && event.packageName == packageName
                }
            true
        } ?: false
    }

    /**
     * Wait for a node with specific view ID to appear.
     *
     * @param viewId Resource ID of the target node
     * @param timeoutMs Maximum time to wait
     * @return The node snapshot if found, null if timeout
     */
    suspend fun waitForNode(
        viewId: String,
        timeoutMs: Long = 5000
    ): NodeSnapshot? {
        return waitUntil(timeoutMs) {
            UITreeInspector(service).snapshot().any { it.viewId == viewId }
        }.let { found ->
            if (found) {
                UITreeInspector(service).snapshot().find { it.viewId == viewId }
            } else null
        }
    }

    /**
     * Wait for a node containing specific text to appear.
     *
     * @param text Text to search for (case-insensitive)
     * @param timeoutMs Maximum time to wait
     * @return The node snapshot if found, null if timeout
     */
    suspend fun waitForText(
        text: String,
        timeoutMs: Long = 5000
    ): NodeSnapshot? {
        return waitUntil(timeoutMs) {
            UITreeInspector(service).snapshot().any { node ->
                node.text?.contains(text, ignoreCase = true) == true ||
                node.contentDescription?.contains(text, ignoreCase = true) == true
            }
        }.let { found ->
            if (found) {
                UITreeInspector(service).snapshot().find { node ->
                    node.text?.contains(text, ignoreCase = true) == true ||
                    node.contentDescription?.contains(text, ignoreCase = true) == true
                }
            } else null
        }
    }

    /**
     * Wait for text in a specific node to change.
     *
     * @param viewId Resource ID of the node to watch
     * @param timeoutMs Maximum time to wait
     * @return New text if changed, null if timeout or node not found
     */
    suspend fun waitForTextChange(
        viewId: String,
        timeoutMs: Long = 5000
    ): String? {
        // Capture initial text
        val initialNode = UITreeInspector(service).snapshot().find { it.viewId == viewId }
        val initialText = initialNode?.text

        return waitUntil(timeoutMs) {
            val currentNode = UITreeInspector(service).snapshot().find { it.viewId == viewId }
            currentNode?.text != initialText
        }.let { changed ->
            if (changed) {
                UITreeInspector(service).snapshot().find { it.viewId == viewId }?.text
            } else null
        }
    }

    /**
     * Wait for a dialog to appear.
     * Detects dialogs by looking for common dialog indicators.
     */
    suspend fun waitForDialog(timeoutMs: Long = 5000): Boolean {
        return waitUntil(timeoutMs) {
            val snapshot = UITreeInspector(service).snapshot()
            snapshot.any { node ->
                node.className?.contains("Dialog", ignoreCase = true) == true ||
                node.className?.contains("AlertDialog", ignoreCase = true) == true ||
                node.viewId?.contains("dialog", ignoreCase = true) == true
            }
        }
    }

    /**
     * Wait for a toast message containing specific text.
     *
     * @param text Text to look for in toast
     * @param timeoutMs Maximum time to wait
     * @return The full toast message if found, null if timeout
     */
    suspend fun waitForToast(
        text: String,
        timeoutMs: Long = 5000
    ): String? {
        return withTimeoutOrNull(timeoutMs) {
            service.eventBus.events
                .first { event ->
                    event is UIEvent.ToastShown && 
                    event.message.contains(text, ignoreCase = true)
                }
                .let { (it as UIEvent.ToastShown).message }
        }
    }

    /**
     * Wait for any node matching a predicate.
     *
     * @param timeoutMs Maximum time to wait
     * @param predicate Condition to match nodes
     * @return Matching node if found, null if timeout
     */
    suspend fun waitForNodeMatching(
        timeoutMs: Long = 5000,
        predicate: (NodeSnapshot) -> Boolean
    ): NodeSnapshot? {
        return waitUntil(timeoutMs) {
            UITreeInspector(service).snapshot().any(predicate)
        }.let { found ->
            if (found) {
                UITreeInspector(service).snapshot().find(predicate)
            } else null
        }
    }
}
