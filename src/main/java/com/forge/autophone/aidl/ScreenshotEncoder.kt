package com.forge.autophone.aidl

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Encodes a screenshot for transport across the AIDL boundary.
 *
 * ## Why this exists
 *
 * `AutoPhoneToolRegistry.screenshot()` returns a [Bitmap], but
 * `IAutoPhoneService.screenshot()` returns a [String] - a Bitmap is not a
 * parcelable type and cannot cross Binder directly. Until now there was no
 * conversion anywhere in the project, so `AutoPhoneService.screenshot()` was
 * hardcoded to return an error and Forge OS could never obtain an image.
 *
 * ## Why JPEG, downscaled
 *
 * Binder transactions are capped at roughly 1 MB. A 1080x2400 screenshot as
 * PNG is ~3-8 MB before Base64 expansion (which adds ~33%), so PNG cannot
 * reliably cross the boundary.
 *
 * Downscaling to [MAX_WIDTH_PX] and encoding as JPEG at [JPEG_QUALITY] lands
 * around 80-150 KB for typical phone screens - comfortably inside the limit
 * while remaining more than legible enough for ML Kit OCR, which is the main
 * consumer.
 *
 * If full-resolution pixels are ever genuinely needed (e.g. small-text
 * extraction where downscaling hurts accuracy), move to a shared file via
 * FileProvider rather than raising these numbers.
 */
object ScreenshotEncoder {

    /** Downscale target. 720px wide is ample for ML Kit text recognition. */
    private const val MAX_WIDTH_PX = 720

    /** JPEG quality. 75 keeps text legible at a fraction of the size. */
    private const val JPEG_QUALITY = 75

    /**
     * Encode [bitmap] as a base64 JPEG string, downscaled so the widest side is
     * at most [MAX_WIDTH_PX].
     *
     * Aspect ratio is preserved. The original bitmap is never recycled - the
     * caller may still own it.
     *
     * @return base64 (no data-URI prefix) suitable for embedding in JSON.
     */
    fun toBase64Jpeg(bitmap: Bitmap): String {
        val scaled = downscale(bitmap, MAX_WIDTH_PX)
        try {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    /** Full JSON payload for the AIDL response. */
    fun toJson(bitmap: Bitmap): String {
        val scaled = downscale(bitmap, MAX_WIDTH_PX)
        return try {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            // Width/height describe the DECODED image the caller will get, so
            // coordinate mapping back to screen pixels can be computed.
            """{"ok":true,"mime":"image/jpeg","width":${scaled.width},"height":${scaled.height},"data":"$b64"}"""
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    /**
     * Scale [bitmap] down so its width is at most [maxWidth]. Returns the
     * original instance when no scaling is needed, so callers avoid a copy.
     */
    private fun downscale(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val height = (bitmap.height.toFloat() * maxWidth / bitmap.width).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }
}