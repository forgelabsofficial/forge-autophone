package com.forge.autophone.context

import android.content.pm.PackageManager
import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.inspector.UITreeInspector
import com.forge.autophone.model.NodeSnapshot

/**
 * AppContextTracker — detects app state and screen types.
 *
 * Analyzes the UI tree to determine:
 * - What app is running
 * - What type of screen is displayed (login, settings, chat, etc.)
 * - What UI patterns are present (bottom nav, drawer, toolbar, etc.)
 *
 * Enables context-aware automation: "I'm on a login screen, so I should
 * find username/password fields."
 */
class AppContextTracker(private val service: AutoPhoneAccessibilityService) {

    private val packageManager: PackageManager = service.packageManager
    private var cachedContext: AppContext? = null
    private var cacheTimestamp: Long = 0
    private val cacheValidityMs = 1000L // Cache for 1 second

    /**
     * Get the current app context.
     * Results are cached for 1 second to avoid repeated analysis.
     */
    fun getCurrentContext(): AppContext? {
        val now = System.currentTimeMillis()
        
        // Return cached result if still valid
        if (cachedContext != null && now - cacheTimestamp < cacheValidityMs) {
            return cachedContext
        }

        val root = service.getActiveWindowRoot() ?: return null
        val snapshot = UITreeInspector(service).snapshot()

        val packageName = root.packageName?.toString() ?: return null
        val activityName = root.className?.toString() ?: ""
        val appName = getAppName(packageName)

        val screenType = detectScreenType(snapshot, packageName, activityName)
        val uiPatterns = detectUIPatterns(snapshot)

        val context = AppContext(
            packageName = packageName,
            activityName = activityName,
            appName = appName,
            screenType = screenType,
            uiPatterns = uiPatterns
        )

        cachedContext = context
        cacheTimestamp = now

        return context
    }

    /**
     * Detect the type of screen being displayed.
     */
    private fun detectScreenType(
        snapshot: List<NodeSnapshot>,
        packageName: String,
        activityName: String
    ): ScreenType {
        val texts = snapshot.mapNotNull { it.text?.lowercase() }
        val hints = snapshot.mapNotNull { it.contentDescription?.lowercase() }
        val viewIds = snapshot.mapNotNull { it.viewId?.lowercase() }
        val classNames = snapshot.mapNotNull { it.className?.lowercase() }

        // Login screen detection
        if (detectLogin(texts, hints, viewIds)) return ScreenType.LOGIN

        // Settings screen detection
        if (detectSettings(texts, hints, viewIds, activityName)) return ScreenType.SETTINGS

        // Chat screen detection
        if (detectChat(texts, hints, viewIds, classNames)) return ScreenType.CHAT

        // Browser detection
        if (detectBrowser(packageName, classNames)) return ScreenType.BROWSER

        // Form detection
        if (detectForm(snapshot)) return ScreenType.FORM

        // List detection
        if (detectList(classNames)) return ScreenType.LIST

        // Grid detection
        if (detectGrid(classNames)) return ScreenType.GRID

        // Media player detection
        if (detectMediaPlayer(viewIds, classNames)) return ScreenType.MEDIA_PLAYER

        // Map detection
        if (detectMap(packageName, classNames)) return ScreenType.MAP

        // Camera detection
        if (detectCamera(packageName, viewIds)) return ScreenType.CAMERA

        // Search detection
        if (detectSearch(snapshot)) return ScreenType.SEARCH

        // Loading screen detection
        if (detectLoading(snapshot)) return ScreenType.LOADING

        // Error screen detection
        if (detectError(texts)) return ScreenType.ERROR

        // Default to MAIN if no specific type detected
        return ScreenType.MAIN
    }

    /**
     * Detect UI patterns present in the current screen.
     */
    private fun detectUIPatterns(snapshot: List<NodeSnapshot>): List<UIPattern> {
        val patterns = mutableListOf<UIPattern>()
        val viewIds = snapshot.mapNotNull { it.viewId?.lowercase() }
        val classNames = snapshot.mapNotNull { it.className?.lowercase() }

        // Bottom navigation
        if (viewIds.any { it.contains("bottom") && it.contains("nav") }) {
            patterns.add(UIPattern.BOTTOM_NAV)
        }

        // Tab bar
        if (viewIds.any { it.contains("tab") } || 
            classNames.any { it.contains("tablayout") }) {
            patterns.add(UIPattern.TAB_BAR)
        }

        // Drawer
        if (viewIds.any { it.contains("drawer") } ||
            classNames.any { it.contains("drawerlayout") }) {
            patterns.add(UIPattern.DRAWER)
        }

        // Toolbar
        if (viewIds.any { it.contains("toolbar") || it.contains("actionbar") } ||
            classNames.any { it.contains("toolbar") }) {
            patterns.add(UIPattern.TOOLBAR)
        }

        // Floating action button
        if (viewIds.any { it.contains("fab") || it.contains("floating") } ||
            classNames.any { it.contains("floatingactionbutton") }) {
            patterns.add(UIPattern.FLOATING_ACTION)
        }

        // Dialog
        if (classNames.any { it.contains("dialog") }) {
            patterns.add(UIPattern.DIALOG)
        }

        // Progress indicator
        if (classNames.any { it.contains("progress") }) {
            patterns.add(UIPattern.PROGRESS_INDICATOR)
        }

        // Search bar
        if (viewIds.any { it.contains("search") } ||
            snapshot.any { it.className?.contains("SearchView", ignoreCase = true) == true }) {
            patterns.add(UIPattern.SEARCH_BAR)
        }

        if (patterns.isEmpty()) {
            patterns.add(UIPattern.NONE)
        }

        return patterns
    }

