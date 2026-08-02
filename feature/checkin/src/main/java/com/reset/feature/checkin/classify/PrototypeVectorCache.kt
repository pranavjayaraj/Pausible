package com.reset.feature.checkin.classify

import android.content.Context
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.TextEmbedder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes [NeedPrototypes]' phrase vectors via the embedder exactly once, then caches them
 * to a small file in app-private storage — every classify() call after the first cold start
 * (post-model-download) reuses the cache instead of re-embedding ~50 fixed phrases. Returns
 * null whenever the embedder itself isn't ready; nothing here ever throws into a caller.
 */
@Singleton
class PrototypeVectorCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val embedder: TextEmbedder,
) {
    @Volatile
    private var inMemory: Map<NeedState, List<FloatArray>>? = null

    suspend fun vectors(): Map<NeedState, List<FloatArray>>? {
        inMemory?.let { return it }
        loadFromDisk()?.let { inMemory = it; return it }
        val computed = compute() ?: return null
        inMemory = computed
        saveToDisk(computed)
        return computed
    }

    /** Null the moment any phrase fails to embed (model not ready mid-computation) — a
     *  partial prototype set would silently bias the nearest-neighbor search. */
    private suspend fun compute(): Map<NeedState, List<FloatArray>>? {
        val result = mutableMapOf<NeedState, List<FloatArray>>()
        for ((need, phrases) in NeedPrototypes.PHRASES) {
            val vectors = phrases.map { phrase -> embedder.embed(phrase) ?: return null }
            result[need] = vectors
        }
        return result
    }

    private fun cacheFile() = File(context.filesDir, CACHE_FILE_NAME)

    /** Format: per need, a "<ordinal>:<count>" line then `count` comma-joined-float lines.
     *  Any parse failure (corrupt file, a schema change) just misses the cache — [compute]
     *  rebuilds it, so there's nothing to migrate. A cache written before a need was added to
     *  [NeedPrototypes] parses fine but is *stale*, and a stale map would quietly make the new
     *  need unreachable, so [isStale] rejects it too. */
    private fun loadFromDisk(): Map<NeedState, List<FloatArray>>? = readFromDisk()?.takeUnless { isStale(it) }

    /** A cache is stale unless it holds a vector for every phrase of every current need —
     *  phrase edits change the count, additions change the key set. */
    private fun isStale(cached: Map<NeedState, List<FloatArray>>): Boolean =
        NeedPrototypes.PHRASES.any { (need, phrases) -> cached[need]?.size != phrases.size }

    private fun readFromDisk(): Map<NeedState, List<FloatArray>>? = runCatching {
        val file = cacheFile()
        if (!file.exists()) return null
        val lines = file.readLines()
        var i = 0
        val result = mutableMapOf<NeedState, List<FloatArray>>()
        while (i < lines.size) {
            val (ordinalStr, countStr) = lines[i].split(":")
            val need = NeedState.entries[ordinalStr.toInt()]
            val count = countStr.toInt()
            i++
            val vectors = (0 until count).map { offset ->
                lines[i + offset].split(",").map { it.toFloat() }.toFloatArray()
            }
            i += count
            result[need] = vectors
        }
        result
    }.getOrNull()

    private fun saveToDisk(vectors: Map<NeedState, List<FloatArray>>) {
        runCatching {
            cacheFile().bufferedWriter().use { writer ->
                for ((need, list) in vectors) {
                    writer.write("${need.ordinal}:${list.size}")
                    writer.newLine()
                    for (vector in list) {
                        writer.write(vector.joinToString(","))
                        writer.newLine()
                    }
                }
            }
        }
    }

    private companion object {
        const val CACHE_FILE_NAME = "checkin_prototype_vectors.cache"
    }
}
