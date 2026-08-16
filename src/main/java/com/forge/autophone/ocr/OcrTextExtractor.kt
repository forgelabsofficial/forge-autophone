package com.forge.autophone.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OcrTextExtractor — ML Kit-powered OCR for AutoPhone.
 *
 * Enables text recognition on screen content, even when apps don't expose
 * proper accessibility labels. Essential for controlling apps with poor
 * accessibility support (games, custom UIs, image-heavy content).
 */
class OcrTextExtractor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract all visible text from a screenshot.
     * Returns text blocks with bounding boxes and confidence scores.
     */
    suspend fun extractText(screenshot: Bitmap): List<OcrTextBlock> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(screenshot, 0)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        OcrTextBlock(
                            text = block.text,
                            bounds = block.boundingBox ?: Rect(),
                            confidence = 1.0f, // MLKit Text API v2 doesn't expose confidence per block
                            lines = block.lines.map { line ->
                                OcrTextLine(
                                    text = line.text,
                                    bounds = line.boundingBox ?: Rect(),
                                    confidence = 1.0f // MLKit Text API v2 doesn't expose confidence per line
                                )
                            }
                        )
                    }
                    continuation.resume(blocks)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
                
            continuation.invokeOnCancellation {
                // Clean up if coroutine is cancelled
            }
        }
    }

    /**
     * Find text anywhere on screen, even if not in accessibility tree.
     * Case-insensitive search with partial matching.
     */
    suspend fun findText(screenshot: Bitmap, query: String): OcrTextBlock? {
        val blocks = extractText(screenshot)
        return blocks.firstOrNull { 
            it.text.contains(query, ignoreCase = true) 
        }
    }

    /**
     * Find all occurrences of text on screen.
     */
    suspend fun findAllText(screenshot: Bitmap, query: String): List<OcrTextBlock> {
        val blocks = extractText(screenshot)
        return blocks.filter { 
            it.text.contains(query, ignoreCase = true) 
        }
    }

    /**
     * Get center point of a text block (for tapping).
     */
    fun getCenterPoint(block: OcrTextBlock): Pair<Float, Float> {
        val bounds = block.bounds
        return Pair(
            (bounds.left + bounds.right) / 2f,
            (bounds.top + bounds.bottom) / 2f
        )
    }

    /**
     * Clean up resources when done.
     */
    fun close() {
        recognizer.close()
    }
}

/**
 * OCR text block with metadata.
 */
data class OcrTextBlock(
    val text: String,
    val bounds: Rect,
    val confidence: Float,
    val lines: List<OcrTextLine> = emptyList()
) {
    val centerX: Float get() = (bounds.left + bounds.right) / 2f
    val centerY: Float get() = (bounds.top + bounds.bottom) / 2f
}

/**
 * OCR text line (sub-component of block).
 */
data class OcrTextLine(
    val text: String,
    val bounds: Rect,
    val confidence: Float
)
