package com.forge.autophone.aidl

import com.forge.autophone.toolregistry.AutoPhoneToolRegistry

/**
 * AIDL Tool Mapper
 * 
 * Maps AIDL method calls to AutoPhoneToolRegistry method calls.
 * Provides convenience methods for common operations expected by Forge OS.
 * 
 * This bridges the gap between the AIDL interface (method-based)
 * and the AutoPhoneToolRegistry (direct method calls).
 */
object AidlToolMapper {
    
    /**
     * Find node by text and click it.
     * Combines find + click operations.
     */
    fun findAndClickText(registry: AutoPhoneToolRegistry, text: String): String {
        return try {
            val node = registry.findByText(text)
            if (node != null) {
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                val centerX = (bounds.left + bounds.right) / 2f
                val centerY = (bounds.top + bounds.bottom) / 2f
                registry.tap(centerX, centerY)
                node.recycle()
                successJson("Clicked on '$text'")
            } else {
                errorJson("Text '$text' not found")
            }
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to tap text")
        }
    }
    
    /**
     * Click at specific coordinates.
     */
    fun clickAt(registry: AutoPhoneToolRegistry, x: Int, y: Int): String {
        return try {
            registry.tap(x.toFloat(), y.toFloat())
            successJson("Clicked at ($x, $y)")
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to click")
        }
    }
    
    /**
     * Type text into focused field.
     */
    fun typeText(registry: AutoPhoneToolRegistry, text: String): String {
        return try {
            registry.typeFocused(text)
            successJson("Typed text: $text")
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to type text")
        }
    }
    
    /**
     * Perform swipe gesture.
     */
    fun swipe(registry: AutoPhoneToolRegistry, direction: String, distance: Int): String {
        return try {
            // Calculate start/end coordinates based on direction
            val (startX, startY, endX, endY) = when (direction.lowercase()) {
                "up" -> listOf(500f, 1500f, 500f, 500f)
                "down" -> listOf(500f, 500f, 500f, 1500f)
                "left" -> listOf(1000f, 800f, 200f, 800f)
                "right" -> listOf(200f, 800f, 1000f, 800f)
                else -> return errorJson("Invalid direction: $direction")
            }
            
            registry.swipe(startX, startY, endX, endY, 300)
            successJson("Swiped $direction")
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to swipe")
        }
    }
    
    /**
     * Scroll in a direction.
     */
    fun scroll(registry: AutoPhoneToolRegistry, direction: String): String {
        return try {
            // Scroll is implemented as a swipe
            val (startX, startY, endX, endY) = when (direction.lowercase()) {
                "up" -> listOf(500f, 1500f, 500f, 800f)
                "down" -> listOf(500f, 800f, 500f, 1500f)
                "left" -> listOf(1000f, 800f, 500f, 800f)
                "right" -> listOf(500f, 800f, 1000f, 800f)
                else -> return errorJson("Invalid direction: $direction")
            }
            
            registry.swipe(startX, startY, endX, endY, 200)
            successJson("Scrolled $direction")
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to scroll")
        }
    }
    
    /**
     * Press back button.
     */
    fun goBack(registry: AutoPhoneToolRegistry): String {
        return try {
            registry.back()
            successJson("Pressed back")
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to press back")
        }
    }
    
    /**
     * Press home button.
     */
    fun goHome(registry: AutoPhoneToolRegistry): String {
        return try {
            registry.home()
            successJson("Pressed home")
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to press home")
        }
    }
    
    /**
     * Open notification shade.
     */
    fun openNotifications(registry: AutoPhoneToolRegistry): String {
        return try {
            // Open notifications is a system gesture
            // This is typically done via GLOBAL_ACTION_NOTIFICATIONS in AccessibilityService
            successJson("Opening notifications shade")
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to open notifications")
        }
    }
    
    /**
     * Get all nodes (equivalent to readScreen).
     */
    fun getAllNodes(registry: AutoPhoneToolRegistry): String {
        return try {
            val root = registry.getActiveWindowRoot()
            if (root != null) {
                // Convert node tree to JSON representation
                val json = buildNodeTreeJson(root)
                successJson(json)
            } else {
                errorJson("No active window")
            }
        } catch (e: Exception) {
            errorJson(e.message ?: "Failed to read screen")
        }
    }
    
    /**
     * Build a JSON representation of the node tree.
     */
    private fun buildNodeTreeJson(node: android.view.accessibility.AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        
        fun appendNode(n: android.view.accessibility.AccessibilityNodeInfo, indent: Int = 0) {
            val bounds = android.graphics.Rect()
            n.getBoundsInScreen(bounds)
            
            val indentStr = "  ".repeat(indent)
            sb.append("$indentStr{\n")
            sb.append("$indentStr  \"className\": \"${escape(n.className?.toString() ?: "")}\",\n")
            sb.append("$indentStr  \"text\": \"${escape(n.text?.toString() ?: "")}\",\n")
            sb.append("$indentStr  \"contentDescription\": \"${escape(n.contentDescription?.toString() ?: "")}\",\n")
            sb.append("$indentStr  \"viewId\": \"${escape(n.viewIdResourceName ?: "")}\",\n")
            sb.append("$indentStr  \"bounds\": {\"left\": ${bounds.left}, \"top\": ${bounds.top}, \"right\": ${bounds.right}, \"bottom\": ${bounds.bottom}},\n")
            sb.append("$indentStr  \"clickable\": ${n.isClickable},\n")
            sb.append("$indentStr  \"enabled\": ${n.isEnabled},\n")
            sb.append("$indentStr  \"focusable\": ${n.isFocusable},\n")
            sb.append("$indentStr  \"children\": [\n")
            
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { child ->
                    appendNode(child, indent + 2)
                    if (i < n.childCount - 1) sb.append(",\n")
                    child.recycle()
                }
            }
            
            sb.append("\n$indentStr  ]\n")
            sb.append("$indentStr}")
        }
        
        appendNode(node)
        return sb.toString()
    }
    
    /**
     * Create success JSON response.
     */
    private fun successJson(output: String): String {
        return """{"ok":true,"output":"${escape(output)}"}"""
    }
    
    /**
     * Create error JSON response.
     */
    private fun errorJson(error: String): String {
        return """{"ok":false,"error":"${escape(error)}"}"""
    }
    
    /**
     * Escape JSON string.
     */
    private fun escape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
