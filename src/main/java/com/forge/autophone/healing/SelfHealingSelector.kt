package com.forge.autophone.healing

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.inspector.UITreeInspector
import com.forge.autophone.model.NodeSnapshot
import kotlin.math.abs

/**
 * SelfHealingSelector — ML-based selector healing for brittle UI automation.
 *
 * Problem: App updates change view IDs, class names, or layouts. Hard-coded
 * selectors break. Agent must be manually updated.
 *
 * Solution: Learn from successful selector matches and use similarity scoring
 * to find equivalent nodes when original selectors fail.
 *
 * Example:
 * ```
 * // Original: find("com.example:id/submit_button")
 * // App update removes ID → fallback to similarity matching
 * // Finds button with similar: position, text, siblings, size
 * ```
 */
class SelfHealingSelector(private val service: AutoPhoneAccessibilityService) {

    // History of successful selector matches (selector → matched nodes)
    private val selectorHistory = mutableMapOf<String, MutableList<HistoricalMatch>>()
    
    // Maximum history entries per selector (to prevent unbounded growth)
    private val maxHistorySize = 10

    /**
     * Find a node using self-healing logic.
     * 
     * 1. Try original selector (view ID, text, etc.)
     * 2. If fails, use ML similarity to find best match from history
     * 3. Learn from successful matches for future healing
     */
    fun findWithHealing(
        selector: SelectorSpec,
        confidenceThreshold: Double = 0.7
    ): HealingResult {
        val currentSnapshot = UITreeInspector(service).snapshot()
        
        // Step 1: Try direct match
        val directMatch = findDirect(selector, currentSnapshot)
        if (directMatch != null) {
            // Learn from successful match
            recordMatch(selector, directMatch)
            return HealingResult.DirectMatch(directMatch)
        }

        // Step 2: Selector failed - attempt healing
        val history = selectorHistory[selector.key] ?: return HealingResult.NotFound
        if (history.isEmpty()) return HealingResult.NotFound

        // Step 3: Find most similar node using ML scoring
        val candidate = findBestMatch(history, currentSnapshot, confidenceThreshold)
        
        return if (candidate != null) {
            // Learn from healed match
            recordMatch(selector, candidate.node)
            HealingResult.HealedMatch(candidate.node, candidate.confidence, candidate.reason)
        } else {
            HealingResult.NotFound
        }
    }

    /**
     * Find node using original selector (no healing).
     */
    private fun findDirect(selector: SelectorSpec, snapshot: List<NodeSnapshot>): NodeSnapshot? {
        return when (selector) {
            is SelectorSpec.ById -> snapshot.find { it.viewId == selector.viewId }
            is SelectorSpec.ByText -> snapshot.find { it.text == selector.text }
            is SelectorSpec.ByContentDescription -> snapshot.find { 
                it.contentDescription == selector.contentDescription 
            }
            is SelectorSpec.ByClass -> snapshot.find { it.className == selector.className }
            is SelectorSpec.Composite -> {
                snapshot.find { node ->
                    selector.matchers.all { matcher -> matcher.matches(node) }
                }
            }
        }
    }

    /**
     * Find best matching node using similarity scoring.
     */
    private fun findBestMatch(
        history: List<HistoricalMatch>,
        currentSnapshot: List<NodeSnapshot>,
        threshold: Double
    ): SimilarityCandidate? {
        // Use most recent historical match as reference
        val reference = history.last().node
        
        val candidates = currentSnapshot.map { candidate ->
            val score = computeSimilarity(reference, candidate)
            SimilarityCandidate(candidate, score, explainSimilarity(reference, candidate))
        }.filter { it.confidence >= threshold }
        
        return candidates.maxByOrNull { it.confidence }
    }

