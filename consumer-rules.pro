# Keep AutoPhone accessibility service entry point
-keep class com.forge.autophone.AutoPhoneAccessibilityService { *; }
-keep class com.forge.autophone.AutoPhoneApplication { *; }

# Keep all DI module bindings
-keep class com.forge.autophone.di.** { *; }

# Keep tool registry (called reflectively by agent runtime)
-keep class com.forge.autophone.toolregistry.** { *; }

# Keep model classes (serialised for agent reasoning)
-keep class com.forge.autophone.model.** { *; }

# Keep OCR data classes (serialized for tool responses)
-keep class com.forge.autophone.ocr.** { *; }

# Keep event classes (may be serialized)
-keep class com.forge.autophone.events.** { *; }

# Keep vision/icon matching classes (serialized for tool responses)
-keep class com.forge.autophone.vision.** { *; }

# Keep context awareness classes (Phase 3)
-keep class com.forge.autophone.context.** { *; }

# Keep verification classes (Phase 3)
-keep class com.forge.autophone.verification.** { *; }

# Keep diff classes (Phase 3)
-keep class com.forge.autophone.diff.** { *; }

# Keep self-healing classes (Phase 4)
-keep class com.forge.autophone.healing.** { *; }

# Keep gesture recording classes (Phase 4)
-keep class com.forge.autophone.recording.** { *; }

# Keep form automation classes (Phase 4)
-keep class com.forge.autophone.form.** { *; }

# Keep telemetry classes (Phase 4)
-keep class com.forge.autophone.telemetry.** { *; }

# Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ML Kit dependencies
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# OpenCV dependencies
-keep class org.opencv.** { *; }
-keepclassmembers class org.opencv.** { *; }
-dontwarn org.opencv.**
