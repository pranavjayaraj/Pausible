package com.reset.feature.sessions.utils

import com.reset.feature.sessions.content.SessionPreviewRepositoryImpl
import com.reset.feature.sessions.content.SessionSelectorRepositoryImpl
import com.reset.model.domain.checkin.SessionPreviewRepository
import com.reset.model.domain.checkin.SessionSelectorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class SessionsModule {

    @Binds
    abstract fun bindHapticsDelegate(impl: HapticsDelegateImpl): HapticsDelegate

    @Binds
    abstract fun bindSessionAudioDelegate(impl: SessionAudioDelegateImpl): SessionAudioDelegate

    @Binds
    abstract fun bindSessionSelectorRepository(impl: SessionSelectorRepositoryImpl): SessionSelectorRepository

    @Binds
    abstract fun bindSessionPreviewRepository(impl: SessionPreviewRepositoryImpl): SessionPreviewRepository
}
