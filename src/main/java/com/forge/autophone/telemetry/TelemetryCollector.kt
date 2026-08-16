package com.forge.autophone.telemetry

import android.os.SystemClock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * TelemetryCollector — Performance monitoring and usage analytics for AutoPhone.
 *
 * Tracks:
 * - Tool call frequency and duration
 * - Success/failure rates
 * - Error patterns
 * - Performance bottlenecks
 * - Memory and resource usage
 *
 * Helps:
 * - Debug automation failures
 * - Optimize performance
 * - Understand agent behavior
 * - Identify problematic apps or patterns
 */
class TelemetryCollector {

    private val metrics = ConcurrentLinkedQueue<ToolCallMetric>()
    private val sessionMetrics = mutableMapOf<String, SessionMetrics>()
    private val errorTracker = ErrorTracker()
    
    private val maxMetricsSize = 1000 // Prevent unbounded growth
    private val sessionStartTime = System.currentTimeMillis()
    
    private val json = Json { prettyPrint = true }

    /**
     * Record a tool call execution.
     */
    fun <T> recordToolCall(toolName: String, block: () -> T): T {
        val startTime = SystemClock.elapsedRealtime()
        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        return try {
            val result = block()
            val duration = SystemClock.elapsedRealtime() - startTime
            val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryDelta = endMemory - startMemory
            
            recordSuccess(toolName, duration, memoryDelta)
            result
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            recordFailure(toolName, duration, e)
            throw e
        }
    }

    /**
     * Record a successful tool call.
     */
    private fun recordSuccess(toolName: String, durationMs: Long, memoryDelta: Long) {
        val metric = ToolCallMetric(
            toolName = toolName,
            timestamp = System.currentTimeMillis(),
            durationMs = durationMs,
            success = true,
            errorMessage = null,
            errorType = null,
            memoryDeltaBytes = memoryDelta
        )
        
        addMetric(metric)
        updateSessionMetrics(toolName, success = true, durationMs)
    }

    /**
     * Record a failed tool call.
     */
    private fun recordFailure(toolName: String, durationMs: Long, exception: Exception) {
        val metric = ToolCallMetric(
            toolName = toolName,
            timestamp = System.currentTimeMillis(),
            durationMs = durationMs,
            success = false,
            errorMessage = exception.message,
            errorType = exception.javaClass.simpleName,
            memoryDeltaBytes = 0
        )
        
        addMetric(metric)
        updateSessionMetrics(toolName, success = false, durationMs)
        errorTracker.recordError(toolName, exception)
    }

    /**
     * Add metric to queue with size limit.
     */
    private fun addMetric(metric: ToolCallMetric) {
        metrics.add(metric)
        
        // Remove oldest if exceeding limit
        while (metrics.size > maxMetricsSize) {
            metrics.poll()
        }
    }

    /**
     * Update session-level metrics.
     */
    private fun updateSessionMetrics(toolName: String, success: Boolean, durationMs: Long) {
        val session = sessionMetrics.getOrPut(toolName) {
            SessionMetrics(toolName = toolName)
        }
        
        session.totalCalls++
        if (success) session.successfulCalls++
        session.totalDurationMs += durationMs
        session.avgDurationMs = session.totalDurationMs / session.totalCalls
        
        if (durationMs > session.maxDurationMs) {
            session.maxDurationMs = durationMs
        }
        if (durationMs < session.minDurationMs || session.minDurationMs == 0L) {
            session.minDurationMs = durationMs
        }
    }

    /**
     * Get all recorded metrics.
     */
    fun getMetrics(): List<ToolCallMetric> {
        return metrics.toList()
    }

    /**
     * Get metrics for specific tool.
     */
    fun getMetricsForTool(toolName: String): List<ToolCallMetric> {
        return metrics.filter { it.toolName == toolName }
    }

    /**
     * Get session metrics for all tools.
     */
    fun getSessionMetrics(): List<SessionMetrics> {
        return sessionMetrics.values.toList()
    }

    /**
     * Get session metrics for specific tool.
     */
    fun getSessionMetricsForTool(toolName: String): SessionMetrics? {
        return sessionMetrics[toolName]
    }

    /**
     * Get overall statistics.
     */
    fun getOverallStats(): OverallStats {
        val totalCalls = metrics.size
        val successfulCalls = metrics.count { it.success }
        val failedCalls = totalCalls - successfulCalls
        val avgDuration = if (totalCalls > 0) {
            metrics.map { it.durationMs }.average()
        } else 0.0
        
        val mostUsedTool = sessionMetrics.values.maxByOrNull { it.totalCalls }?.toolName
        val slowestTool = sessionMetrics.values.maxByOrNull { it.avgDurationMs }?.toolName
        
        return OverallStats(
            totalCalls = totalCalls,
            successfulCalls = successfulCalls,
            failedCalls = failedCalls,
            successRate = if (totalCalls > 0) successfulCalls.toDouble() / totalCalls else 0.0,
            avgDurationMs = avgDuration,
            sessionDurationMs = System.currentTimeMillis() - sessionStartTime,
            mostUsedTool = mostUsedTool,
            slowestTool = slowestTool,
            uniqueTools = sessionMetrics.size
        )
    }

    /**
     * Get error statistics.
     */
    fun getErrorStats(): ErrorStats {
        return errorTracker.getStats()
    }

