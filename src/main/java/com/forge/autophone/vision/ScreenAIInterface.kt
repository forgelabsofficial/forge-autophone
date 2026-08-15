package com.forge.autophone.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.forge.autophone.AutoPhoneAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ScreenAIInterface — Preparation for Google ScreenAI integration.
 *
 * ScreenAI provides full screen understanding using vision-language models:
 * - "What's on screen?" → "A login form with username and password fields"
 * - "Where's the submit button?" → Returns bounding box coordinates
 * - "What does this icon do?" → Semantic understanding of UI elements
 *
 * This is a future-ready interface. When ScreenAI becomes available:
 * 1. Add dependency: `implementation "ai.google.dev:screenaudio:1.0.0"`
 * 2. Implement the actual model inference
 * 3. Enable in build.gradle
 *
 * For now, provides fallback behavior using existing capabilities.
 */
class ScreenAIInterface(private val service: AutoPhoneAccessibilityService) {

    private var isScreenAIAvailable = false
    
    /**
     * Initialize ScreenAI model (when available).
     * Currently a no-op placeholder.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        // TODO: Load ScreenAI model when SDK is available
        // val model = ScreenAI.create(context)
        // isScreenAIAvailable = model != null
        isScreenAIAvailable = false
        return@withContext isScreenAIAvailable
    }

    /**
     * Ask natural language question about screen content.
     * 
     * Examples:
     * - "What's on screen?" → Description of UI
     * - "Where's the login button?" → Location
     * - "What fields are required?" → List of required fields
     * - "Is there an error message?" → Yes/no with details
     */
    suspend fun askAboutScreen(
        screenshot: Bitmap,
        question: String
    ): ScreenAIResponse {
        if (!isScreenAIAvailable) {
            return fallbackToOCR(screenshot, question)
        }
        
        // TODO: Implement actual ScreenAI inference
        // val response = screenAIModel.query(screenshot, question)
        // return ScreenAIResponse(response.text, response.boundingBoxes, response.confidence)
        
        return fallbackToOCR(screenshot, question)
    }

    /**
     * Find UI element by natural language description.
     * 
     * Examples:
     * - "the blue button at the bottom"
     * - "the text field that says 'Email'"
     * - "the settings icon in the top right"
     */
    suspend fun findElementByDescription(
        screenshot: Bitmap,
        description: String
    ): ElementLocation? {
        if (!isScreenAIAvailable) {
            return fallbackFindElement(screenshot, description)
        }
        
        // TODO: Implement ScreenAI element detection
        // val element = screenAIModel.findElement(screenshot, description)
        // return ElementLocation(element.bounds, element.confidence, element.label)
        
        return fallbackFindElement(screenshot, description)
    }

    /**
     * Describe entire screen content semantically.
     * Returns structured understanding of UI hierarchy.
     */
    suspend fun describeScreen(screenshot: Bitmap): ScreenDescription {
        if (!isScreenAIAvailable) {
            return fallbackDescribeScreen(screenshot)
        }
        
        // TODO: Implement ScreenAI screen understanding
        // val description = screenAIModel.describe(screenshot)
        // return description
        
        return fallbackDescribeScreen(screenshot)
    }

    /**
     * Classify screen type using vision model.
     * More accurate than text-based heuristics.
     */
    suspend fun classifyScreenType(screenshot: Bitmap): ScreenClassification {
        if (!isScreenAIAvailable) {
            return fallbackClassifyScreen(screenshot)
        }
        
        // TODO: Implement ScreenAI classification
        // val classification = screenAIModel.classify(screenshot)
        // return ScreenClassification(classification.type, classification.confidence)
        
        return fallbackClassifyScreen(screenshot)
    }

    /**
     * Detect UI anomalies or errors visually.
     * Finds error dialogs, toast messages, loading spinners, etc.
     */
    suspend fun detectAnomalies(screenshot: Bitmap): List<VisualAnomaly> {
        if (!isScreenAIAvailable) {
            return fallbackDetectAnomalies(screenshot)
        }
        
        // TODO: Implement ScreenAI anomaly detection
        // return screenAIModel.detectAnomalies(screenshot)
        
        return fallbackDetectAnomalies(screenshot)
    }

    // ── Fallback implementations using existing capabilities ────────────────

    /**
     * Fallback to OCR-based question answering.
     */
    private suspend fun fallbackToOCR(
        screenshot: Bitmap,
        question: String
    ): ScreenAIResponse {
        val ocrBlocks = service.ocrExtractor.extractText(screenshot)
        val allText = ocrBlocks.joinToString(" ") { it.text }
        
        // Simple keyword matching for common questions
        val answer = when {
            question.contains("what's on screen", ignoreCase = true) -> 
                "Screen contains: $allText"
            question.contains("where", ignoreCase = true) -> {
                val keyword = extractKeyword(question)
                val block = ocrBlocks.find { it.text.contains(keyword, ignoreCase = true) }
                if (block != null) {
                    "Found at coordinates (${block.bounds.centerX()}, ${block.bounds.centerY()})"
                } else {
                    "Not found on screen"
                }
            }
            question.contains("error", ignoreCase = true) -> {
                val hasError = allText.contains("error", ignoreCase = true)
                if (hasError) "Yes, error detected: ${allText.substringAfter("error", "")}"
                else "No error messages visible"
            }
            else -> "OCR fallback: Screen text is: $allText"
        }
        
        return ScreenAIResponse(
            text = answer,
            boundingBoxes = ocrBlocks.map { it.bounds },
            confidence = 0.5f,
            source = "OCR Fallback"
        )
    }

