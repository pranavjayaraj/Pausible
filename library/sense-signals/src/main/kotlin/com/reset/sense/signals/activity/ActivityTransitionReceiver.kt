package com.reset.sense.signals.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.reset.sense.ml.ActivityState

/**
 * Receives Activity Recognition Transition API events and persists the latest
 * state. Deliberately DI-free: transition broadcasts fire on their own
 * schedule and the store is cheap to construct.
 */
class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val store = ActivityStateStore(context)

        for (event in result.transitionEvents) {
            if (event.transitionType != ActivityTransition.ACTIVITY_TRANSITION_ENTER) continue
            val state = when (event.activityType) {
                DetectedActivity.STILL -> ActivityState.STILL
                DetectedActivity.WALKING,
                DetectedActivity.RUNNING,
                DetectedActivity.ON_FOOT -> ActivityState.ON_FOOT
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_BICYCLE -> ActivityState.IN_VEHICLE
                else -> ActivityState.UNKNOWN
            }
            // elapsedRealTimeNanos is boot-relative; convert to wall clock.
            val bootMs = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
            val timestampMs = bootMs + event.elapsedRealTimeNanos / 1_000_000L
            store.update(state, timestampMs)
        }
    }
}
