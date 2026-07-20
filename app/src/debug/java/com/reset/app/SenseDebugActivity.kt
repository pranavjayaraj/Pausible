package com.reset.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.reset.sense.delivery.QuietHours
import com.reset.sense.delivery.QuietHoursSource
import com.reset.sense.delivery.SenseDeliveryConstants
import com.reset.sense.delivery.SenseEvaluator
import com.reset.sense.delivery.SnapshotSource
import com.reset.sense.ml.FeatureSchema
import com.reset.sense.ml.ResponseHistory
import com.reset.sense.ml.RulesEngine
import com.reset.sense.signals.SenseSnapshots
import com.reset.sense.signals.android.UsageEventsSource
import com.reset.sense.signals.usage.RawUsageEvent
import com.reset.sense.store.DecisionEntity
import com.reset.sense.store.DecisionLogDao
import com.reset.sense.store.PromptOutcome
import com.reset.sense.store.SenseDecisionLog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DEBUG BUILDS ONLY (src/debug): a disposable inspector for the Sense
 * notification pipeline. Shows every logged evaluation tick — when a prompt
 * was shown, WHY (gate reason vs. scores vs. environment), what the user did
 * with it — plus the exact [ResponseHistory] the engine would see right now.
 *
 * Launch: the extra "Sense Debug" launcher icon on debug installs, or
 *   adb shell am start -n com.reset.app/.SenseDebugActivity
 *
 * TO REMOVE: delete this file and its <activity> entry in
 * src/debug/AndroidManifest.xml. Nothing else references it. (The
 * `DecisionLogDao.recent()` query it reads is harmless to leave behind.)
 */
private data class DebugData(
    val rows: List<DecisionEntity>,
    val history: ResponseHistory,
    val promptsToday: Int,
    val dailyCap: Int,
    /** Live phone-usage capture through the same seam the evaluator uses. */
    val snapshots: SenseSnapshots?,
    val snapshotError: String? = null,
    /** Gate-context inputs the evaluator would assemble right now. */
    val quietHours: QuietHours?,
    val windDownsTonight: Int,
    /** Raw usage-event telemetry for the last hour, newest first. Queried
     *  transiently for display — raw events are never persisted anywhere. */
    val rawEvents: List<RawUsageEvent>,
    val lastTickResult: String? = null,
)

class SenseDebugActivity : ComponentActivity() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun dao(): DecisionLogDao
        fun decisionLog(): SenseDecisionLog
        fun evaluator(): SenseEvaluator
        fun snapshotSource(): SnapshotSource
        fun quietHoursSource(): QuietHoursSource
        fun usageEventsSource(): UsageEventsSource
    }

    private var data by mutableStateOf<DebugData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reload()
        setContent {
            MaterialTheme {
                SenseDebugScreen(
                    data = data,
                    onRefresh = { reload() },
                    onRunTick = { runTick() },
                )
            }
        }
    }

    private fun deps(): Dependencies =
        EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)

    private fun reload(lastTickResult: String? = null) {
        lifecycleScope.launch {
            data = withContext(Dispatchers.IO) { load(lastTickResult) }
        }
    }

    private fun runTick() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { deps().evaluator().evaluateNow() }
                    .fold(
                        onSuccess = { "tick → ${it.action} ${it.breakType} (${it.reason})" },
                        onFailure = { "tick FAILED: $it" },
                    )
            }
            data = withContext(Dispatchers.IO) { load(result) }
        }
    }

    private suspend fun load(lastTickResult: String?): DebugData {
        val d = deps()
        val nowMs = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val midnightMs = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val history = d.decisionLog().responseHistory(nowMs, hour)
        val snapshots = runCatching { d.snapshotSource().capture(nowMs) }
        val quietHours = runCatching { d.quietHoursSource().quietHours() }.getOrNull()
        return DebugData(
            rows = d.dao().recent(ROW_LIMIT),
            history = history,
            promptsToday = d.decisionLog().promptsShownToday(midnightMs),
            dailyCap = RulesEngine().dailyPromptCap(history),
            snapshots = snapshots.getOrNull(),
            snapshotError = snapshots.exceptionOrNull()?.toString(),
            quietHours = quietHours,
            windDownsTonight = quietHours?.let {
                d.decisionLog().windDownShownSince(quietStartMs(nowMs, it.startHour))
            } ?: 0,
            rawEvents = runCatching {
                d.usageEventsSource().query(nowMs - RAW_EVENT_WINDOW_MS, nowMs).asReversed()
            }.getOrDefault(emptyList()),
            lastTickResult = lastTickResult,
        )
    }

    /** Most recent occurrence of the quiet-hours start (mirrors SenseEvaluator). */
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
        const val ROW_LIMIT = 200
        const val RAW_EVENT_WINDOW_MS = 60 * 60 * 1000L
    }
}

// ---------------------------------------------------------------- UI

private val GREEN = Color(0xFF2E7D32)
private val AMBER = Color(0xFFB07D00)
private val RED = Color(0xFFC62828)
private val GRAY = Color(0xFF757575)
private val BLUE = Color(0xFF1565C0)

