package com.reset.sense.signals.android

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class BatterySample(
    val charging: Boolean,
    val batteryPercent: Int,
)

/** Charging + battery level from the sticky battery broadcast (no permission). */
interface DeviceStateSource {
    fun battery(): BatterySample
}

class AndroidDeviceStateSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceStateSource {

    override fun battery(): BatterySample {
        // Sticky broadcast: registerReceiver(null, …) reads it without a receiver.
        val intent: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 50
        return BatterySample(charging = charging, batteryPercent = percent)
    }
}
