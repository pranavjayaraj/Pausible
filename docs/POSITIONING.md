# Product Positioning & Data Classification Brief

> **Status:** source of truth · **Decided:** 2026-07-25 · **Buyer:** B2C (individuals)
>
> This one-pager freezes the positioning decision so every downstream choice —
> classifier label set, nudge copy, analytics lanes — derives from it instead of
> being re-litigated. If a change contradicts this doc, change the doc first.

---

## 1. Positioning

> **A personal focus & energy companion that notices when you're running low and
> hands you the right 60-second reset — no timers to set, it just knows.**

| | |
|---|---|
| **Category** | Productivity / focus (light wellbeing flavor). App Store: Productivity. |
| **Outcome we sell** | Stay focused; avoid the burnout dip. |
| **Mechanism we deliver** | Well-timed micro-breaks. |
| **The moat / wedge** | Passive, on-device state detection. Competitors make you set timers and self-track; we *notice*. Lead every message with **"it just knows."** |
| **NOT** | A meditation app, a mental-health app, a mood tracker. We never market emotional inference. |

**Why this corner:** biggest + lowest-stigma market, lightest compliance burden, and it's
the one position that actually uses the moat (a dumb timer or a content catalog does not).

---

## 2. Taxonomy tiers

All 17 need-states stay in the model. What changes is **how each tier is surfaced.**

| Tier | States | Surfacing |
|---|---|---|
| **1 — Headline (user-facing)** | BODY_TENSION, HAND_STRAIN, EYE_STRAIN, SCATTERED, OVERWHELMED, CONFUSED_LOST, TASK_PARALYSIS, SENSORY_OVERLOAD, RESTLESS, ACCOMPLISHED_FLOW | Ordinary productivity/ergonomic telemetry. Freely user-facing and cloud-trackable. |
| **2 — Borderline (framed as energy)** | DRAINED, BIOLOGICAL_DEPLETION, CANT_WIND_DOWN | Health-*adjacent*; frame as energy/focus, handle with light care. |
| **3 — Confirmed by user (never marketed)** | LOW_MOOD, WOUND_UP, STUCK_ON_A_THOUGHT, DISCONNECTED + mood | **On-device only.** Text may *suggest* emotional chips, but the user **always confirms** — never silently auto-applied or stored from inference. Shapes nudge timing; never a marketed feature. |

Five of the seventeen — CONFUSED_LOST, TASK_PARALYSIS, SENSORY_OVERLOAD, BIOLOGICAL_DEPLETION,
ACCOMPLISHED_FLOW — are **text-only** (`NeedState.textOnly`). The chip grid stays a scannable
ten; these are states people describe rather than shop for in a grid, and they exist to make the
text path visibly better than tapping. All of them are Tier 1–2, so nothing text-only depends on
the emotional-confirmation rule above.

---

## 2a. Inference boundary (privacy by design)

Two paths, split by sensitivity:

- **Non-emotional (Tier 1–2: focus / energy / ergonomics):** inferred silently from text and
  **auto-applied**. Chips appear only when the classifier is unsure.
- **Emotional / mood (Tier 3):** text is used **only to *offer* chips** — the user **always
  confirms**, at any confidence. Emotional states are **never** silently auto-applied or stored
  from inference alone. The recorded mood is always the user's choice. If text is ambiguous
  *between* an emotional and a non-emotional state → show chips (the safe side).

> We infer to *suggest*; the user *decides*. The transient emotional inference (text → candidate
> chips) is on-device and assistive, not asserting — which keeps covert profiling off the table
> while still using the text. It does **not** downgrade the data: mood is still Class 2 (§4), and
> only the user's confirmed choice is stored — never the raw inferred candidate.

**Chips fire when:** the top candidate is **emotional** (always) **or** the result is
**ambiguous** (near-tie / low-confidence, any tier). Silent auto-apply happens **only** when the
read is non-emotional **and** confident **and** unambiguous. Nothing scores (below `T_low`) → ask
to say more.

---

