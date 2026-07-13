# Sense — the context-aware microbreak engine

> **Read this before touching anything under `library/sense-*/` or `library/ml/`.** It explains the
> whole system — signals → features → model → decision → notification → outcome →
> personalization — and the exact procedure for modifying each part safely.
> It is written for both humans and AI agents working in this repo.

---

## 1. What Sense is, in one paragraph

Sense decides **when to deliver a microbreak notification, how strongly, and which
break to suggest** — and adapts per user from their responses. It combines four signal
layers (app usage, system usage, physical activity, notification feedback) into a
34-float feature vector, evaluated every ~15 minutes by a blend of a **deterministic
rules engine** and a **tiny on-device neural network** (~1.5K params, 17.5 KB). The
product principle is restraint: hard gates the model can never override (driving,
quiet hours, 3-prompts/day cap), honest labels (only completed breaks are true
positives), and a reward structure whose real objective is *"the user never disables
notifications."* Everything runs on-device; no raw signal ever leaves the phone.

**Never claim emotion/stress detection.** Outputs are behavioral likelihoods
("likely overextended", "good break window"). Keep this framing in code, copy, and docs.

---

## 2. Architecture map

```
library/ml/                          Python training pipeline (numpy only)
  feature_spec.py            ← SINGLE SOURCE OF TRUTH for the feature schema
  bootstrap_data.py          synthetic cold-start dataset (scenario simulator)
  train.py                   MLP trainer + ship gate + artifact export
  out/model.json             exported weights + schema metadata

library/sense-ml/                    Pure decision core (no Android in the logic)
  FeatureSchema.kt           Kotlin mirror of feature_spec.py (hash-checked)
  Inputs.kt                  pure-data inputs (UsageSnapshot, DeviceSnapshot, …)
  FeatureBuilder.kt          snapshots → FloatArray(34)
  AcceptanceModel.kt         hand-rolled MLP forward pass (NO TFLite — see §5)
  ModelArtifact.kt           model.json contract + SchemaGuard handshake
  RulesEngine.kt             hard gates + interpretable Stage-0 score
  BreakDecisionEngine.kt     gates → rules ⊕ model α-blend → action + break type
  src/main/assets/riverbloom_acceptance_v3.json   the bundled trained model

library/sense-signals/               Android signals → pure-data snapshots
  usage/AppSessionNormalizer.kt   per-Activity events → app-level sessions
  usage/UsageWindowAggregator.kt  sessions+screen+unlocks → UsageSnapshot
  android/*                  UsageStatsManager / PackageManager / battery adapters
  activity/*                 Activity Recognition Transition API + persisted store
  SensePermissions.kt        status-only permission tiers (FULL/REDUCED/MINIMAL)
  SenseSnapshotProvider.kt   assembles everything at evaluation time

library/sense-store/                 The learning-loop ledger (Room)
  DecisionEntity.kt          one row per tick; shown rows = training data
  PromptOutcome.kt           label taxonomy (see §7 — the semantics matter)
  SenseDecisionLog.kt        writes outcomes, assembles ResponseHistory

library/sense-delivery/              Tick + notification + outcome capture
  SenseEvaluator.kt          one tick end-to-end (the orchestrator)
  SenseTickWorker.kt         WorkManager periodic tick + snooze re-eval
  AndroidBreakPresenter.kt   notification build/post (intensity-mapped)
  PromptActionReceiver.kt    tap/swipe/snooze → labeled outcome
  AssetModelLoader.kt        assets → AcceptanceModel; failure → rules-only

app/                         Host wiring
  App.kt                     starts scheduler, transition registrar, coordinator
  SenseBreakCoordinator.kt   accepted prompt → break session; COMPLETED reporting
  MainActivity.kt            handleSenseAction() deep-link (zero-transition start)
  ui/onboarding/SensePermissionsScreen.kt   3-permission onboarding step
  src/debug/…/SenseDebugReceiver.kt         adb-triggerable tick (debug only)
```

