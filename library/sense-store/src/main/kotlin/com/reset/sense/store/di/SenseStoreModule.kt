package com.reset.sense.store.di

import android.content.Context
import androidx.room.Room
import com.reset.sense.store.DecisionLogDao
import com.reset.sense.store.SenseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SenseStoreModule {

    @Provides
    @Singleton
    fun provideSenseDatabase(@ApplicationContext context: Context): SenseDatabase =
        Room.databaseBuilder(context, SenseDatabase::class.java, SenseDatabase.NAME)
            // v1 ships pre-launch; once real user data exists, replace with
            // explicit Migration objects — never destructive fallback.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDecisionLogDao(database: SenseDatabase): DecisionLogDao =
        database.decisionLogDao()
}
