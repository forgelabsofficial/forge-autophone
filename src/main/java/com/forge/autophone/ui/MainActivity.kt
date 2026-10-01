package com.forge.autophone.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.forge.autophone.service.ScreenshotHub
import com.forge.autophone.service.ScreenshotService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

/**
 * AutoPhone Main Activity
 * 
 * Standalone app UI for testing and demonstrating AutoPhone capabilities.
 * Provides access to all 78 automation tools, telemetry, and configuration.
 * 
 * Shows real-time accessibility service status and guides user through setup.
 * AutoPhone integrates with Forge OS's existing External API permission system.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * MediaProjection capture consent. This is the single source of Bitmaps for
     * every vision tool (OCR, icon matching, screen AI) - without it those tools
     * silently return nothing.
     */
    private val requestCapture = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        captureGranted = result.resultCode == RESULT_OK &&
            result.data != null &&
            screenshotService.initialize(result.resultCode, result.data)

        // Share the live instance with the accessibility service, which is a
        // different component and previously had no way to reach it.
        if (captureGranted) ScreenshotHub.set(screenshotService) else ScreenshotHub.clear()
    }

    /** Android 13+ prompt so the foreground-service notification can show. */
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* the service still runs even if this is denied */ }

    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    private lateinit var screenshotService: ScreenshotService

    private var captureGranted by mutableStateOf(false)
    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenshotService = ScreenshotService(appContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AutoPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isServiceEnabled = serviceEnabled,
                        isCaptureGranted = captureGranted,
                        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onRequestScreenCapture = ::requestScreenCapture,
                        onOpenBatterySettings = ::requestIgnoreBatteryOptimizations,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have just returned from Settings having enabled the
        // accessibility service, so re-read both grants on every resume.
        serviceEnabled = appContext.isAccessibilityServiceEnabled()
        captureGranted = ScreenshotHub.isReady()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** Launch the system screen-capture consent dialog. */
    private fun requestScreenCapture() {
        try {
            requestCapture.launch(screenshotService.requestScreenshotPermission())
        } catch (e: Exception) {
            Timber.e(e, "Could not request screen capture")
        }
    }

    /**
     * Ask to be exempt from battery optimisation.
     *
     * An automation tool is backgrounded almost all of the time, and OEMs like
     * Xiaomi/Oppo/Huawei kill long-running accessibility services without this.
     * That is a common cause of "it worked for a while, then stopped".
     */
    private fun requestIgnoreBatteryOptimizations() {
        if (isIgnoringBatteryOptimizations()) return
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (e: Exception) {
            // Not every OEM exposes the direct request; fall back to the list.
            runCatching {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            Timber.e(e, "Direct battery-optimisation request unavailable")
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean =
        runCatching {
            getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(packageName) ?: false
        }.getOrDefault(false)
}

/**
 * Check if AutoPhone accessibility service is enabled
 */
fun Context.isAccessibilityServiceEnabled(): Boolean {
    val service = "com.forge.autophone/com.forge.autophone.AutoPhoneAccessibilityService"
    
    return try {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        enabledServices.contains(service)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun MainScreen(
    isServiceEnabled: Boolean,
    isCaptureGranted: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestScreenCapture: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "🤖 AutoPhone",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "AI-Powered Accessibility Automation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Service Status Card
        ServiceStatusCard(
            isEnabled = isServiceEnabled,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings
        )
        
        Spacer(modifier = Modifier.height(24.dp))
Spacer(modifier = Modifier.height(24.dp))

        // Screen capture + background running. Without these the vision tools
        // (OCR, icon matching) have no way to obtain a Bitmap, and the
        // accessibility service gets killed once the app is backgrounded.
        PermissionCard(
            isCaptureGranted = isCaptureGranted,
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
            onRequestScreenCapture = onRequestScreenCapture,
            onOpenBatterySettings = onOpenBatterySettings,
        )

        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.height(24.dp))
        
        // Forge OS Integration Card
        ForgeOSIntegrationCard(isServiceEnabled = isServiceEnabled)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Capabilities Overview
        CapabilitiesCard()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Actions
        QuickActionsCard()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info Card
        InfoCard()
    }
}

@Composable
fun ServiceStatusCard(
    isEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Service Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnabled) "🟢" else "⚪",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnabled) "Service Running" else "Service Not Enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isEnabled) {
                Button(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Accessibility Service")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enable AutoPhone in Accessibility Settings to use automation tools",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Service is enabled
                Text(
                    text = "✓ All 78 automation tools are now available",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Accessibility Settings")
                }
            }
        }
    }
}

