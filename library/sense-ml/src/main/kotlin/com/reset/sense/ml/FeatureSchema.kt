package com.reset.sense.ml

import java.security.MessageDigest

/**
 * Single source of truth for the acceptance-model feature schema on the
 * Kotlin side. Mirrors `ml/feature_spec.py` exactly.
 *
 * Any change to feature order, count, normalization constants, or window size
 * MUST bump [SCHEMA_VERSION] and be replicated in the Python spec. The spec
 * hash below is compared against the hash stamped into every model artifact —
 * a model trained against a different pipeline refuses to load
 * (see [SchemaGuard]). This makes normalization drift a loud failure instead
 * of a silent accuracy bug.
 */
object FeatureSchema {

    const val SCHEMA_VERSION = 3
    const val FEATURE_COUNT = 34
    const val WINDOW_MINUTES = 5
    const val MODEL_TASK = "accept_given_prompt"

    // Feature indices — the layout of the FloatArray(34) built by FeatureBuilder.
    const val IDX_FOREGROUND_DURATION = 0
    const val IDX_APP_SWITCH_COUNT = 1
    const val IDX_UNIQUE_APP_COUNT = 2
    const val IDX_LONGEST_SESSION = 3
    const val IDX_SCREEN_ON_TIME = 4
    const val IDX_CONTINUOUS_SCREEN_ON = 5
    const val IDX_DISTRACTING_RETURNS = 6
    const val IDX_COLD_OPENS = 7
    const val IDX_UNLOCKS_LAST_HOUR = 8
    const val IDX_CAT_FIRST = 9 // 9..14 one-hot, ordinal of AppCategory
    const val IDX_HOUR_SIN = 15
    const val IDX_HOUR_COS = 16
    const val IDX_DOW_SIN = 17
    const val IDX_DOW_COS = 18
    const val IDX_LATE_NIGHT = 19
    const val IDX_ACTIVITY_FIRST = 20 // 20..22 one-hot: still, on_foot, unknown
    const val IDX_MINUTES_IN_ACTIVITY = 23
    const val IDX_CHARGING = 24
    const val IDX_BATTERY = 25
    const val IDX_MIN_SINCE_FIRST_UNLOCK = 26
    const val IDX_MIN_SINCE_LAST_BREAK = 27
    const val IDX_DISMISS_24H = 28
    const val IDX_SNOOZE_24H = 29
    const val IDX_ACCEPT_RATE_7D = 30
    const val IDX_ACCEPT_RATE_THIS_HOUR = 31
    const val IDX_AVG_RESPONSE_DELAY = 32
    const val IDX_BREAK_COMPLETION_RATE = 33

    // Normalization divisors — must match feature_spec.py.
    const val NORM_FOREGROUND_SEC = 300f
    const val NORM_SWITCH_COUNT = 20f
    const val NORM_UNIQUE_APPS = 10f
    const val NORM_LONGEST_SESSION_SEC = 1800f
    const val NORM_SCREEN_ON_SEC = 300f
    const val NORM_CONTINUOUS_SCREEN_MIN = 60f
    const val NORM_DISTRACTING_RETURNS = 5f
    const val NORM_COLD_OPENS = 10f
    const val NORM_UNLOCKS_HOUR = 15f
    const val NORM_MINUTES_IN_ACTIVITY = 120f
    const val NORM_BATTERY_PCT = 100f
    const val NORM_MIN_FIRST_UNLOCK = 960f
    const val NORM_MIN_SINCE_BREAK = 240f
    const val NORM_DISMISS = 5f
    const val NORM_SNOOZE = 5f
    const val NORM_RESPONSE_DELAY_SEC = 120f

    /**
     * Byte-for-byte identical to `feature_spec.canonical_spec_string()` in
     * Python. Hashed to detect cross-language spec drift.
     */
    fun canonicalSpecString(): String = buildString {
        appendLine("schema_version:$SCHEMA_VERSION")
        appendLine("window_minutes:$WINDOW_MINUTES")
        appendLine("0:foreground_duration_sec:div:300")
        appendLine("1:app_switch_count:div:20")
        appendLine("2:unique_app_count:div:10")
        appendLine("3:longest_session_sec:div:1800")
        appendLine("4:screen_on_time_sec:div:300")
        appendLine("5:continuous_screen_on_min:div:60")
        appendLine("6:distracting_return_count:div:5")
        appendLine("7:cold_open_count:div:10")
        appendLine("8:unlock_count_last_hour:div:15")
        appendLine("9:cat_work:onehot")
        appendLine("10:cat_social:onehot")
        appendLine("11:cat_video:onehot")
        appendLine("12:cat_game_dating:onehot")
        appendLine("13:cat_chat:onehot")
        appendLine("14:cat_other:onehot")
        appendLine("15:hour_sin:cyc_sin:24")
        appendLine("16:hour_cos:cyc_cos:24")
        appendLine("17:dow_sin:cyc_sin:7")
        appendLine("18:dow_cos:cyc_cos:7")
        appendLine("19:late_night_flag:flag")
        appendLine("20:activity_still:onehot")
        appendLine("21:activity_on_foot:onehot")
        appendLine("22:activity_unknown:onehot")
        appendLine("23:minutes_in_current_activity:div:120")
        appendLine("24:charging_flag:flag")
        appendLine("25:battery_pct:div:100")
        appendLine("26:min_since_first_unlock_today:div:960")
        appendLine("27:min_since_last_completed_break:div:240")
        appendLine("28:dismiss_count_24h:div:5")
        appendLine("29:snooze_count_24h:div:5")
        appendLine("30:accept_rate_7d:rate")
        appendLine("31:accept_rate_this_hour:rate")
        appendLine("32:avg_response_delay_sec:div:120")
        append("33:break_completion_rate:rate")
    }

    /** SHA-256 of the canonical spec — compared with the model artifact's hash. */
    fun specHash(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(canonicalSpecString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
