# 🧪 AutoPhone Testing Guide

## Quick Testing Checklist

Once the APK is built and installed, follow these steps to verify the integration works.

---

## 📱 Installation

### 1. Download APK
```bash
# From GitHub Actions artifacts page:
# https://github.com/forgelabsofficial/forge-autophone/actions

# Download: forge-autophone-release-unsigned.apk
```

### 2. Install on Device
```bash
adb install forge-autophone-release-unsigned.apk

# Or drag-and-drop APK to device and install manually
```

### 3. Launch App
```bash
adb shell am start -n com.forge.autophone/.ui.MainActivity

# Or tap AutoPhone icon in launcher
```

---

## ⚙️ Enable Accessibility Service

### Method 1: Via App UI
1. Open AutoPhone app
2. See status: ⚪ Service Disabled
3. Tap "Open Accessibility Settings" button
4. Find "AutoPhone" in accessibility services list
5. Toggle ON
6. Confirm permission dialog
7. Return to AutoPhone app
8. Status should now show: 🟢 Service Enabled

### Method 2: Via ADB
```bash
# Enable accessibility service via ADB
adb shell settings put secure enabled_accessibility_services \
  com.forge.autophone/.AutoPhoneAccessibilityService

adb shell settings put secure accessibility_enabled 1
```

### Verify Service Running
```bash
adb shell dumpsys accessibility | grep AutoPhone

# Expected output:
# Service[label=AutoPhone, feedbackType=0, ...]
```

---

## 🔌 Test AIDL Binding from Forge OS

### 1. Check Service Discoverable
```kotlin
// In Forge OS code:
val intent = Intent("com.forge.autophone.IAutoPhoneService")
    .setPackage("com.forge.autophone")

val resolveInfo = packageManager.resolveService(intent, 0)
Timber.i("AutoPhone service found: ${resolveInfo != null}")

// Expected: true
```

### 2. Bind to Service
```kotlin
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val autoPhone = IAutoPhoneService.Stub.asInterface(binder)
        Timber.i("AutoPhone connected!")
        
        // Test methods
        testAutoPhoneMethods(autoPhone)
    }
    
    override fun onServiceDisconnected(name: ComponentName) {
        Timber.w("AutoPhone disconnected")
    }
}

val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
Timber.i("Bind request sent: $bound")

// Expected: bound = true, onServiceConnected called
```

### 3. Test AIDL Methods
```kotlin
fun testAutoPhoneMethods(autoPhone: IAutoPhoneService) {
    // Test 1: Check service active
    val isActive = autoPhone.isServiceActive()
    Timber.i("Service active: $isActive")
    // Expected: true (if accessibility service enabled)
    
    // Test 2: Read screen
    val screenJson = autoPhone.readScreen()
    Timber.i("Screen data: $screenJson")
    // Expected: {"ok":true,"output":"{...node tree...}"}
    
    // Test 3: Go home
    val homeResult = autoPhone.goHome()
    Timber.i("Home result: $homeResult")
    // Expected: {"ok":true,"output":"Pressed home"}
    
    // Test 4: Tap by text (if button exists)
    val tapResult = autoPhone.tapByText("Settings")
    Timber.i("Tap result: $tapResult")
    // Expected: {"ok":true,"output":"Clicked on 'Settings'"} or error if not found
}
```

---

## 🧪 Test Cases

### Test Case 1: Service Status Detection ✅

**Steps:**
1. Open AutoPhone app
2. Observe status indicator

**Expected (Service Disabled):**
- ⚪ White indicator
- Text: "Service Disabled"
- Button: "Open Accessibility Settings"

**Expected (Service Enabled):**
- 🟢 Green indicator  
- Text: "Service Enabled"
- Same button available

---

### Test Case 2: AIDL Binding ✅

**Steps:**
1. Install Forge OS
2. Launch Forge OS
3. Forge OS attempts to bind to AutoPhone

**Expected:**
```
# Forge OS logs:
AutoPhoneConnection: Binding to AutoPhone...
AutoPhoneConnection: AutoPhone connected!

# AutoPhone logs:
AutoPhoneService: AutoPhone AIDL service bound by com.forge.os
```

---

