package com.reset.app.di

import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.sense.delivery.QuietHours
import com.reset.sense.delivery.QuietHoursSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Binds the Sense engine's config seams to the app's own preference store.
 * The sense modules never read host storage directly — this adapter is the
 * one place where Riverbloom's DataStore-backed settings flow into the
 * engine (and the pattern future SDK hosts will replicate).
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SenseConfigModule {

    @Provides
    @Singleton
    fun provideQuietHoursSource(preferencesRepository: PreferencesRepository): QuietHoursSource =
        QuietHoursSource {
            val prefs = preferencesRepository.preferences.first()
            QuietHours(
                startHour = prefs.quietHoursStartHour,
                // start == end is an empty window (the engine's quiet check never
                // matches), so a disabled toggle erases quiet hours without the
                // sense modules needing an enabled flag of their own.
                endHour = if (prefs.quietHoursEnabled) {
                    prefs.quietHoursEndHour
                } else {
                    prefs.quietHoursStartHour
                },
            )
        }
}
