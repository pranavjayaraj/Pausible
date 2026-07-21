package com.reset.repository.data.checkin

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CheckInPropensityModule {

    @Provides
    @Singleton
    fun provideCheckInPropensityDatabase(@ApplicationContext context: Context): CheckInPropensityDatabase =
        Room.databaseBuilder(context, CheckInPropensityDatabase::class.java, CheckInPropensityDatabase.NAME)
            // v1 ships pre-launch; once real user data exists, replace with explicit
            // Migration objects — never destructive fallback.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCheckInPropensityDao(database: CheckInPropensityDatabase): CheckInPropensityDao =
        database.checkInPropensityDao()
}