Dependency direction (enforced; keep it this way — it is what makes SDK extraction cheap):

```
app → sense-delivery → { sense-signals, sense-store } → sense-ml → (kotlinx-serialization only)
```

`sense-ml` contains **zero Android imports in its logic**. Never add any.

---

## 3. The evaluation loop

```
WorkManager tick (15 min, battery-not-low)          SenseTickWorker
 → capture snapshots (usage/device/time)            SenseSnapshotProvider
 → resolve wind-down successes (see below)          SenseDecisionLog.resolveWindDownOutcomes
 → sweep stale PENDING → IGNORED (30 min timeout)   SenseDecisionLog.sweepIgnored
 → assemble ResponseHistory + GateContext           SenseDecisionLog
 → host app on screen? → suppress, no shown row     HostStateSource seam
      (never notify someone already IN the app; MainActivity.onResume also
       clears any stale prompt still showing when the user opens the app)
      Race guard: an overdue tick is often RELEASED by the very app-open it
      would interrupt (Doze/standby deferral ends on app-open, and the early
      gate can pass during cold start). AndroidBreakPresenter therefore
      re-checks foreground at the final instant before notify(); a withheld
      prompt is marked outcome=NOT_SHOWN, which every cap/rate/training query
      excludes — the user saw nothing, so it counts nowhere.
 → HARD GATES (before any scoring):                 RulesEngine.gate
      IN_VEHICLE (unless active phone use = passenger)
      quiet hours (user-configurable, default 22–07, midnight-wrapping)
        → bounds flow in per tick via the QuietHoursSource seam
          (sense-delivery interface; the app binds it to HomeRepository's
          DataStore prefs in SenseConfigModule; edited from the Profile tab's
          Quiet Hours card) → see WIND-DOWN EXCEPTION below
      cooldown (< 20 min since completed break)
      daily cap — earned-trust adaptive: base 3/day, rises to 5 only when
        trailing completion rate ≥ 0.6 over ≥ 20 labeled outcomes
        (RulesEngine.dailyPromptCap; self-decays with the trailing rate)
      dead hour (≥5 prompts at this hour, <5% acceptance)
 → build FloatArray(34)                             FeatureBuilder
 → ruleScore (interpretable 0..1)                   RulesEngine.score
 → modelScore = P(accept | context)                 AcceptanceModel.predict
 → blend: final = α·model + (1−α)·rules             BreakDecisionEngine
      α = 0.7 · min(1, labeledOutcomes / 200)       ← cold start runs pure rules
 → threshold by SenseMode:
      GENTLE 0.70/0.55 · BALANCED 0.60/0.40 · ASSERTIVE 0.50/0.32
      (≥ first → FULL_PROMPT, ≥ second → SOFT_NUDGE, else SUPPRESS)
 → break type from context (see §6)
 → log decision (+ features ONLY if shown)          SenseDecisionLog.logDecision
 → notification                                     AndroidBreakPresenter
      tap    → ACCEPTED  → app deep-links into the break session
      swipe  → DISMISSED_FAST (≤5 s) / DISMISSED_SLOW
      snooze → SNOOZED + one re-evaluation in 10 min (gates re-checked)
      silence→ IGNORED (next tick's sweep)
 → break finishes → CelebrationEvent.BreakFinished  SenseBreakCoordinator
      → outcome upgraded to COMPLETED               ← the true positive label
```

### The wind-down exception (quiet hours ≠ off)

Quiet hours means "never WAKE the user" — not "ignore a 1 AM doomscroll".
Inside quiet hours, `RulesEngine.gate` returns a third state, `WindDownOnly`,
when ALL of these hold (otherwise plain `QUIET_HOURS`/`NIGHT_CAP`/… suppression):

- continuous screen-on ≥ 20 min (`windDownMinActiveMin`) — the user is
  demonstrably awake and mid-scroll; a quick alarm check never qualifies;
- fewer than 1 wind-down shown tonight (`nightlyWindDownCap`; "tonight" spans
  the most recent quiet-hours start, computed in `SenseEvaluator.quietStartMs`);
