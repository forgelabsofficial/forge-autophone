package com.forge.autophone.di

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.accessibility.TextEntryService
import com.forge.autophone.service.GestureHandler
import com.forge.autophone.service.NavigationActions
import com.forge.autophone.toolregistry.AutoPhoneToolRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AutoPhoneModule — Hilt DI bindings for the AutoPhone accessibility layer.
 *
 * Note: [AutoPhoneAccessibilityService] is a system-bound service; its instance
 * is managed by Android. We expose the singleton via [AutoPhoneAccessibilityService.instance]
 * and guard against null with a lateinit guard in client code.
 */
@Module
@InstallIn(SingletonComponent::class)
object AutoPhoneModule {

    @Provides
    @Singleton
    fun provideGestureHandler(service: AutoPhoneAccessibilityService): GestureHandler =
        service.gestureHandler

    @Provides
    @Singleton
    fun provideTextEntryService(service: AutoPhoneAccessibilityService): TextEntryService =
        service.textEntry

    @Provides
    @Singleton
    fun provideNavigationActions(service: AutoPhoneAccessibilityService): NavigationActions =
        service.navigation

    @Provides
    @Singleton
    fun provideAutoPhoneToolRegistry(service: AutoPhoneAccessibilityService): AutoPhoneToolRegistry =
        AutoPhoneToolRegistry(service)
}