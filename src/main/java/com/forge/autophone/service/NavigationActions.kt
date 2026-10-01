package com.forge.autophone.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap

/**
 * NavigationActions — system-level navigation for the Forge OS accessibility layer.
 *
 * Wraps [AccessibilityService.performGlobalAction] to give the agent runtime
 * clean, typed navigation primitives.
 */
class NavigationActions(private val service: AccessibilityService) {

    fun back() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

    fun home() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

    fun recents() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

    fun notifications() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

    fun quickSettings() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

    fun lockScreen() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    /**
     * Take a screenshot of the current screen.
     *
     * Requires MediaProjection consent, which the user grants from the
     * "Screen & Background" card in MainActivity; the live instance is shared
     * through [ScreenshotHub].
     *
     * @return Bitmap of the screen
     * @throws IllegalStateException when capture consent has not been granted.
     *   Callers should prefer the accessibility tree (get_tree / find_by_text)
     *   for text - it needs no capture permission and is far cheaper.
     */
    fun takeScreenshot(): Bitmap {
        // The shared ScreenshotService is registered by MainActivity once the
        // user grants MediaProjection consent. This used to read a local
        // `screenshotService` property that nothing ever assigned, so every
        // call fell through to the placeholder below and OCR silently
        // returned nothing.
        ScreenshotHub.readyService()?.let { capture ->
            capture.captureScreenshotSync()?.let { return it }
        }

        // No projection available.
        //
        // The previous fallback called GLOBAL_ACTION_TAKE_SCREENSHOT - which
        // saves a copy into the user's gallery - and then returned a 1x1
        // blank bitmap. That had two bad properties: it polluted the gallery
        // every time a tool asked for the screen, and it made the failure
        // look like "the screen is empty" rather than "capture is not set up".
        //
        // Screen content that apps expose to accessibility does not need pixels
        // at all - use getActiveWindowRoot()/findByText() for that. So the
        // honest answer here is to throw and let the caller report a real
        // reason.
        throw IllegalStateException(
            "Screen capture unavailable: MediaProjection consent not granted. " +
                "Ask the user to open AutoPhone and allow Screen capture in the " +
                "'Screen & Background' card. For text, prefer the accessibility " +
                "tree (get_tree/find_by_text) which needs no capture permission."
        )
    }

    /** Perform any action by its raw [GlobalAction] enum. */
    fun perform(action: GlobalAction) = when (action) {
        GlobalAction.BACK -> back()
        GlobalAction.HOME -> home()
        GlobalAction.RECENTS -> recents()
        GlobalAction.NOTIFICATIONS -> notifications()
        GlobalAction.QUICK_SETTINGS -> quickSettings()
        GlobalAction.LOCK_SCREEN -> lockScreen()
        GlobalAction.SCREENSHOT -> { takeScreenshot(); true }
    }
}

enum class GlobalAction {
    BACK,
    HOME,
    RECENTS,
    NOTIFICATIONS,
    QUICK_SETTINGS,
    LOCK_SCREEN,
    SCREENSHOT
}
