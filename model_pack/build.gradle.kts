plugins {
    alias(libs.plugins.android.assetPack)
}

// The Tier-2 text-embedding model, delivered on-demand via Play Asset Delivery — data only,
// no code (see docs on TextEmbedder/EmbedModelManager in :model for why: PAD is for deferred
// *assets*, not deferred code, so it needs no dynamic-feature module, reflection boundary, or
// SplitInstallManager). Downloaded the first time a user's typed Check In text needs Tier-2.
assetPack {
    packName = "sense_embed_model"
    dynamicDelivery {
        deliveryType = "on-demand"
    }
}
