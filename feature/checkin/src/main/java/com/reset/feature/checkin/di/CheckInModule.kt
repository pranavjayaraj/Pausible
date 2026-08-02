package com.reset.feature.checkin.di

import com.reset.feature.checkin.classify.CascadeNeedStateClassifier
import com.reset.feature.checkin.classify.EmbeddingNeedStateClassifier
import com.reset.feature.checkin.classify.NeedStateClassifier
import com.reset.feature.checkin.classify.PrototypeVectorCache
import com.reset.model.domain.checkin.TextEmbedder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class CheckInModule {

    /** Text → NeedState: the cascade (Tier-1 lexicon, escalating to Tier-2 embedding when
     *  unsure and ready) is the seam's single bound implementation — the ViewModel never
     *  knows which tier actually answered. */
    @Binds
    abstract fun bindNeedStateClassifier(impl: CascadeNeedStateClassifier): NeedStateClassifier

    companion object {
        /** [TextEmbedder] itself is bound wherever the app links a concrete embedder (see
         *  `:text-embed`'s own Hilt module) — this feature only ever sees the `:model`
         *  interface, never `:text-embed`'s MediaPipe implementation. */
        @Provides
        fun provideEmbeddingNeedStateClassifier(
            embedder: TextEmbedder,
            prototypeVectorCache: PrototypeVectorCache,
        ): EmbeddingNeedStateClassifier = EmbeddingNeedStateClassifier(
            embedder = embedder,
            prototypeVectors = prototypeVectorCache::vectors,
        )
    }
}