- cooldown and dead-hour gates still pass (repeated dismissals at 1 AM make
  the exception self-extinguishing via the dead-hour gate).

In `WindDownOnly` mode the engine constrains the outcome: the ONLY possible
prompt is a **silent SOFT_NUDGE with BreakType.WIND_DOWN**, and only when the
blended score ≥ `windDownThreshold` (0.55 — the rules score already carries a
−0.15 late-night penalty, so this is a daytime-equivalent bar of ~0.70).
Full/heads-up prompts are unreachable at night by construction.

**Wind-down success label:** the true positive for a wind-down is not a tap —
it is *the screen going dark*. If the most recent screen-off lands within
10 min after a pending WIND_DOWN prompt, `resolveWindDownOutcomes` upgrades it
to COMPLETED (this is why resolution runs BEFORE the ignore sweep). The signal
travels as `lastScreenOffMs` on `SenseSnapshots` (from `UsageWindowAggregator`).

Design invariants (do not break):
- **Stateless between ticks.** Everything is re-derived from `queryEvents` + Room.
  There is no "last tick" memory to corrupt; Doze deferrals are harmless.
- **A failed tick is a skipped tick.** The evaluator/worker never throw into the host.
- **Gates run before inference.** The model must never get the chance to learn its way
  around a safety rule.
- **Notifications blocked ⇒ no shown row.** Otherwise unseeable prompts rot into
  IGNORED and poison the labels (see `SenseEvaluator`).

---

## 4. The feature vector (schema v3, 34 floats)

Canonical definition: [`library/ml/feature_spec.py`](../library/ml/feature_spec.py), mirrored
byte-for-byte by [`FeatureSchema.kt`](../library/sense-ml/src/main/kotlin/com/reset/sense/ml/FeatureSchema.kt).
All values normalized to ≈[0,1] (cyclical encodings are [−1,1]).

| Idx | Feature | Normalization | Source |
|---|---|---|---|
| 0 | foreground_duration_sec | ÷300 | UsageEvents |
| 1 | app_switch_count | ÷20 | UsageEvents |
| 2 | unique_app_count | ÷10 | UsageEvents |
| 3 | longest_session_sec | ÷1800 (30-min lookback) | UsageEvents |
| 4 | screen_on_time_sec | ÷300 | screen events |
| 5 | continuous_screen_on_min | ÷60 | screen events |
| 6 | distracting_return_count | ÷5 | sessions + category |
| 7 | cold_open_count | ÷10 (unlocks − notif-driven) | unlock events |
| 8 | unlock_count_last_hour | ÷15 | unlock events |
| 9–14 | app category one-hot | work/social/video/game_dating/chat/other | PackageManager |
| 15–18 | hour & day-of-week sin/cos | cyclical (24 h / 7 d) | clock |
| 19 | late_night_flag | 23:00–05:00 | clock |
| 20–22 | activity one-hot | still/on_foot/unknown | Transition API |
| 23 | minutes_in_current_activity | ÷120 | Transition store |
| 24 | charging_flag | 0/1 | BatteryManager |
| 25 | battery_pct | ÷100 | BatteryManager |
| 26 | min_since_first_unlock_today | ÷960 | unlock events |
| 27 | min_since_last_completed_break | ÷240 | decision log |
| 28 | dismiss_count_24h | ÷5 | decision log |
| 29 | snooze_count_24h | ÷5 | decision log |
| 30 | accept_rate_7d | 0–1 (0.5 cold-start prior) | decision log |
| 31 | accept_rate_this_hour | 0–1 (0.5 prior) | decision log |
| 32 | avg_response_delay_sec | ÷120 | decision log |
| 33 | break_completion_rate | 0–1 (0.5 prior) | decision log |

Notes:
- **Block 27–33 is the personalization block.** Two users in identical contexts get
  different predictions because their own history flows through these features —
  personalization exists even before any on-device training.
