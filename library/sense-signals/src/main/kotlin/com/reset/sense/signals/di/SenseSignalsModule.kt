package com.reset.sense.signals.di

import com.reset.sense.signals.android.AndroidAppCategoryResolver
import com.reset.sense.signals.android.AndroidDeviceStateSource
import com.reset.sense.signals.android.AndroidUsageEventsSource
import com.reset.sense.signals.android.AppCategoryResolver
import com.reset.sense.signals.android.DeviceStateSource
import com.reset.sense.signals.android.UsageEventsSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SenseSignalsModule {

    @Binds
    @Singleton
    abstract fun bindUsageEventsSource(impl: AndroidUsageEventsSource): UsageEventsSource

    @Binds
    @Singleton
    abstract fun bindAppCategoryResolver(impl: AndroidAppCategoryResolver): AppCategoryResolver

    @Binds
    @Singleton
    abstract fun bindDeviceStateSource(impl: AndroidDeviceStateSource): DeviceStateSource
}
