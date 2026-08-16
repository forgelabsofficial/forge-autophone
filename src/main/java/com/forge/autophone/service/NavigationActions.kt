package com.forge.autophone.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi

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
     * Take a screenshot and return as Bitmap.
     * 
     * Note: This is a simplified implementation that triggers the system screenshot.
     * For actual bitmap capture, MediaProjection API would be needed (requires user permission).
     * 
     * Returns a placeholder 1x1 bitmap for now. Real implementation requires:
     * 1. MediaProjection permission from user
     * 2. ImageReader to capture screen content
     * 3. Async callback handling
     */
    fun takeScreenshot(): Bitmap {
        // Trigger system screenshot (this saves to gallery)
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        
        // Return placeholder bitmap
        // TODO: Implement MediaProjection-based screenshot capture
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