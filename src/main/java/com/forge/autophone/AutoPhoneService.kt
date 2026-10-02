package com.forge.autophone

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.forge.autophone.aidl.AidlToolMapper
import com.forge.autophone.aidl.jsonEscape
import com.forge.autophone.aidl.runBlockingIo
import com.forge.autophone.aidl.toJson
import com.forge.autophone.aidl.ScreenshotEncoder
import com.forge.autophone.service.ScreenshotHub
import com.forge.autophone.toolregistry.AutoPhoneToolRegistry
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * AutoPhone AIDL Service
 * 
 * Exposes AutoPhone's automation capabilities to Forge OS via AIDL binding.
 * 
 * Forge OS connects to this service using:
 *   Intent("com.forge.autophone.IAutoPhoneService")
 *     .setPackage("com.forge.autophone")
 * 
 * This service acts as a bridge between AIDL method calls and the
 * AutoPhoneToolRegistry, formatting responses as JSON for Forge OS.
 * 
 * Note: The accessibility service must be enabled for most operations to work.
 * Methods will return error responses if the service is not available.
 */
@AndroidEntryPoint
class AutoPhoneService : Service() {
    
    /**
     * Get the tool registry if the accessibility service is running.
     * Returns null if the service is not enabled.
     */
    private fun getToolRegistry(): AutoPhoneToolRegistry? {
        val service = AutoPhoneAccessibilityService.instance ?: return null
        return AutoPhoneToolRegistry(service)
    }
    
    /**
     * Execute a tool operation, handling the case where the service is not available.
     */
    private fun <T> withToolRegistry(operation: (AutoPhoneToolRegistry) -> T, onError: () -> T): T {
        val registry = getToolRegistry()
        return if (registry != null) {
            try {
                operation(registry)
            } catch (t: Throwable) {
                // Tool implementations may throw - e.g. screen capture without
                // MediaProjection consent. Never let that cross the AIDL
                // boundary as a raw RemoteException; report it as a tool error.
                Timber.e(t, "AutoPhone tool failed")
                onError()
            }
        } else {
            onError()
        }
    }

    /**
     * Suspend counterpart to [withToolRegistry] for tools whose registry method
     * is itself suspend (the OCR and icon-search paths). Kept separate so the
     * synchronous callers keep a non-suspending signature.
     */
    private suspend fun <T> withToolRegistrySuspend(
        operation: suspend (AutoPhoneToolRegistry) -> T,
        onError: () -> T,
    ): T {
        val registry = getToolRegistry()
        return if (registry != null) {
            try {
                operation(registry)
            } catch (t: Throwable) {
                Timber.e(t, "AutoPhone tool failed")
                onError()
            }
        } else {
            onError()
        }
    }
    
