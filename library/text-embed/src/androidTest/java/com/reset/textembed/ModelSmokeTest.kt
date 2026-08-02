package com.reset.textembed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder as MpTextEmbedder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import kotlin.math.sqrt
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Loads the real checked-in `universal_sentence_encoder.tflite` (bundled here under
 * src/androidTest/assets — production code never reads a model from APK assets, see
 * [MediaPipeTextEmbedder]'s doc) and runs live on-device inference through MediaPipe's
 * TextEmbedder, the same API surface [MediaPipeTextEmbedder] wraps. This is a smoke test
 * for the model file itself, not a mock of our wrapper.
 */
@RunWith(AndroidJUnit4::class)
class ModelSmokeTest {

    private lateinit var embedder: MpTextEmbedder

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = File(context.cacheDir, "universal_sentence_encoder.tflite")
        context.assets.open("universal_sentence_encoder.tflite").use { input ->
            FileOutputStream(modelFile).use { output -> input.copyTo(output) }
        }
        val modelBuffer = RandomAccessFile(modelFile, "r").use { file ->
            file.channel.use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }
        }
        val options = MpTextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(modelBuffer).build())
            .build()
        embedder = MpTextEmbedder.createFromOptions(context, options)
    }

    @After
    fun tearDown() {
        embedder.close()
    }

    @Test
    fun modelLoadsAndProducesNonZeroEmbeddings() {
        val vector = embed("I feel exhausted today")
        assertTrue("embedding should be non-empty", vector.isNotEmpty())
        assertTrue("embedding should not be all zeros", vector.any { it != 0f })
    }

    @Test
    fun semanticallySimilarSentencesScoreHigherThanUnrelatedOnes() {
        val anchor = embed("my neck and shoulders are so tense right now")
        val similar = embed("I'm carrying a lot of tension in my shoulders")
        val unrelated = embed("the train to Boston leaves at nine tomorrow")

        val simToSimilar = cosineSimilarity(anchor, similar)
        val simToUnrelated = cosineSimilarity(anchor, unrelated)

        assertTrue(
            "expected similar-meaning sentences ($simToSimilar) to score higher than an " +
                "unrelated one ($simToUnrelated)",
            simToSimilar > simToUnrelated,
        )
    }

    @Test
    fun repeatedCallsOnTheSameTextAreDeterministic() {
        val first = embed("I need a moment to breathe")
        val second = embed("I need a moment to breathe")
        assertTrue(cosineSimilarity(first, second) > 0.999f)
    }

    private fun embed(text: String): FloatArray =
        embedder.embed(text).embeddingResult().embeddings().first().floatEmbedding()

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }
}