private val timeFormat = SimpleDateFormat("MMM dd  HH:mm:ss", Locale.US)
private val timeOnlyFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
private fun SenseDebugScreen(
    data: DebugData?,
    onRefresh: () -> Unit,
    onRunTick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Sense Debug", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = onRunTick) { Text("Run tick") }
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
        }
        if (data == null) {
            Text("Loading…", color = GRAY)
            return@Column
        }
        data.lastTickResult?.let {
            Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = BLUE)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { EngineCard(data) }
            item { UsageCard(data) }
            item { HistoryCard(data) }
            item { RawEventsCard(data.rawEvents) }
            item {
                Text(
                    "Decision log (newest first, last ${data.rows.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (data.rows.isEmpty()) {
                item { Text("No ticks logged yet — press \"Run tick\".", color = GRAY) }
            }
            items(data.rows, key = { it.id }) { row -> DecisionRow(row) }
        }
    }
}

@Composable
private fun EngineCard(data: DebugData) {
    val n = data.history.labeledOutcomeCount
    val alphaIfEnabled = 0.7f * min(1f, n / 200f)
    DebugCard("Engine") {
        Mono("model: " + if (SenseDeliveryConstants.MODEL_ENABLED) "ENABLED" else "DISABLED (rules-only)")
        Mono("schema: v${FeatureSchema.SCHEMA_VERSION}   mode: BALANCED (full ≥ 0.60, soft ≥ 0.40)")
        Mono("α: 0.00 in effect (would be %.2f at %d labeled outcomes)".format(alphaIfEnabled, n))
        Mono("prompts today: ${data.promptsToday} / ${data.dailyCap} cap")
        val q = data.quietHours
        Mono(
            if (q == null) "quiet hours: unavailable"
            else "quiet hours: %02d–%02d   wind-downs tonight: %d / 1".format(
                q.startHour, q.endHour, data.windDownsTonight,
            ),
        )
    }
}

@Composable
private fun UsageCard(data: DebugData) {
    DebugCard("Phone usage (live capture — what a tick NOW would see)") {
        val s = data.snapshots
        if (s == null) {
            Mono("capture failed: ${data.snapshotError}", RED)
            return@DebugCard
        }
        val u = s.usage
        val d = s.device
        Mono("signals tier: ${s.accuracyTier} (FULL = all permissions granted)")
        Mono("window: foreground ${u.foregroundDurationSec}s / 300s   category ${u.foregroundCategory}")
        Mono("screen-on: ${u.screenOnTimeSec}s window · continuous ${u.continuousScreenOnMin} min (wind-down needs ≥ 20)")
        Mono("switches: ${u.appSwitchCount}   unique apps: ${u.uniqueAppCount}   longest session: ${u.longestSessionSec}s")
        Mono("distracting returns: ${u.distractingReturnCount}   cold opens: ${u.coldOpenCount}   unlocks/hr: ${u.unlockCountLastHour}")
        Mono(
            "last screen-off: " + (
                s.lastScreenOffMs?.let { "${(System.currentTimeMillis() - it) / 60_000} min ago" } ?: "n/a"
                ),
        )
        Mono("activity: ${d.activityState} for ${d.minutesInCurrentActivity} min")
        Mono(
            "battery: ${d.batteryPercent}%" + (if (d.charging) " charging" else "") +
                "   first unlock: ${d.minutesSinceFirstUnlockToday} min ago",
        )
    }
}

@Composable
private fun HistoryCard(data: DebugData) {
    val h = data.history
    DebugCard("ResponseHistory (what the engine sees NOW)") {
        val sinceBreak =
            if (h.minutesSinceLastCompletedBreak == Int.MAX_VALUE) "never"
            else "${h.minutesSinceLastCompletedBreak} min ago"
        Mono("last completed break: $sinceBreak")
        Mono("accept 7d: %.2f   this hour: %.2f (raw %.2f)".format(h.acceptRate7d, h.acceptRateThisHour, h.acceptRateThisHourRaw))
        Mono("completion rate 30d: %.2f   labeled outcomes: %d".format(h.breakCompletionRate, h.labeledOutcomeCount))
        Mono("last 24h: ${h.dismissCount24h} dismissed, ${h.snoozeCount24h} snoozed")
        Mono("prompts ever at this hour: ${h.promptsShownThisHourHistoric}")
    }
}

/**
 * Raw usage-event telemetry: the exact event stream the aggregator distills
 * into UsageSnapshot. Queried transiently per refresh — never persisted, and
 * package names never leave this debug screen (the model only ever sees the
 * 6-way coarse category).
 */
