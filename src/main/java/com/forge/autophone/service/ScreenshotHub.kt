package com.forge.autophone.service

import android.util.Log

/**
 * Process-wide holder for the single [ScreenshotService] instance.
 *
 * ## Why this exists
 *
 * Screen capture is needed from two places that never meet directly:
 *
 *  - [com.forge.autophone.ui.MainActivity], which owns the MediaProjection
 *    consent flow and calls `initialize()` on the user's answer.
 *  - [com.forge.autophone.service.NavigationActions.takeScreenshot], called
 *    from the accessibility service and from Forge OS over AIDL.
 *
 * `NavigationActions.screenshotService` was declared but **never assigned**,
 * so every capture fell through to a 1x1 placeholder bitmap and OCR silently
 * returned nothing. Giving each side its own instance does not work either:
 * MediaProjection is a single system-granted resource, and the token cannot be
 * replayed into a second instance.
 *
 * So: one instance, created lazily, shared by both.
 */
object ScreenshotHub {

    private const val TAG = "AutoPhone"

    @Volatile
    private var instance: ScreenshotService? = null

    /**
     * The shared capture service, or null before the user grants consent.
     * Safe to call from any thread.
     */
    fun get(): ScreenshotService? = instance

    /**
     * Register the live instance. Called by MainActivity once
     * [ScreenshotService.initialize] succeeds.
     */
    fun set(service: ScreenshotService) {
        instance = service
        Log.i(TAG, "ScreenshotService shared instance registered")
    }

    /**
     * Drop the shared instance (projection stopped or revoked). Any later
     * [get] returns null so callers fail loudly instead of using a stale
     * projection that yields no frames.
     */
    fun clear() {
        instance = null
        Log.i(TAG, "ScreenshotService shared instance cleared")
    }

    /** True when a real capture is available. */
    fun isReady(): Boolean = instance?.isReady() == true

    /**
     * The shared service only if it is actually ready to produce frames, else
     * null. Callers use this to fail loudly rather than reading a blank frame.
     */
    fun readyService(): ScreenshotService? = instance?.takeIf { it.isReady() }
}
