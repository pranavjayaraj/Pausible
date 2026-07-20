
Great feature to invest in — this is the screenshot-and-share surface, and we're sitting on unusually good data for it. Here's the catalog, organized by what powers each (important, because some work off the decision log today while others need the daily-rollup table first). The design principle throughout: a kind mirror, not a report card — this is a wellness app; the same fact can shame ("4.2 hours wasted") or reflect ("your longest unbroken stretch was 96 minutes — your brain asked for water at minute 40").

Daily reflections ("How today went")
1. The Day Rhythm strip — an hour-by-hour intensity ribbon (calm teal → saturated amber) showing when the day was heavy, when it breathed. One glance = the day's shape. Needs: hourly rollup.

2. First Reach — "Your phone met you at 6:42 today — 11 minutes after waking." The single most humanizing stat we have; everyone feels this number. Powered by: min_since_first_unlock — cheap rollup.

3. The Deep Block — longest uninterrupted focus stretch, framed as an achievement with context: "94 minutes on one thing. That's rarer than you'd think." Needs: rollup of longest_session.

4. Mental Tab Count — the app-switching story: "You held 7 apps in rotation at 2 PM — your switchiest hour." Names the fragmentation feeling people can't articulate. Needs: hourly switch counts.

5. The Pull — cold opens, phrased kindly: "You checked your phone 23 times. 9 of those, nothing was waiting." That second clause is the reflection — habit vs. purpose. Needs: rollup.

6. The Save (wind-down wins) — when last night's wind-down ended in screens-off: "Last night's nudge bought you ~40 minutes of earlier dark." Works today — decision log.

Weekly reflections ("How the week was")
7. The Fingerprint — a 7×24 heatmap of the week. Genuinely unique per person, beautiful, and the shareable artifact. Needs: hourly rollups.

8. Your Hours, Revealed — from accept/completion rates: "3 PM you says yes to breaks. 9 AM you doesn't want to hear it. We've stopped asking." Works today — this is literally the dead-hour data.

9. Week Persona — a computed archetype card: Tuesday Deep-Diver, Reformed Night Owl, Momentum Builder, The Wanderer (high on-foot minutes). Rule-based assignment from the week's dominant pattern, 8–12 personas, seasonal rotation. This is the Wrapped move — identity, not statistics. Mostly works today + rollups make it richer.

10. The Rescued-Minutes Ledger — cumulative "time given back": completed breaks × recovery estimate + wind-down saves. A number that only goes up, week after week — the retention hook: "Sense has returned 6.2 hours to you since June." Works today.

11. Gentle Deltas — week-over-week movement with directional kindness: improvements celebrated specifically, regressions framed as observations ("later nights than last week — anything going on?"). Never red arrows. Needs: weekly rollups.

12. Body vs. Screen wheel — still/walking/vehicle minutes against screen time: "You moved 2× more than last week. Your longest walk was Wednesday." Needs: activity rollup.

13. Mood × Rhythm echo — join the app's existing mood check-ins: "On days you completed 2+ breaks, your mood check-ins ran higher." Framed strictly as noticing, never causation. Works today — mood data exists in the app.

The differentiator nobody else can build
14. The Engine's Diary — Sense narrating its own restraint: "This week I considered 672 moments. I stayed quiet 661 times. I spoke 11 times — you took 7 of them. I've learned Thursday mornings are yours alone." Every decision (including suppressions!) is already logged, so this is pure decision-log reads. It converts the invisible ML into a felt relationship, it's a trust/transparency flex no competitor with server-side spray-and-pray can copy, and it quietly explains why the app notifies so rarely — turning restraint from an absence into a character.

Recommended starter set
If I were sequencing: build the daily rollup entity first (the prerequisite for half the catalog), then ship five: First Reach, The Deep Block, The Fingerprint, The Engine's Diary, Rescued-Minutes Ledger. That set covers daily + weekly, needs only data we control, includes the two most shareable artifacts (7 and 14), and every one degrades gracefully when permissions are missing (the tier system already tells us what's populated).

Two build notes when we start: the rollup writes belong in the existing evaluator tick (it already captures snapshots — persisting an hourly aggregate is nearly free), and the templates live app-side, not in the SDK — reflections are Riverbloom's voice, timing is Sense's job, and that boundary keeps the SaaS story clean.

