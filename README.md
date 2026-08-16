# 🤖 AutoPhone - AI-Powered Accessibility Automation

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

AutoPhone is a powerful accessibility automation layer for Android that provides 78 comprehensive tools for programmatic UI control, text extraction, gesture automation, and intelligent form handling.

---

## ✨ Features

### 🎯 78 Automation Tools
- **UI Inspection** - Find and query any UI element
- **Text Input** - Type, clear, and edit text fields
- **Gestures** - Click, swipe, pinch, zoom, rotate
- **OCR** - Extract text from anywhere on screen (ML Kit)
- **Icon Matching** - Find elements by image (OpenCV)
- **Smart Waiting** - Wait for conditions intelligently
- **Scrolling** - Scroll until element found
- **Events** - Real-time UI change monitoring
- **Context Awareness** - Detect screen types and patterns
- **Verification** - Validate actions and rollback on failure
- **Self-Healing** - Selectors that adapt to UI changes
- **Gesture Recording** - Capture and replay complex interactions
- **Form Automation** - Smart field detection and validation
- **Telemetry** - Performance monitoring and analytics

### 🎨 Modern UI
- **Real-Time Status Detection** - Shows if service is enabled (🟢/⚪)
- **Automatic Updates** - UI refreshes when returning from Settings
- **Color-Coded Indicators** - Green when ready, gray when not
- **Integration Guide** - Clear explanation of Forge OS usage
- **Material 3 Design** - Beautiful, modern interface
- **Lifecycle-Aware** - Smart status tracking

### 🎨 Modern Architecture
- **Kotlin 2.0** - Modern language features
- **Jetpack Compose** - Declarative UI
- **Material 3** - Latest design system
- **Hilt** - Dependency injection
- **ML Kit** - On-device OCR
- **OpenCV** - Computer vision
- **Coroutines** - Async operations

---

## 📱 Screenshots

```
┌─────────────────────────────┐
│   🤖 AutoPhone             │
│   AI-Powered Accessibility  │
├─────────────────────────────┤
│                             │
│  📱 Service Status          │
│  [Enable Accessibility]     │
│                             │
│  📊 Capabilities            │
│  • 78 Automation Tools      │
│  • OCR ✓ Icons ✓ Forms ✓   │
│                             │
│  ⚡ Quick Actions           │
│  • Test tools               │
│  • Record gestures          │
│  • Monitor performance      │
│                             │
└─────────────────────────────┘
```

---

## 🚀 Quick Start

### Installation

1. **Download APK:**
   - Go to [GitHub Releases](../../releases)
   - Download latest `forge-autophone-release.apk`
   - Or build from source (see below)

2. **Install on device:**
   ```bash
   adb install forge-autophone-release-unsigned.apk
   ```

3. **Enable Accessibility Service:**
   - Open AutoPhone app
   - Tap "Enable Accessibility Service"
   - Toggle ON in Settings
   - Grant permissions

### Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/forge-autophone.git
cd forge-autophone

# Build release APK
./gradlew assembleRelease --no-daemon -Pkotlin.incremental=false

# Output: build/outputs/apk/release/forge-autophone-release-unsigned.apk
```

---

## 🛠️ Usage

### As Standalone App
1. Install and enable accessibility service
2. Use AutoPhone UI to test tools
3. Monitor performance and telemetry

### As Library (Future)
```kotlin
// In your app's build.gradle
dependencies {
    implementation("com.forge:autophone:1.0.0")
}

