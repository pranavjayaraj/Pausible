package com.reset.sense.delivery

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The periodic evaluation tick. A plain CoroutineWorker + EntryPoint lookup —
 * deliberately NOT @HiltWorker, which would force HiltWorkerFactory
 * configuration onto every host app once this ships as an SDK.
 *
 * The worker assumes nothing about when it last ran: the evaluator re-derives
 * all context from queryEvents + stores, so Doze deferral is harmless.
 */
class SenseTickWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun evaluator(): SenseEvaluator
    }

    override suspend fun doWork(): Result {
        val evaluator = EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .evaluator()
        // A failed tick is a skipped tick: the next one re-derives everything,
        // so retrying with stale context has no value.
        runCatching { evaluator.evaluateNow() }
        return Result.success()
    }
}

/** Schedules the periodic tick and the one-shot snooze re-evaluation. */
class SenseScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Idempotent; call from Application.onCreate (or SDK init later). */
    fun start() {
        val request = PeriodicWorkRequestBuilder<SenseTickWorker>(
            SenseDeliveryConstants.TICK_INTERVAL_MINUTES, TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true) // the battery-aware promise
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SenseDeliveryConstants.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun stop() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(SenseDeliveryConstants.PERIODIC_WORK_NAME)
    }

    /** Snooze = "ask again soon": one re-evaluation (gates included) in 10 min. */
    fun scheduleSnoozeReevaluation() {
        val request = OneTimeWorkRequestBuilder<SenseTickWorker>()
            .setInitialDelay(SenseDeliveryConstants.SNOOZE_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SenseDeliveryConstants.SNOOZE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
