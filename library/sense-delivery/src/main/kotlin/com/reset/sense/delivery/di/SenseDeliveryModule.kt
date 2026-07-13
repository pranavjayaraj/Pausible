package com.reset.sense.delivery.di

import com.reset.sense.delivery.AndroidBreakPresenter
import com.reset.sense.delivery.AndroidHostStateSource
import com.reset.sense.delivery.AndroidSnapshotSource
import com.reset.sense.delivery.AssetModelLoader
import com.reset.sense.delivery.BreakPresenter
import com.reset.sense.delivery.HostStateSource
import com.reset.sense.delivery.SnapshotSource
import com.reset.sense.ml.BreakDecisionEngine
import com.reset.sense.ml.SenseMode
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SenseDeliveryModule {

    @Binds
    @Singleton
    abstract fun bindSnapshotSource(impl: AndroidSnapshotSource): SnapshotSource

    @Binds
    @Singleton
    abstract fun bindBreakPresenter(impl: AndroidBreakPresenter): BreakPresenter

    @Binds
    @Singleton
    abstract fun bindHostStateSource(impl: AndroidHostStateSource): HostStateSource

    companion object {

        @Provides
        @Singleton
        fun provideBreakDecisionEngine(modelLoader: AssetModelLoader): BreakDecisionEngine =
            BreakDecisionEngine(
                model = modelLoader.loadOrNull(), // null → rules-only, by design
                mode = SenseMode.BALANCED,
                // DORMANT: raise to ~0.05 once the user base is large enough
                // to spread the exploration tax thin (see SENSE_ML.md §8.6).
                // The propensity trail is logged either way from day one.
                explorationEpsilon = 0f,
            )
    }
}
