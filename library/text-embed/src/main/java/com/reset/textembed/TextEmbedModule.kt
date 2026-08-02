package com.reset.textembed

import com.reset.model.domain.checkin.TextEmbedder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Links the `:model` seam to this module's concrete embedder — the only place in the app
 *  that knows [MediaPipeTextEmbedder] exists. `:feature:checkin` only ever sees [TextEmbedder]. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class TextEmbedModule {

    @Binds
    abstract fun bindTextEmbedder(impl: MediaPipeTextEmbedder): TextEmbedder
}