    /**
     * Compute similarity score between two nodes (0.0 to 1.0).
     * 
     * Considers multiple factors:
     * - Class name match (30%)
     * - Text similarity (25%)
     * - Content description similarity (20%)
     * - Spatial similarity (position, size) (15%)
     * - Sibling context similarity (10%)
     */
    private fun computeSimilarity(reference: NodeSnapshot, candidate: NodeSnapshot): Double {
        var score = 0.0
        
        // 1. Class name match (30%)
        if (reference.className == candidate.className) {
            score += 0.30
        } else if (reference.className?.substringAfterLast('.') == 
                   candidate.className?.substringAfterLast('.')) {
            // Same class name, different package
            score += 0.15
        }
        
        // 2. Text similarity (25%)
        score += textSimilarity(reference.text, candidate.text) * 0.25
        
        // 3. Content description similarity (20%)
        score += textSimilarity(reference.contentDescription, candidate.contentDescription) * 0.20
        
        // 4. Spatial similarity (15%)
        score += spatialSimilarity(reference, candidate) * 0.15
        
        // 5. Property similarity (10%)
        score += propertySimilarity(reference, candidate) * 0.10
        
        return score
    }

    /**
     * Compute text similarity using Levenshtein distance.
     */
    private fun textSimilarity(text1: String?, text2: String?): Double {
        if (text1 == null && text2 == null) return 1.0
        if (text1 == null || text2 == null) return 0.0
        if (text1 == text2) return 1.0
        
        val maxLen = maxOf(text1.length, text2.length)
        if (maxLen == 0) return 1.0
        
        val distance = levenshteinDistance(text1, text2)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    /**
     * Compute spatial similarity (position and size).
     */
    private fun spatialSimilarity(ref: NodeSnapshot, candidate: NodeSnapshot): Double {
        val refCenterX = (ref.boundsLeft + ref.boundsRight) / 2.0
        val refCenterY = (ref.boundsTop + ref.boundsBottom) / 2.0
        val candCenterX = (candidate.boundsLeft + candidate.boundsRight) / 2.0
        val candCenterY = (candidate.boundsTop + candidate.boundsBottom) / 2.0
        
        // Screen dimensions (approximate)
        val screenWidth = 1080.0
        val screenHeight = 2400.0
        
        // Normalized distance between centers (0.0 = same position, 1.0 = opposite corners)
        val deltaX = abs(refCenterX - candCenterX) / screenWidth
        val deltaY = abs(refCenterY - candCenterY) / screenHeight
        val positionDist = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
        
        // Size similarity
        val refWidth = ref.boundsRight - ref.boundsLeft
        val refHeight = ref.boundsBottom - ref.boundsTop
        val candWidth = candidate.boundsRight - candidate.boundsLeft
        val candHeight = candidate.boundsBottom - candidate.boundsTop
        
        val widthRatio = if (refWidth > 0) minOf(candWidth, refWidth).toDouble() / maxOf(candWidth, refWidth) else 1.0
        val heightRatio = if (refHeight > 0) minOf(candHeight, refHeight).toDouble() / maxOf(candHeight, refHeight) else 1.0
        val sizeSimilarity = (widthRatio + heightRatio) / 2.0
        
        // Combine position and size (70% position, 30% size)
        return (1.0 - positionDist) * 0.7 + sizeSimilarity * 0.3
    }

    /**
     * Compute property similarity (clickable, enabled, editable, etc.).
     */
    private fun propertySimilarity(ref: NodeSnapshot, candidate: NodeSnapshot): Double {
        var matches = 0
        var total = 0
        
        // Clickable
        total++
        if (ref.isClickable == candidate.isClickable) matches++
        
        // Enabled
        total++
        if (ref.isEnabled == candidate.isEnabled) matches++
        
        // Editable
        total++
        if (ref.isEditable == candidate.isEditable) matches++
        
        // Note: isScrollable removed as it's not in NodeSnapshot
        
        return matches.toDouble() / total
    }

    /**
     * Explain why a match was chosen (for debugging).
     */
    private fun explainSimilarity(ref: NodeSnapshot, candidate: NodeSnapshot): String {
        val reasons = mutableListOf<String>()
        
        if (ref.className == candidate.className) {
            reasons.add("same class")
        }
        if (ref.text == candidate.text && ref.text != null) {
            reasons.add("same text: '${ref.text}'")
        }
        if (ref.contentDescription == candidate.contentDescription && ref.contentDescription != null) {
            reasons.add("same description")
        }
        if (spatialSimilarity(ref, candidate) > 0.8) {
            reasons.add("similar position")
        }
        
        return reasons.joinToString(", ")
    }

    /**
     * Record successful match for future healing.
     */
    private fun recordMatch(selector: SelectorSpec, node: NodeSnapshot) {
        val history = selectorHistory.getOrPut(selector.key) { mutableListOf() }
        
        history.add(HistoricalMatch(
            node = node,
            timestamp = System.currentTimeMillis()
        ))
        
        // Limit history size
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
    }

    /**
     * Clear history for a specific selector.
     */
    fun clearHistory(selector: SelectorSpec) {
        selectorHistory.remove(selector.key)
    }

    /**
     * Clear all history (useful for testing or memory management).
     */
    fun clearAllHistory() {
        selectorHistory.clear()
    }

    /**
     * Get statistics about healing success.
     */
    fun getStats(): HealingStats {
        val totalSelectors = selectorHistory.size
        val totalMatches = selectorHistory.values.sumOf { it.size }
        return HealingStats(totalSelectors, totalMatches)
    }

    // ── Helper functions ─────────────────────────────────────────────────────

    /**
     * Compute Levenshtein distance between two strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
}

/**
 * Selector specification (what to find).
 */
sealed class SelectorSpec {
    abstract val key: String
    
    data class ById(val viewId: String) : SelectorSpec() {
        override val key = "id:$viewId"
    }
    
    data class ByText(val text: String) : SelectorSpec() {
        override val key = "text:$text"
    }
    
    data class ByContentDescription(val contentDescription: String) : SelectorSpec() {
        override val key = "desc:$contentDescription"
    }
    
    data class ByClass(val className: String) : SelectorSpec() {
        override val key = "class:$className"
    }
    
    data class Composite(val matchers: List<NodeMatcher>) : SelectorSpec() {
        override val key = "composite:${matchers.joinToString("|") { it.toString() }}"
    }
}

/**
 * Node matcher for composite selectors.
 */
sealed class NodeMatcher {
    abstract fun matches(node: NodeSnapshot): Boolean
    
    data class HasText(val text: String) : NodeMatcher() {
        override fun matches(node: NodeSnapshot) = node.text == text
    }
    
    data class HasClass(val className: String) : NodeMatcher() {
        override fun matches(node: NodeSnapshot) = node.className == className
    }
    
    data class IsClickable(val clickable: Boolean) : NodeMatcher() {
        override fun matches(node: NodeSnapshot) = node.isClickable == clickable
    }
}

/**
 * Historical match record.
 */
data class HistoricalMatch(
    val node: NodeSnapshot,
    val timestamp: Long
)

/**
 * Similarity candidate with confidence score.
 */
data class SimilarityCandidate(
    val node: NodeSnapshot,
    val confidence: Double,
    val reason: String
)

/**
 * Result of self-healing selector attempt.
 */
sealed class HealingResult {
    /** Found using original selector (no healing needed) */
    data class DirectMatch(val node: NodeSnapshot) : HealingResult()
    
    /** Found using similarity matching (selector was healed) */
    data class HealedMatch(
        val node: NodeSnapshot,
        val confidence: Double,
        val reason: String
    ) : HealingResult()
    
    /** Could not find match (even with healing) */
    object NotFound : HealingResult()
}

/**
 * Healing statistics.
 */
data class HealingStats(
    val totalSelectors: Int,
    val totalMatches: Int
)
