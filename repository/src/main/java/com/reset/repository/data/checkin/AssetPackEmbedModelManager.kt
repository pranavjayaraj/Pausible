package com.reset.repository.data.checkin

import android.app.Activity
import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.reset.model.domain.checkin.EmbedModelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [EmbedModelManager] backed by Play Asset Delivery. Every public method is wrapped so a
 * missing Play Store, an unsupported install source (a sideloaded debug APK), or any SDK
 * failure degrades to "not ready" / [EmbedModelManager.FetchState.Failed] instead of
 * throwing — text classification's Tier-1 + chips fallback must never depend on this
 * succeeding.
 */
@Singleton
class AssetPackEmbedModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : EmbedModelManager {

    private val assetPackManager: AssetPackManager by lazy { AssetPackManagerFactory.getInstance(context) }

    override suspend fun isReady(): Boolean = modelFile() != null

    override suspend fun modelFile(): File? = runCatching {
        val assetsPath = assetPackManager.getPackLocation(PACK_NAME)?.assetsPath() ?: return@runCatching null
        File(assetsPath).listFiles()?.firstOrNull { it.isFile }
    }.getOrNull()

    override fun fetch(): Flow<EmbedModelManager.FetchState> = callbackFlow {
        val listener = AssetPackStateUpdateListener { state ->
            if (state.name() == PACK_NAME) trySend(state.toFetchState())
        }
        assetPackManager.registerListener(listener)

        runCatching { assetPackManager.fetch(listOf(PACK_NAME)) }
            .onFailure { trySend(EmbedModelManager.FetchState.Failed(it.message)) }

        awaitClose { assetPackManager.unregisterListener(listener) }
    }

    override fun confirmCellularDownload(activity: Activity) {
        // Fire-and-forget: the dialog's outcome (confirmed → download resumes; declined →
        // stays pending) surfaces through the same fetch() listener, not a callback here.
        runCatching { assetPackManager.showCellularDataConfirmation(activity) }
    }

    private fun AssetPackState.toFetchState(): EmbedModelManager.FetchState = when (status()) {
        AssetPackStatus.PENDING, AssetPackStatus.DOWNLOADING, AssetPackStatus.TRANSFERRING ->
            EmbedModelManager.FetchState.Downloading(percentComplete())
        AssetPackStatus.COMPLETED -> EmbedModelManager.FetchState.Downloaded
        AssetPackStatus.WAITING_FOR_WIFI -> EmbedModelManager.FetchState.WaitingForWifi
        AssetPackStatus.REQUIRES_USER_CONFIRMATION -> EmbedModelManager.FetchState.RequiresConfirmation
        AssetPackStatus.FAILED -> EmbedModelManager.FetchState.Failed(errorCode().toString())
        AssetPackStatus.CANCELED -> EmbedModelManager.FetchState.Failed("canceled")
        else -> EmbedModelManager.FetchState.Downloading(percentComplete())
    }

    private fun AssetPackState.percentComplete(): Int {
        val total = totalBytesToDownload()
        if (total <= 0L) return 0
        return ((bytesDownloaded() * 100L) / total).toInt().coerceIn(0, 100)
    }

    private companion object {
        const val PACK_NAME = "sense_embed_model"
    }
}