- **IN_VEHICLE never reaches the model** (hard-gated upstream; maps defensively to
  the `unknown` slot in `FeatureBuilder`).
- **What is deliberately absent:** package names, notification content, locations,
  contacts, anything typed/read. Only coarse categories and counts. This is the
  privacy boundary (`AppCategoryResolver` is where it is enforced).

---

## 5. The model

- Architecture: **MLP 34 → 32 (ReLU) → 16 (ReLU) → 1 (sigmoid)** ≈ 1,537 params.
- Predicts: **P(user accepts a prompt shown now)** — `model_task: accept_given_prompt`.
- Artifact: `model.json` ≈ 17.5 KB (weights as raw arrays + metadata handshake).
- Current bootstrap metrics: **val PR-AUC 0.711 vs rules baseline 0.575** (base rate 0.43).

**Why no TFLite:** at this size the forward pass is three matrix-vector products —
[`AcceptanceModel.kt`](../library/sense-ml/src/main/kotlin/com/reset/sense/ml/AcceptanceModel.kt)
is ~40 lines of dependency-free Kotlin. Zero native libs, zero version conflicts with
host apps, microsecond inference, fully debuggable. Accumulation is done in `Double`
to keep parity with the float64 training pipeline. **Adopt LiteRT only if** a future
model genuinely needs an interpreter (sequence models over event timelines, >100K
params). The `predict()` contract is the module boundary; swap internals freely.

### The cross-language contract (this is what keeps the system honest)

Three mechanisms make Python-training ↔ Kotlin-inference drift a **loud** failure:

1. **Spec hash** — `feature_spec.canonical_spec_string()` (Python) and
   `FeatureSchema.canonicalSpecString()` (Kotlin) must be byte-identical; their
   SHA-256 is stamped into every artifact and re-checked at load.
2. **SchemaGuard** — validates `schema_version`, `feature_count`, `window_size_min`,
   `model_task`, and the spec hash before any weight is touched. Mismatch →
   `SchemaMismatchException` → fall back to bundled model → fall back to rules.
   `model_task` matters most once multiple artifacts exist (it prevents a future
   break-type bandit from loading into the acceptance slot).
3. **Golden-vector parity test** — `library/ml/train.py` exports 16 probe inputs + outputs;
   `ModelParityTest` replays them through the Kotlin forward pass (tolerance 1e-4).

---

## 6. Rules engine & break-type mapping

