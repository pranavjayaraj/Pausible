package com.reset.feature.sessions.content

import com.reset.feature.sessions.api.SessionDestination

/**
 * The signature session catalog. Voice rules (see docs/SENSE_ML.md-adjacent
 * content guide): second person, present tense, body-first, zero mysticism,
 * lightly wry, never shaming. Arrival lines may reference Sense's trigger
 * context — the one thing no competitor's canned content can do.
 *
 * Sense trigger names (BreakType.name strings, kept as string keys so this
 * module needs no sense-ml dependency): CONTINUOUS-focus and fragmentation
 * arrive as "STRETCH"/"RECOVERY_BREAK"/"BREATHING_RESET"; night arrives as
 * "WIND_DOWN"; eyes as "EYE_BREAK".
 */
object SessionScripts {

    // ------------------------------------------------------------ The Sigh

    val THE_SIGH = SessionScript(
        id = "sigh_v1",
        displayName = "The Sigh",
        arrivalByTrigger = mapOf(
            "BREATHING_RESET" to CopyPool(
                listOf(
                    "Seven tabs open in your head. Let's close a few.",
                    "That was a lot of switching. One thing now: air.",
                    "Your attention's been sprinting. Thirty seconds of walking.",
                    "Scattered is a state, not a personality. Breathe.",
                ),
            ),
        ),
        arrivalDefault = CopyPool(
            listOf(
                "The fastest reset your body knows. Ready?",
                "Two breaths in, one long one out. That's the whole trick.",
                "Borrowed from your own biology: the sigh.",
            ),
        ),
        guide = listOf(
            GuideStep.Breath(
                pattern = BreathPattern.SIGH,
                cycles = 4,
                cue = CopyPool(
                    listOf(
                        "Breathe in… a little more in… and let it all go.",
                        "Fill up… top it off… and empty slowly.",
                        "In through the nose… once more… out long through the mouth.",
                    ),
                ),
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Notice your shoulders. Lower than a minute ago, right?",
                "That slight drop you feel — that's your nervous system saying thanks.",
                "One more normal breath. No technique. Just yours.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — one thing at a time now.",
                "Go on. The tabs will still be there; you're different.",
                "That's it. Pick ONE thing to return to.",
            ),
        ),
    )

    // ------------------------------------------------------------ Horizon

