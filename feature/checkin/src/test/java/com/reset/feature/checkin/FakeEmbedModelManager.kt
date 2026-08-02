package com.reset.feature.checkin

import android.app.Activity
import com.reset.model.domain.checkin.EmbedModelManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/** In-memory [EmbedModelManager] for Check In ViewModel tests. Defaults to "not ready, a
 *  fetch call succeeds immediately" — the common cold-start case. */
class FakeEmbedModelManager(
    private var ready: Boolean = false,
    private val fetchStates: List<EmbedModelManager.FetchState> = listOf(EmbedModelManager.FetchState.Downloaded),
) : EmbedModelManager {

    var fetchCallCount = 0
        private set
    var confirmCellularCallCount = 0
        private set

    override suspend fun isReady(): Boolean = ready

    override suspend fun modelFile(): File? = if (ready) File("fake-model-file") else null

    override fun fetch(): Flow<EmbedModelManager.FetchState> {
        fetchCallCount++
        return flow {
            for (fetchState in fetchStates) {
                emit(fetchState)
                if (fetchState == EmbedModelManager.FetchState.Downloaded) ready = true
            }
        }
    }

    override fun confirmCellularDownload(activity: Activity) {
        confirmCellularCallCount++
    }
}
