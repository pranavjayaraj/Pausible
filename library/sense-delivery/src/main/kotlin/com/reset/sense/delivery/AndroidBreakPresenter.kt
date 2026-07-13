package com.reset.sense.delivery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reset.sense.delivery.SenseDeliveryConstants.ACTION_ACCEPTED
import com.reset.sense.delivery.SenseDeliveryConstants.ACTION_DISMISSED
import com.reset.sense.delivery.SenseDeliveryConstants.ACTION_SNOOZED
import com.reset.sense.delivery.SenseDeliveryConstants.CHANNEL_ID
import com.reset.sense.delivery.SenseDeliveryConstants.EXTRA_DECISION_ID
import com.reset.sense.delivery.SenseDeliveryConstants.NOTIFICATION_ID
import com.reset.sense.ml.Decision
import com.reset.sense.ml.PromptAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Builds and posts the break prompt. Intensity maps to notification behavior:
 * SOFT_NUDGE is silent/low (glanceable, no interruption), FULL_PROMPT is a
 * default-importance heads-up. Accept/dismiss/snooze all round-trip through
 * [PromptActionReceiver] carrying the decision id — that broadcast IS the
 * labeled outcome.
 */
class AndroidBreakPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hostStateSource: HostStateSource,
) : BreakPresenter {

    override fun canPresent(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun present(decision: Decision, decisionId: Long): Boolean {
        // Last-moment race guard: an overdue WorkManager tick is often
        // released by the very app-open it would interrupt (Doze/standby
        // deferral ends when the user opens the app). The evaluator's early
        // gate can pass during cold start before the first activity reaches
        // STARTED — this check runs at the final instant before notify().
        if (hostStateSource.isHostForeground()) return false
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(SenseDeliveryConstants.titleFor(decision.breakType))
            .setContentText(SenseDeliveryConstants.bodyFor(decision.breakType))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(
                if (decision.action == PromptAction.SOFT_NUDGE) NotificationCompat.PRIORITY_LOW
                else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setSilent(decision.action == PromptAction.SOFT_NUDGE)
            .setContentIntent(actionIntent(ACTION_ACCEPTED, decisionId, decision.breakType.name))
            .setDeleteIntent(actionIntent(ACTION_DISMISSED, decisionId))
            .addAction(
                0,
                SenseDeliveryConstants.SNOOZE_LABEL,
                actionIntent(ACTION_SNOOZED, decisionId),
            )
            .build()

        // SecurityException if permission was revoked mid-flight — a withheld
        // prompt, never a crashed host.
        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }.isSuccess
    }

    override fun dismissCurrent() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun actionIntent(
        action: String,
        decisionId: Long,
        breakType: String? = null,
    ): PendingIntent {
        val intent = Intent(context, PromptActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_DECISION_ID, decisionId)
            .apply { breakType?.let { putExtra(SenseDeliveryConstants.EXTRA_BREAK_TYPE, it) } }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        // Request code mixes action + id so the three intents never collide.
        return PendingIntent.getBroadcast(
            context, (action.hashCode() * 31 + decisionId.toInt()), intent, flags,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                SenseDeliveryConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = SenseDeliveryConstants.CHANNEL_DESCRIPTION },
        )
    }
}
