# Keep AutoPhone accessibility service entry point
-keep class com.forge.autophone.AutoPhoneAccessibilityService { *; }
-keep class com.forge.autophone.AutoPhoneApplication { *; }

# Keep all DI module bindings
-keep class com.forge.autophone.di.** { *; }

# Keep tool registry (called reflectively by agent runtime)
-keep class com.forge.autophone.toolregistry.** { *; }

# Keep model classes (serialised for agent reasoning)
-keep class com.forge.autophone.model.** { *; }

# Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