@Composable
fun ForgeOSIntegrationCard(isServiceEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔗 Forge OS Integration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!isServiceEnabled) {
                Text(
                    text = "⚠️ Service not enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enable the accessibility service above to allow Forge OS to use AutoPhone's 78 automation tools.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "✓ Ready for Forge OS",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "AutoPhone is ready! Forge OS can now connect via AIDL and use all 78 automation tools.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "How it works:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "1. AutoPhone provides accessibility tools\n" +
                      "2. Forge OS connects via AIDL binding\n" +
                      "3. Forge OS manages permissions via ExternalApiBridge\n" +
                      "4. AutoPhone shows as trusted app in Forge OS settings\n" +
                      "5. All access is logged and auditable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📋 Permission Management",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AutoPhone doesn't manage permissions. Forge OS handles all external app permissions through its ExternalApiBridge system.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CapabilitiesCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CapabilityItem("78", "Automation Tools")
            CapabilityItem("4", "Phases Implemented")
            CapabilityItem("✓", "OCR Text Recognition")
            CapabilityItem("✓", "Icon Matching")
            CapabilityItem("✓", "Smart Gestures")
            CapabilityItem("✓", "Self-Healing Selectors")
            CapabilityItem("✓", "Form Automation")
            CapabilityItem("✓", "Performance Telemetry")
        }
    }
}

@Composable
fun CapabilityItem(value: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun QuickActionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚡ Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Tool testing UI coming soon!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Future features:\n" +
                      "• Test all 78 automation tools\n" +
                      "• Record and replay gestures\n" +
                      "• View OCR results\n" +
                      "• Monitor performance\n" +
                      "• Configure settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ℹ️ About AutoPhone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "AutoPhone is an AI-powered accessibility automation layer " +
                      "that provides 78 tools for UI inspection, gesture control, " +
                      "text recognition, and form automation.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Version 1.0.0 • Built with Kotlin & Jetpack Compose",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AutoPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = dynamicDarkColorScheme(),
        content = content
    )
}

@Composable
fun dynamicDarkColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = MaterialTheme.colorScheme.primary,
        secondary = MaterialTheme.colorScheme.secondary,
        tertiary = MaterialTheme.colorScheme.tertiary
    )
}

/**
 * Screen capture + background running.
 *
 * These two grants are what make AutoPhone actually usable rather than
 * merely installed:
 *
 *  - **Screen capture** — every vision tool (OCR, icon matching, screen AI)
 *    takes a `Bitmap`, and MediaProjection is the only source of one. Without
 *    it those tools return nothing at all, with no obvious error.
 *  - **Background running** — an automation tool is backgrounded almost always.
 *    Aggressive OEMs kill the accessibility service (and with it any live
 *    projection) unless the app is exempt from battery optimisation.
 */
@Composable
fun PermissionCard(
    isCaptureGranted: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestScreenCapture: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    val allGranted = isCaptureGranted && isIgnoringBatteryOptimizations

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Screen & Background",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (allGranted) {
                    "Everything AutoPhone needs is granted."
                } else {
                    "Grant both so AutoPhone can read the screen while running in the background."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                label = "Screen capture",
                granted = isCaptureGranted,
                actionLabel = "Allow",
                onClick = onRequestScreenCapture
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = "Background running",
                granted = isIgnoringBatteryOptimizations,
                actionLabel = "Allow",
                onClick = onOpenBatterySettings
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (granted) "✅" else "⭕",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        if (granted) {
            Text(
                text = "Granted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp)) {
                Text(actionLabel)
            }
        }
    }
}
