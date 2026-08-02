package com.reset.model.domain.checkin

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Download orchestration for the Tier-2 embedding model's Play Asset Delivery pack
 * (`:model_pack`, on-demand delivery). Every method is idempotent and never throws into the
 * caller — a declined, failed, or offline download is a normal state, not an error, because
 * text classification always has a lexicon-only fallback (see `CascadeNeedStateClassifier`).
 *
 * On-demand PAD only resolves through a real Play-delivered install (an .aab via Play, or
 * `bundletool`/internal app sharing for local testing) — a plain sideloaded debug APK will
 * never see the pack, and [isReady] must degrade to `false` gracefully there, not throw.
 */
interface EmbedModelManager {

    /** True once the pack is downloaded and its on-disk location is resolvable. */
    suspend fun isReady(): Boolean

    /** The downloaded model file, or null if [isReady] is false. */
    suspend fun modelFile(): File?

    /** Starts (or attaches to an in-flight) download. Safe to call repeatedly — an
     *  already-downloaded pack completes immediately with [FetchState.Downloaded]. */
    fun fetch(): Flow<FetchState>

    /** Resolves a [FetchState.RequiresConfirmation] by showing Play's cellular-data consent
     *  dialog. [activity] is used transiently only — never stored, matching the app's
     *  leak-safe activity-launching rule — so call this from a Route composable, never from
     *  a ViewModel. The outcome surfaces as further emissions on the same [fetch] collector. */
    fun confirmCellularDownload(activity: Activity)

    sealed interface FetchState {
        data class Downloading(val percentComplete: Int) : FetchState
        data object Downloaded : FetchState
        data object WaitingForWifi : FetchState

        /** The pack is large enough that Play wants explicit user consent to spend cellular
         *  data — the UI layer resolves this via `showCellularDataConfirmation(activity)`,
         *  which needs a live Activity the manager itself must never hold. */
        data object RequiresConfirmation : FetchState

        data class Failed(val message: String?) : FetchState
    }
}
