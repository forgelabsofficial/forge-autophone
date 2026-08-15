package com.forge.autophone.scroll

import android.view.accessibility.AccessibilityNodeInfo
import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.inspector.UITreeInspector
import com.forge.autophone.model.NodeSnapshot
import kotlinx.coroutines.delay

/**
 * ScrollHelper — smart scrolling patterns for AutoPhone.
 *
 * Eliminates the need for agents to manually script "scroll → check → scroll → check" loops.
 * Provides high-level scroll operations with automatic retry and detection.
 */
class ScrollHelper(private val service: AutoPhoneAccessibilityService) {

    /**
     * Scroll until a target node is found.
     * 
     * @param scrollableId View ID of the scrollable container (e.g., RecyclerView)
     * @param targetMatcher Predicate to identify the target node
     * @param maxScrolls Maximum number of scroll attempts before giving up
     * @param scrollDelayMs Delay after each scroll to wait for content to load
     * @return The found node, or null if not found after maxScrolls attempts
     */
    suspend fun scrollUntilFound(
        scrollableId: String,
        targetMatcher: (NodeSnapshot) -> Boolean,
        maxScrolls: Int = 20,
        scrollDelayMs: Long = 300
    ): NodeSnapshot? {
        repeat(maxScrolls) { attempt ->
            // Check if target is visible in current viewport
            val snapshot = UITreeInspector(service).snapshot()
            val found = snapshot.firstOrNull(targetMatcher)
            if (found != null) return found

            // Scroll forward (down for vertical, right for horizontal)
            val scrollable = service.findById(scrollableId).firstOrNull() 
                ?: return null // Scrollable not found
            
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (!scrolled) {
                // Can't scroll anymore — reached the end
                return null
            }

            delay(scrollDelayMs) // Wait for scroll animation + content load
        }
        return null // Not found after maxScrolls attempts
    }

    /**
     * Scroll until text is found.
     */
    suspend fun scrollUntilText(
        scrollableId: String,
        text: String,
        maxScrolls: Int = 20
    ): NodeSnapshot? {
        return scrollUntilFound(scrollableId, { node ->
            node.text?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.contains(text, ignoreCase = true) == true
        }, maxScrolls)
    }

    /**
     * Scroll until a view with specific ID is found.
     */
    suspend fun scrollUntilViewId(
        scrollableId: String,
        targetViewId: String,
        maxScrolls: Int = 20
    ): NodeSnapshot? {
        return scrollUntilFound(scrollableId, { node ->
            node.viewId == targetViewId
        }, maxScrolls)
    }

    /**
     * Scroll to the top of a scrollable container.
     * Uses repeated SCROLL_BACKWARD until no more scrolling is possible.
     */
    suspend fun scrollToTop(
        scrollableId: String,
        maxScrolls: Int = 50,
        scrollDelayMs: Long = 200
    ) {
        val scrollable = service.findById(scrollableId).firstOrNull() ?: return
        
        repeat(maxScrolls) {
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            if (!scrolled) return // Reached top
            delay(scrollDelayMs)
        }
    }

    /**
     * Scroll to the bottom of a scrollable container.
     * Uses repeated SCROLL_FORWARD until no more scrolling is possible.
     */
    suspend fun scrollToBottom(
        scrollableId: String,
        maxScrolls: Int = 50,
        scrollDelayMs: Long = 200
    ) {
        val scrollable = service.findById(scrollableId).firstOrNull() ?: return
        
        repeat(maxScrolls) {
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (!scrolled) return // Reached bottom
            delay(scrollDelayMs)
        }
    }

    /**
     * Fling scroll to top (fast scroll with momentum).
     * Useful for long lists where scrollToTop() would be too slow.
     */
    suspend fun flingToTop(scrollableId: String) {
        val scrollable = service.findById(scrollableId).firstOrNull() ?: return
        
        // Perform fast fling gesture
        service.gestureHandler.performSwipe(
            startX = scrollable.boundsInScreen.centerX().toFloat(),
            startY = scrollable.boundsInScreen.bottom.toFloat() - 100f,
            endX = scrollable.boundsInScreen.centerX().toFloat(),
            endY = scrollable.boundsInScreen.top.toFloat() + 100f,
            durationMs = 100 // Fast swipe = fling
        )
        
        delay(500) // Wait for fling animation to settle
    }

    /**
     * Fling scroll to bottom (fast scroll with momentum).
     */
    suspend fun flingToBottom(scrollableId: String) {
        val scrollable = service.findById(scrollableId).firstOrNull() ?: return
        
        service.gestureHandler.performSwipe(
            startX = scrollable.boundsInScreen.centerX().toFloat(),
            startY = scrollable.boundsInScreen.top.toFloat() + 100f,
            endX = scrollable.boundsInScreen.centerX().toFloat(),
            endY = scrollable.boundsInScreen.bottom.toFloat() - 100f,
            durationMs = 100
        )
        
        delay(500)
    }

    /**
     * Check if a scrollable container can scroll forward (down/right).
     */
    fun canScrollForward(scrollableId: String): Boolean {
        val scrollable = service.findById(scrollableId).firstOrNull() ?: return false
        return scrollable.actionList.any { 
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD 
        }
    }

    /**
     * Check if a scrollable container can scroll backward (up/left).
     */
    fun canScrollBackward(scrollableId: String): Boolean {
        val scrollable = service.findById(scrollableId).firstOrNull() ?: return false
        return scrollable.actionList.any { 
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD 
        }
    }
}

// Extension to get bounds in screen coordinates
private val AccessibilityNodeInfo.boundsInScreen: android.graphics.Rect
    get() {
        val rect = android.graphics.Rect()
        getBoundsInScreen(rect)
        return rect
    }
