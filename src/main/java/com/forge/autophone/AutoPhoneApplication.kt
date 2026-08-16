package com.forge.autophone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AutoPhone Application Class
 * 
 * Main application entry point for the AutoPhone standalone app.
 * Initializes Hilt dependency injection and sets up global app state.
 */
@HiltAndroidApp
class AutoPhoneApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Application initialization
        // All DI is handled by Hilt
    }
}