### Test Case 3: Read Screen ✅

**Steps:**
1. Ensure accessibility service enabled
2. From Forge OS: `autoPhone.readScreen()`

**Expected Response:**
```json
{
  "ok": true,
  "output": "{\"className\":\"android.widget.FrameLayout\",\"text\":\"\",\"children\":[...]}"
}
```

**Parse JSON:**
```kotlin
val response = Json.decodeFromString<Map<String, Any>>(screenJson)
if (response["ok"] == true) {
    val nodeTree = response["output"] as String
    // Parse node tree for UI elements
}
```

---

### Test Case 4: Tap by Text ✅

**Setup:**
1. Open an app with a visible button (e.g., "Settings")
2. From Forge OS: `autoPhone.tapByText("Settings")`

**Expected (Text Found):**
```json
{"ok": true, "output": "Clicked on 'Settings'"}
```

**Expected (Text Not Found):**
```json
{"ok": false, "error": "Text 'Settings' not found"}
```

**Device Behavior:**
- Button should be tapped
- Action should execute (e.g., opens settings)

---

### Test Case 5: Type Text ✅

**Setup:**
1. Open an app with a text field
2. Focus the text field
3. From Forge OS: `autoPhone.typeText("Hello AutoPhone")`

**Expected:**
```json
{"ok": true, "output": "Typed text: Hello AutoPhone"}
```

**Device Behavior:**
- Text appears in focused field
- Cursor after typed text

---

### Test Case 6: Navigation Actions ✅

**Test 6a: Go Home**
```kotlin
autoPhone.goHome()
```
**Expected:**
- Device goes to home screen
- Response: `{"ok": true, "output": "Pressed home"}`

**Test 6b: Go Back**
```kotlin
autoPhone.goBack()
```
**Expected:**
- Device navigates back
- Response: `{"ok": true, "output": "Pressed back"}`

---

### Test Case 7: Service Disabled Handling ✅

**Steps:**
1. Disable AutoPhone accessibility service
2. From Forge OS: `autoPhone.readScreen()`

**Expected:**
```json
{"ok": false, "error": "Accessibility service not enabled"}
```

**All methods should return this error when service disabled.**

---

### Test Case 8: Swipe Gestures ✅

**Test 8a: Swipe Up**
```kotlin
autoPhone.swipe("up", 500)
```
**Expected:**
- Screen content swipes up (scroll down effect)
- Response: `{"ok": true, "output": "Swiped up"}`

**Test 8b: Swipe Left**
```kotlin
autoPhone.swipe("left", 500)
```
**Expected:**
- Screen content swipes left
- Response: `{"ok": true, "output": "Swiped left"}`

---

## 🐛 Troubleshooting

### Issue 1: Service Not Binding

**Symptoms:**
```
AutoPhoneConnection: Binding failed
resolveService() returns null
```

**Solutions:**
1. Verify AutoPhone is installed:
   ```bash
   adb shell pm list packages | grep forge.autophone
   ```

2. Verify service declared in manifest:
   ```bash
   adb shell dumpsys package com.forge.autophone | grep -A5 "Service"
   ```

3. Check intent filter:
   ```xml
   <action android:name="com.forge.autophone.IAutoPhoneService" />
   ```

---

### Issue 2: Methods Return "Service Not Enabled"

**Symptoms:**
```json
{"ok": false, "error": "Accessibility service not enabled"}
```

**Solutions:**
1. Enable accessibility service (see instructions above)

2. Verify service running:
   ```bash
   adb shell dumpsys accessibility | grep AutoPhone
   ```

3. Check service instance:
   ```kotlin
   AutoPhoneAccessibilityService.instance // Should not be null
   ```

---

### Issue 3: Tap by Text Fails

**Symptoms:**
```json
{"ok": false, "error": "Text 'Button' not found"}
```

**Possible Causes:**
1. Text is case-sensitive - try exact match
2. Text is in content description, not text field
3. Element not in accessibility tree (image button, etc.)
4. Window not active

**Debug:**
```kotlin
// First, read the screen to see what's available:
val screenJson = autoPhone.readScreen()
// Parse JSON and check if text exists in tree
```

---

### Issue 4: Build Fails