    /**
     * Detect form fields and their types.
     */
    fun detectFormFields(): List<DetectedField> {
        val snapshot = UITreeInspector(service).snapshot()
        val fields = mutableListOf<DetectedField>()

        snapshot.filter { it.isEditable }.forEach { node ->
            val fieldType = classifyFieldType(node)
            fields.add(DetectedField(
                viewId = node.viewId,
                fieldType = fieldType,
                hint = node.contentDescription,
                isRequired = node.contentDescription?.contains("required", ignoreCase = true) == true,
                snapshot = node
            ))
        }

        return fields
    }

    private fun classifyFieldType(node: NodeSnapshot): FieldType {
        val hint = node.contentDescription?.lowercase() ?: ""
        val viewId = node.viewId?.lowercase() ?: ""

        return when {
            hint.contains("username") || viewId.contains("username") -> FieldType.USERNAME
            hint.contains("password") || viewId.contains("password") -> FieldType.PASSWORD
            hint.contains("email") || viewId.contains("email") -> FieldType.EMAIL
            hint.contains("phone") || viewId.contains("phone") -> FieldType.PHONE
            hint.contains("search") || viewId.contains("search") -> FieldType.SEARCH
            node.className?.contains("NumberPicker", ignoreCase = true) == true -> FieldType.NUMBER
            node.className?.contains("DatePicker", ignoreCase = true) == true -> FieldType.DATE
            else -> FieldType.TEXT
        }
    }

    // ── Detection helpers ────────────────────────────────────────────────────

    private fun detectLogin(texts: List<String>, hints: List<String>, viewIds: List<String>): Boolean {
        val loginKeywords = listOf("login", "sign in", "username", "password", "log in")
        return texts.any { text -> loginKeywords.any { text.contains(it) } } ||
               hints.any { hint -> loginKeywords.any { hint.contains(it) } } ||
               viewIds.any { id -> loginKeywords.any { id.contains(it) } }
    }

    private fun detectSettings(texts: List<String>, hints: List<String>, viewIds: List<String>, activity: String): Boolean {
        val settingsKeywords = listOf("settings", "preferences", "options", "configuration")
        return texts.any { text -> settingsKeywords.any { text.contains(it) } } ||
               hints.any { hint -> settingsKeywords.any { hint.contains(it) } } ||
               viewIds.any { id -> settingsKeywords.any { id.contains(it) } } ||
               activity.contains("settings", ignoreCase = true)
    }

    private fun detectChat(texts: List<String>, hints: List<String>, viewIds: List<String>, classNames: List<String>): Boolean {
        val chatKeywords = listOf("message", "chat", "conversation", "send")
        return (texts.any { text -> chatKeywords.any { text.contains(it) } } ||
                hints.any { hint -> chatKeywords.any { hint.contains(it) } } ||
                viewIds.any { id -> chatKeywords.any { id.contains(it) } }) &&
               classNames.any { it.contains("recyclerview") || it.contains("listview") }
    }

    private fun detectBrowser(packageName: String, classNames: List<String>): Boolean {
        return packageName.contains("chrome") || packageName.contains("browser") ||
               classNames.any { it.contains("webview") }
    }

    private fun detectForm(snapshot: List<NodeSnapshot>): Boolean {
        val editableCount = snapshot.count { it.isEditable }
        return editableCount >= 2 // At least 2 input fields suggests a form
    }

    private fun detectList(classNames: List<String>): Boolean {
        return classNames.any { it.contains("recyclerview") || it.contains("listview") }
    }

    private fun detectGrid(classNames: List<String>): Boolean {
        return classNames.any { it.contains("gridview") || it.contains("gridlayout") }
    }

    private fun detectMediaPlayer(viewIds: List<String>, classNames: List<String>): Boolean {
        val mediaKeywords = listOf("play", "pause", "video", "audio", "player")
        return viewIds.any { id -> mediaKeywords.any { id.contains(it) } } ||
               classNames.any { it.contains("mediacontroller") || it.contains("videoview") }
    }

    private fun detectMap(packageName: String, classNames: List<String>): Boolean {
        return packageName.contains("maps") || classNames.any { it.contains("mapview") }
    }

    private fun detectCamera(packageName: String, viewIds: List<String>): Boolean {
        return packageName.contains("camera") || viewIds.any { it.contains("camera") || it.contains("shutter") }
    }

    private fun detectSearch(snapshot: List<NodeSnapshot>): Boolean {
        return snapshot.any { 
            it.className?.contains("SearchView", ignoreCase = true) == true ||
            it.viewId?.contains("search", ignoreCase = true) == true
        }
    }

    private fun detectLoading(snapshot: List<NodeSnapshot>): Boolean {
        return snapshot.any { 
            it.className?.contains("ProgressBar", ignoreCase = true) == true ||
            it.text?.contains("loading", ignoreCase = true) == true
        }
    }

    private fun detectError(texts: List<String>): Boolean {
        val errorKeywords = listOf("error", "failed", "unable", "cannot", "try again", "oops")
        return texts.any { text -> errorKeywords.any { text.contains(it) } }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Clear the cache to force fresh detection.
     */
    fun clearCache() {
        cachedContext = null
        cacheTimestamp = 0
    }
}
