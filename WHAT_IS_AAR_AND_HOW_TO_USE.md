# What is an AAR and How to Use It?

> **Complete guide to Android Archive (AAR) files**

---

## What is an AAR?

**AAR = Android Archive**

Think of it like a **ZIP file** that contains a compiled Android library. It's the Android equivalent of a JAR file, but with Android-specific stuff included.

### AAR vs APK vs JAR

| File Type | What It Is | Use Case |
|-----------|------------|----------|
| **APK** | Android Package | **Install on phone** (it's an app) |
| **AAR** | Android Archive | **Include in other apps** (it's a library) |
| **JAR** | Java Archive | Java library (no Android resources) |

**Simple analogy:**
- **APK** = A complete sandwich you can eat
- **AAR** = Ingredients you add to make your sandwich better
- **JAR** = Just the bread (basic Java code, no Android stuff)

---

## What's Inside an AAR?

When you unzip `forge-autophone-release.aar`, you'll find:

```
forge-autophone-release.aar
├── AndroidManifest.xml          ← Service declarations, permissions
├── classes.jar                  ← Compiled code (all 78 tools!)
├── R.txt                        ← Resource IDs
├── res/                         ← Android resources
│   ├── values/                  ← Strings, colors
│   └── xml/                     ← Accessibility service config
├── libs/                        ← Bundled dependencies
│   ├── mlkit-text-recognition.jar
│   └── opencv-android.jar
└── proguard.txt                 ← ProGuard rules
```

**In short**: Everything AutoPhone needs to work!

---

## What Do You DO With an AAR?

### The Big Picture

```
1. You have: forge-autophone-release.aar (AutoPhone library)
2. You want: To use AutoPhone in Forge OS main app
3. You do: Add AAR to Forge OS project
4. Result: Forge OS can now use all 78 AutoPhone tools!
```

---

## Step-by-Step: Using AAR in Forge OS

### Step 1: Get the AAR File

**After GitHub Actions builds it:**

1. Go to: `https://github.com/YOUR_USERNAME/forge-autophone/actions`
2. Click: Latest successful workflow run (green ✓)
3. Scroll: To "Artifacts" section at bottom
4. Download: `forge-autophone-release` (downloads as .zip)
5. Extract: Get `forge-autophone-release.aar` file (~45-50 MB)

### Step 2: Copy AAR to Forge OS Project

**Put it in the `libs` folder:**

```bash
# Navigate to Forge OS main project
cd C:\path\to\forge-os-main

# Create libs folder if it doesn't exist
mkdir app\libs

# Copy the AAR file
copy C:\Downloads\forge-autophone-release.aar app\libs\
```

**Result:**
```
forge-os-main/
└── app/
    ├── build.gradle
    └── libs/
        └── forge-autophone-release.aar  ← Your library here!
```

### Step 3: Add AAR to build.gradle

**Edit:** `forge-os-main/app/build.gradle`

**Add this to dependencies:**

```gradle
dependencies {
    // ... existing dependencies ...

    // AutoPhone Library (all 78 tools)
    implementation files('libs/forge-autophone-release.aar')
    
    // AutoPhone's dependencies (if not already included)
    implementation "com.google.mlkit:text-recognition:16.0.0"
    implementation "org.opencv:opencv-android:4.8.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3"
    
    // If Forge OS doesn't already use Hilt
    implementation "com.google.dagger:hilt-android:2.51.1"
    kapt "com.google.dagger:hilt-compiler:2.51.1"
}
```

### Step 4: Sync Gradle

In Android Studio:
- Click "Sync Now" banner at top
- Or: File → Sync Project with Gradle Files

### Step 5: Use AutoPhone in Your Code!

**Now you can use all 78 AutoPhone tools:**

```kotlin
import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.toolregistry.AutoPhoneToolRegistry

class ForgeOSAgent {
    fun automateApp() {
        // Get the AutoPhone service
        val service = AutoPhoneAccessibilityService.instance
        
        if (service != null) {
            // Create tool registry
            val tools = AutoPhoneToolRegistry(service)
            
            // Use any of the 78 tools!
            
            // Basic gestures
            tools.tap(540f, 1200f)
            tools.swipe(100f, 500f, 900f, 500f)
            
            // OCR (Phase 1)
            tools.ocrTapText("Sign In")
            val text = tools.ocrReadScreen()
            
            // Icons (Phase 2)
            tools.registerIcon("menu", menuIconBase64)
            tools.tapIcon("menu")
            
            // Context awareness (Phase 3)
            if (tools.isLoginScreen()) {
                val fields = tools.detectFormFields()
                // Auto-fill login...
            }
            
            // Self-healing (Phase 4)
            val result = tools.findWithHealing(
                SelectorSpec.ById("submit_btn")
            )
            
            // Form automation (Phase 4)
            tools.autoFillForm(mapOf(
                "email" to "user@example.com",
                "password" to "pass123"
            ))
            tools.submitForm()
            
            // Telemetry (Phase 4)
            val stats = tools.getOverallStats()
            println("Success rate: ${stats.successRate}")
        }
    }
}
```

---

## Alternative: Building Forge OS with AutoPhone

### Instead of separate AAR, you can include AutoPhone as a module:

**Option A: AAR File (Easier)**
- ✅ Simple: Just copy file
- ✅ Fast: No compilation needed
- ✅ Stable: Locked version
- ❌ Updates require new AAR

**Option B: Module (More Flexible)**
- ✅ Live updates: Edit code directly
- ✅ Debugging: Full source access
- ❌ Slower builds: Compiles every time

### To use as Module:

1. **Copy AutoPhone folder to Forge OS:**
   ```bash
   xcopy forge-autophone C:\path\to\forge-os-main\forge-autophone /E /I
   ```

2. **Add to settings.gradle.kts:**
   ```kotlin
   include(":app", ":forge-autophone")
   ```

3. **Add to app/build.gradle:**
   ```gradle
   dependencies {
       implementation project(":forge-autophone")
   }
   ```

---

## What Happens When You Add the AAR?

### Before Adding AAR:
```
Forge OS App
├── Can't access Android UI
├── No automation tools
└── Limited to Java/Kotlin APIs
```

### After Adding AAR:
```
Forge OS App + AutoPhone
├── ✅ Full UI access (accessibility service)
├── ✅ 78 automation tools
├── ✅ OCR text recognition
├── ✅ Icon matching
├── ✅ Self-healing selectors
├── ✅ Gesture recording
├── ✅ Form automation
└── ✅ Complete Android automation platform!
```

---

## Real-World Usage Example

### Forge OS Agent Using AutoPhone:

```kotlin
class ForgeOSAutomationAgent {
    private lateinit var tools: AutoPhoneToolRegistry
    
    suspend fun automateInstagramPost(caption: String, imagePath: String) {
        // Initialize AutoPhone
        val service = AutoPhoneAccessibilityService.instance ?: return
        tools = AutoPhoneToolRegistry(service)
        
        // 1. Open Instagram
        tools.home()
        tools.ocrTapText("Instagram")
        tools.waitUntilIdle()
        
        // 2. Create new post
        tools.tapIcon("add_post_icon")  // Icon matching
        tools.waitForWindow("com.instagram.android")
        
        // 3. Select image
        // ... (use AutoPhone tools to navigate)
        
        // 4. Add caption
        val captionField = tools.detectFormFields()
            .find { it.fieldType == FieldType.TEXT }
        
        if (captionField != null) {
            tools.fillFormField(captionField, caption)
        }
        
        // 5. Post
        tools.submitForm()  // Finds and taps Share button
        
        // 6. Verify success
        val result = tools.detectVisualAnomalies()
        if (result.any { it.type == AnomalyType.ERROR_MESSAGE }) {
            println("Post failed!")
        } else {
            println("Post successful!")
        }
    }
}
```

---

## Troubleshooting AAR Issues

### "Cannot resolve symbol AutoPhoneAccessibilityService"

**Problem**: Android Studio can't find AutoPhone classes

**Solution**:
1. Check AAR is in `app/libs/` folder
2. Check `build.gradle` has: `implementation files('libs/forge-autophone-release.aar')`
3. Click: File → Invalidate Caches → Invalidate and Restart
4. Rebuild project

### "Duplicate class found" Error

**Problem**: AAR dependencies conflict with app dependencies

**Solution**: Exclude conflicting dependencies:
```gradle
implementation(files('libs/forge-autophone-release.aar')) {
    exclude group: 'androidx.core', module: 'core-ktx'
}
```

### AAR is Too Large (50 MB)

**This is normal!** It includes:
- ML Kit (~18 MB)
- OpenCV (~23 MB)
- AutoPhone code (~2-3 MB)

**To reduce size** (advanced):
- Use ProGuard/R8 in release builds
- Enable code shrinking in `build.gradle`:
  ```gradle
  buildTypes {
      release {
          minifyEnabled true
          proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
      }
  }
  ```

### Build Time Increased

**Normal behavior**: Adding AutoPhone adds dependencies

**Typical impact**:
- Debug build: +10-20 seconds
- Release build: +30-60 seconds (ProGuard optimization)

---

## Summary: AAR Usage Flow

```
1. GitHub Actions builds AAR
   ↓
2. Download forge-autophone-release.aar
   ↓
3. Copy to Forge OS: app/libs/forge-autophone-release.aar
   ↓
4. Update build.gradle: implementation files('libs/...')
   ↓
5. Sync Gradle
   ↓
6. Import classes: import com.forge.autophone.*
   ↓
7. Use 78 tools in your agent code!
```

---

## Quick Reference

### Essential Commands

```bash
# Copy AAR to Forge OS
copy forge-autophone-release.aar C:\path\to\forge-os-main\app\libs\

# Build Forge OS with AutoPhone included
cd C:\path\to\forge-os-main
gradlew assembleDebug
```

### Essential build.gradle Addition

```gradle
dependencies {
    implementation files('libs/forge-autophone-release.aar')
    implementation "com.google.mlkit:text-recognition:16.0.0"
    implementation "org.opencv:opencv-android:4.8.0"
}
```

### Essential Code

```kotlin
val service = AutoPhoneAccessibilityService.instance
if (service != null) {
    val tools = AutoPhoneToolRegistry(service)
    // Use tools...
}
```

---

## What's Next?

After integrating the AAR:

1. ✅ **Test basic tools**: Try `tools.tap()`, `tools.ocrReadScreen()`
2. ✅ **Build Forge OS**: Create APK with AutoPhone included
3. ✅ **Install on device**: Test automation
4. ✅ **Enable accessibility**: Settings → Accessibility → AutoPhone
5. ✅ **Build agent logic**: Use 78 tools to automate anything!

---

**In Simple Terms:**

AAR = Your compiled AutoPhone library  
Using it = Copy to Forge OS libs folder + add one line to build.gradle  
Result = Forge OS can now automate ANY Android app! 🚀