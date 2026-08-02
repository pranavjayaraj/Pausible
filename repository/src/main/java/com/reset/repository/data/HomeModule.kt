package com.reset.repository.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.reset.model.domain.breakprefs.BreakPreferencesRepository
import com.reset.model.domain.CelebrationStore
import com.reset.model.domain.checkin.CheckInPropensityLog
import com.reset.model.domain.checkin.EmbedModelManager
import com.reset.model.domain.closeperson.ClosePersonRepository
import com.reset.model.domain.mood.MoodRepository
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.presets.PresetsRepository
import com.reset.model.domain.sense.SenseSuggestionRepository
import com.reset.model.domain.SoundController
import com.reset.model.domain.stats.StatsRepository
import com.reset.model.domain.TimeProvider
import com.reset.repository.data.breakprefs.BreakPreferencesRepositoryImpl
import com.reset.repository.data.checkin.AssetPackEmbedModelManager
import com.reset.repository.data.checkin.CheckInPropensityLogImpl
import com.reset.repository.data.closeperson.ClosePersonRepositoryImpl
import com.reset.repository.data.mood.MoodRepositoryImpl
import com.reset.repository.data.preferences.PreferencesRepositoryImpl
import com.reset.repository.data.presets.PresetsRepositoryImpl
import com.reset.repository.data.sense.SenseSuggestionRepositoryImpl
import com.reset.repository.data.stats.StatsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import kotlin.random.Random

private val Context.afkDataStore: DataStore<Preferences> by preferencesDataStore(name = "afk_prefs")

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    abstract fun bindSenseSuggestionRepository(impl: SenseSuggestionRepositoryImpl): SenseSuggestionRepository

    @Binds
    abstract fun bindClosePersonRepository(impl: ClosePersonRepositoryImpl): ClosePersonRepository

    @Binds
    abstract fun bindBreakPreferencesRepository(impl: BreakPreferencesRepositoryImpl): BreakPreferencesRepository

    @Binds
    abstract fun bindCheckInPropensityLog(impl: CheckInPropensityLogImpl): CheckInPropensityLog

    @Binds
    abstract fun bindEmbedModelManager(impl: AssetPackEmbedModelManager): EmbedModelManager

    @Binds
    abstract fun bindMoodRepository(impl: MoodRepositoryImpl): MoodRepository

    @Binds
    abstract fun bindPresetsRepository(impl: PresetsRepositoryImpl): PresetsRepository

    @Binds
    @Singleton
    abstract fun bindSoundController(impl: AndroidSoundController): SoundController

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindCelebrationStore(impl: CelebrationStoreImpl): CelebrationStore

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.afkDataStore

        @Provides
        fun provideRandom(): Random = Random.Default

        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