    /**
     * Get performance report.
     */
    fun getPerformanceReport(): PerformanceReport {
        val sessions = getSessionMetrics().sortedByDescending { it.totalCalls }
        val errors = getErrorStats()
        val overall = getOverallStats()
        
        return PerformanceReport(
            overall = overall,
            topTools = sessions.take(10),
            errors = errors,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Export metrics to JSON.
     */
    fun exportToJson(): String {
        val report = getPerformanceReport()
        return json.encodeToString(report)
    }

    /**
     * Clear all metrics (useful for testing or memory management).
     */
    fun clear() {
        metrics.clear()
        sessionMetrics.clear()
        errorTracker.clear()
    }

    /**
     * Get real-time performance snapshot.
     */
    fun getRealTimeSnapshot(): RealTimeSnapshot {
        val recentMetrics = metrics.toList().takeLast(10)
        val recentSuccessRate = if (recentMetrics.isNotEmpty()) {
            recentMetrics.count { it.success }.toDouble() / recentMetrics.size.toDouble()
        } else 0.0
        
        val recentAvgDuration = if (recentMetrics.isNotEmpty()) {
            recentMetrics.map { it.durationMs }.average()
        } else 0.0
        
        val memoryUsage = Runtime.getRuntime().let {
            (it.totalMemory() - it.freeMemory()).toDouble() / it.maxMemory()
        }
        
        return RealTimeSnapshot(
            recentSuccessRate = recentSuccessRate,
            recentAvgDurationMs = recentAvgDuration,
            memoryUsagePercent = memoryUsage * 100,
            activeTools = sessionMetrics.size,
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Error tracker for pattern analysis.
 */
class ErrorTracker {
    private val errors = ConcurrentLinkedQueue<ErrorRecord>()
    private val errorCounts = mutableMapOf<String, AtomicLong>()
    private val errorsByTool = mutableMapOf<String, MutableList<ErrorRecord>>()
    
    fun recordError(toolName: String, exception: Exception) {
        val errorType = exception.javaClass.simpleName
        val record = ErrorRecord(
            toolName = toolName,
            errorType = errorType,
            message = exception.message ?: "Unknown error",
            stackTrace = exception.stackTraceToString(),
            timestamp = System.currentTimeMillis()
        )
        
        errors.add(record)
        errorCounts.getOrPut(errorType) { AtomicLong(0) }.incrementAndGet()
        errorsByTool.getOrPut(toolName) { mutableListOf() }.add(record)
        
        // Limit size
        while (errors.size > 500) {
            errors.poll()
        }
    }
    
    fun getStats(): ErrorStats {
        val totalErrors = errors.size
        val errorTypeBreakdown = errorCounts.mapValues { it.value.get() }
        val toolsWithErrors = errorsByTool.keys.toList()
        val mostCommonError = errorTypeBreakdown.maxByOrNull { it.value }?.key
        
        return ErrorStats(
            totalErrors = totalErrors,
            errorsByType = errorTypeBreakdown,
            toolsWithErrors = toolsWithErrors,
            mostCommonError = mostCommonError,
            recentErrors = errors.toList().takeLast(10)
        )
    }
    
    fun clear() {
        errors.clear()
        errorCounts.clear()
        errorsByTool.clear()
    }
}

// ── Data classes ─────────────────────────────────────────────────────────────

/**
 * Individual tool call metric.
 */
@Serializable
data class ToolCallMetric(
    val toolName: String,
    val timestamp: Long,
    val durationMs: Long,
    val success: Boolean,
    val errorMessage: String?,
    val errorType: String?,
    val memoryDeltaBytes: Long
)

/**
 * Session-level metrics for a tool.
 */
@Serializable
data class SessionMetrics(
    val toolName: String,
    var totalCalls: Int = 0,
    var successfulCalls: Int = 0,
    var totalDurationMs: Long = 0,
    var avgDurationMs: Long = 0,
    var minDurationMs: Long = 0,
    var maxDurationMs: Long = 0
) {
    val failureRate: Double
        get() = if (totalCalls > 0) {
            (totalCalls - successfulCalls).toDouble() / totalCalls
        } else 0.0
}

/**
 * Overall statistics across all tools.
 */
@Serializable
data class OverallStats(
    val totalCalls: Int,
    val successfulCalls: Int,
    val failedCalls: Int,
    val successRate: Double,
    val avgDurationMs: Double,
    val sessionDurationMs: Long,
    val mostUsedTool: String?,
    val slowestTool: String?,
    val uniqueTools: Int
)

/**
 * Error statistics.
 */
@Serializable
data class ErrorStats(
    val totalErrors: Int,
    val errorsByType: Map<String, Long>,
    val toolsWithErrors: List<String>,
    val mostCommonError: String?,
    val recentErrors: List<ErrorRecord>
)

/**
 * Error record.
 */
@Serializable
data class ErrorRecord(
    val toolName: String,
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val timestamp: Long
)

/**
 * Performance report.
 */
@Serializable
data class PerformanceReport(
    val overall: OverallStats,
    val topTools: List<SessionMetrics>,
    val errors: ErrorStats,
    val timestamp: Long
)

/**
 * Real-time performance snapshot.
 */
@Serializable
data class RealTimeSnapshot(
    val recentSuccessRate: Double,
    val recentAvgDurationMs: Double,
    val memoryUsagePercent: Double,
    val activeTools: Int,
    val timestamp: Long
)
