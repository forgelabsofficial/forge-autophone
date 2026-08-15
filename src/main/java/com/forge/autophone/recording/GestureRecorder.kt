package com.forge.autophone.recording

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * GestureRecorder — Record and playback complex gesture sequences.
 *
 * Enables recording of:
 * - Multi-finger gestures (pinch, zoom, rotate)
 * - Complex swipe patterns (unlock patterns, signatures)
 * - Precise timing and pressure variations
 * - Composite gesture sequences (tap → hold → drag)
 *
 * Use cases:
 * - Record unlock patterns once, replay for testing
 * - Capture game gestures (drawing, swiping combos)
 * - Store complex navigation flows
 * - Create gesture-based automation macros
 */
class GestureRecorder(private val service: AccessibilityService) {

    private var recording: MutableList<RecordedStroke> = mutableListOf()
    private var isRecording = false
    private var recordingStartTime = 0L
    private val json = Json { prettyPrint = true }

    /**
     * Start recording gestures.
     * Captures all touch events until stopRecording() is called.
     */
    fun startRecording() {
        recording.clear()
        isRecording = true
        recordingStartTime = SystemClock.uptimeMillis()
    }

    /**
     * Record a touch stroke (single finger path).
     */
    fun recordStroke(events: List<TouchEvent>) {
        if (!isRecording) return
        
        val points = events.map { event ->
            RecordedPoint(
                x = event.x,
                y = event.y,
                pressure = event.pressure,
                timestamp = event.timestamp - recordingStartTime
            )
        }
        
        if (points.isNotEmpty()) {
            recording.add(RecordedStroke(
                points = points,
                startTime = points.first().timestamp,
                endTime = points.last().timestamp
            ))
        }
    }

    /**
     * Stop recording and return the captured gesture.
     */
    fun stopRecording(name: String): RecordedGesture {
        isRecording = false
        val totalDuration = if (recording.isNotEmpty()) {
            recording.maxOf { it.endTime }
        } else {
            0L
        }
        
        return RecordedGesture(
            name = name,
            strokes = recording.toList(),
            durationMs = totalDuration,
            recordedAt = System.currentTimeMillis()
        )
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Cancel recording without saving.
     */
    fun cancelRecording() {
        isRecording = false
        recording.clear()
    }

    /**
     * Serialize gesture to JSON string for storage.
     */
    fun serializeGesture(gesture: RecordedGesture): String {
        return json.encodeToString(gesture)
    }

    /**
     * Deserialize gesture from JSON string.
     */
    fun deserializeGesture(jsonString: String): RecordedGesture {
        return json.decodeFromString(jsonString)
    }
}

/**
 * GesturePlayer — Replay recorded gestures with precise timing.
 */
class GesturePlayer(private val service: AccessibilityService) {

    /**
     * Replay a recorded gesture.
     * 
     * @param gesture The gesture to replay
     * @param speedMultiplier Speed adjustment (1.0 = normal, 2.0 = 2x speed, 0.5 = half speed)
     * @param callback Callback when playback completes
     */
    suspend fun replay(
        gesture: RecordedGesture,
        speedMultiplier: Float = 1.0f,
        callback: ((Boolean) -> Unit)? = null
    ) = withContext(Dispatchers.Main) {
        try {
            val gestureDescription = buildGestureDescription(gesture, speedMultiplier)
            
            suspendCancellableCoroutine<Boolean> { continuation ->
                service.dispatchGesture(
                    gestureDescription,
                    object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            callback?.invoke(true)
                            continuation.resume(true) {}
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            callback?.invoke(false)
                            continuation.resume(false) {}
                        }
                    },
                    null
                )
            }
        } catch (e: Exception) {
            callback?.invoke(false)
            throw e
        }
    }

    /**
     * Build GestureDescription from recorded gesture.
     */
    private fun buildGestureDescription(
        gesture: RecordedGesture,
        speedMultiplier: Float
    ): GestureDescription {
        val builder = GestureDescription.Builder()
        
        gesture.strokes.forEach { stroke ->
            val path = buildPath(stroke.points)
            val adjustedStartTime = (stroke.startTime / speedMultiplier).toLong()
            val adjustedDuration = ((stroke.endTime - stroke.startTime) / speedMultiplier).toLong()
            
            builder.addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    adjustedStartTime,
                    adjustedDuration,
                    stroke.points.size > 1 // willContinue if multi-point stroke
                )
            )
        }
        
        return builder.build()
    }

    /**
     * Build Android Path from recorded points.
     */
    private fun buildPath(points: List<RecordedPoint>): Path {
        val path = Path()
        
        if (points.isEmpty()) return path
        
        path.moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { point ->
            path.lineTo(point.x, point.y)
        }
        
        return path
    }

    /**
     * Preview gesture path (for debugging).
     * Returns list of points for visualization.
     */
    fun getGesturePath(gesture: RecordedGesture): List<PointF> {
        return gesture.strokes.flatMap { stroke ->
            stroke.points.map { PointF(it.x, it.y) }
        }
    }
}

/**
 * GestureLibrary — Manage saved gestures.
 */
class GestureLibrary {
    private val gestures = mutableMapOf<String, RecordedGesture>()
    
    /**
     * Save a gesture to the library.
     */
    fun saveGesture(gesture: RecordedGesture) {
        gestures[gesture.name] = gesture
    }
    
