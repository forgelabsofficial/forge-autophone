package com.forge.autophone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.forge.autophone.accessibility.TextEntryService
import com.forge.autophone.context.AppContextTracker
import com.forge.autophone.diff.UITreeDiffer
import com.forge.autophone.events.UIEvent
import com.forge.autophone.events.UIEventBus
import com.forge.autophone.ocr.OcrTextExtractor
import com.forge.autophone.scroll.ScrollHelper
import com.forge.autophone.service.GestureHandler
import com.forge.autophone.service.NavigationActions
import com.forge.autophone.verification.ActionVerifier
import com.forge.autophone.vision.IconMatcher
import com.forge.autophone.wait.SmartWaiter
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

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var gestureHandler: GestureHandler
        private set

    lateinit var textEntry: TextEntryService
        private set

    lateinit var navigation: NavigationActions
        private set

    lateinit var eventBus: UIEventBus
        private set

    lateinit var smartWaiter: SmartWaiter
        private set

    lateinit var scrollHelper: ScrollHelper
        private set

    lateinit var ocrExtractor: OcrTextExtractor
        private set

    lateinit var iconMatcher: IconMatcher
        private set

    lateinit var appContextTracker: AppContextTracker
        private set

    lateinit var actionVerifier: ActionVerifier
        private set

    lateinit var uiTreeDiffer: UITreeDiffer
        private set

    lateinit var selfHealingSelector: com.forge.autophone.healing.SelfHealingSelector
        private set

    lateinit var gestureRecorder: com.forge.autophone.recording.GestureRecorder
        private set

    lateinit var gesturePlayer: com.forge.autophone.recording.GesturePlayer
        private set

    lateinit var gestureLibrary: com.forge.autophone.recording.GestureLibrary
        private set

    lateinit var formAutomation: com.forge.autophone.form.AdvancedFormAutomation
        private set

    lateinit var screenAI: com.forge.autophone.vision.ScreenAIInterface
        private set

    lateinit var telemetry: com.forge.autophone.telemetry.TelemetryCollector
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
        eventBus = UIEventBus()
        smartWaiter = SmartWaiter(this)
        scrollHelper = ScrollHelper(this)
        ocrExtractor = OcrTextExtractor()
        iconMatcher = IconMatcher(applicationContext)
        appContextTracker = AppContextTracker(this)
        actionVerifier = ActionVerifier(this)
        uiTreeDiffer = UITreeDiffer(this)
        selfHealingSelector = com.forge.autophone.healing.SelfHealingSelector(this)
        gestureRecorder = com.forge.autophone.recording.GestureRecorder(this)
        gesturePlayer = com.forge.autophone.recording.GesturePlayer(this)
        gestureLibrary = com.forge.autophone.recording.GestureLibrary()
        formAutomation = com.forge.autophone.form.AdvancedFormAutomation(this)
        screenAI = com.forge.autophone.vision.ScreenAIInterface(this)
        telemetry = com.forge.autophone.telemetry.TelemetryCollector()

        // Expose singleton reference for tool registry and inspector
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Forward events to the event bus for reactive UI inspection
        val uiEvent = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                UIEvent.WindowChanged(
                    packageName = event.packageName?.toString() ?: "",
                    className = event.className?.toString() ?: ""
                )
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source
                val bounds = android.graphics.Rect()
                source?.getBoundsInScreen(bounds)
                UIEvent.ViewClicked(
                    viewId = source?.viewIdResourceName,
                    x = bounds.centerX().toFloat(),
                    y = bounds.centerY().toFloat()
                )
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                UIEvent.TextChanged(
                    viewId = event.source?.viewIdResourceName,
                    oldText = event.beforeText?.toString(),
                    newText = event.text?.firstOrNull()?.toString()
                )
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                UIEvent.ViewFocused(
                    viewId = event.source?.viewIdResourceName
                )
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                UIEvent.ViewScrolled(
                    viewId = event.source?.viewIdResourceName,
                    scrollX = event.scrollX,
                    scrollY = event.scrollY
                )
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                // Check if it's a toast
                val isToast = event.parcelableData == null
                if (isToast && event.text.isNotEmpty()) {
                    UIEvent.ToastShown(
                        message = event.text.joinToString(" ")
                    )
                } else {
                    UIEvent.AccessibilityEvent(
                        eventType = event.eventType,
                        packageName = event.packageName?.toString(),
                        className = event.className?.toString()
                    )
                }
            }
            else -> {
                UIEvent.AccessibilityEvent(
                    eventType = event.eventType,
                    packageName = event.packageName?.toString(),
                    className = event.className?.toString()
                )
            }
        }
        
        eventBus.emit(uiEvent)
    }

    override fun onInterrupt() {
        // Service interrupted — e.g. user toggled accessibility off.
    }

    override fun onDestroy() {
        instance = null
        ocrExtractor.close()
        iconMatcher.cleanup()
        telemetry.clear()
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