// Use AutoPhone tools
val toolRegistry = AutoPhoneToolRegistry()
toolRegistry.executeTool("click", mapOf("nodeId" to "button_submit"))
```

### With Forge OS
AutoPhone serves as the accessibility layer for Forge OS AI agent, enabling autonomous app control and automation.

---

## 📚 Documentation

- **[Quick Start](QUICKSTART.md)** - Get started in 5 minutes
- **[Permissions & Integration](PERMISSIONS_AND_INTEGRATION.md)** - How Forge OS uses AutoPhone
- **[UI States Guide](UI_STATES_GUIDE.md)** - Visual guide to app states
- **[Complete Implementation](COMPLETE_IMPLEMENTATION_SUMMARY.md)** - Full overview
- **[All 78 Tools](AUTOPHONE_COMPLETE.md)** - Detailed tool reference
- **[Build Guide](BUILD_GUIDE.md)** - Building and deployment
- **[Roadmap](AUTOPHONE_IMPROVEMENT_ROADMAP.md)** - Design and architecture

---

## 🧰 Technology Stack

### Core
- **Language:** Kotlin 2.0
- **Min SDK:** 26 (Android 8.0+)
- **Target SDK:** 35 (Android 15)
- **Build Tool:** Gradle 8.7

### UI
- Jetpack Compose
- Material 3
- AndroidX Core & Lifecycle

### Dependencies
- **Hilt** - Dependency injection
- **ML Kit Text Recognition** (16.0.0) - OCR
- **OpenCV Android** (4.8.0) - Computer vision
- **Kotlinx Serialization** (1.6.3) - Data persistence
- **Kotlin Coroutines** - Async operations

---

## 🗂️ Project Structure

```
forge-autophone/
├── src/main/
│   ├── java/com/forge/autophone/
│   │   ├── AutoPhoneApplication.kt         # App entry point
│   │   ├── AutoPhoneAccessibilityService.kt # Main service
│   │   ├── AutoPhoneToolRegistry.kt         # 78 tools
│   │   │
│   │   ├── ui/                              # Compose UI
│   │   ├── service/                         # Core services
│   │   ├── ocr/                             # Text extraction
│   │   ├── vision/                          # Icon matching
│   │   ├── scroll/                          # Smart scrolling
│   │   ├── wait/                            # Wait strategies
│   │   ├── events/                          # Event streaming
│   │   ├── context/                         # Context tracking
│   │   ├── verification/                    # Action verification
│   │   ├── diff/                            # UI diffing
│   │   ├── healing/                         # Self-healing
│   │   ├── recording/                       # Gesture recording
│   │   ├── form/                            # Form automation
│   │   └── telemetry/                       # Performance
│   │
│   └── res/                                 # Resources
│
├── build.gradle.kts                         # Build config
├── gradle.properties                        # Gradle settings
└── README.md                                # This file
```

---

## 🔧 Development

### Requirements
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35
- Gradle 8.7

### Setup
```bash
# Clone repository
git clone https://github.com/yourusername/forge-autophone.git

# Open in Android Studio
# File → Open → Select forge-autophone folder

# Sync Gradle
# File → Sync Project with Gradle Files

# Run on device
# Run → Run 'app'
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run lint
./gradlew lint

# Build debug APK
./gradlew assembleDebug
```

---

## 🎯 Use Cases

### 1. UI Automation Testing
- Automate user workflows
- Test complex interactions
- Validate form handling
- Monitor performance

### 2. Accessibility Enhancement
- Screen reader support
- Voice control
- Switch access
- Custom navigation

### 3. AI Agent Control
- Autonomous app navigation
- Task automation
- Cross-app workflows
- Intelligent form filling

### 4. Development & Debugging
- Inspect UI hierarchies
- Monitor UI changes
- Test gesture handling
- Performance profiling

---

## 📊 78 Tools Overview

### Categories
| Category | Tools | Description |
|----------|-------|-------------|
| UI Inspection | 10 | Find and query elements |
| Text Input | 4 | Type and edit text |
| Gestures | 9 | Clicks, swipes, multi-touch |
| OCR | 5 | Text extraction |
| Waiting | 5 | Smart wait strategies |
| Scrolling | 5 | Scroll automation |
| Events | 4 | Real-time monitoring |
| Icons | 5 | Image matching |
| Context | 5 | Screen detection |
| Verification | 6 | Action validation |
| Diffing | 5 | UI change tracking |
| Self-Healing | 5 | Adaptive selectors |
| Recording | 6 | Gesture capture |
| Forms | 3 | Form automation |
| Telemetry | 3 | Performance metrics |

**Total: 78 automation tools** 🎯

---

## 🚧 Roadmap

### Phase 1: Core ✅
- [x] OCR text extraction
- [x] Smart waiting
- [x] Scrolling automation
- [x] Event streaming

### Phase 2: Vision ✅
- [x] Icon matching
- [x] Multi-touch gestures
- [x] Advanced gestures

### Phase 3: Intelligence ✅
- [x] Context awareness
- [x] Action verification
- [x] UI diffing

### Phase 4: Advanced ✅
- [x] Self-healing selectors
- [x] Gesture recording
- [x] Form automation
- [x] Telemetry

### Phase 5: UI Enhancement 🔄
- [ ] Tool testing panel
- [ ] Gesture recorder UI
- [ ] OCR visualization
- [ ] Performance dashboard
- [ ] Settings screen

### Phase 6: Integration 📋
- [ ] Forge OS integration
- [ ] AIDL interface
- [ ] Remote control API
- [ ] Cloud gesture library

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

### Guidelines
1. Follow Kotlin coding conventions
2. Add tests for new features
3. Update documentation
4. Ensure CI passes

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **ForgeOS Team** - *Initial work*

---

## 🙏 Acknowledgments

- ML Kit for OCR capabilities
- OpenCV for computer vision
- Android Accessibility Framework
- Jetpack Compose team
- Kotlin community

---

## 📞 Support

- **Documentation:** See `/docs` folder
- **Issues:** [GitHub Issues](../../issues)
- **Discussions:** [GitHub Discussions](../../discussions)

---

## 🌟 Show Your Support

Give a ⭐️ if this project helped you!

---

**🤖 AutoPhone: AI-Powered Accessibility Automation 📱**

*Transforming Android automation with intelligent tools and modern architecture*
