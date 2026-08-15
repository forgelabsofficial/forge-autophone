package com.forge.autophone.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forge.autophone.viewmodel.PermissionViewModel

/**
 * PermissionSettingsScreen — Compose UI for managing AutoPhone system permissions.
 *
 * Shows live status of:
 *  - Accessibility Service (BIND_ACCESSIBILITY_SERVICE)
 *  - Display Over Other Apps (SYSTEM_ALERT_WINDOW)
 *
 * Both permissions require navigating to system Settings screens — Android does
 * not allow them to be granted programmatically.
 */
@Composable
fun PermissionSettingsScreen(
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Refresh status on every recomposition (e.g. when returning from Settings)
    val accessibilityEnabled by remember { derivedStateOf { viewModel.isAccessibilityEnabled } }
    val overlayEnabled by remember { derivedStateOf { viewModel.isOverlayEnabled } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "AutoPhone Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "AutoPhone requires the following permissions to operate as Forge OS's accessibility layer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // ── Accessibility Service ────────────────────────────────────────────
        PermissionRow(
            icon = Icons.Default.Accessibility,
            title = "Accessibility Service",
            description = "Allows AutoPhone to read the UI tree and dispatch gestures.",
            isEnabled = accessibilityEnabled,
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        // ── Overlay Permission ───────────────────────────────────────────────
        PermissionRow(
            icon = Icons.Default.LayersClear,
            title = "Display Over Other Apps",
            description = "Required for AutoPhone's on-screen agent overlay.",
            isEnabled = overlayEnabled,
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                )
            }
        )

        Spacer(Modifier.weight(1f))

        if (!accessibilityEnabled || !overlayEnabled) {
            Button(
                onClick = {
                    // Open accessibility settings first
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant All Permissions")
            }
        } else {
            FilledTonalButton(
                onClick = { /* All good — navigate back */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✓ All Permissions Granted")
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isEnabled) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isEnabled) {
            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("ON", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            OutlinedButton(onClick = onOpenSettings) {
                Text("Enable", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}