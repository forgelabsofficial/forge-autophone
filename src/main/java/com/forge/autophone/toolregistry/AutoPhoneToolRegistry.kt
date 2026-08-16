package com.forge.autophone.toolregistry

import android.graphics.Bitmap
import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.model.NodeSnapshot
import com.forge.autophone.ocr.OcrTextBlock
import kotlinx.coroutines.flow.Flow

/**
 * AutoPhoneToolRegistry — binds the Forge OS ReAct agent tool interface to
 * the live [AutoPhoneAccessibilityService] instance.
 *
 * Each tool maps to one accessibility capability:
 *  - tap / swipe / long_press   → GestureHandler
 *  - type_text / clear_field    → TextEntryService
 *  - navigate / screenshot      → NavigationActions
 *  - find_node / get_tree       → service.findById / getActiveWindowRoot
 *  - ocr_* / scroll_* / wait_*  → OcrTextExtractor, ScrollHelper, SmartWaiter
 */
class AutoPhoneToolRegistry(private val service: AutoPhoneAccessibilityService) {

    // ── Gesture tools ────────────────────────────────────────────────────────

    fun tap(x: Float, y: Float) =
        service.gestureHandler.performClick(x, y)

    fun longPress(x: Float, y: Float, durationMs: Long = 600) =
        service.gestureHandler.performLongClick(x, y, durationMs)

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300) =
        service.gestureHandler.performSwipe(startX, startY, endX, endY, durationMs)

    // ── Text tools ───────────────────────────────────────────────────────────

    fun typeText(text: String, viewId: String) =
        service.textEntry.typeIntoViewId(text, viewId)

    fun typeFocused(text: String) =
        service.textEntry.typeIntoFocusedField(text)

    fun clearField(viewId: String) =
        service.textEntry.clearField(viewId)

    // ── Navigation tools ─────────────────────────────────────────────────────

    fun back() = service.navigation.back()
    fun home() = service.navigation.home()
    fun recents() = service.navigation.recents()
    fun screenshot() = service.navigation.takeScreenshot()

    // ── Inspection tools ─────────────────────────────────────────────────────

    fun findById(viewId: String): List<android.view.accessibility.AccessibilityNodeInfo> = 
        service.findById(viewId)
    fun findByText(text: String): List<android.view.accessibility.AccessibilityNodeInfo> = 
        service.findByText(text)
    fun getActiveWindowRoot(): android.view.accessibility.AccessibilityNodeInfo? = 
        service.getActiveWindowRoot()

    // ── OCR tools (NEW - Phase 1) ────────────────────────────────────────────

    /**
     * Extract all visible text from current screen using OCR.
     * Returns text blocks with bounding boxes and confidence scores.
     */
    suspend fun ocrReadScreen(): List<OcrTextBlock> {
        val screenshot = service.navigation.takeScreenshot()
        return service.ocrExtractor.extractText(screenshot)
    }

    /**
     * Find text anywhere on screen using OCR (even if not in accessibility tree).
     * Case-insensitive search with partial matching.
     */
    suspend fun ocrFindText(query: String): OcrTextBlock? {
        val screenshot = service.navigation.takeScreenshot()
        return service.ocrExtractor.findText(screenshot, query)
    }

    /**
     * Find all occurrences of text on screen using OCR.
     */
    suspend fun ocrFindAllText(query: String): List<OcrTextBlock> {
        val screenshot = service.navigation.takeScreenshot()
        return service.ocrExtractor.findAllText(screenshot, query)
    }

    /**
     * Find text using OCR and tap at its center.
     * Returns true if text was found and tapped, false otherwise.
     */
    suspend fun ocrTapText(query: String): Boolean {
        val block = ocrFindText(query) ?: return false
        val (x, y) = service.ocrExtractor.getCenterPoint(block)
        service.gestureHandler.performClick(x, y)
        return true
    }

    // ── Smart Wait tools (NEW - Phase 1) ─────────────────────────────────────

    /**
     * Wait until UI is idle (no events for specified duration).
     * Returns true if idle, false if timeout.
     */
    suspend fun waitUntilIdle(timeoutMs: Long = 5000, idleDurationMs: Long = 500): Boolean =
        service.smartWaiter.waitUntilIdle(timeoutMs, idleDurationMs)

    /**
     * Wait for a specific window (app/activity) to appear.
     */
    suspend fun waitForWindow(packageName: String, timeoutMs: Long = 5000): Boolean =
        service.smartWaiter.waitForWindow(packageName, timeoutMs)

    /**
     * Wait for a node with specific view ID to appear.
     */
    suspend fun waitForNode(viewId: String, timeoutMs: Long = 5000): NodeSnapshot? =
        service.smartWaiter.waitForNode(viewId, timeoutMs)

    /**
     * Wait for a node containing specific text to appear.
     */
    suspend fun waitForText(text: String, timeoutMs: Long = 5000): NodeSnapshot? =
        service.smartWaiter.waitForText(text, timeoutMs)

    /**
     * Wait for text in a specific node to change.
     */
    suspend fun waitForTextChange(viewId: String, timeoutMs: Long = 5000): String? =
        service.smartWaiter.waitForTextChange(viewId, timeoutMs)

    /**
     * Wait for a dialog to appear.
     */
    suspend fun waitForDialog(timeoutMs: Long = 5000): Boolean =
        service.smartWaiter.waitForDialog(timeoutMs)

    /**
     * Wait for a toast message containing specific text.
     */
    suspend fun waitForToast(text: String, timeoutMs: Long = 5000): String? =
        service.smartWaiter.waitForToast(text, timeoutMs)

    // ── Scroll tools (NEW - Phase 1) ─────────────────────────────────────────

    /**
     * Scroll until text is found.
     * Returns the found node, or null if not found after maxScrolls attempts.
     */
    suspend fun scrollUntilText(
        scrollableId: String,
        text: String,
        maxScrolls: Int = 20
    ): NodeSnapshot? =
        service.scrollHelper.scrollUntilText(scrollableId, text, maxScrolls)

    /**
     * Scroll until a view with specific ID is found.
     */
    suspend fun scrollUntilViewId(
        scrollableId: String,
        targetViewId: String,
        maxScrolls: Int = 20
    ): NodeSnapshot? =
        service.scrollHelper.scrollUntilViewId(scrollableId, targetViewId, maxScrolls)

    /**
     * Scroll to the top of a scrollable container.
     */
    suspend fun scrollToTop(scrollableId: String, maxScrolls: Int = 50) =
        service.scrollHelper.scrollToTop(scrollableId, maxScrolls)

    /**
     * Scroll to the bottom of a scrollable container.
     */
    suspend fun scrollToBottom(scrollableId: String, maxScrolls: Int = 50) =
        service.scrollHelper.scrollToBottom(scrollableId, maxScrolls)

    /**
     * Fling scroll to top (fast scroll with momentum).
     */
    suspend fun flingToTop(scrollableId: String) =
        service.scrollHelper.flingToTop(scrollableId)

    /**
     * Fling scroll to bottom (fast scroll with momentum).
     */
    suspend fun flingToBottom(scrollableId: String) =
        service.scrollHelper.flingToBottom(scrollableId)

    /**
     * Check if a scrollable container can scroll forward (down/right).
     */
    fun canScrollForward(scrollableId: String): Boolean =
        service.scrollHelper.canScrollForward(scrollableId)

    /**
     * Check if a scrollable container can scroll backward (up/left).
     */
    fun canScrollBackward(scrollableId: String): Boolean =
        service.scrollHelper.canScrollBackward(scrollableId)

    // ── Event streaming tools (NEW - Phase 1) ────────────────────────────────

    /**
     * Observe UI events in real-time.
     * Returns a Flow that emits UI changes (window switches, text updates, toasts, etc.)
     */
    fun observeUIEvents(): Flow<com.forge.autophone.events.UIEvent> =
        service.eventBus.events

    // ── Icon/Image recognition tools (NEW - Phase 2) ─────────────────────────

    /**
     * Register an icon template for future matching.
     * @param name Unique identifier for this icon
     * @param base64Image Base64-encoded PNG/JPEG of the icon
     */
    fun registerIcon(name: String, base64Image: String) =
        service.iconMatcher.registerIconFromBase64(name, base64Image)

    /**
     * Unregister an icon template.
     */
    fun unregisterIcon(name: String) =
        service.iconMatcher.unregisterIcon(name)

    /**
     * Get list of all registered icons.
     */
    fun getRegisteredIcons(): List<String> =
        service.iconMatcher.getRegisteredIcons()

    /**
     * Find an icon on screen using template matching.
     * @param name Name of the registered icon
     * @param threshold Confidence threshold (0.0 to 1.0, default 0.8)
     * @return Icon match with location and confidence, or null if not found
     */
    suspend fun findIcon(name: String, threshold: Double = 0.8): com.forge.autophone.vision.IconMatch? {
        val screenshot = service.navigation.takeScreenshot()
        return service.iconMatcher.findIcon(name, screenshot, threshold)
    }

    /**
     * Find all occurrences of an icon.
     * @param maxMatches Maximum number of matches to return (default 10)
     */
    suspend fun findAllIcons(name: String, threshold: Double = 0.8, maxMatches: Int = 10): List<com.forge.autophone.vision.IconMatch> {
        val screenshot = service.navigation.takeScreenshot()
        return service.iconMatcher.findAllIcons(name, screenshot, threshold, maxMatches)
    }

    /**
     * Find icon with scale invariance (searches at multiple scales).
     * Slower but more robust to size variations.
     */
    suspend fun findIconMultiScale(name: String, threshold: Double = 0.8): com.forge.autophone.vision.IconMatch? {
        val screenshot = service.navigation.takeScreenshot()
        return service.iconMatcher.findIconMultiScale(name, screenshot, threshold)
    }

    /**
     * Check if an icon is visible on screen.
     */
    suspend fun isIconVisible(name: String, threshold: Double = 0.8): Boolean {
        val screenshot = service.navigation.takeScreenshot()
        return service.iconMatcher.isIconVisible(name, screenshot, threshold)
    }

    /**
     * Find and tap an icon in one operation.
     * @return true if icon was found and tapped, false otherwise
     */
    suspend fun tapIcon(name: String, threshold: Double = 0.8): Boolean {
        val match = findIcon(name, threshold) ?: return false
        service.gestureHandler.performClick(match.centerX, match.centerY)
        return true
    }

    // ── Multi-touch gesture tools (NEW - Phase 2) ────────────────────────────

    /**
     * Pinch zoom in (two fingers moving apart).
     */
    fun pinchZoomIn(
        centerX: Float,
        centerY: Float,
        spreadStart: Float = 50f,
        spreadEnd: Float = 200f,
        durationMs: Long = 300
    ) = service.gestureHandler.performPinchZoomIn(centerX, centerY, spreadStart, spreadEnd, durationMs)

    /**
     * Pinch zoom out (two fingers moving together).
     */
    fun pinchZoomOut(
        centerX: Float,
        centerY: Float,
        spreadStart: Float = 200f,
        spreadEnd: Float = 50f,
        durationMs: Long = 300
    ) = service.gestureHandler.performPinchZoomOut(centerX, centerY, spreadStart, spreadEnd, durationMs)

    /**
     * Two-finger rotation gesture.
     * @param degrees Rotation angle in degrees (positive = clockwise)
     */
    fun rotate(
        centerX: Float,
        centerY: Float,
        radius: Float = 100f,
        degrees: Float,
        durationMs: Long = 300
    ) = service.gestureHandler.performRotate(centerX, centerY, radius, degrees, durationMs)

    /**
     * Two-finger tap (simultaneous tap at two points).
     */
    fun twoFingerTap(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Long = 100
    ) = service.gestureHandler.performTwoFingerTap(x1, y1, x2, y2, durationMs)

    /**
     * Three-finger swipe (useful for system gestures like app switching).
     */
    fun threeFingerSwipe(
        startY: Float,
        endY: Float,
        screenWidth: Float,
        durationMs: Long = 300
    ) = service.gestureHandler.performThreeFingerSwipe(startY, endY, screenWidth, durationMs)

    /**
     * Double tap (two quick taps at the same location).
     */
    fun doubleTap(x: Float, y: Float, delayBetweenTapsMs: Long = 100) =
        service.gestureHandler.performDoubleTap(x, y, delayBetweenTapsMs)

    // ── App context awareness tools (NEW - Phase 3) ──────────────────────────

    /**
     * Get the current app context (package, activity, screen type, UI patterns).
     */
    fun getCurrentAppContext(): com.forge.autophone.context.AppContext? =
        service.appContextTracker.getCurrentContext()

    /**
     * Get detected form fields with their types.
     */
    fun detectFormFields(): List<com.forge.autophone.context.DetectedField> =
        service.appContextTracker.detectFormFields()

    /**
     * Check if current screen is a login screen.
     */
    fun isLoginScreen(): Boolean {
        val context = service.appContextTracker.getCurrentContext()
        return context?.screenType == com.forge.autophone.context.ScreenType.LOGIN
    }

    /**
     * Check if current screen is a settings screen.
     */
    fun isSettingsScreen(): Boolean {
        val context = service.appContextTracker.getCurrentContext()
        return context?.screenType == com.forge.autophone.context.ScreenType.SETTINGS
    }

    /**
     * Check if a specific UI pattern is present.
     */
    fun hasUIPattern(pattern: com.forge.autophone.context.UIPattern): Boolean {
        val context = service.appContextTracker.getCurrentContext()
        return context?.uiPatterns?.contains(pattern) == true
    }

    /**
     * Get all detected UI patterns.
     */
    fun getUIPatterns(): List<com.forge.autophone.context.UIPattern> {
        val context = service.appContextTracker.getCurrentContext()
        return context?.uiPatterns ?: emptyList()
    }

    // ── Action verification tools (NEW - Phase 3) ─────────────────────────────

    /**
     * Tap with verification (checks that expected outcome occurred).
     */
    suspend fun tapVerified(
        x: Float,
        y: Float,
        expectedOutcome: com.forge.autophone.verification.ExpectedOutcome
    ): com.forge.autophone.verification.VerificationResult =
        service.actionVerifier.verifyTap(x, y, expectedOutcome)

    /**
     * Verify that text entry succeeded.
     */
    suspend fun verifyTextEntry(
        viewId: String,
        expectedText: String
    ): com.forge.autophone.verification.VerificationResult =
        service.actionVerifier.verifyTextEntry(viewId, expectedText)

    /**
     * Verify that scroll action succeeded.
     */
    suspend fun verifyScroll(
        scrollableId: String
    ): com.forge.autophone.verification.VerificationResult =
        service.actionVerifier.verifyScroll(scrollableId)

    /**
     * Detect if an error occurred after an action.
     */
    suspend fun detectError(): com.forge.autophone.verification.ErrorDetection =
        service.actionVerifier.detectError()

    /**
     * Create a state checkpoint for potential rollback.
     */
    fun createCheckpoint(): com.forge.autophone.verification.StateCheckpoint =
        service.actionVerifier.createCheckpoint()

    /**
     * Check if rollback is needed (compares current state with checkpoint).
     */
    fun shouldRollback(checkpoint: com.forge.autophone.verification.StateCheckpoint): Boolean =
        service.actionVerifier.shouldRollback(checkpoint)

    // ── UI hierarchy diffing tools (NEW - Phase 3) ────────────────────────────

    /**
     * Get diff between current UI and last snapshot.
     */
    fun getUIDiff(): com.forge.autophone.diff.UIDiff =
        service.uiTreeDiffer.diff()

    /**
     * Check if UI has changed since last snapshot.
     */
    fun hasUIChanges(): Boolean =
        service.uiTreeDiffer.hasChanges()

    /**
     * Check if a specific node changed.
     */
    fun didNodeChange(viewId: String): Boolean =
        service.uiTreeDiffer.didNodeChange(viewId)

    /**
     * Reset the UI differ baseline.
     */
    fun resetUIDiffer() =
        service.uiTreeDiffer.reset()

    /**
     * Update the UI differ baseline to current snapshot.
     */
    fun updateUIDiffBaseline() {
        val snapshot = com.forge.autophone.inspector.UITreeInspector(service).snapshot()
        service.uiTreeDiffer.updateBaseline(snapshot)
    }

    // ── Self-healing selector tools (NEW - Phase 4) ──────────────────────────

    /**
     * Find node using self-healing logic (adapts to UI changes).
     */
    fun findWithHealing(
        selector: com.forge.autophone.healing.SelectorSpec,
        confidenceThreshold: Double = 0.7
    ): com.forge.autophone.healing.HealingResult =
        service.selfHealingSelector.findWithHealing(selector, confidenceThreshold)

    /**
     * Clear healing history for a selector.
     */
    fun clearHealingHistory(selector: com.forge.autophone.healing.SelectorSpec) =
        service.selfHealingSelector.clearHistory(selector)

    /**
     * Get self-healing statistics.
     */
    fun getHealingStats(): com.forge.autophone.healing.HealingStats =
        service.selfHealingSelector.getStats()

    // ── Gesture recording tools (NEW - Phase 4) ──────────────────────────────

    /**
     * Start recording gestures.
     */
    fun startGestureRecording() =
        service.gestureRecorder.startRecording()

    /**
     * Stop recording and save gesture.
     */
    fun stopGestureRecording(name: String): com.forge.autophone.recording.RecordedGesture =
        service.gestureRecorder.stopRecording(name)

    /**
     * Check if currently recording gestures.
     */
    fun isRecordingGesture(): Boolean =
        service.gestureRecorder.isRecording()

    /**
     * Cancel gesture recording.
     */
    fun cancelGestureRecording() =
        service.gestureRecorder.cancelRecording()

    /**
     * Replay a recorded gesture.
     */
    suspend fun replayGesture(
        gesture: com.forge.autophone.recording.RecordedGesture,
        speedMultiplier: Float = 1.0f
    ): Boolean =
        service.gesturePlayer.replay(gesture, speedMultiplier)

    /**
     * Save gesture to library.
     */
    fun saveGesture(gesture: com.forge.autophone.recording.RecordedGesture) =
        service.gestureLibrary.saveGesture(gesture)

    /**
     * Get gesture from library.
     */
    fun getGesture(name: String): com.forge.autophone.recording.RecordedGesture? =
        service.gestureLibrary.getGesture(name)

    /**
     * List all saved gestures.
     */
    fun listGestures(): List<String> =
        service.gestureLibrary.listGestures()

    /**
     * Delete a gesture from library.
     */
    fun deleteGesture(name: String): Boolean =
        service.gestureLibrary.deleteGesture(name)

    // ── Advanced form automation tools (NEW - Phase 4) ───────────────────────

    /**
     * Auto-fill entire form using provided data.
     */
    suspend fun autoFillForm(formData: Map<String, String>): com.forge.autophone.form.FormFillResult =
        service.formAutomation.autoFillForm(formData)

    /**
     * Detect all form fields in current screen.
     */
    fun detectFormFieldsAdvanced(): List<com.forge.autophone.form.FormField> =
        service.formAutomation.detectFormFields()

    /**
     * Fill a single form field with validation.
     */
    suspend fun fillFormField(
        field: com.forge.autophone.form.FormField,
        value: String
    ): com.forge.autophone.form.FieldFillResult =
        service.formAutomation.fillField(field, value)

    /**
     * Validate form before submission.
     */
    fun validateForm(): com.forge.autophone.form.FormValidationResult =
        service.formAutomation.validateForm()

    /**
     * Find and tap submit button.
     */
    suspend fun submitForm(): Boolean =
        service.formAutomation.submitForm()

    // ── ScreenAI tools (NEW - Phase 4) ───────────────────────────────────────

    /**
     * Ask natural language question about screen.
     */
    suspend fun askAboutScreen(
        question: String
    ): com.forge.autophone.vision.ScreenAIResponse {
        val screenshot = service.navigation.takeScreenshot()
        return service.screenAI.askAboutScreen(screenshot, question)
    }

    /**
     * Find element by natural language description.
     */
    suspend fun findElementByDescription(
        description: String
    ): com.forge.autophone.vision.ElementLocation? {
        val screenshot = service.navigation.takeScreenshot()
        return service.screenAI.findElementByDescription(screenshot, description)
    }

    /**
     * Describe entire screen semantically.
     */
    suspend fun describeScreen(): com.forge.autophone.vision.ScreenDescription {
        val screenshot = service.navigation.takeScreenshot()
        return service.screenAI.describeScreen(screenshot)
    }

    /**
     * Classify screen type using vision.
     */
    suspend fun classifyScreenVisually(): com.forge.autophone.vision.ScreenClassification {
        val screenshot = service.navigation.takeScreenshot()
        return service.screenAI.classifyScreenType(screenshot)
    }

    /**
     * Detect visual anomalies (errors, loading, etc.).
     */
    suspend fun detectVisualAnomalies(): List<com.forge.autophone.vision.VisualAnomaly> {
        val screenshot = service.navigation.takeScreenshot()
        return service.screenAI.detectAnomalies(screenshot)
    }

    /**
     * Check if ScreenAI is available.
     */
    fun isScreenAIAvailable(): Boolean =
        service.screenAI.isAvailable()

    // ── Telemetry tools (NEW - Phase 4) ──────────────────────────────────────

    /**
     * Get all telemetry metrics.
     */
    fun getTelemetryMetrics(): List<com.forge.autophone.telemetry.ToolCallMetric> =
        service.telemetry.getMetrics()

    /**
     * Get metrics for specific tool.
     */
    fun getToolMetrics(toolName: String): List<com.forge.autophone.telemetry.ToolCallMetric> =
        service.telemetry.getMetricsForTool(toolName)

    /**
     * Get session metrics.
     */
    fun getSessionMetrics(): List<com.forge.autophone.telemetry.SessionMetrics> =
        service.telemetry.getSessionMetrics()

    /**
     * Get overall statistics.
     */
    fun getOverallStats(): com.forge.autophone.telemetry.OverallStats =
        service.telemetry.getOverallStats()

    /**
     * Get error statistics.
     */
    fun getErrorStats(): com.forge.autophone.telemetry.ErrorStats =
        service.telemetry.getErrorStats()

    /**
     * Get performance report.
     */
    fun getPerformanceReport(): com.forge.autophone.telemetry.PerformanceReport =
        service.telemetry.getPerformanceReport()

    /**
     * Export telemetry to JSON.
     */
    fun exportTelemetryJson(): String =
        service.telemetry.exportToJson()

    /**
     * Get real-time performance snapshot.
     */
    fun getRealTimeSnapshot(): com.forge.autophone.telemetry.RealTimeSnapshot =
        service.telemetry.getRealTimeSnapshot()

    /**
     * Clear all telemetry data.
     */
    fun clearTelemetry() =
        service.telemetry.clear()
}