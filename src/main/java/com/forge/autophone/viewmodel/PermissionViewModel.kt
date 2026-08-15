package com.forge.autophone.viewmodel

import androidx.lifecycle.ViewModel
import com.forge.autophone.AutoPhoneAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * PermissionViewModel — drives the [PermissionSettingsScreen].
 *
 * Checks live status of required AutoPhone permissions and exposes
 * actions to open the system settings screens that grant them.
 */
@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    /** True if the AutoPhone AccessibilityService is currently connected. */
    val isAccessibilityEnabled: Boolean
        get() = AutoPhoneAccessibilityService.instance != null

    /** True if SYSTEM_ALERT_WINDOW overlay permission is granted. */
    val isOverlayEnabled: Boolean
        get() = android.provider.Settings.canDrawOverlays(
            AutoPhoneAccessibilityService.instance
        )

    fun setAccessibilityPermission(enabled: Boolean) {
        // Programmatic enable is not allowed by Android — open system settings.
        // The UI should navigate to Settings.ACTION_ACCESSIBILITY_SETTINGS instead.
    }

    fun setOverlayPermission(enabled: Boolean) {
        // Same: navigate to Settings.ACTION_MANAGE_OVERLAY_PERMISSION.
    }

    fun requestAllPermissions() {
        // Signal to UI to open both settings screens in sequence.
    }
}
