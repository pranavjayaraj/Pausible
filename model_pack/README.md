# :model_pack

Play Asset Delivery, on-demand — the Tier-2 text-embedding model only. No code belongs here.

## Adding the real model

This module ships with `src/main/assets/` empty. Before this can be used for real, drop the
sentence-encoder model file in:

```
model_pack/src/main/assets/<model-file-name>.tflite   (or .task, depending on the export)
```

Recommendation (see `MediaPipeTextEmbedder`'s doc): the Universal Sentence Encoder
(MediaPipe's mobile-optimized export, ~5.8MB), not the smaller average-word-embedding model —
Play Asset Delivery already removed the base-APK size cost that would otherwise have made the
smaller model tempting, so there's no reason not to buy the quality.

The real model file is already checked in at `src/main/assets/universal_sentence_encoder.tflite`
(downloaded from Google's official MediaPipe-hosted URL, verified as a genuine TFL3 flatbuffer).

`EmbedModelManager.modelFile()` resolves the file's on-device path after download via
`AssetPackManager.getPackLocation("sense_embed_model")?.assetsPath()` — `MediaPipeTextEmbedder`
reads whatever single model file it finds there. Keep exactly one model file in this pack.

## Local testing

On-demand Play Asset Delivery only resolves through a real Play-delivered install path: an
`.aab` uploaded to Play (internal testing track is enough), or `bundletool`/internal app
sharing for local testing. **A plain sideloaded debug APK will never see this pack** —
`EmbedModelManager.isReady()` correctly (and silently) returns `false` in that case, and
Check In's text input degrades to the Tier-1 lexicon + chips, exactly as it does for a user
who declines or fails the download. This is expected, not a bug.
