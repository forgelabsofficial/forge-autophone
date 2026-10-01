package com.forge.autophone.aidl

import com.forge.autophone.ocr.OcrTextBlock
import com.forge.autophone.vision.IconMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * JSON encoding helpers for the AIDL surface.
 *
 * ## Why hand-built instead of kotlinx.serialization
 *
 * [OcrTextBlock] and [IconMatch] carry `android.graphics.Rect` / `Point`,
 * which are not serializable types, and neither class is annotated
 * `@Serializable`. Rather than adding a custom serializer for framework types
 * just to cross Binder, the JSON is assembled directly - these payloads are
 * small and flat (a handful of numbers per block).
 */

/** Escape a string for safe embedding inside a JSON string literal. */
fun String.jsonEscape(): String = buildString(length + 8) {
    for (ch in this@jsonEscape) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) {
                append("\\u%04x".format(ch.code))
            } else {
                append(ch)
            }
        }
    }
}

/**
 * Serialize an OCR block.
 *
 * `confidence` is reported as `null` when unknown: ML Kit's Text Recognition
 * API v2 does not expose per-block confidence, and the extractor marks that
 * explicitly rather than inventing a value. A consumer that treats `null` as
 * "no confidence signal" is better served than one shown a fake 1.0.
 */
fun OcrTextBlock.toJson(): String = buildString {
    append("{")
    append("\"text\":\"").append(text.jsonEscape()).append("\",")
    append("\"left\":").append(bounds.left).append(",")
    append("\"top\":").append(bounds.top).append(",")
    append("\"right\":").append(bounds.right).append(",")
    append("\"bottom\":").append(bounds.bottom).append(",")
    append("\"centerX\":").append(bounds.centerX()).append(",")
    append("\"centerY\":").append(bounds.centerY()).append(",")
    append("\"confidence\":")
    if (confidenceKnown) append(confidence) else append("null")
    append("}")
}

/** Serialize an icon-template match. */
fun IconMatch.toJson(): String = buildString {
    append("{")
    append("\"name\":\"").append(name.jsonEscape()).append("\",")
    append("\"left\":").append(topLeft.x).append(",")
    append("\"top\":").append(topLeft.y).append(",")
    append("\"width\":").append(width).append(",")
    append("\"height\":").append(height).append(",")
    append("\"centerX\":").append(centerX).append(",")
    append("\"centerY\":").append(centerY).append(",")
    append("\"confidence\":").append(confidence)
    append("}")
}

/**
 * Run a suspend registry call from a synchronous AIDL method.
 *
 * Safe because binder transactions arrive on a background thread, so this
 * never blocks the UI thread.
 */
inline fun <T> runBlockingIo(crossinline block: suspend () -> T): T =
    runBlocking { withContext(Dispatchers.IO) { block() } }