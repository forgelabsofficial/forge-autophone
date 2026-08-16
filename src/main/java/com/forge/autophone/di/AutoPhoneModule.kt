package com.forge.autophone.di

import com.forge.autophone.AutoPhoneAccessibilityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AutoPhoneModule — Hilt DI bindings for the AutoPhone accessibility layer.
 *
 * Note: [AutoPhoneAccessibilityService] is a system-bound service; its instance
 * is managed by Android. We expose the singleton via [AutoPhoneAccessibilityService.instance]
 * which may be null if the accessibility service is not enabled.
 * 
 * The AutoPhoneService will check for null and return appropriate error responses
 * when the accessibility service is not available.
 */
@Module
@InstallIn(SingletonComponent::class)
object AutoPhoneModule {

    /**
     * Provides the current AutoPhoneAccessibilityService instance if available.
     * Returns null if the accessibility service is not enabled or not running.
     */
    @Provides
    fun provideAutoPhoneAccessibilityService(): AutoPhoneAccessibilityService? =
        AutoPhoneAccessibilityService.instance
}