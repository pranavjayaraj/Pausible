package com.reset.sense.signals.activity

import android.content.Context
import com.reset.sense.ml.ActivityState

/** Latest known physical activity + when we entered it. */
data class ActivityStateSample(
    val state: ActivityState,
    val sinceMs: Long,
)

/**
 * SharedPreferences-backed store for the latest activity transition. Written
 * by [ActivityTransitionReceiver] (fires sporadically, outside any DI scope)
 * and read at evaluation time. Survives process death — transition events can
 * arrive hours apart, far past any in-memory state's lifetime.
 */
class ActivityStateStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("sense_activity_state", Context.MODE_PRIVATE)

    fun update(state: ActivityState, timestampMs: Long) {
        prefs.edit()
            .putString(KEY_STATE, state.name)
            .putLong(KEY_SINCE, timestampMs)
            .apply()
    }

    fun current(nowMs: Long): ActivityStateSample {
        val name = prefs.getString(KEY_STATE, null)
        val state = name?.let { runCatching { ActivityState.valueOf(it) }.getOrNull() }
            ?: ActivityState.UNKNOWN
        val since = prefs.getLong(KEY_SINCE, nowMs)
        // A transition that old is stale — the pipeline may have been dead
        // (permission revoked, play services updated). Degrade to UNKNOWN
        // rather than gate/scoring on a phantom "still for 9 hours".
        return if (nowMs - since > STALE_AFTER_MS) {
            ActivityStateSample(ActivityState.UNKNOWN, nowMs)
        } else {
            ActivityStateSample(state, since)
        }
    }

    private companion object {
        const val KEY_STATE = "state"
        const val KEY_SINCE = "since_ms"
        const val STALE_AFTER_MS = 12 * 60 * 60 * 1000L
    }
}
