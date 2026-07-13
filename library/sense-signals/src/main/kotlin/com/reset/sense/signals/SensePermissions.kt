package com.reset.sense.signals

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

/** What the engine can actually see, given granted permissions. */
enum class SenseAccuracyTier {
    /** Usage access + activity recognition: all 34 features live. */
    FULL,

    /** Usage access only: motion gates lost, usage/screen features live. */
    REDUCED,

    /** Neither: only time/device-state features; rules engine runs degraded. */
    MINIMAL,
}

/**
 * Permission status only — this module never prompts. The host app owns all
 * permission UX; each permission maps to a documented feature set so the
 * host can decide what to request (and justify it in Play review).
 */
object SensePermissions {

    fun usageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun activityRecognitionGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true // pre-Q the GMS permission is install-time granted
        }

    fun tier(context: Context): SenseAccuracyTier = when {
        usageAccessGranted(context) && activityRecognitionGranted(context) -> SenseAccuracyTier.FULL
        usageAccessGranted(context) -> SenseAccuracyTier.REDUCED
        else -> SenseAccuracyTier.MINIMAL
    }
}