    val HORIZON = SessionScript(
        id = "horizon_v1",
        displayName = "Horizon",
        arrivalByTrigger = mapOf(
            "EYE_BREAK" to CopyPool(
                listOf(
                    "Your eyes have been at arm's length for a long while. Give them a mile.",
                    "Close focus, long time. Your eyes earned a horizon.",
                    "Screens are near. You were built for far. Thirty seconds of far.",
                ),
            ),
        ),
        arrivalDefault = CopyPool(
            listOf(
                "This one isn't on the screen. It's out the window.",
                "The 20-20-20 rule, done properly. We'll keep time.",
            ),
        ),
        guide = listOf(
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Find the farthest thing you can see. Rest your eyes on it.",
                        "Look past everything — the farthest point you've got. Stay there.",
                        "Out the window if you can. The end of the room if you can't.",
                    ),
                ),
                dwellSec = 20,
                dimScreen = true, // the screen gets out of the way — it is the problem
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Now blink slowly five times. Let each one finish.",
                        "Five slow blinks — like your eyes are stretching.",
                    ),
                ),
                holdSec = 10,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Feel that? Eyes a little wetter, a little wider.",
                "The strain doesn't announce itself — but its absence does.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to the near world. Come find the far one again in an hour or so.",
                "Done. Your eyes will remember this around minute ninety.",
            ),
        ),
    )

    // ------------------------------------------------------------ The Unfold

    val THE_UNFOLD = SessionScript(
        id = "unfold_v1",
        displayName = "The Unfold",
        arrivalByTrigger = mapOf(
            "STRETCH" to CopyPool(
                listOf(
                    "Long stretch of focus. Your neck kept the score.",
                    "Great run of work. Your shoulders have been up near your ears for most of it.",
                    "Deep focus folds you up. Let's unfold.",
                ),
            ),
            "RECOVERY_BREAK" to CopyPool(
                listOf(
                    "That was a marathon sit. Three minutes for the body that carried it.",
                    "Long haul. Time to pay the body back a little.",
                ),
            ),
        ),
        arrivalDefault = CopyPool(
            listOf(
                "Desk posture is a shape, not a sentence. Undo it.",
                "Sixty seconds of unfolding what the chair folded.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Drop your right ear toward your right shoulder. Don't pull — just let it hang.",
                        "Tilt your head right, ear to shoulder. Gravity does the work, not you.",
                    ),
                ),
                holdSec = 12,
                poseAsset = "pose_neck_right",
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Other side. Left ear down, shoulder heavy.",
                        "Now the left. Notice which side had more to say.",
                    ),
                ),
                holdSec = 12,
                poseAsset = "pose_neck_left",
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Roll both shoulders up to your ears… and let them fall. Twice more.",
                        "Shoulders up on the inhale… dropped on the exhale. Three of those.",
                    ),
                ),
                holdSec = 15,
                poseAsset = "pose_shoulder_roll",
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Unclench your jaw. Tongue off the roof of your mouth. Yes, really.",
                        "Let your jaw hang slightly open a moment. It's been working too.",
                    ),
                ),
                holdSec = 8,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Sit tall once — not forever, just to remember where tall is.",
                "That looseness has a shelf life. Come back for more later.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — shoulders stay down this round.",
                "Go. Your neck votes for a walk at some point today.",
            ),
        ),
    )

    // ------------------------------------------------------------ Ember

    val EMBER = SessionScript(
        id = "ember_v1",
        displayName = "Ember",
        arrivalByTrigger = mapOf(
            "WIND_DOWN" to CopyPool(
                listOf(
                    "Still up. No judgment — let's just make the next few minutes softer.",
                    "The feed doesn't end. You get to.",
                    "Late one. Let's dim things from the inside first.",
                ),
            ),
        ),
        arrivalDefault = CopyPool(
            listOf(
                "A slow one. Nothing to achieve here.",
                "Low light, long breaths. That's all this is.",
            ),
        ),
        guide = listOf(
            GuideStep.Breath(
                pattern = BreathPattern.EMBER,
                cycles = 10,
                cue = CopyPool(
                    listOf(
                        "In gently… and out longer, like a fire settling.",
                        "Breathe in low… let the exhale take its time.",
                    ),
                ),
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "The screen will fade now. You can be the one who doesn't bring it back.",
                        "Going dark. The next good thing is the pillow.",
                    ),
                ),
                dwellSec = 10,
                dimScreen = true, // the session's goal is its own abandonment
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Heavy eyelids are a feature tonight.",
                "That slower heartbeat is the whole point.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Phone down, face down. See you tomorrow.",
                "This is the good stopping point. Take it.",
            ),
        ),
    )

    // ------------------------------------------------------------ catalog

    private val ALL = listOf(THE_SIGH, HORIZON, THE_UNFOLD, EMBER)

    fun byId(id: String): SessionScript? = ALL.find { it.id == id }

    /**
     * Break kind (the session graph's typed arg) → default script. The future
     * session bandit replaces this table with per-user learned selection;
     * scripts are its arms, which is why [SessionScript.id] is stable.
     */
    fun forBreakKind(breakKind: String?): SessionScript = when (breakKind) {
        SessionDestination.KIND_STRETCH -> THE_UNFOLD
        SessionDestination.KIND_MEDITATE -> HORIZON
        SessionDestination.KIND_BREATHING -> THE_SIGH
        else -> THE_SIGH
    }

    /** Night context overrides the kind mapping — Ember owns the wind-down. */
    fun forBreak(breakKind: String?, senseTrigger: String?): SessionScript =
        if (senseTrigger == "WIND_DOWN") EMBER else forBreakKind(breakKind)
}
