package com.forge.autophone.service

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ScreenshotService - Captures screenshots using MediaProjection API.
 * 
 * This service handles the MediaProjection permission flow and screenshot capture.
 * It requires user permission via a system dialog on first use.
 * 
 * Usage:
 * 1. Call requestScreenshotPermission() to get permission intent
 * 2. Launch intent with startActivityForResult()
 * 3. Pass result to initialize()
 * 4. Call captureScreenshot() to take screenshots
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenshotService(private val context: Context) {
    
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    
    init {
        windowManager.defaultDisplay.getMetrics(displayMetrics)
    }
    
    /**
     * Get the permission request intent.
     * Launch this with startActivityForResult() to request screenshot permission.
     */
    fun requestScreenshotPermission(): Intent {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return manager.createScreenCaptureIntent()
    }
    
    /**
     * Initialize MediaProjection with the permission result.
     * Call this in onActivityResult() after user approves permission.
     * 
     * @param resultCode Result code from onActivityResult
     * @param data Intent data from onActivityResult
     * @return true if initialization successful
     */
    fun initialize(resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data == null) {
            Timber.w("Screenshot permission denied")
            return false
        }
        
        try {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, data)
            
            // Setup ImageReader
            imageReader = ImageReader.newInstance(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                PixelFormat.RGBA_8888,
                2
            )
            
            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AutoPhone-Screenshot",
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                Handler(Looper.getMainLooper())
            )
            
            Timber.i("Screenshot service initialized")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize screenshot service")
            return false
        }
    }
    
    /**
     * Check if the service is ready to capture screenshots.
     */
    fun isReady(): Boolean {
        return mediaProjection != null && imageReader != null && virtualDisplay != null
    }
    
    /**
     * Capture a screenshot asynchronously.
     * Requires MediaProjection to be initialized first.
     * 
     * @return Bitmap of the current screen, or null if capture fails
     */
    @SuppressLint("WrongConstant")
    suspend fun captureScreenshot(): Bitmap? = suspendCancellableCoroutine { continuation ->
        if (!isReady()) {
            continuation.resumeWithException(IllegalStateException("Screenshot service not initialized"))
            return@suspendCancellableCoroutine
        }
        
        try {
            // Small delay to ensure display is ready
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val image = imageReader?.acquireLatestImage()
                    if (image == null) {
                        continuation.resume(null)
                        return@postDelayed
                    }
                    
                    val bitmap = imageToBitmap(image)
                    image.close()
                    
                    continuation.resume(bitmap)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to capture screenshot")
                    continuation.resume(null)
                }
            }, 100)
        } catch (e: Exception) {
            Timber.e(e, "Failed to capture screenshot")
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Capture a screenshot synchronously (blocking).
     * Use captureScreenshot() for async/suspend version.
     */
    fun captureScreenshotSync(): Bitmap? {
        if (!isReady()) {
            Timber.w("Screenshot service not initialized")
            return null
        }
        
        try {
            Thread.sleep(100) // Small delay
            val image = imageReader?.acquireLatestImage() ?: return null
            val bitmap = imageToBitmap(image)
            image.close()
            return bitmap
        } catch (e: Exception) {
            Timber.e(e, "Failed to capture screenshot")
            return null
        }
    }
    
    /**
     * Convert Image to Bitmap.
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * displayMetrics.widthPixels
        
        val bitmap = Bitmap.createBitmap(
            displayMetrics.widthPixels + rowPadding / pixelStride,
            displayMetrics.heightPixels,
            Bitmap.Config.ARGB_8888
        )
        
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Crop if there's padding
        return if (rowPadding == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
            )
        }
    }
    
    /**
     * Release resources.
     * Call this when screenshots are no longer needed.
     */
    fun release() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        
        Timber.i("Screenshot service released")
    }
}
