package com.reset.app

import android.app.Application
import com.reset.model.domain.ReminderScheduler
import com.reset.sense.delivery.SenseScheduler
import com.reset.sense.signals.activity.ActivityTransitionRegistrar
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var senseScheduler: SenseScheduler

    @Inject
    lateinit var activityTransitionRegistrar: ActivityTransitionRegistrar

    @Inject
    lateinit var senseBreakCoordinator: SenseBreakCoordinator

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        // Arm the reminder chain for the persisted preferences (KEEP — never resets a
        // pending countdown). Covers first launch and app updates; WorkManager itself
        // persists the chain across reboots.
        appScope.launch { reminderScheduler.ensureScheduled() }

        // Sense: the context-aware microbreak engine.
        // - periodic evaluation tick (KEEP; battery-aware; survives reboots via WorkManager)
        // - activity transitions (no-op until ACTIVITY_RECOGNITION is granted; re-invoked
        //   idempotently every launch so a grant takes effect on the next start)
        // - completion observer: upgrades a sense-launched break's outcome to COMPLETED
        senseScheduler.start()
        activityTransitionRegistrar.register()
        senseBreakCoordinator.startObserving(appScope)
    }
}
