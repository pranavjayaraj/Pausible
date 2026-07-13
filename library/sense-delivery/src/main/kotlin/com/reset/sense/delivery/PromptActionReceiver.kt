package com.reset.sense.delivery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reset.sense.delivery.SenseDeliveryConstants.ACTION_ACCEPTED
import com.reset.sense.delivery.SenseDeliveryConstants.ACTION_DISMISSED
import com.reset.sense.delivery.SenseDeliveryConstants.ACTION_SNOOZED
import com.reset.sense.delivery.SenseDeliveryConstants.EXTRA_DECISION_ID
import com.reset.sense.store.SenseDecisionLog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Terminal point of the feedback loop: the user's response to a prompt
 * arrives here and becomes a labeled outcome in the decision log.
 *
 * Accept additionally launches the host app (the tap must land IN the break —
 * zero-transition start), and snooze schedules a one-shot re-evaluation.
 *
 * Uses a Hilt EntryPoint rather than @AndroidEntryPoint so the dependency
 * lookup is one contained call — the SDK extraction swaps this single line
 * for a service locator.
 */
class PromptActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun decisionLog(): SenseDecisionLog
        fun scheduler(): SenseScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        val decisionId = intent.getLongExtra(EXTRA_DECISION_ID, -1L)
        if (decisionId < 0) return
        val action = intent.action ?: return
        val deps = EntryPointAccessors.fromApplication(context, Dependencies::class.java)
        val nowMs = System.currentTimeMillis()

        // goAsync keeps the process alive for the short DB write.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_ACCEPTED -> {
                        deps.decisionLog().recordAccepted(decisionId, nowMs)
                        launchHostApp(
                            context,
                            decisionId,
                            intent.getStringExtra(SenseDeliveryConstants.EXTRA_BREAK_TYPE),
                        )
                    }
                    ACTION_DISMISSED -> deps.decisionLog().recordDismissed(decisionId, nowMs)
                    ACTION_SNOOZED -> {
                        deps.decisionLog().recordSnoozed(decisionId, nowMs)
                        dismissNotification(context)
                        // Snooze is a soft yes: re-evaluate (not re-fire) soon.
                        deps.scheduler().scheduleSnoozeReevaluation()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun launchHostApp(context: Context, decisionId: Long, breakType: String?) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Host deep-links straight into the break (zero-transition start).
        launch.putExtra(EXTRA_DECISION_ID, decisionId)
        breakType?.let { launch.putExtra(SenseDeliveryConstants.EXTRA_BREAK_TYPE, it) }
        context.startActivity(launch)
    }

    private fun dismissNotification(context: Context) {
        androidx.core.app.NotificationManagerCompat.from(context)
            .cancel(SenseDeliveryConstants.NOTIFICATION_ID)
    }
}
