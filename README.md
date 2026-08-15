# forge-autophone

> **Forge OS Accessibility Layer** — Android `AccessibilityService` module providing the AI agent runtime with full programmatic control over any foreground app's UI.

---

## What it does

AutoPhone is the "hands and eyes" of the Forge OS agent on Android. It sits as a system `AccessibilityService` and exposes three capability groups to the agent tool registry:

| Capability | Class | What it enables |
|---|---|---|
| **UI Inspection** | `AutoPhoneAccessibilityService` | Read the full live node tree of any app |
| **Gesture Dispatch** | `GestureHandler` | Tap, swipe, long-press, pinch via `dispatchGesture()` |
| **Text Input** | `TextEntryService` | Type into any field via `ACTION_SET_TEXT` |
| **Navigation** | `NavigationActions` | Back, Home, Recents, Screenshot, Quick Settings |
| **Tree Snapshot** | `UITreeInspector` | Walk nodes → stable `NodeSnapshot` objects for agent reasoning |

---

## Module structure

```
forge-autophone/
├── build.gradle.kts
├── consumer-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── res/
    │   │   ├── values/strings.xml
    │   │   └── xml/autophone_accessibility_config.xml
    │   └── java/com/forge/autophone/
    │       ├── AutoPhoneAccessibilityService.kt   ← root entry point
    │       ├── AutoPhoneApplication.kt            ← @HiltAndroidApp
    │       ├── accessibility/
    │       │   └── TextEntryService.kt
    │       ├── di/
    │       │   └── AutoPhoneModule.kt             ← Hilt @Module
    │       ├── extensions/
    │       │   ├── AccessibilityNodeInfoExtensions.kt
    │       │   ├── GestureExtensions.kt
    │       │   └── TextTypingExtensions.kt
    │       ├── inspector/
    │       │   └── UITreeInspector.kt
    │       ├── model/
    │       │   └── NodeSnapshot.kt
    │       ├── service/
    │       │   ├── GestureHandler.kt
    │       │   └── NavigationActions.kt
    │       ├── toolregistry/
    │       │   └── AutoPhoneToolRegistry.kt       ← agent tool bindings
    │       ├── ui/settings/
    │       │   └── PermissionSettingsScreen.kt    ← Compose M3
    │       └── viewmodel/
    │           └── PermissionViewModel.kt
    └── test/
        └── java/com/forge/autophone/
            ├── NodeSnapshotTest.kt
            └── service/NavigationActionsTest.kt
```

---

## Required Android permissions

Declared in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

The user must manually enable AutoPhone in **Settings → Accessibility → Forge AutoPhone**.  
The `PermissionSettingsScreen` guides them through both required grants.

---

## Accessibility service config

Key flags in `res/xml/autophone_accessibility_config.xml`:

| Flag | Value | Why |
|---|---|---|
| `canPerformGestures` | `true` | Required for `dispatchGesture()` |
| `canRetrieveWindowContent` | `true` | Required for `rootInActiveWindow` |
| `accessibilityEventTypes` | `typeAllMask` | Full UI event coverage |
| `accessibilityFlags` | `flagIncludeNotImportantViews \| flagReportViewIds` | See all nodes + resource IDs |

---

## Agent tool usage (via `AutoPhoneToolRegistry`)

```kotlin
val tools = AutoPhoneToolRegistry(AutoPhoneAccessibilityService.instance!!)

// Tap a button by coordinates
tools.tap(540f, 1200f)

// Type into a field by view ID
tools.typeText("hello@forge.ai", "com.example.app:id/email_input")

// Navigate home
tools.home()

// Get all clickable nodes for agent reasoning
val clickable = tools.getActiveWindowRoot()
    ?.let { UITreeInspector(service).clickableNodes() }
```

---

## Adding to your Forge OS host app

In your host app's `settings.gradle.kts`:
```kotlin
include(":forge-autophone")
project(":forge-autophone").projectDir = File("../forge-autophone")
```

In the host module's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":forge-autophone"))
}
```
