package com.forge.autophone.toolregistry

import com.forge.autophone.AutoPhoneAccessibilityService

/**
 * AutoPhoneToolRegistry — binds the Forge OS ReAct agent tool interface to
 * the live [AutoPhoneAccessibilityService] instance.
 *
 * Each tool maps to one accessibility capability:
 *  - tap / swipe / long_press   → GestureHandler
 *  - type_text / clear_field    → TextEntryService
 *  - navigate / screenshot      → NavigationActions
 *  - find_node / get_tree       → service.findById / getActiveWindowRoot
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

    fun findById(viewId: String) = service.findById(viewId)
    fun findByText(text: String) = service.findByText(text)
    fun getActiveWindowRoot() = service.getActiveWindowRoot()
}