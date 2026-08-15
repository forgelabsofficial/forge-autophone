package com.forge.autophone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AutoPhone Application class.
 * Required by Hilt — triggers the Hilt component code generation.
 *
 * If forge-autophone is consumed as a library module by a host app,
 * the host app's Application class should use @HiltAndroidApp instead.
 * This class is for standalone module testing / demo builds only.
 */
@HiltAndroidApp
class AutoPhoneApplication : Application()