    /**
     * Fallback element finding using OCR + icon matching.
     */
    private suspend fun fallbackFindElement(
        screenshot: Bitmap,
        description: String
    ): ElementLocation? {
        // Try OCR first
        val ocrBlocks = service.ocrExtractor.extractText(screenshot)
        val keywords = description.split(" ").filter { it.length > 3 }
        
        ocrBlocks.forEach { block ->
            if (keywords.any { block.text.contains(it, ignoreCase = true) }) {
                return ElementLocation(
                    bounds = block.bounds,
                    confidence = 0.6f,
                    label = block.text,
                    source = "OCR Match"
                )
            }
        }
        
        // Try icon matching if description suggests an icon
        if (description.contains("icon") || description.contains("button")) {
            // Future: use icon matcher here
        }
        
        return null
    }

    /**
     * Fallback screen description using OCR + context tracker.
     */
    private suspend fun fallbackDescribeScreen(screenshot: Bitmap): ScreenDescription {
        val ocrBlocks = service.ocrExtractor.extractText(screenshot)
        val context = service.appContextTracker.getCurrentContext()
        
        val elements = ocrBlocks.map { block ->
            UIElement(
                type = "text",
                label = block.text,
                bounds = block.bounds,
                confidence = block.confidence
            )
        }
        
        return ScreenDescription(
            summary = "Screen type: ${context?.screenType}, Contains ${elements.size} text elements",
            elements = elements,
            screenType = context?.screenType?.name ?: "UNKNOWN",
            confidence = 0.5f,
            source = "OCR + Context Fallback"
        )
    }

    /**
     * Fallback screen classification using context tracker.
     */
    private suspend fun fallbackClassifyScreen(screenshot: Bitmap): ScreenClassification {
        val context = service.appContextTracker.getCurrentContext()
        
        return ScreenClassification(
            type = context?.screenType?.name ?: "UNKNOWN",
            confidence = 0.6f,
            uiPatterns = context?.uiPatterns?.map { it.name } ?: emptyList(),
            source = "Context Tracker Fallback"
        )
    }

    /**
     * Fallback anomaly detection using verification tools.
     */
    private suspend fun fallbackDetectAnomalies(screenshot: Bitmap): List<VisualAnomaly> {
        val anomalies = mutableListOf<VisualAnomaly>()
        
        // Check for error text using OCR
        val ocrBlocks = service.ocrExtractor.extractText(screenshot)
        ocrBlocks.forEach { block ->
            if (block.text.contains("error", ignoreCase = true) ||
                block.text.contains("failed", ignoreCase = true)) {
                anomalies.add(VisualAnomaly(
                    type = AnomalyType.ERROR_MESSAGE,
                    bounds = block.bounds,
                    message = block.text,
                    confidence = 0.8f
                ))
            }
        }
        
        // Check for loading indicators (via context)
        val context = service.appContextTracker.getCurrentContext()
        if (context?.screenType?.name == "LOADING") {
            anomalies.add(VisualAnomaly(
                type = AnomalyType.LOADING_INDICATOR,
                bounds = Rect(0, 0, 0, 0),
                message = "Loading screen detected",
                confidence = 0.7f
            ))
        }
        
        return anomalies
    }

    private fun extractKeyword(question: String): String {
        // Simple keyword extraction: remove question words
        val stopWords = setOf("where", "is", "the", "a", "an", "what", "how", "why")
        return question.split(" ")
            .filter { it.length > 2 && !stopWords.contains(it.lowercase()) }
            .firstOrNull() ?: ""
    }

    /**
     * Check if ScreenAI is available.
     */
    fun isAvailable(): Boolean = isScreenAIAvailable
}

// ── Data classes ─────────────────────────────────────────────────────────────

/**
 * Response from ScreenAI question answering.
 */
data class ScreenAIResponse(
    val text: String,
    val boundingBoxes: List<Rect>,
    val confidence: Float,
    val source: String = "ScreenAI"
)

/**
 * Location of a UI element found by description.
 */
data class ElementLocation(
    val bounds: Rect,
    val confidence: Float,
    val label: String,
    val source: String = "ScreenAI"
)

/**
 * Semantic description of entire screen.
 */
data class ScreenDescription(
    val summary: String,
    val elements: List<UIElement>,
    val screenType: String,
    val confidence: Float,
    val source: String = "ScreenAI"
)

/**
 * Individual UI element in screen.
 */
data class UIElement(
    val type: String, // "button", "text", "input", "icon", etc.
    val label: String,
    val bounds: Rect,
    val confidence: Float
)

/**
 * Screen type classification result.
 */
data class ScreenClassification(
    val type: String,
    val confidence: Float,
    val uiPatterns: List<String>,
    val source: String = "ScreenAI"
)

/**
 * Visual anomaly detected on screen.
 */
data class VisualAnomaly(
    val type: AnomalyType,
    val bounds: Rect,
    val message: String,
    val confidence: Float
)

/**
 * Types of visual anomalies.
 */
enum class AnomalyType {
    ERROR_MESSAGE,
    LOADING_INDICATOR,
    PERMISSION_DIALOG,
    CRASH_DIALOG,
    NETWORK_ERROR,
    UNEXPECTED_SCREEN,
    UNKNOWN
}