    private val binder = object : IAutoPhoneService.Stub() {
        
        // ── Screen-control tools ──────────────────────────────────────────────
        
        override fun readScreen(): String {
            return withToolRegistry(
                operation = { AidlToolMapper.getAllNodes(it) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun tapByText(text: String): String {
            return withToolRegistry(
                operation = { AidlToolMapper.findAndClickText(it, text) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun tapAt(x: Int, y: Int): String {
            return withToolRegistry(
                operation = { AidlToolMapper.clickAt(it, x, y) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun typeText(text: String): String {
            return withToolRegistry(
                operation = { AidlToolMapper.typeText(it, text) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun swipe(direction: String, amount: Int): String {
            return withToolRegistry(
                operation = { AidlToolMapper.swipe(it, direction, amount) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun scroll(direction: String): String {
            return withToolRegistry(
                operation = { AidlToolMapper.scroll(it, direction) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun launchApp(packageOrLabel: String): String {
            return errorJson("Launch app functionality not yet implemented")
        }
        
        override fun goBack(): String {
            return withToolRegistry(
                operation = { AidlToolMapper.goBack(it) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun goHome(): String {
            return withToolRegistry(
                operation = { AidlToolMapper.goHome(it) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun openNotifications(): String {
            return withToolRegistry(
                operation = { AidlToolMapper.openNotifications(it) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun screenshot(): String {
            return withToolRegistry(
                operation = { registry ->
                    ScreenshotEncoder.toJson(registry.screenshot())
                },
                onError = {
                    // Two distinct failures land here and the user needs to know
                    // which. withToolRegistry converts the IllegalStateException
                    // from takeScreenshot() (no MediaProjection consent) into this
                    // path, so check readiness to tell them apart.
                    if (ScreenshotHub.isReady()) {
                        errorJson("Accessibility service not enabled")
                    } else {
                        errorJson(
                            "Screen capture not permitted. Open AutoPhone and allow " +
                                "'Screen capture' in the Screen & Background card, then retry."
                        )
                    }
                }
            )
        }
        
        override fun findAndTap(text: String): String {
            return withToolRegistry(
                operation = { AidlToolMapper.findAndClickText(it, text) },
                onError = { errorJson("Accessibility service not enabled") }
            )
        }
        
        override fun isServiceActive(): Boolean {
            return AutoPhoneAccessibilityService.instance != null
        }
        
        // ── Notification tools ────────────────────────────────────────────────
        
        override fun readNotifications(): String {
            val listener = com.forge.autophone.service.AutoPhoneNotificationListener.instance
            return if (listener != null) {
                listener.readNotificationsJson()
            } else {
                errorJson("Notification listener not enabled. Enable in Settings > Notifications > Notification Access")
            }
        }
        
        override fun dismissNotification(key: String): String {
            val listener = com.forge.autophone.service.AutoPhoneNotificationListener.instance
            return if (listener != null) {
                val success = listener.dismissNotification(key)
                if (success) {
                    successJson("Notification dismissed")
                } else {
                    errorJson("Failed to dismiss notification")
                }
            } else {
                errorJson("Notification listener not enabled")
            }
        }
        
        override fun replyToNotification(key: String, text: String): String {
            val listener = com.forge.autophone.service.AutoPhoneNotificationListener.instance
            return if (listener != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    val success = listener.replyToNotification(key, text)
                    if (success) {
                        successJson("Reply sent")
                    } else {
                        errorJson("Failed to send reply - notification may not support replies")
                    }
                } else {
                    errorJson("Reply feature requires Android 7.0 (API 24) or higher")
                }
            } else {
                errorJson("Notification listener not enabled")
            }
        }
        
        override fun isNotificationListenerActive(): Boolean {
            return com.forge.autophone.service.AutoPhoneNotificationListener.instance != null
        }

// OCR. Registry exposes these as suspend; AIDL methods are synchronous,
        // so bridge on an IO dispatcher. runBlocking is safe here because
        // binder transactions already arrive off the main thread.

        override fun ocrReadScreen(): String = runBlockingIo {
            withToolRegistrySuspend(
                operation = { registry ->
                    val blocks = registry.ocrReadScreen()
                    """{"ok":true,"blocks":${blocks.joinToString(",") { it.toJson() }}}"""
                },
                onError = { errorJson(captureOrServiceError()) }
            )
        }

        override fun ocrFindText(query: String): String = runBlockingIo {
            withToolRegistrySuspend(
                operation = { registry ->
                    val block = registry.ocrFindText(query)
                        ?: return@withToolRegistrySuspend """{"ok":false,"error":"not found on screen"}"""
                    """{"ok":true,"block":${block.toJson()}}"""
                },
                onError = { errorJson(captureOrServiceError()) }
            )
        }

        override fun ocrFindAllText(query: String): String = runBlockingIo {
            withToolRegistrySuspend(
                operation = { registry ->
                    val blocks = registry.ocrFindAllText(query)
                    """{"ok":true,"blocks":${blocks.joinToString(",") { it.toJson() }}}"""
                },
                onError = { errorJson(captureOrServiceError()) }
            )
        }

        override fun ocrTapText(query: String): Boolean = runBlockingIo {
            withToolRegistrySuspend(
                operation = { registry -> registry.ocrTapText(query) },
                onError = { false }
            )
        }

        // Icon templates. Registering does not need capture; finding does.

        override fun registerIcon(name: String, base64Image: String): String =
            withToolRegistry(
                operation = { registry ->
                    registry.registerIcon(name, base64Image)
                    """{"ok":true,"name":"${name.jsonEscape()}"}"""
                },
                onError = { errorJson("Could not register icon - is base64Image valid PNG/JPEG?") }
            )

        override fun unregisterIcon(name: String): String =
            withToolRegistry(
                operation = { registry ->
                    registry.unregisterIcon(name)
                    """{"ok":true,"name":"${name.jsonEscape()}"}"""
                },
                onError = { errorJson("Could not unregister icon") }
            )

        override fun listIcons(): String =
            withToolRegistry(
                operation = { registry ->
                    val names = registry.getRegisteredIcons()
                    """{"ok":true,"icons":[${names.joinToString(",") { "\"${it.jsonEscape()}\"" }}]}"""
                },
                onError = { errorJson("Could not list icons") }
            )

        override fun findIcon(name: String, threshold: Double): String = runBlockingIo {
            withToolRegistrySuspend(
                operation = { registry ->
                    val match = registry.findIcon(name, threshold)
                        ?: return@withToolRegistrySuspend """{"ok":false,"error":"icon not found"}"""
                    """{"ok":true,"match":${match.toJson()}}"""
                },
                onError = { errorJson(captureOrServiceError()) }
            )
        }

        override fun findAllIcons(name: String, threshold: Double, maxMatches: Int): String =
            runBlockingIo {
                withToolRegistrySuspend(
                    operation = { registry ->
                        val matches = registry.findAllIcons(name, threshold, maxMatches)
                        """{"ok":true,"matches":[${matches.joinToString(",") { it.toJson() }}]}"""
                    },
                    onError = { errorJson(captureOrServiceError()) }
                )
            }

        override fun isIconVisible(name: String, threshold: Double): Boolean = runBlockingIo {
            withToolRegistrySuspend(
                operation = { registry -> registry.isIconVisible(name, threshold) },
                onError = { false }
            )
        }

        override fun describeContext(): String =
            withToolRegistry(
                operation = { registry -> registry.describeContextJson() },
                onError = { errorJson("Accessibility service not enabled") }
            )

        
        // ── Schedule lifecycle ────────────────────────────────────────────────
        
        override fun notifyScheduleStarted(scheduleId: String, planSummary: String) {
            Timber.i("Schedule started: $scheduleId - $planSummary")
        }
        
        override fun notifyScheduleCompleted(scheduleId: String, ok: Boolean, result: String) {
            Timber.i("Schedule completed: $scheduleId - ok=$ok")
        }
    }
    
    // ── Helpers ───────────────────────────────────────────────────────────
    
    private fun successJson(output: String, message: String? = null): String {
        return try {
            buildString {
                append("{\"ok\":true,\"output\":\"")
                append(escape(output))
                append("\"")
                if (message != null) {
                    append(",\"message\":\"")
                    append(escape(message))
                    append("\"")
                }
                append("}")
            }
        } catch (e: Exception) {
            """{"ok":true,"output":"$output"}"""
        }
    }

    /**
    * Distinguish the two failure modes the OCR/icon tools hit: no capture
    * consent, versus the accessibility service being off. withToolRegistry
    * funnels both here, so check readiness to tell the user which to fix.
    */
    private fun captureOrServiceError(): String =
        if (ScreenshotHub.isReady()) {
            "Accessibility service not enabled"
        } else {
            "Screen capture not permitted. Open AutoPhone and allow " +
                "'Screen capture' in the Screen & Background card, then retry."
        }

    
    private fun errorJson(error: String): String {
        return """{"ok":false,"error":"${escape(error)}"}"""
    }
    
    private fun escape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    override fun onBind(intent: Intent): IBinder {
        Timber.i("AutoPhone AIDL service bound by ${intent.`package`}")
        return binder
    }
    
    override fun onUnbind(intent: Intent): Boolean {
        Timber.i("AutoPhone AIDL service unbound")
        return super.onUnbind(intent)
    }
}
