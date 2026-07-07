package com.reset.repository.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.reset.model.domain.CelebrationStore
import com.reset.model.domain.HomeRepository
import com.reset.model.domain.SoundController
import com.reset.model.domain.TimeProvider
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
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

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
