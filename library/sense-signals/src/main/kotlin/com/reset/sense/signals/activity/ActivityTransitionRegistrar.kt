package com.reset.sense.signals.activity

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.reset.sense.signals.SensePermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Registers for activity transitions. Call once at app start (and after the
 * ACTIVITY_RECOGNITION permission is granted); registration is idempotent —
 * re-registering with the same PendingIntent replaces the previous request.
 */
class ActivityTransitionRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @SuppressLint("MissingPermission") // guarded by SensePermissions check below
    fun register() {
        if (!SensePermissions.activityRecognitionGranted(context)) return

        val transitions = listOf(
            DetectedActivity.STILL,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
        ).map { activityType ->
            ActivityTransition.Builder()
                .setActivityType(activityType)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        }

        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent())
        // Failure is non-fatal: without transitions the store stays UNKNOWN
        // and the engine simply loses motion gating (REDUCED accuracy tier).
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        // Play services appends the transition result to the intent, so it
        // must be MUTABLE on S+.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private companion object {
        const val REQUEST_CODE = 5601
    }
}