    /**
     * Get a gesture by name.
     */
    fun getGesture(name: String): RecordedGesture? {
        return gestures[name]
    }
    
    /**
     * List all saved gesture names.
     */
    fun listGestures(): List<String> {
        return gestures.keys.toList()
    }
    
    /**
     * Delete a gesture.
     */
    fun deleteGesture(name: String): Boolean {
        return gestures.remove(name) != null
    }
    
    /**
     * Check if gesture exists.
     */
    fun hasGesture(name: String): Boolean {
        return gestures.containsKey(name)
    }
    
    /**
     * Clear all gestures.
     */
    fun clear() {
        gestures.clear()
    }
    
    /**
     * Import gestures from JSON.
     */
    fun importFromJson(json: String, recorder: GestureRecorder) {
        val gesture = recorder.deserializeGesture(json)
        saveGesture(gesture)
    }
    
    /**
     * Export gesture to JSON.
     */
    fun exportToJson(name: String, recorder: GestureRecorder): String? {
        val gesture = getGesture(name) ?: return null
        return recorder.serializeGesture(gesture)
    }
}

/**
 * Built-in common gestures.
 */
object CommonGestures {
    
    /**
     * Create a double-tap gesture.
     */
    fun doubleTap(x: Float, y: Float, delayMs: Long = 100): RecordedGesture {
        return RecordedGesture(
            name = "double_tap",
            strokes = listOf(
                RecordedStroke(
                    points = listOf(RecordedPoint(x, y, 1.0f, 0)),
                    startTime = 0,
                    endTime = 50
                ),
                RecordedStroke(
                    points = listOf(RecordedPoint(x, y, 1.0f, delayMs + 50)),
                    startTime = delayMs + 50,
                    endTime = delayMs + 100
                )
            ),
            durationMs = delayMs + 100,
            recordedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create a pinch zoom in gesture.
     */
    fun pinchZoomIn(
        centerX: Float,
        centerY: Float,
        spreadStart: Float = 50f,
        spreadEnd: Float = 200f,
        durationMs: Long = 300
    ): RecordedGesture {
        return RecordedGesture(
            name = "pinch_zoom_in",
            strokes = listOf(
                // Finger 1: left side moving out
                RecordedStroke(
                    points = listOf(
                        RecordedPoint(centerX - spreadStart, centerY, 1.0f, 0),
                        RecordedPoint(centerX - spreadEnd, centerY, 1.0f, durationMs)
                    ),
                    startTime = 0,
                    endTime = durationMs
                ),
                // Finger 2: right side moving out
                RecordedStroke(
                    points = listOf(
                        RecordedPoint(centerX + spreadStart, centerY, 1.0f, 0),
                        RecordedPoint(centerX + spreadEnd, centerY, 1.0f, durationMs)
                    ),
                    startTime = 0,
                    endTime = durationMs
                )
            ),
            durationMs = durationMs,
            recordedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create a pinch zoom out gesture.
     */
    fun pinchZoomOut(
        centerX: Float,
        centerY: Float,
        spreadStart: Float = 200f,
        spreadEnd: Float = 50f,
        durationMs: Long = 300
    ): RecordedGesture {
        return pinchZoomIn(centerX, centerY, spreadStart, spreadEnd, durationMs)
            .copy(name = "pinch_zoom_out")
    }
    
    /**
     * Create an L-shaped unlock pattern.
     */
    fun unlockPatternL(
        startX: Float,
        startY: Float,
        cellSize: Float = 200f
    ): RecordedGesture {
        return RecordedGesture(
            name = "unlock_pattern_L",
            strokes = listOf(
                RecordedStroke(
                    points = listOf(
                        RecordedPoint(startX, startY, 1.0f, 0),
                        RecordedPoint(startX, startY + cellSize, 1.0f, 150),
                        RecordedPoint(startX, startY + 2 * cellSize, 1.0f, 300),
                        RecordedPoint(startX + cellSize, startY + 2 * cellSize, 1.0f, 450),
                        RecordedPoint(startX + 2 * cellSize, startY + 2 * cellSize, 1.0f, 600)
                    ),
                    startTime = 0,
                    endTime = 600
                )
            ),
            durationMs = 600,
            recordedAt = System.currentTimeMillis()
        )
    }
}

// ── Data classes ─────────────────────────────────────────────────────────────

/**
 * Recorded gesture with all strokes and metadata.
 */
@Serializable
data class RecordedGesture(
    val name: String,
    val strokes: List<RecordedStroke>,
    val durationMs: Long,
    val recordedAt: Long
)

/**
 * Single stroke (finger path) in a gesture.
 */
@Serializable
data class RecordedStroke(
    val points: List<RecordedPoint>,
    val startTime: Long, // Relative to gesture start
    val endTime: Long
)

/**
 * Single touch point in a stroke.
 */
@Serializable
data class RecordedPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long // Relative to gesture start
)

/**
 * Touch event data (for recording).
 */
data class TouchEvent(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long,
    val action: Int
) {
    companion object {
        fun fromMotionEvent(event: MotionEvent): TouchEvent {
            return TouchEvent(
                x = event.x,
                y = event.y,
                pressure = event.pressure,
                timestamp = event.eventTime,
                action = event.action
            )
        }
    }
}