## 3. The hard copy rule: action, not diagnosis

The app may **think** a state; it may only **say** an action + benefit. This single rule is
what keeps us on the productivity side of the line.

| Model thinks | ✅ App says | ❌ App must never say |
|---|---|---|
| `LOW_MOOD` | "Feeling heavy? Here's a quick 2-minute reset." | "You seem down / depressed." |
| `WOUND_UP` | "Take a breath — a 60-second physiological sigh." | "You seem angry." |
| `DISCONNECTED` | "Send a quick thank-you to someone who helped this week." | "You seem lonely." |
| `SCATTERED` | "Let's refocus for one minute." | (fine either way — Tier 1) |

The moment the UI names an emotion at the user, we've become the wellness app we're avoiding.

---

## 4. Data classification

Classify the **dimension/property**, not just the event. The test:

> **Is this value reverse-mappable to a need-state?** If yes → Lane A or aggregate-only.

| Class | Examples | Where it may go |
|---|---|---|
| **1 — Raw content** | the sentence the user typed | Device only. **Never** cloud. |
| **2 — Emotional (inferred *or* self-reported)** | `need_state`, **self-reported mood**, chip labels, emotion-mapped screen/session names, `nudge_trigger` | Device only. Cloud **only** as k-anonymous aggregate counts. |
| **3 — Interaction metadata** | word_count, chip_count, duration, completion bool, neutral screen views | Cloud OK (with consent). |
| **4 — Derived aggregates** | completion rate, DAU, cohort flow counts | Cloud OK. |

> **Self-reported ≠ non-sensitive.** Volunteered mood is still Class 2 and follows the same
> rules. Self-report improves the *consent* story, not the *classification*.

---

## 5. Analytics lanes

**Lane A — on-device ledger** (mirrors `SenseDecisionLog`): logs everything, including Class 1/2.
Powers the ML and doubles as weak-label training data. Never leaves the device.

**Lane B — cloud product analytics** (behind the `AnalyticsManager` seam; consented): Class 3/4 only.

**Lane B green-list (safe):**
`app_opened`, `screen_viewed`*(neutral screens only)*, `checkin_submitted{word_count}`,
`chip_shown{count}`, `chip_selected`*(fact only)*, `nudge_shown` / `nudge_accepted` /
`nudge_declined`*(no trigger state)*, `session_started/completed/abandoned`*(generic type)*,
`streak_reached`, funnels, retention, cohort aggregates.

**Never crosses to cloud:** raw text · which need-state · which chips · emotion-mapped
screen/session names · per-user emotional timelines · identity-linked fingerprintable
timestamp patterns.

**Want per-emotion product insight?** Aggregate on-device → export k-anonymous cohort counts
("LOW_MOOD flow viewed 1,240× this week"), never per-user events.

---

## 6. Compliance posture

- **Disclose, don't market.** The privacy policy states plainly that the app performs on-device
  emotional inference to time nudges. It is not a selling point and not hidden.
- **Tier 3 is sensitive regardless of branding.** "Productivity" is positioning; an inference
  about sadness/loneliness/anxiety is special-category (GDPR Art. 9-adjacent) data no matter the
  label. Treat it as such — which the Lane-A-only rule already does.
- **Consent-gate Lane B**, and audit the analytics SDK for auto-collected identifiers.
- Not legal advice — confirm specifics with counsel for your target markets.

---

## 7. Reviewer checklist (mechanical)

- [ ] Does any cloud event name or property reverse-map to a need-state? → move to Lane A / aggregate.
- [ ] Does any user-facing string name an emotion? → rewrite as action + benefit.
- [ ] Does raw check-in text leave the device anywhere? → it must not.
- [ ] Is a Tier-3 state used as a marketed feature? → it must not be.
- [ ] Is Lane B gated on consent? → required.
- [ ] Is any mood / Tier-3 state *auto-applied or stored from inference* without the user confirming via chips? → it must not be.
- [ ] For emotional text, do chips appear *regardless of confidence* (never bypassed by a confident guess)? → required.
