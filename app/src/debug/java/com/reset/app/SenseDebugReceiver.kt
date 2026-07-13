package com.reset.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reset.sense.delivery.SenseEvaluator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * DEBUG BUILDS ONLY (src/debug): forces an evaluation tick on demand so the
 * loop can be exercised without waiting out the 15-minute WorkManager cadence.
 *
 *   adb shell am broadcast -a com.reset.app.DEBUG_SENSE_TICK com.reset.app
 *
 * The full decision (action, break type, scores, gate) lands in logcat under
 * the SenseDebug tag; the row lands in the decision log like any real tick.
 */
class SenseDebugReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun evaluator(): SenseEvaluator
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val evaluator = EntryPointAccessors
            .fromApplication(context.applicationContext, Dependencies::class.java)
            .evaluator()

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val decision = evaluator.evaluateNow()
                Timber.tag(TAG).i(
                    "action=%s breakType=%s %s",
                    decision.action, decision.breakType, decision.reason,
                )
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "debug tick failed")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.reset.app.DEBUG_SENSE_TICK"
        private const val TAG = "SenseDebug"
    }
}
