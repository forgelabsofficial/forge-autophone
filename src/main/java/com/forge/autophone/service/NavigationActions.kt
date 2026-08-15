package com.forge.autophone.service

import android.accessibilityservice.AccessibilityService

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

    fun takeScreenshot() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)

    /** Perform any action by its raw [GlobalAction] enum. */
    fun perform(action: GlobalAction) = when (action) {
        GlobalAction.BACK -> back()
        GlobalAction.HOME -> home()
        GlobalAction.RECENTS -> recents()
        GlobalAction.NOTIFICATIONS -> notifications()
        GlobalAction.QUICK_SETTINGS -> quickSettings()
        GlobalAction.LOCK_SCREEN -> lockScreen()
        GlobalAction.SCREENSHOT -> takeScreenshot()
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