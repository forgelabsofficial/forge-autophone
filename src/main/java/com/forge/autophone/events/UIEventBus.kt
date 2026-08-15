package com.forge.autophone.events

import com.forge.autophone.model.NodeSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * UIEventBus — real-time UI event streaming for AutoPhone.
 *
 * Enables reactive programming patterns:
 * - Agent can wait for specific events instead of polling
 * - Detect UI changes (window switches, dialogs, text updates, toasts)
 * - Build event-driven automation workflows
 *
 * Events are emitted from [AutoPhoneAccessibilityService.onAccessibilityEvent].
 */
class UIEventBus {
    private val _events = MutableSharedFlow<UIEvent>(
        replay = 100, // Keep last 100 events for late subscribers
        extraBufferCapacity = 50
    )
    
    /**
     * Observable stream of UI events.
     * Collect this flow to receive real-time UI changes.
     */
    val events: SharedFlow<UIEvent> = _events.asSharedFlow()

    /**
     * Emit a UI event to all subscribers.
     */
    fun emit(event: UIEvent) {
        _events.tryEmit(event)
    }

    /**
     * Get the last N events (from replay buffer).
     */
    fun getRecentEvents(count: Int = 10): List<UIEvent> {
        // Note: This is a simplified implementation.
        // In production, you'd maintain a separate ring buffer.
        return emptyList() // Placeholder
    }
}

/**
 * Sealed class hierarchy of UI events.
 */
sealed class UIEvent {
    abstract val timestamp: Long

    /**
     * Window changed — user switched apps or moved to a different activity.
     */
    data class WindowChanged(
        val packageName: String,
        val className: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * A new node appeared in the UI tree.
     */
    data class NodeAppeared(
        val snapshot: NodeSnapshot,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * A node disappeared from the UI tree.
     */
    data class NodeDisappeared(
        val viewId: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * Text in a node changed.
     */
    data class TextChanged(
        val viewId: String?,
        val oldText: String?,
        val newText: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * Toast message shown (transient notification).
     */
    data class ToastShown(
        val message: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * View was clicked (user or agent interaction).
     */
    data class ViewClicked(
        val viewId: String?,
        val x: Float,
        val y: Float,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * View received focus.
     */
    data class ViewFocused(
        val viewId: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * Scrolling occurred in a view.
     */
    data class ViewScrolled(
        val viewId: String?,
        val scrollX: Int,
        val scrollY: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * Notification posted.
     */
    data class NotificationPosted(
        val packageName: String,
        val title: String?,
        val text: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()

    /**
     * Generic accessibility event (fallback).
     */
    data class AccessibilityEvent(
        val eventType: Int,
        val packageName: String?,
        val className: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UIEvent()
}