Want me to start with the rollup entity + the first card?


This is a natural fit — and you're sitting on an unfair head start: **the app already has Sprout**, the mascot living in onboarding and peeking over the streak card. Don't introduce avatars; *evolve the character users already met*. A disconnected avatar system is gamification bolted on; Sprout growing because *you* rested is the product's thesis made visible.

Here's the system I'd design — three layers, from permanent to momentary:

## Layer 1: The Growth Line (permanent evolution)

Sprout advances through life stages, fed by **completed breaks** (note: completions, not taps, not app-opens — the same reward-hygiene lesson from our training labels applies to gamification; reward the wrong currency and users learn to farm it):

| Stage | Name | Threshold (completed breaks) | Character |
|---|---|---|---|
| 1 | **Seed** | 0 | Day one — mostly asleep, one eye open |
| 2 | **Sprout** | 7 | The mascot as known today |
| 3 | **Seedling** | 25 | First true leaf; a little posture |
| 4 | **Sapling** | 60 | Stands on its own; sways when idle |
| 5 | **Bloom** | 150 | First flower — the screenshot moment |
| 6 | **Grove Keeper** | 365 | Mature, calm, occasionally waters *smaller background plants* |

**The one iron rule: stages never regress.** A lapsed week doesn't shrink the tree. Wellness gamification dies the moment it punishes — Duolingo can afford guilt mechanics; an app whose promise is "we respect you" cannot. Growth pauses; it never reverses.

## Layer 2: The Daily Expression (today, reflected)

On top of the permanent stage, Sprout carries **today's state** — the avatar as a living mirror of the Sense data:

- Completed a break recently → perky, small idle bounce
- Long doomscroll afternoon → slightly wilted, *but framed as invitation, not judgment* — a droopy leaf and a thought-bubble water droplet says "I could use a break" (Sprout needs it, not "you failed")
- Wind-down honored last night → dewdrops this morning
- Deep-focus day → Sprout wearing tiny reading glasses

This layer is where the emotional loop lives: the *reason to take a break* becomes partly "my sprout perks up when I do." Tamagotchi mechanics, minus the death.

## Layer 3: Rare Forms & Collections (variety and surprise)

The behaviors we uniquely detect become **rare cosmetic unlocks** — this is where our data does things no other app can:

- **Night Bloom** — a moonflower grows after 5 honored wind-downs ("some flowers only open for people who sleep")
- **The Wanderer** — moss + tiny walking boots for on-foot break streaks
- **Deep Root** — for completing a `RECOVERY_BREAK` after 90+ min stationary sessions, 10 times
- **Four Seasons** — complete all four signature sessions (Sigh, Horizon, Unfold, Ember) in one week
- **Weekly leaves** — each week's persona (from the reflections feature) presses a leaf into a collection book; a year of use = a garden journal of 52 distinct weeks

## Where Sprout lives

1. **Home tab** — the primary presence, idle-animated (Rive: one rig, stage + expression + accessories as inputs — exactly the video-vs-code decision again, and again code/Rive wins)
2. **Session landing** — growth moments happen *here*: finish a break → "+1" → occasional stage-up celebration (celebration store already exists for exactly this)
3. **Home-screen widget** — Sprout's current state on the launcher is the quietest retention mechanic there is: a wilting sprout on the home screen *is* a break reminder that costs zero notification budget
4. **The Engine's Diary crossover** — "I stayed quiet 94 times this week. Sprout grew two leaves."

## Why this design fits *this* product

- Every input already exists: `breaksTaken`, streaks, wind-down saves, break types, activity — no new collection, just new expression of the decision log and stats.
- The currency is honest: completions (our +1.0 label) drive growth, so the gamification and the ML optimize the *same* objective — a rare and beautiful alignment; most apps' game layer fights their health layer.
- It compounds with everything queued: reflections reference Sprout, the widget feeds the in-app-banner surface, rare forms give the session bandit's variety a visible payoff.

**Build order:** Layer 1 alone ships first (a stage function over `breaksTaken` + static art per stage — days of work with placeholder art), Layer 2 needs the daily rollup (already queued for reflections), Layer 3 rides on both. Want me to spec the stage/expression model as data (same config pattern as the session scripts) so art can be swapped in when it's ready?