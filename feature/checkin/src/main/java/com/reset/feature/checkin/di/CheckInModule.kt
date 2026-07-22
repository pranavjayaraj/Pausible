package com.reset.feature.checkin.di

import com.reset.feature.checkin.classify.LexiconNeedStateClassifier
import com.reset.feature.checkin.classify.NeedStateClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class CheckInModule {

    /** Text → NeedState (Tier-1 lexicon). Interface so a future embedding/on-device-LLM
     *  classifier can replace it without touching the ViewModel — the same seam discipline
     *  as chips: only the [com.reset.model.domain.checkin.NeedState] escapes. */
    @Binds
    abstract fun bindNeedStateClassifier(impl: LexiconNeedStateClassifier): NeedStateClassifier
}
