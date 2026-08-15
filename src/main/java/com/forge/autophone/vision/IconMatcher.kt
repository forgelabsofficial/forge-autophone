package com.forge.autophone.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * IconMatcher — OpenCV-powered icon and image recognition for AutoPhone.
 *
 * Enables template matching to find icon-only buttons, logos, and UI elements
 * that don't have text or proper accessibility labels.
 *
 * Uses normalized cross-correlation for robust matching across different scales
 * and lighting conditions.
 */
class IconMatcher(private val context: Context) {
    
    private val templates = mutableMapOf<String, Mat>()
    
    init {
        // Initialize OpenCV (must be called before any OpenCV operations)
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    }

    /**
     * Register an icon template for future matching.
     * The template should be a cropped image of the icon to find.
     *
     * @param name Unique identifier for this icon
     * @param templateBitmap Bitmap of the icon to match against
     */
    fun registerIcon(name: String, templateBitmap: Bitmap) {
        templates[name] = bitmapToMat(templateBitmap)
    }

    /**
     * Register an icon from base64-encoded PNG/JPEG.
     * Useful for agent-provided icons.
     */
    fun registerIconFromBase64(name: String, base64Data: String) {
        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        registerIcon(name, bitmap)
    }

    /**
     * Unregister an icon template.
     */
    fun unregisterIcon(name: String) {
        templates.remove(name)?.release()
    }

    /**
     * Get all registered icon names.
     */
    fun getRegisteredIcons(): List<String> = templates.keys.toList()

    /**
     * Find an icon on screen using template matching.
     *
     * @param name Name of the registered icon
     * @param screenshot Current screenshot to search in
     * @param threshold Confidence threshold (0.0 to 1.0, default 0.8)
     * @return Location of the icon (top-left corner) and confidence, or null if not found
     */
    fun findIcon(name: String, screenshot: Bitmap, threshold: Double = 0.8): IconMatch? {
        val template = templates[name] ?: return null
        val screen = bitmapToMat(screenshot)
        
        // Perform template matching
        val result = Mat()
        Imgproc.matchTemplate(screen, template, result, Imgproc.TM_CCOEFF_NORMED)
        
        // Find best match
        val minMaxResult = Core.minMaxLoc(result)
        
        result.release()
        screen.release()
        
        return if (minMaxResult.maxVal >= threshold) {
            IconMatch(
                name = name,
                topLeft = Point(minMaxResult.maxLoc.x.toInt(), minMaxResult.maxLoc.y.toInt()),
                width = template.cols(),
                height = template.rows(),
                confidence = minMaxResult.maxVal
            )
        } else null
    }

    /**
     * Find all occurrences of an icon (with non-maximum suppression).
     *
     * @param name Name of the registered icon
     * @param screenshot Current screenshot to search in
     * @param threshold Confidence threshold
     * @param maxMatches Maximum number of matches to return (default 10)
     * @return List of icon matches, sorted by confidence (highest first)
     */
    fun findAllIcons(
        name: String,
        screenshot: Bitmap,
        threshold: Double = 0.8,
        maxMatches: Int = 10
    ): List<IconMatch> {
        val template = templates[name] ?: return emptyList()
        val screen = bitmapToMat(screenshot)
        
        val result = Mat()
        Imgproc.matchTemplate(screen, template, result, Imgproc.TM_CCOEFF_NORMED)
        
        val matches = mutableListOf<IconMatch>()
        
        // Find all matches above threshold
        for (y in 0 until result.rows()) {
            for (x in 0 until result.cols()) {
                val value = result.get(y, x)[0]
                if (value >= threshold) {
                    matches.add(IconMatch(
                        name = name,
                        topLeft = Point(x, y),
                        width = template.cols(),
                        height = template.rows(),
                        confidence = value
                    ))
                }
            }
        }
        
        result.release()
        screen.release()
        
        // Apply non-maximum suppression to remove overlapping matches
        val filtered = nonMaximumSuppression(matches, overlapThreshold = 0.3)
        
        // Sort by confidence and limit
        return filtered.sortedByDescending { it.confidence }.take(maxMatches)
    }

