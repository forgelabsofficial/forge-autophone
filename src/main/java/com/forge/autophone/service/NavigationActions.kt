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

    /**
     * Screenshot service for MediaProjection-based capture.
     * Must be initialized separately with user permission.
     */
    var screenshotService: ScreenshotService? = null

    fun back() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

    fun home() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

    fun recents() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

    fun notifications() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

    fun quickSettings() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

    fun lockScreen() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    /**
     * Take a screenshot and return as Bitmap.
     * 
     * If ScreenshotService is initialized (MediaProjection permission granted),
     * uses that for high-quality capture. Otherwise, triggers system screenshot
     * and returns a placeholder.
     * 
     * To enable MediaProjection:
     * 1. Create ScreenshotService
     * 2. Request permission via requestScreenshotPermission()
     * 3. Initialize with result
     * 4. Set screenshotService property
     * 
     * @return Bitmap of screen, or 1x1 placeholder if MediaProjection not available
     */
    fun takeScreenshot(): Bitmap {
        // Try MediaProjection-based capture if available
        screenshotService?.let { service ->
            if (service.isReady()) {
                val bitmap = service.captureScreenshotSync()
                if (bitmap != null) {
                    return bitmap
                }
            }
        }
        
        // Fallback: Trigger system screenshot (saves to gallery)
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        
        // Return placeholder - for OCR/icon matching to work without MediaProjection,
        // alternative approaches like AccessibilityNodeInfo tree analysis should be used
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
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