**Check Build Log:**
```bash
# Download build-log.txt from GitHub Actions artifacts
# Look for:
# - Compilation errors
# - Missing dependencies
# - AIDL generation issues
```

**Common Issues:**
- AIDL interface mismatch between AutoPhone and Forge OS
- Timber import missing
- Hilt dependency not resolved

---

## 📊 Success Criteria

### ✅ Installation
- [ ] APK installs without errors
- [ ] App appears in launcher
- [ ] App opens successfully

### ✅ UI Functionality  
- [ ] Status indicator shows correctly
- [ ] Status updates when service enabled/disabled
- [ ] Settings button opens accessibility settings

### ✅ AIDL Integration
- [ ] Forge OS can discover service
- [ ] Binding succeeds
- [ ] `isServiceActive()` returns correct value

### ✅ Screen Reading
- [ ] `readScreen()` returns UI tree JSON
- [ ] JSON is valid and parseable
- [ ] Node structure is correct

### ✅ Automation Actions
- [ ] `tapByText()` taps buttons
- [ ] `typeText()` enters text
- [ ] `goHome()` navigates home
- [ ] `goBack()` navigates back
- [ ] `swipe()` performs gestures

### ✅ Error Handling
- [ ] Returns proper errors when service disabled
- [ ] Returns proper errors when text not found
- [ ] No crashes on invalid input

---

## 🎯 Expected Test Results

### When Accessibility Service Enabled ✅

| Method | Input | Expected Output |
|--------|-------|-----------------|
| `isServiceActive()` | - | `true` |
| `readScreen()` | - | `{"ok":true,"output":"{...}"}` |
| `tapByText("Settings")` | "Settings" | `{"ok":true,"output":"Clicked on 'Settings'"}` |
| `typeText("test")` | "test" | `{"ok":true,"output":"Typed text: test"}` |
| `goHome()` | - | `{"ok":true,"output":"Pressed home"}` |
| `swipe("up", 500)` | "up", 500 | `{"ok":true,"output":"Swiped up"}` |

### When Accessibility Service Disabled ❌

| Method | Expected Output |
|--------|-----------------|
| `isServiceActive()` | `false` |
| `readScreen()` | `{"ok":false,"error":"Accessibility service not enabled"}` |
| `tapByText("...")` | `{"ok":false,"error":"Accessibility service not enabled"}` |
| All automation methods | Same error |

---

## 📝 Test Log Template

```
# AutoPhone Integration Test Results

## Environment
- Device: [Model]
- Android Version: [Version]
- AutoPhone APK: forge-autophone-release-unsigned.apk
- Forge OS Version: [Version]
- Test Date: [Date]

## Installation ✅/❌
- [ ] APK installed successfully
- [ ] App launches
- [ ] UI displays correctly

## Accessibility Service ✅/❌
- [ ] Service appears in accessibility settings
- [ ] Can enable service
- [ ] Status indicator updates
- [ ] Service persists after reboot

## AIDL Binding ✅/❌
- [ ] Service discoverable from Forge OS
- [ ] Binding succeeds
- [ ] Methods callable
- [ ] Responses valid

## Method Tests ✅/❌
- [ ] isServiceActive()
- [ ] readScreen()
- [ ] tapByText()
- [ ] tapAt()
- [ ] typeText()
- [ ] swipe()
- [ ] scroll()
- [ ] goHome()
- [ ] goBack()

## Error Handling ✅/❌
- [ ] Proper errors when service disabled
- [ ] Proper errors when element not found
- [ ] No crashes on edge cases

## Notes
[Any observations, issues, or improvements needed]
```

---

## 🚀 Next Steps After Testing

### If All Tests Pass ✅
1. Document successful integration
2. Implement placeholder methods:
   - `launchApp()`
   - `screenshot()` (with MediaProjection)
   - Notification listener methods
3. Add more automation tools
4. Performance optimization
5. Production release

### If Tests Fail ❌
1. Capture detailed logs
2. Identify root cause
3. Fix issues
4. Rebuild and retest

---

**🧪 Happy Testing! 🎉**

**Report issues at:** https://github.com/forgelabsofficial/forge-autophone/issues
