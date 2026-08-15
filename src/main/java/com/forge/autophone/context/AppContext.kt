package com.forge.autophone.context

import com.forge.autophone.model.NodeSnapshot

/**
 * AppContext — Current app and screen state.
 *
 * Provides semantic understanding of what app is running and what kind of
 * screen the user is viewing. Enables context-aware automation decisions.
 */
data class AppContext(
    val packageName: String,
    val activityName: String,
    val appName: String,
    val screenType: ScreenType,
    val uiPatterns: List<UIPattern>,
    val confidence: Float = 1.0f
)

/**
 * Screen type classifications.
 */
enum class ScreenType {
    /** Login/signup screen */
    LOGIN,
    
    /** Main app screen (home, dashboard) */
    MAIN,
    
    /** Settings/preferences screen */
    SETTINGS,
    
    /** Chat/messaging interface */
    CHAT,
    
    /** Browser or webview */
    BROWSER,
    
    /** Form input screen */
    FORM,
    
    /** List view (contacts, messages, items) */
    LIST,
    
    /** Grid view (photos, apps, products) */
    GRID,
    
    /** Media player (video, audio) */
    MEDIA_PLAYER,
    
    /** Map view */
    MAP,
    
    /** Camera interface */
    CAMERA,
    
    /** Search interface */
    SEARCH,
    
    /** Details/profile view */
    DETAIL,
    
    /** Loading/splash screen */
    LOADING,
    
    /** Error screen */
    ERROR,
    
    /** Unknown screen type */
    UNKNOWN
}

/**
 * Common UI patterns detected in the current screen.
 */
enum class UIPattern {
    /** Bottom navigation bar */
    BOTTOM_NAV,
    
    /** Tab bar (top or bottom) */
    TAB_BAR,
    
    /** Navigation drawer (hamburger menu) */
    DRAWER,
    
    /** Toolbar/action bar at top */
    TOOLBAR,
    
    /** Floating action button */
    FLOATING_ACTION,
    
    /** Dialog overlay */
    DIALOG,
    
    /** Popup menu */
    POPUP_MENU,
    
    /** Snackbar notification */
    SNACKBAR,
    
    /** Progress indicator (loading) */
    PROGRESS_INDICATOR,
    
    /** Search bar */
    SEARCH_BAR,
    
    /** Card-based layout */
    CARD_LAYOUT,
    
    /** Swipeable content */
    SWIPEABLE,
    
    /** None detected */
    NONE
}

/**
 * Common field types detected in forms.
 */
enum class FieldType {
    USERNAME,
    PASSWORD,
    EMAIL,
    PHONE,
    SEARCH,
    TEXT,
    NUMBER,
    DATE,
    UNKNOWN
}

/**
 * Detected form field with metadata.
 */
data class DetectedField(
    val viewId: String?,
    val fieldType: FieldType,
    val hint: String?,
    val isRequired: Boolean,
    val snapshot: NodeSnapshot
)
