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
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME_DATING
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
                ApplicationInfo.CATEGORY_NEWS -> AppCategory.OTHER
                else -> AppCategory.OTHER
            }
        }
        return AppCategory.OTHER
    }

    private companion object {
        /** Curated, on-device only. Extend freely; keep it boring and factual. */
        val OVERRIDES: Map<String, AppCategory> = mapOf(
            // social
            "com.instagram.android" to AppCategory.SOCIAL,
            "com.twitter.android" to AppCategory.SOCIAL,
            "com.zhiliaoapp.musically" to AppCategory.SOCIAL, // TikTok
            "com.snapchat.android" to AppCategory.SOCIAL,
            "com.facebook.katana" to AppCategory.SOCIAL,
            "com.reddit.frontpage" to AppCategory.SOCIAL,
            "com.linkedin.android" to AppCategory.SOCIAL,
            "com.pinterest" to AppCategory.SOCIAL,
            // video
            "com.google.android.youtube" to AppCategory.VIDEO,
            "com.netflix.mediaclient" to AppCategory.VIDEO,
            "in.startv.hotstar" to AppCategory.VIDEO,
            "com.amazon.avod.thirdpartyclient" to AppCategory.VIDEO,
            "app.revanced.android.youtube" to AppCategory.VIDEO,
            // dating (grouped with games as the compulsive-loop bucket)
            "com.tinder" to AppCategory.GAME_DATING,
            "com.bumble.app" to AppCategory.GAME_DATING,
            "co.hinge.app" to AppCategory.GAME_DATING,
            // chat
            "com.whatsapp" to AppCategory.CHAT,
            "org.telegram.messenger" to AppCategory.CHAT,
            "com.discord" to AppCategory.CHAT,
            "org.thoughtcrime.securesms" to AppCategory.CHAT, // Signal
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
