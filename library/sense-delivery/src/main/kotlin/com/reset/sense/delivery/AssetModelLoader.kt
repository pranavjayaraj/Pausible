package com.reset.sense.delivery

import android.content.Context
import android.util.Log
import com.reset.sense.ml.AcceptanceModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Loads the bundled acceptance model from `:sense-ml` assets. Any failure —
 * missing asset, malformed JSON, [com.reset.sense.ml.SchemaMismatchException]
 * from a drifted pipeline — returns null and the engine runs pure rules.
 * A degraded engine is a feature; a crashed one is a bug.
 */
class AssetModelLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun loadOrNull(): AcceptanceModel? = runCatching {
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
    }.mapCatching { json ->
        AcceptanceModel.fromJson(json)
    }.onFailure { error ->
        Log.w(TAG, "acceptance model unavailable, running rules-only", error)
    }.getOrNull()

    private companion object {
        const val ASSET_NAME = "riverbloom_acceptance_v3.json"
        const val TAG = "SenseModelLoader"
    }
}
