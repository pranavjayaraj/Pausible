package com.reset.sense.signals.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.reset.sense.ml.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Package name → coarse [AppCategory]. This is the privacy boundary: the
 * category is the ONLY thing derived from a package name that ever reaches
 * the model.
 *
 * Resolution order: curated overrides first, manifest category second —
 * `ApplicationInfo.category` is self-declared by developers and frequently
 * `CATEGORY_UNDEFINED`, so the override list is primary, never the fallback.
 */
interface AppCategoryResolver {
    fun categoryOf(packageName: String): AppCategory
}

class AndroidAppCategoryResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppCategoryResolver {

    private val cache = ConcurrentHashMap<String, AppCategory>()

    override fun categoryOf(packageName: String): AppCategory =
        cache.getOrPut(packageName) { resolve(packageName) }

    private fun resolve(packageName: String): AppCategory {
        OVERRIDES[packageName]?.let { return it }
        OVERRIDE_PREFIXES.entries.firstOrNull { packageName.startsWith(it.key) }?.let { return it.value }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val info = runCatching { context.packageManager.getApplicationInfo(packageName, 0) }
                .getOrNull() ?: return AppCategory.OTHER
            return when (info.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL_FEED
                ApplicationInfo.CATEGORY_VIDEO -> AppCategory.STREAMING
                ApplicationInfo.CATEGORY_GAME -> AppCategory.REWARD_LOOP
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
                // News apps are feed-scrollers behaviorally — a 30-minute
                // headline doomscroll should count as distracting returns.
                ApplicationInfo.CATEGORY_NEWS -> AppCategory.SOCIAL_FEED
                else -> AppCategory.OTHER
            }
        }
        return AppCategory.OTHER
    }

    private companion object {
        /** Curated, on-device only. Extend freely; keep it boring and factual. */
        val OVERRIDES: Map<String, AppCategory> = mapOf(
            // feeds (social networks, short-video, doomscroll surfaces)
            "com.instagram.android" to AppCategory.SOCIAL_FEED,
            "com.twitter.android" to AppCategory.SOCIAL_FEED, // X kept the package
            "com.zhiliaoapp.musically" to AppCategory.SOCIAL_FEED, // TikTok
            "com.snapchat.android" to AppCategory.SOCIAL_FEED,
            "com.facebook.katana" to AppCategory.SOCIAL_FEED,
            "com.reddit.frontpage" to AppCategory.SOCIAL_FEED,
            "com.linkedin.android" to AppCategory.SOCIAL_FEED,
            "com.pinterest" to AppCategory.SOCIAL_FEED,
            "in.mohalla.sharechat" to AppCategory.SOCIAL_FEED,
            "in.mohalla.video" to AppCategory.SOCIAL_FEED, // Moj
            "com.eterno.shortvideos" to AppCategory.SOCIAL_FEED, // Josh
            // streaming (long-form passive watching)
            "com.google.android.youtube" to AppCategory.STREAMING,
            "com.netflix.mediaclient" to AppCategory.STREAMING,
            "in.startv.hotstar" to AppCategory.STREAMING,
            "com.amazon.avod.thirdpartyclient" to AppCategory.STREAMING,
            "app.revanced.android.youtube" to AppCategory.STREAMING,
            "com.jio.media.ondemand" to AppCategory.STREAMING, // JioCinema
            "tv.twitch.android.app" to AppCategory.STREAMING,
            // reward loops (dating swipes; games arrive via CATEGORY_GAME;
            // fantasy-sports apps are sideloaded so the manifest never helps)
            "com.tinder" to AppCategory.REWARD_LOOP,
            "com.bumble.app" to AppCategory.REWARD_LOOP,
            "co.hinge.app" to AppCategory.REWARD_LOOP,
            "com.app.dream11Pro" to AppCategory.REWARD_LOOP, // Dream11
            // messaging
            "com.whatsapp" to AppCategory.MESSAGING,
            "org.telegram.messenger" to AppCategory.MESSAGING,
            "com.discord" to AppCategory.MESSAGING,
            "org.thoughtcrime.securesms" to AppCategory.MESSAGING, // Signal
            // work
            "com.slack" to AppCategory.WORK,
            "com.google.android.gm" to AppCategory.WORK,
            "com.microsoft.office.outlook" to AppCategory.WORK,
            "com.google.android.apps.docs.editors.docs" to AppCategory.WORK,
            "com.notion.id" to AppCategory.WORK,
            "com.microsoft.teams" to AppCategory.WORK,
        )

        val OVERRIDE_PREFIXES: Map<String, AppCategory> = mapOf(
            "com.google.android.apps.docs" to AppCategory.WORK,
            "com.microsoft.office" to AppCategory.WORK,
        )
    }
}