    /**
     * Find icon with scale invariance (searches at multiple scales).
     * Slower but more robust to size variations.
     */
    fun findIconMultiScale(
        name: String,
        screenshot: Bitmap,
        threshold: Double = 0.8,
        scales: List<Double> = listOf(0.5, 0.75, 1.0, 1.25, 1.5)
    ): IconMatch? {
        val template = templates[name] ?: return null
        var bestMatch: IconMatch? = null
        
        scales.forEach { scale ->
            val scaledTemplate = Mat()
            val size = Size((template.cols() * scale).toInt().toDouble(), 
                           (template.rows() * scale).toInt().toDouble())
            Imgproc.resize(template, scaledTemplate, size)
            
            val screen = bitmapToMat(screenshot)
            val result = Mat()
            Imgproc.matchTemplate(screen, scaledTemplate, result, Imgproc.TM_CCOEFF_NORMED)
            
            val minMaxResult = Core.minMaxLoc(result)
            
            if (minMaxResult.maxVal >= threshold) {
                val match = IconMatch(
                    name = name,
                    topLeft = Point(minMaxResult.maxLoc.x.toInt(), minMaxResult.maxLoc.y.toInt()),
                    width = scaledTemplate.cols(),
                    height = scaledTemplate.rows(),
                    confidence = minMaxResult.maxVal
                )
                
                if (bestMatch == null || match.confidence > bestMatch!!.confidence) {
                    bestMatch = match
                }
            }
            
            result.release()
            screen.release()
            scaledTemplate.release()
        }
        
        return bestMatch
    }

    /**
     * Check if an icon is visible on screen (boolean check).
     */
    fun isIconVisible(name: String, screenshot: Bitmap, threshold: Double = 0.8): Boolean {
        return findIcon(name, screenshot, threshold) != null
    }

    /**
     * Clean up all registered templates.
     */
    fun cleanup() {
        templates.values.forEach { it.release() }
        templates.clear()
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale for better template matching
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        mat.release()
        
        return gray
    }

    private fun nonMaximumSuppression(
        matches: List<IconMatch>,
        overlapThreshold: Double
    ): List<IconMatch> {
        if (matches.isEmpty()) return emptyList()
        
        val sorted = matches.sortedByDescending { it.confidence }
        val keep = mutableListOf<IconMatch>()
        val suppressed = mutableSetOf<Int>()
        
        sorted.forEachIndexed { i, match ->
            if (i !in suppressed) {
                keep.add(match)
                
                // Suppress overlapping matches
                sorted.forEachIndexed { j, other ->
                    if (j > i && j !in suppressed) {
                        val overlap = calculateOverlap(match, other)
                        if (overlap > overlapThreshold) {
                            suppressed.add(j)
                        }
                    }
                }
            }
        }
        
        return keep
    }

    private fun calculateOverlap(a: IconMatch, b: IconMatch): Double {
        val x1 = maxOf(a.topLeft.x, b.topLeft.x)
        val y1 = maxOf(a.topLeft.y, b.topLeft.y)
        val x2 = minOf(a.topLeft.x + a.width, b.topLeft.x + b.width)
        val y2 = minOf(a.topLeft.y + a.height, b.topLeft.y + b.height)
        
        if (x2 < x1 || y2 < y1) return 0.0
        
        val intersectionArea = (x2 - x1) * (y2 - y1)
        val aArea = a.width * a.height
        val bArea = b.width * b.height
        val unionArea = aArea + bArea - intersectionArea
        
        return intersectionArea.toDouble() / unionArea.toDouble()
    }
}

/**
 * Result of icon matching operation.
 */
data class IconMatch(
    val name: String,
    val topLeft: Point,
    val width: Int,
    val height: Int,
    val confidence: Double
) {
    val centerX: Float get() = topLeft.x + width / 2f
    val centerY: Float get() = topLeft.y + height / 2f
    
    val bounds: android.graphics.Rect
        get() = android.graphics.Rect(
            topLeft.x,
            topLeft.y,
            topLeft.x + width,
            topLeft.y + height
        )
}