`RulesEngine.score()` (must stay in sync with `rules_baseline_score()` in `library/ml/train.py`
— the trainer's ship gate uses it as the bar to beat):

```
+0.40·continuous_screen_on  +0.20·app_switching  +0.15·stillness·still
+0.10·distracting_returns   +0.10·cold_opens     +0.05·charging
−0.30·cooldown(<60 min)     −0.20·dismissals_24h −0.15·late_night
```

Break-type mapping (`BreakDecisionEngine.mapBreakType`, priority order = safety →
circadian → cognitive load):

| Context | BreakType | In-app session (SenseBreakCoordinator) |
|---|---|---|
| ON_FOOT | PASSIVE_REMINDER | breathing 1 min |
| late night | WIND_DOWN | breathing 3 min, 6 s pace |
| switch rate ≥ 0.5 | BREATHING_RESET | breathing 1 min |
| screen ≥ 45 min + still ≥ 90 min | RECOVERY_BREAK | stretch 3 min |
| screen ≥ 30 min | STRETCH | stretch 1 min |
| default | EYE_BREAK | meditate 1 min |

During quiet hours the mapping is bypassed entirely: `WindDownOnly` mode forces
`WIND_DOWN` (silent SOFT_NUDGE) or nothing — see §3. Note a wind-down shown at
night still counts toward the 3/day cap (`promptsShownToday` counts all shown
prompts); this is deliberate conservatism — fewer total prompts, never more.

---

## 7. Labels & the learning loop

The decision log ([`DecisionEntity`](../library/sense-store/src/main/kotlin/com/reset/sense/store/DecisionEntity.kt))
is simultaneously the personalization source, the gate input, and the training dataset.

**Outcome taxonomy** (`PromptOutcome`) — the distinctions carry training semantics.
The weights below are ENFORCED IN CODE as `PromptOutcome.trainingWeight` (signed
sample weight: sign = label, magnitude = how much the row teaches); the training
export `SenseDecisionLog.trainingRows()` returns `(features, weight)` pairs, so
the trainer consumes this philosophy directly. Changing the value table is a
design decision — expect it to be challenged in review.

| Outcome | Meaning | `trainingWeight` |
|---|---|---|
| COMPLETED | break finished (host-reported); for WIND_DOWN: screen off ≤10 min after the nudge | **+1.0 — the true positive** |
| ACCEPTED | tapped, not (yet) completed | +0.2 (attention ≠ success) |
| SNOOZED | right idea, wrong minute | +0.1 (timing signal, not rejection) |
| IGNORED | never responded (30-min sweep) | −0.1 (may not have seen it) |
| DISMISSED_SLOW | swiped after >5 s | −0.5 (considered, declined) |
| DISMISSED_FAST | swiped ≤5 s | **−1.0 — hard negative** (bad timing) |
| NOT_SHOWN | withheld at the last moment (host foregrounded mid-tick); user saw nothing | 0.0 — excluded from every cap, rate, and training query |
| — notifications disabled — | terminal event | the REAL loss function (−10); design everything to avoid it |

Note: `PromptOutcome.isPositive` (tap OR completion) still exists but feeds only
the accept-rate FEATURES (Block 5), where responsiveness is the right question.
It is not, and must never become, the training label.

**Labeling rules (non-negotiable):**
- Only windows where a prompt was **shown** are labeled. Suppressed ticks are logged
  (diagnostics) but **never** become training rows. Do not "helpfully" label
  unprompted windows with heuristics — the model would just re-learn the rules
  engine (leakage/circularity).
- Train on **completion, not clicks**, or the copy bandit (future) will drift toward
  clickbait that gets taps and abandons.
- Selection bias: the rules engine only shows prompts where rules already fire.
  Before training v2 on real data, turn on **ε-exploration** (~5%) at borderline
  scores inside allowed windows — without it the model cannot learn beyond the
  rules. The plumbing is shipped and dormant (§8.6).

**Propensity trail (shipped, active from day one):** every decision row carries
`appliedThreshold`, `explorationEpsilon`, and `explored` alongside
`blendedScore`. Together these reconstruct P(show | context) for every
historical decision — the input off-policy evaluation (IPS/doubly-robust)
needs to score a candidate model against logged data BEFORE shipping it.
These fields cannot be back-filled; that is why they ship before launch even
though nothing consumes them yet. Do not remove or stop populating them.

Export seam: `SenseDecisionLog.trainingRows()` → `(FloatArray(34), signed weight)` pairs
(schema-filtered; see §8.1).

---

## 8. How to modify things (checklists)

### 8.1 Add / change / remove a feature
1. Edit `SPEC` in [`library/ml/feature_spec.py`](../library/ml/feature_spec.py) **and** the mirrored
   constants + `canonicalSpecString()` in `FeatureSchema.kt` — identically.
2. **Bump `SCHEMA_VERSION` in both. No exceptions** — even for "just" changing a
   normalization divisor. Silent normalization drift is the classic on-device ML bug;
   the hash check only saves you if the bump is mechanical.
3. Update `FeatureBuilder.kt` and `bootstrap_data.featurize()` to populate it.
4. Update the producing layer (`UsageWindowAggregator` / `SenseSnapshotProvider` / DAO).
5. Retrain: `cd ml && python3 train.py` (needs numpy only). The ship gate aborts the
   export if the model stops beating the rules baseline.
6. Refresh artifacts:
   ```bash
   cp library/ml/out/model.json library/sense-ml/src/main/assets/riverbloom_acceptance_v3.json
   cp library/ml/out/model.json library/ml/out/golden_vectors.json library/sense-ml/src/test/resources/
   cp library/ml/out/model.json library/sense-delivery/src/test/resources/
   ```
   (Consider renaming the asset when the schema version bumps: `…_v4.json` + update
   `AssetModelLoader.ASSET_NAME`.)
7. `./gradlew :sense-ml:test :sense-signals:test :sense-store:test :sense-delivery:test`
   — the parity + spec-hash tests will catch any divergence you missed.

Decision-log rows are stamped with the `SCHEMA_VERSION` that produced their
features (`DecisionEntity.schemaVersion`); after a bump, `trainingRows()`
automatically excludes old-schema rows from training (their floats no longer
align with the new indices) while every outcome-stat read keeps using them.
No manual data cleanup is needed when bumping the schema.

### 8.2 Retrain only (same schema)
Steps 5–7 above. Nothing else changes; `SchemaGuard` accepts any artifact whose
metadata matches the compiled pipeline.

### 8.3 Change gates / thresholds / cadence
- Gates & rule weights: `RulesEngine` (constructor params + `score()`); if you touch
  `score()`, mirror it in `rules_baseline_score()` in `library/ml/train.py`.
- Mode thresholds: `SenseMode` enum. α ramp: `BreakDecisionEngine` constructor.
- Tick cadence / snooze delay / daily copy: `SenseDeliveryConstants`.
- Cooldown / dead-hour / cap: `RulesEngine` constructor defaults. The daily cap
  is earned-trust adaptive (base→earned on completion evidence); raising the
  BASE cap is an anti-pattern — more notifications must be earned by completed
  breaks, never granted for engagement.

### 8.4 Change the model architecture
- Layer sizes: edit `HIDDEN` in `library/ml/train.py` — the Kotlin side reads shapes from the
  artifact, so **no Kotlin change needed** for width/depth changes (ReLU hidden +
  sigmoid output assumed; anything else needs `AcceptanceModel` support).
- New model *kind* (GRU, bandit head, …): new `model_task` string, new loader path,
  keep `predict()`-style contract behind the module boundary. Never reuse
  `accept_given_prompt` for a different prediction target.

### 8.5 Swap bootstrap data for real data
When enough labeled rows exist (thousands): export via `trainingRows()`, write a
loader in `library/ml/` replacing `bootstrap_data.generate()`, keep everything else
(trainer, ship gate, export) identical. Bootstrap labels are simulated — their only
job was cold-start weights that roughly agree with the rules while already using
the personalization features.

### 8.5b Schema bumps after real data exists: retrain vs. backfill
Standing policy: **just retrain** (old-schema rows stay excluded via
`DecisionEntity.schemaVersion`). Per-user personalization survives regardless —
it lives in outcome stats, not weights (§ "what survives" reasoning): the only
loss is old feature vectors, and while the simulator is the primary corpus
there is nothing real to lose.

**Backfill (impute + retrain) is the documented exception**, allowed only when
ALL THREE hold:
1. the change is a **pure append** (existing indices/normalizations untouched);
2. real logged rows are the primary training source AND fresh-schema rows are
   still scarce;
3. old rows are **down-weighted (~0.3×)** and the new column is padded with its
   cold-start prior — never a value that asserts a false observation.

Why not backfill by default: padded columns are constant across the old corpus,
so the new signal learns only from fresh rows while old rows anchor the network
toward "this feature doesn't vary" — and the model cannot distinguish
"unknown" from "neutral". Implementation, when needed, is a ~10-line change in
the (future) real-data loader: filter by `schemaVersion`, pad old rows to the
new width, apply the down-weight. Never backfill across a reorder, removal, or
renormalization — strict exclusion is the only honest option there.

### 8.6 Roadmap to a world-class timing model (design agreed; staged by data volume)

The meta-principle: this problem is ~20% model architecture, ~80% data regime.
Exploration, propensities, honest counterfactual evaluation, and an objective
matching the product truth compound; fancy models on biased logged data plateau.
Each stage below has an ACTIVATION TRIGGER — building it earlier is premature.

1. **ε-exploration** — PLUMBING SHIPPED, DORMANT.
   `BreakDecisionEngine.explorationEpsilon` (0f in SenseDeliveryModule).
   Exploration only ever upgrades a borderline SUPPRESS (within 0.10 of the
   soft threshold) to a SOFT_NUDGE; never through hard gates, never in
   wind-down, never to FULL_PROMPT. The propensity trail (§7) is logged from
   day one regardless.
   *Trigger: raise ε to ~0.05 once ~hundreds of users spread the tax thin.*

2. **Empirical-Bayes shrinkage** for the personalization block: shrink
   per-hour accept rates toward the user's overall rate, and the user's rate
   toward the population rate — "2 of 3 accepted at 14:00" should read ~0.55,
   not 0.67. ~10 lines in `SenseDecisionLog.responseHistory()`.
   *Trigger: first users with 10+ outcomes.*

3. **Off-policy evaluation harness** in `ml/` (IPS / doubly-robust over the
   propensity trail): score any candidate model against logged data BEFORE
   shipping. Turns retraining from "train and hope" into "train and measure".
   *Trigger: ~5–10k labeled rows.*

4. **Budget-aware dynamic threshold**: with 3–5 prompts/day, pointwise
   P(accept) is the wrong objective — firing at a 0.62 morning window is a
   mistake if 0.80 comes at 14:00. Track each user's daily score distribution
   and fire only above their personal ~85th percentile, pacing intraday
   (ad-delivery pacing math). *Trigger: alongside stage 3.*

5. **Real-data retrain** with the completion-graded sample weights
   (§8.5 loader; weights from `PromptOutcome.trainingWeight`).
   *Trigger: same ballpark; ship only if it beats rules AND the bootstrap
   model on OPE.*

6. **Thompson sampling** over a Bayesian last layer (replaces ε-greedy:
   explores where uncertainty is highest, converging faster per annoyance
   spent) + calibrated abstention (widen toward rules when uncertain).
   **Copy-variant bandit** (4–6 archetypes as arms, `copy_variant` on the
   decision row) and **break-type bandit** (`model_task: break_type_bandit` —
   never load into the acceptance slot). **On-device per-user head**
   (logistic layer / bandit posterior over the frozen global model, bounded
   state in Room — never retrain the full net on device).
   *Trigger: after stages 1–5 prove the loop compounds.*

7. **The long game**: uplift objective (optimize INCREMENTAL completed breaks,
   `P(break|prompt) − P(break|silence)`, estimable only with exploration data —
   raw acceptance rewards prompting people who'd have rested anyway); small
   temporal model (GRU over event sequences) — only if it beats the MLP on
   OPE, since it costs the pure-Kotlin inference story (LiteRT).
   **`trained_at` staleness policy**: decay α toward rules if the global
   model is > 6 months old.

Supporting signal work, any time: notification-listener seam (makes
`cold_open_count` honest), mood check-in joins as outcome enrichment,
k-anonymized aggregate calibration of the global prior (opt-in only, never
raw rows — see §10).

---

## 9. Testing map

| Suite | Module | What it proves |
|---|---|---|
| `ModelParityTest` | sense-ml | Python and Kotlin are numerically the same model; spec hash matches |
| `SchemaGuardTest` | sense-ml | every drift dimension is rejected loudly |
| `BreakDecisionEngineTest` | sense-ml | gates (passenger lift, midnight wrap), α ramp, modes, break mapping, wind-down exception (one silent nudge, nightly cap, self-extinguishing), earned-trust daily cap ladder |
| `FeatureBuilderTest` | sense-ml | normalization, one-hots, cyclical continuity at midnight, cold-start priors |
| `AppSessionNormalizerTest` | sense-signals | split-screen, PAUSED→STOPPED dedupe, debounce merge semantics |
| `UsageWindowAggregatorTest` | sense-signals | every derived usage signal, degraded (no-event) paths, lastScreenOffMs |
| `SenseDecisionLogTest` | sense-store | outcome classification, history assembly, priors, retention, training-weight hierarchy (completion > clicks), schema-orphan exclusion, wind-down screen-off resolution |
| `SenseEvaluatorTest` | sense-delivery | the full tick with fakes, real-bundled-model run, user-configured quiet hours, wind-down loop end-to-end |
| `ProfileViewModelTest` | feature:profile | quiet-hours card: load from prefs, stepper wrap (22+3→1), persistence |

Run all: `./gradlew :sense-ml:test :sense-signals:test :sense-store:test :sense-delivery:test :feature:profile:test`
(95 tests at time of writing).

**Device smoke test:**
```bash
./gradlew :app:installDebug
adb shell am broadcast -a com.reset.app.DEBUG_SENSE_TICK com.reset.app
adb logcat -s SenseDebug     # → action=… breakType=… blended=… (rules=…, model=…, α=…)
```
A fresh install mostly logs SUPPRESS (cold-start cooldown priors, empty usage window).
That is correct behavior. Use the phone a few minutes and re-trigger.

---

## 10. Privacy invariants (never violate)

1. Raw signals stay on-device. If any cloud layer is ever added, it may receive only
   summarized state ("good break window"), never raw logs or the feature vector.
2. The model never sees package names — only the 6-way coarse category
   (`AppCategoryResolver` is the enforcement point; curated overrides first,
   self-declared manifest category second, never the reverse).
3. No notification content, contacts, locations, or typed text — anywhere.
4. `SensePermissions` reports status only; the host owns all permission UX. Every
   permission maps to a documented feature (the onboarding copy doubles as the
   Play-review justification).
5. Retention: decision log purges at 90 days.

---

## 11. SDK extraction notes

The `sense-*` modules are deliberately SDK-shaped (est. ~1 week to a standalone AAR
set). The rules that keep it that way:

- Constructors stay plain (`@Inject` but no Hilt types in signatures); Hilt appears
  only at three edges — `PromptActionReceiver`, `SenseTickWorker` (EntryPoint lookup,
  one-line swap to a service locator), and the DI modules.
- No `@HiltWorker`/`HiltWorkerFactory` — would force config onto host apps.
- The evaluator depends on interfaces (`SnapshotSource`, `BreakPresenter`), never on
  the Android implementations.
- `sense-ml` stays pure Kotlin; features never import from `app/` or feature modules.
- Public API sketch, naming (`riverbloom-sense`, host schedules *intent + window*,
  never timestamps), and the full friction table live in the session design notes;
  the one real refactor is de-Hilt.

### KMP / iOS portability (assessed; not planned work)

The decision core is ~1 week from `commonMain`: `:sense-ml` has two JVM leaks
(`MessageDigest` in `specHash()` → expect/actual, `"%.2f".format()` in reason
strings), the signals pure-logic half (`RawUsageEvent`, normalizer, aggregator)
is already platform-independent by design, `SenseEvaluator` needs
`java.util.Calendar` → kotlinx-datetime, Room has KMP support (2.7+), and the
de-Hilt bill is shared with SDK extraction. Skipping TFLite pays off again
here: the pure-Kotlin forward pass runs unchanged on iOS.

The hard parts are platform, not Kotlin — and no architecture fixes them:
- **iOS signal poverty**: no `UsageStatsManager` equivalent (Screen Time APIs
  are entitlement-locked). Only ~15 of 34 features are populatable (motion,
  charging, time, notification outcomes, own-app usage). iOS Sense would be a
  degraded tier — closer to smart-scheduled reminders than context detection.
- **iOS background model**: `BGAppRefreshTask` is opportunistic, not periodic.
  The evaluator's statelessness (every tick re-derives everything) is already
  the right shape; the strategy shifts to evaluate-on-foreground + granted
  refreshes + pre-scheduled local notifications.

Discipline that keeps the option open (costs nothing now): no new `java.*`
imports in `:sense-ml` or the pure-logic halves; platform capabilities enter
only through the seams; prefer schema features with graceful neutral values.
