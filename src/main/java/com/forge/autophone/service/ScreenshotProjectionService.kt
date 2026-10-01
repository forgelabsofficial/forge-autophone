package com.forge.autophone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import timber.log.Timber

/**
 * Foreground service that keeps the MediaProjection session alive.
 *
 * ## Why this exists
 *
 * Screen capture is the foundation of every vision tool here — OCR
 * ([com.forge.autophone.ocr.OcrTextExtractor]), icon matching
 * ([com.forge.autophone.vision.IconMatcher]) and the AI screen interface all
 * take a `Bitmap`, and the only source of that Bitmap is MediaProjection.
 *
 * Two Android rules made this mandatory rather than optional:
 *
 *  1. **Android 14 (API 34)** refuses to hand out a MediaProjection token
 *     unless the app declares `FOREGROUND_SERVICE_MEDIA_PROJECTION` *and* runs
 *     a foreground service of type `mediaProjection`. Without this the
 *     `createScreenCaptureIntent()` flow fails outright on modern devices.
 *  2. A projection stops delivering frames once the owning process is not
 *     foreground. For a tool whose whole job is reading the screen in the
 *     background, that meant every capture returned `null`.
 *
 * The service itself does no work — it exists to hold the projection alive and
 * to satisfy the platform's foreground-service contract. Frame production is
 * handled by the VirtualDisplay inside
 * [com.forge.autophone.service.ScreenshotService].
 */
class ScreenshotProjectionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat()
        Timber.i("ScreenshotProjectionService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart if the system kills us: the projection is the whole point of
        // this service, so surviving process death matters.
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.i("ScreenshotProjectionService stopped")
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ requires the type to be passed here as well as in the
                // manifest; on 34+ an incorrect type throws SecurityException.
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // Never let this crash the caller: failing to start the service
            // should degrade screen capture, not take down the app.
            Timber.e(e, "Failed to enter foreground")
        }
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoPhone active")
            .setContentText("Reading the screen for Forge OS")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen capture",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Shown while AutoPhone reads the screen" }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "autophone_projection"
        private const val NOTIFICATION_ID = 4242

        /**
         * Start the service. Safe to call repeatedly; starting an already-running
         * foreground service is a no-op.
         */
        fun start(context: Context) {
            val intent = Intent(context, ScreenshotProjectionService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not start projection service")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ScreenshotProjectionService::class.java))
            } catch (e: Exception) {
                Timber.e(e, "Could not stop projection service")
            }
        }
    }
}