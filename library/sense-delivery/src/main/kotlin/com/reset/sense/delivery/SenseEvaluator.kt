package com.reset.sense.delivery

import com.reset.sense.ml.BreakDecisionEngine
import com.reset.sense.ml.BreakType
import com.reset.sense.ml.Decision
import com.reset.sense.ml.DecisionInput
import com.reset.sense.ml.FeatureBuilder
import com.reset.sense.ml.GateContext
import com.reset.sense.ml.PromptAction
import com.reset.sense.store.SenseDecisionLog
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One evaluation tick, end to end:
 *
 *   sweep stale outcomes → capture snapshots → assemble history/gates from
 *   the decision log → decide → log the decision → present if warranted.
 *
 * Stateless between ticks: everything is re-derived, so Doze deferrals and
 * process death cannot corrupt the loop. Never throws — a failed tick is a
 * skipped tick, not a crashed host process.
 */
@Singleton
class SenseEvaluator @Inject constructor(
    private val snapshotSource: SnapshotSource,
    private val decisionLog: SenseDecisionLog,
    private val engine: BreakDecisionEngine,
    private val presenter: BreakPresenter,
    private val quietHoursSource: QuietHoursSource,
    private val hostStateSource: HostStateSource,
) {

    suspend fun evaluateNow(nowMs: Long = System.currentTimeMillis()): Decision {
        val snapshots = snapshotSource.capture(nowMs)

        // Outcome hygiene, in order: wind-down successes FIRST (screen went
        // dark soon after the nudge = COMPLETED), then the ignore sweep —
        // otherwise a successful wind-down would rot into IGNORED before the
        // success could be observed.
        decisionLog.resolveWindDownOutcomes(nowMs, snapshots.lastScreenOffMs)
        decisionLog.sweepIgnored(nowMs)
        decisionLog.purgeOldRows(nowMs)

        // Notifications blocked = nothing we show can be seen. Log a suppress
        // (diagnostics) but never create a shown row that would sweep to
        // IGNORED and poison the labels with prompts nobody could see.
        if (!presenter.canPresent()) {
            val suppressed = suppressedDecision("notifications_disabled")
            decisionLog.logDecision(suppressed, null, nowMs, hourOf(nowMs))
            return suppressed
        }

        // The host app is on screen: a notification now interrupts someone
        // who is ALREADY in the product. Suppress (a good window here is the
        // future in-app banner's job, not the notification channel's) and,
        // like the blocked-notifications path, never create a shown row.
        if (hostStateSource.isHostForeground()) {
            val suppressed = suppressedDecision("host_foreground")
            decisionLog.logDecision(suppressed, null, nowMs, hourOf(nowMs))
            return suppressed
        }

        val hour = snapshots.time.hourOfDay
        val history = decisionLog.responseHistory(nowMs, hour)
        val quietHours = quietHoursSource.quietHours()
        val input = DecisionInput(
            usage = snapshots.usage,
            device = snapshots.device,
            time = snapshots.time,
            history = history,
            gate = GateContext(
                // Sustained screen-on inside the window ⇒ the user is actively
                // on the phone (lifts the vehicle gate for passengers).
                activePhoneUse = snapshots.usage.screenOnTimeSec >= ACTIVE_USE_MIN_SCREEN_SEC,
                promptsShownToday = decisionLog.promptsShownToday(localMidnightMs(nowMs)),
                quietHoursStart = quietHours.startHour,
                quietHoursEnd = quietHours.endHour,
                windDownShownTonight = decisionLog.windDownShownSince(
                    quietStartMs(nowMs, quietHours.startHour),
                ),
            ),
        )

        val decision = engine.decide(input)
        val shown = decision.action != PromptAction.SUPPRESS
        // Features are persisted ONLY for shown prompts — that row + its
        // outcome is one labeled training example. Suppressed ticks never
        // become training data (no-heuristic-labels rule).
        val features = if (shown) FeatureBuilder.build(input) else null
        val decisionId = decisionLog.logDecision(decision, features, nowMs, hour)

        if (shown && !presenter.present(decision, decisionId)) {
            // Withheld at the last moment (host foregrounded mid-tick, or
            // posting failed): the user saw nothing, so the row must not
            // count as shown anywhere — caps, rates, or training.
            decisionLog.recordNotShown(decisionId, nowMs)
        }
        return decision
    }

    private fun suppressedDecision(reason: String) = Decision(
        action = PromptAction.SUPPRESS,
        breakType = BreakType.NONE,
        blendedScore = 0f,
        ruleScore = 0f,
        modelScore = null,
        modelAlpha = 0f,
        gateReason = null,
        reason = reason,
    )

    private fun hourOf(nowMs: Long): Int =
        Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)

    private fun localMidnightMs(nowMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /**
     * Start of "tonight": the most recent occurrence of the quiet-hours start
     * hour. At 01:00 with quiet start 22, that is YESTERDAY 22:00 — the
     * nightly wind-down cap spans the whole night, not the calendar day.
     */
    private fun quietStartMs(nowMs: Long, quietStartHour: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, quietStartHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis > nowMs) calendar.add(Calendar.DAY_OF_YEAR, -1)
        return calendar.timeInMillis
    }

    private companion object {
        const val ACTIVE_USE_MIN_SCREEN_SEC = 60
    }
}
