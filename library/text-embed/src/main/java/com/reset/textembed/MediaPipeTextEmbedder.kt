package com.reset.textembed

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder as MpTextEmbedder
import com.reset.model.domain.checkin.EmbedModelManager
import com.reset.model.domain.checkin.TextEmbedder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TextEmbedder] backed by MediaPipe's on-device sentence encoder (Universal Sentence
 * Encoder, ~5.8MB — chosen over the smaller avg-word model because Play Asset Delivery already
 * removed the base-APK size cost, so the quality is worth buying).
 *
 * Loads its model from the Play Asset Delivery-downloaded file — [EmbedModelManager] is the
 * *only* source of the file path, never an APK asset (`setModelAssetPath` is for in-APK
 * assets only; a PAD-delivered file needs `setModelAssetBuffer`, which is what this reads the
 * file into). Until the pack is downloaded, [embed] simply returns null.
 *
 * Native runtime boundary: MediaPipe's embedder wraps a native (.so) TFLite delegate, which
 * the JVM unit-test target cannot load — this class is exercised only by instrumented/
 * on-device tests, never `testDebugUnitTest`. The real, fast-running unit coverage lives on
 * [com.reset.feature.checkin.classify.EmbeddingNeedStateClassifier], against a fake
 * [TextEmbedder]; none of the cosine-similarity/threshold logic depends on MediaPipe actually
 * running.
 *
 * Privacy: inference runs entirely on-device via the native runtime — [embed] never makes a
 * network call. Only [EmbedModelManager]'s model *bytes* were ever fetched, and only once.
 */
@Singleton
class MediaPipeTextEmbedder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val embedModelManager: EmbedModelManager,
) : TextEmbedder {

    private val initLock = Mutex()

    @Volatile
    private var delegate: MpTextEmbedder? = null

    override suspend fun embed(text: String): FloatArray? = try {
        val embedder = delegate ?: initLock.withLock { delegate ?: createDelegate()?.also { delegate = it } }
        if (embedder == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                embedder.embed(text).embeddingResult().embeddings().firstOrNull()?.floatEmbedding()
            }
        }
    } catch (e: CancellationException) {
        throw e // never swallow cancellation.
    } catch (e: Exception) {
        // Any native/runtime failure (corrupt model file, delegate init failure, OOM)
        // degrades to "not ready" — this must never crash the Send flow.
        null
    }

    /** Builds the MediaPipe delegate from the PAD-downloaded file, off the main thread — both
     *  the file read and native delegate construction are blocking work. */
    private suspend fun createDelegate(): MpTextEmbedder? {
        val modelFile = embedModelManager.modelFile() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val modelBuffer = mapModelFile(modelFile)
                val options = MpTextEmbedder.TextEmbedderOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(modelBuffer).build())
                    .build()
                MpTextEmbedder.createFromOptions(context, options)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Memory-maps the ~5.8MB model file rather than reading it into a heap byte array. */
    private fun mapModelFile(modelFile: java.io.File): MappedByteBuffer =
        RandomAccessFile(modelFile, "r").use { file ->
            file.channel.use { channel -> channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()) }
        }
}
