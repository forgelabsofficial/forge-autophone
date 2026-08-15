package com.forge.autophone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.forge.autophone.accessibility.TextEntryService
import com.forge.autophone.service.GestureHandler
import com.forge.autophone.service.NavigationActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * AutoPhoneAccessibilityService — Forge OS Accessibility Layer
 *
 * The root [AccessibilityService] for Forge OS. This is the system-bound entry
 * point that gives the AI agent runtime full access to:
 *
 *  - **UI inspection**   → [getActiveWindowRoot], [findById], [findByText]
 *  - **Gesture dispatch** → [gestureHandler] (tap, swipe, long-press, pinch)
 *  - **Text input**      → [textEntry] (ACTION_SET_TEXT via Bundle)
 *  - **Navigation**      → [navigation] (Back, Home, Recents, Screenshot, …)
 *
 * Declared in AndroidManifest.xml with:
 *   android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
 *
 * Config XML: res/xml/autophone_accessibility_config.xml
 *   - canPerformGestures="true"
 *   - canRetrieveWindowContent="true"
 *   - accessibilityEventTypes="typeAllMask"
 */
@AndroidEntryPoint
class AutoPhoneAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var gestureHandler: GestureHandler
        private set

    lateinit var textEntry: TextEntryService
        private set

    lateinit var navigation: NavigationActions
        private set

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Supplement the XML config programmatically
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = (AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                    or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE)
            notificationTimeout = 100
        }

        gestureHandler = GestureHandler(this)
        textEntry = TextEntryService(this)
        navigation = NavigationActions(this)

        // Expose singleton reference for tool registry and inspector
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Forwarded to the Forge OS agent event bus for reactive UI inspection.
        // Subscribers register via AutoPhoneEventBus (future implementation).
    }

    override fun onInterrupt() {
        // Service interrupted — e.g. user toggled accessibility off.
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── UI Inspection API ─────────────────────────────────────────────────────

    /** Full UI node tree of the currently active window, or null if unavailable. */
    fun getActiveWindowRoot(): AccessibilityNodeInfo? = rootInActiveWindow

    /** Find nodes by view resource ID, e.g. "com.example.app:id/submit_btn". */
    fun findById(viewId: String): List<AccessibilityNodeInfo> =
        rootInActiveWindow?.findAccessibilityNodeInfosByViewId(viewId) ?: emptyList()

    /** Find nodes by visible text content. */
    fun findByText(text: String): List<AccessibilityNodeInfo> =
        rootInActiveWindow?.findAccessibilityNodeInfosByText(text) ?: emptyList()

    // ── Singleton ─────────────────────────────────────────────────────────────

    companion object {
        /**
         * Live singleton reference, set in [onServiceConnected] and cleared in [onDestroy].
         * Null means the service is not currently connected (user hasn't granted permission,
         * or the service was interrupted).
         */
        @Volatile
        var instance: AutoPhoneAccessibilityService? = null
            private set
    }
}
