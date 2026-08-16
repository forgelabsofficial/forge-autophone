package com.forge.autophone

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.forge.autophone.aidl.AidlToolMapper
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
            operation(registry)
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
            return errorJson("Screenshot requires MediaProjection permission")
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
            return successJson("[]", "Notification listener not yet implemented")
        }
        
        override fun dismissNotification(key: String): String {
            return errorJson("Notification dismiss not yet implemented")
        }
        
        override fun replyToNotification(key: String, text: String): String {
            return errorJson("Notification reply not yet implemented")
        }
        
        override fun isNotificationListenerActive(): Boolean {
            return false
        }
        
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
