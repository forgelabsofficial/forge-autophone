# 🚀 AutoPhone - Quick Start Guide

## One-Page Setup Guide

### Step 1: Build APK (2 minutes)

```bash
cd C:\Users\user\Documents\projects\forge-os\forge-autophone

git add .
git commit -m "Standalone app with 78 automation tools"
git push origin main
```

**Wait 5-10 minutes** → Go to GitHub Actions → Download APK artifact

---

### Step 2: Install on Phone (1 minute)

```bash
adb install forge-autophone-release-unsigned.apk
```

Or manually: Copy APK to phone → Open with Files → Install

---

### Step 3: Enable Service (1 minute)

1. Open **AutoPhone** app
2. Tap **"Enable Accessibility Service"**
3. In Settings, toggle **ON** "AutoPhone Accessibility"
4. Grant permissions

**Done!** 🎉 AutoPhone is now active with 78 automation tools.

---

## What You Can Do

### Current Features
✅ View service status  
✅ See 78 automation tools available  
✅ Access capabilities overview  

### Available Tools (via service)
✅ UI inspection & navigation  
✅ Text input & gestures  
✅ OCR text extraction  
✅ Icon/image matching  
✅ Smart scrolling & waiting  
✅ Multi-touch gestures  
✅ Self-healing selectors  
✅ Form automation  
✅ Gesture recording/playback  
✅ Performance telemetry  

### Future UI Features
⏳ Tool testing panel  
⏳ Gesture recorder interface  
⏳ OCR visualization  
⏳ Performance dashboard  
⏳ Settings screen  

---

## App Interface

```
🤖 AutoPhone
AI-Powered Accessibility Automation

┌─────────────────────────┐
│ 📱 Service Status       │
│ [Enable Accessibility]  │
└─────────────────────────┘

┌─────────────────────────┐
│ 📊 Capabilities         │
│ • 78 Tools             │
│ • 4 Phases Complete    │
│ • OCR ✓ Icons ✓        │
└─────────────────────────┘

┌─────────────────────────┐
│ ⚡ Quick Actions        │
│ Tool testing coming!    │
└─────────────────────────┘
```

---

## 78 Tools Available

### Categories
1. **UI Inspection** - Find & query elements (10 tools)
2. **Text Input** - Type, clear, edit text (4 tools)
3. **Gestures** - Click, swipe, pinch, rotate (9 tools)
4. **OCR** - Extract text from screen (5 tools)
5. **Waiting** - Smart wait strategies (5 tools)
6. **Scrolling** - Scroll until found (5 tools)
7. **Events** - Real-time UI monitoring (4 tools)
8. **Icons** - Image matching (5 tools)
9. **Context** - Screen type detection (5 tools)
10. **Verification** - Action validation (6 tools)
11. **Diffing** - UI change tracking (5 tools)
12. **Self-Healing** - Adaptive selectors (5 tools)
13. **Recording** - Gesture capture (6 tools)
14. **Forms** - Smart form filling (3 tools)
15. **Telemetry** - Performance tracking (3 tools)

**Total: 78 automation tools** 🎯

---

## Tech Stack

- **Kotlin 2.0** - Language
- **Jetpack Compose** - UI
- **Material 3** - Design
- **Hilt** - Dependency injection
- **ML Kit** - OCR
- **OpenCV** - Icon matching
- **Android 8.0+** - Min SDK 26
- **Android 15** - Target SDK 35

---

## Quick Commands

### Build
```bash
.\gradlew.bat assembleRelease --no-daemon -Pkotlin.incremental=false
```

### Install
```bash
adb install forge-autophone-release-unsigned.apk
```

### Launch
```bash
adb shell am start -n com.forge.autophone/.ui.MainActivity
```

### Check Service
```bash
adb shell settings get secure enabled_accessibility_services
```

---

## Documentation

1. **QUICKSTART.md** ← You are here
2. **READY_TO_BUILD.md** - Detailed build guide
3. **COMPLETE_IMPLEMENTATION_SUMMARY.md** - Full overview
4. **STANDALONE_CONVERSION_SUMMARY.md** - What changed
5. **AUTOPHONE_COMPLETE.md** - All 78 tools reference
6. **BUILD_GUIDE.md** - Build options

---

## Troubleshooting

### Can't find app after install?
- Check app drawer for "AutoPhone"
- Verify: `adb shell pm list packages | findstr autophone`

### Service not in Settings?
- Restart phone
- Force stop and relaunch app

### Build fails locally?
- Use GitHub Actions instead (recommended)
- Check JDK 17 installed
- Verify Android SDK 35 available

---

## What's Next?

### Immediate
1. ✅ Build APK via GitHub Actions
2. ✅ Install on phone
3. ✅ Enable accessibility service
4. ✅ Verify service runs

### Future Enhancements
- 🔲 Add tool testing UI
- 🔲 Interactive gesture recorder
- 🔲 OCR visualization
- 🔲 Performance dashboard
- 🔲 Settings screen
- 🔲 Integrate with Forge OS

---

## Support

### GitHub
- Repository: forge-autophone
- Actions: Automated APK builds
- Issues: Report bugs

### Documentation
- See `/docs/` folder for detailed guides
- All 78 tools documented with examples

---

**🎉 Ready to automate Android like never before! 🤖**

*AutoPhone: AI-Powered Accessibility Automation*
