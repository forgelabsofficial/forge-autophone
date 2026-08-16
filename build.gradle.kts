plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization") version "2.0.0"
}

android {
    namespace = "com.forge.autophone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.forge.autophone"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        ndk {
            // Modern 64-bit phones only (Android 7+ on arm64).
            // Matches Forge OS configuration for compatibility.
            abiFilters += listOf("arm64-v8a")
        }
    }
    
    signingConfigs {
        create("release") {
            // Reads from keystore.properties if it exists (for production signing)
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = java.util.Properties()
                props.load(propsFile.inputStream())
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            } else {
                // Fallback to debug keystore for development builds
                // The resulting APK won't update an existing signed install
                // but is fine for local testing
                val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
                if (debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        compose = true
        aidl = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Compose Material Icons Extended (for Accessibility, LayersClear icons)
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Kotlinx Serialization for gesture recording
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Timber logging
    implementation(libs.timber)

    // ML Kit for OCR
    implementation(libs.mlkit.text.recognition)

    // OpenCV for icon/image recognition (Phase 2)
    implementation(libs.opencv)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