@Composable
private fun RawEventsCard(events: List<RawUsageEvent>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    DebugCard("Telemetry: raw usage events, last hour (${events.size})") {
        Mono(
            if (expanded) "▲ tap to hide" else "▼ tap to show (transient — raw events are never stored)",
            GRAY,
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
        )
        if (!expanded) return@DebugCard
        if (events.isEmpty()) {
            Mono("no events — usage-access permission missing?", RED)
        }
        events.take(RAW_EVENTS_SHOWN).forEach { e ->
            Mono("${timeOnlyFormat.format(Date(e.timestampMs))}  ${e.type.name.padEnd(10)} ${e.packageName}")
        }
        if (events.size > RAW_EVENTS_SHOWN) {
            Mono("… ${events.size - RAW_EVENTS_SHOWN} older events not shown", GRAY)
        }
    }
}

private const val RAW_EVENTS_SHOWN = 80

/** Feature names parsed straight from the canonical spec — always in sync. */
private val featureNames: List<String> by lazy {
    FeatureSchema.canonicalSpecString().lines()
        .filter { it.firstOrNull()?.isDigit() == true }
        .map { it.split(':')[1] }
}

@Composable
private fun DecisionRow(row: DecisionEntity) {
    var expanded by rememberSaveable(row.id) { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(
            Modifier
                .clickable(enabled = row.featuresCsv != null) { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    timeFormat.format(Date(row.timestampMs)),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = GRAY,
                )
                Text(
                    row.action + if (row.breakType != "NONE") " · ${row.breakType}" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = actionColor(row.action),
                )
            }
            Mono(whyLine(row))
            if (row.wasShown) {
                val outcome = row.outcomeEnum
                Row {
                    Text(
                        "→ ${row.outcome} (weight ${outcome.trainingWeight})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = outcomeColor(outcome),
                    )
                    row.responseDelaySec?.let {
                        Spacer(Modifier.width(8.dp))
                        Text("after ${it}s", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GRAY)
                    }
                }
            }
            if (row.schemaVersion != FeatureSchema.SCHEMA_VERSION) {
                Mono("⚠ old schema v${row.schemaVersion} — excluded from training", RED)
            }
            if (row.featuresCsv != null) {
                Mono(if (expanded) "▲ feature vector" else "▼ feature vector (tap)", GRAY)
                if (expanded) FeatureDump(row.featuresCsv!!)
            }
        }
    }
}

/** The persisted 34-float model input, named per the schema. Out-of-range
 *  values (normalization bugs) are flagged red. */
@Composable
private fun FeatureDump(csv: String) {
    val values = csv.split(',').map { it.toFloatOrNull() ?: Float.NaN }
    if (values.size != FeatureSchema.FEATURE_COUNT) {
        Mono("⚠ ${values.size} floats, expected ${FeatureSchema.FEATURE_COUNT}", RED)
    }
    values.forEachIndexed { i, v ->
        val lowerBound = if (i in CYCLICAL_RANGE) -1f else 0f
        val bad = v.isNaN() || v < lowerBound - RANGE_EPS || v > 1f + RANGE_EPS
        Mono(
            "%2d %-30s %s%.3f".format(i, featureNames.getOrElse(i) { "?" }, if (bad) "⚠ " else "", v),
            if (bad) RED else Color.Unspecified,
        )
    }
}

private val CYCLICAL_RANGE = 15..18 // hour/dow sin+cos encode to [-1, 1]
private const val RANGE_EPS = 1e-3f

/** One line answering "why did this tick do what it did". */
private fun whyLine(row: DecisionEntity): String = when {
    row.gateReason != null -> "hard gate: ${row.gateReason}"
    // Environment suppressions (notifications off / host app on screen) log
    // no threshold and no gate — SenseEvaluator.suppressedDecision().
    row.action == "SUPPRESS" && row.appliedThreshold == null ->
        "env suppress: notifications disabled or app already on screen"
    else -> buildString {
        append("scored: blended %.2f".format(row.blendedScore))
        append(" (rules %.2f".format(row.ruleScore))
        append(row.modelScore?.let { ", model %.2f, α %.2f".format(it, row.modelAlpha) } ?: ", no model")
        append(")")
        row.appliedThreshold?.let { append(" vs bar %.2f".format(it)) }
        if (row.explored) append("  ε-EXPLORED")
    }
}

private fun actionColor(action: String) = when (action) {
    "FULL_PROMPT" -> GREEN
    "SOFT_NUDGE" -> AMBER
    else -> GRAY
}

private fun outcomeColor(outcome: PromptOutcome) = when (outcome) {
    PromptOutcome.COMPLETED -> GREEN
    PromptOutcome.ACCEPTED,
    PromptOutcome.OPENED_APP,
    -> if (PromptOutcome.CLICK_IS_SUCCESS) GREEN else AMBER
    PromptOutcome.SNOOZED -> AMBER
    PromptOutcome.DISMISSED_FAST -> RED
    PromptOutcome.DISMISSED_SLOW -> Color(0xFFE65100)
    PromptOutcome.PENDING -> BLUE
    else -> GRAY // IGNORED, NOT_SHOWN
}

@Composable
private fun DebugCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            content()
        }
    }
}

@Composable
private fun Mono(text: String, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = color, modifier = modifier)
}
