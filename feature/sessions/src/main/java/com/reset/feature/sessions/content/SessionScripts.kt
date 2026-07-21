package com.reset.feature.sessions.content

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
        modality = Modality.CALM,
        evidence = Evidence(
            grade = 5,
            citation = "Balban, Bhatt et al., 2023, Cell Reports Medicine",
            finding = "Cyclic sighing (double inhale, long exhale) reduced respiratory rate " +
                "and improved mood more than mindfulness meditation or box breathing.",
        ),
        requirements = setOf(Requirement.AUDIO),
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
        modality = Modality.EYES,
        evidence = Evidence(
            grade = 3,
            citation = "American Academy of Ophthalmology, 20-20-20 rule",
            finding = "Shifting focus to something 20 feet away for 20 seconds every 20 " +
                "minutes of screen work reduces digital eye strain symptoms.",
        ),
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
        modality = Modality.STRETCH,
        evidence = Evidence(
            grade = 5,
            citation = "Galinsky, Swanson, Sauter, Hurrell & Schleifer, 2000, Ergonomics",
            finding = "Supplementary rest breaks with brief stretching reduced " +
                "musculoskeletal discomfort in sustained computer work.",
        ),
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
        modality = Modality.CALM,
        evidence = Evidence(
            grade = 4,
            citation = "Lehrer & Gevirtz, 2014, Frontiers in Psychology (HRV biofeedback review)",
            finding = "Slow-paced, exhale-weighted breathing increases heart rate " +
                "variability and parasympathetic activity, supporting pre-sleep wind-down.",
        ),
        requirements = setOf(Requirement.AUDIO),
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

    // ------------------------------------------------------------ Unclench

    val UNCLENCH = SessionScript(
        id = "unclench_v1",
        displayName = "Unclench",
        modality = Modality.CALM,
        evidence = Evidence(
            grade = 4,
            citation = "Manzoni, Pagnini, Castelnuovo & Molinari, 2008, BMC Psychiatry",
            finding = "A ten-year systematic review found progressive muscle relaxation " +
                "(deliberate clench-then-release) reliably reduces subjective tension and " +
                "anxious arousal in brief, single-session use.",
        ),
        arrivalDefault = CopyPool(
            listOf(
                "You're braced for something that already happened. Let go of it.",
                "Squeeze it all, on purpose, so you can actually drop it.",
                "The tension has a grip. Time to have the last word.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Make two tight fists. Hold… hold… and let them fall open.",
                        "Squeeze your fists as hard as you can. Now release, fingers loose.",
                    ),
                ),
                holdSec = 10,
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Scrunch your whole face — eyes, jaw, everything. Hold… and soften.",
                        "Clench your jaw and squeeze your eyes shut. Now let your face go slack.",
                    ),
                ),
                holdSec = 10,
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Shrug your shoulders up to your ears. Hold… and drop them heavy.",
                        "Pull your shoulders up tight. Hold, then let gravity have them.",
                    ),
                ),
                holdSec = 12,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "That looseness is what 'relaxed' actually feels like — not the absence of effort.",
                "Notice the difference. You just showed your body what off feels like.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — carrying less than you were.",
                "Go on. Re-clench slowly this time, if at all.",
            ),
        ),
    )

    // ------------------------------------------------------------ The Loop

    val THE_LOOP = SessionScript(
        id = "loop_v1",
        displayName = "The Loop",
        modality = Modality.MOVE,
        evidence = Evidence(
            grade = 5,
            citation = "Thayer, 1987, Journal of Personality and Social Psychology",
            finding = "A brief bout of self-paced walking measurably raised energetic arousal " +
                "and lowered tension versus sitting still, with effects lasting into the " +
                "following hour.",
        ),
        requirements = setOf(Requirement.MOVE_SPACE),
        arrivalDefault = CopyPool(
            listOf(
                "Restless is a request from your body, not a character flaw. Answer it.",
                "Sitting still is the wrong tool right now. Try moving in a small circle instead.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Find a loop — around the desk, down the hall and back. Walk it, unhurried.",
                        "Pace a small circuit. No destination, just the walking.",
                    ),
                ),
                holdSec = 40,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Notice your feet landing. That's the whole trick.",
                        "Feel where your weight moves with each step.",
                    ),
                ),
                dwellSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Legs did something. That counts.",
                "The fidget got somewhere to go. Feel that settle.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to the chair — it'll hold you better now.",
                "Go on. You bought yourself a calmer next hour.",
            ),
        ),
    )

    // ------------------------------------------------------------ Wrists & Hands

    val WRISTS_AND_HANDS = SessionScript(
        id = "wrists_hands_v1",
        displayName = "Wrists & Hands",
        modality = Modality.STRETCH,
        evidence = Evidence(
            grade = 3,
            citation = "Galinsky, Swanson, Sauter, Hurrell & Schleifer, 2000, Ergonomics",
            finding = "Supplementary rest breaks with brief stretching reduced " +
                "musculoskeletal discomfort in sustained computer work, including the " +
                "hands and wrists that carry the typing load.",
        ),
        arrivalDefault = CopyPool(
            listOf(
                "Your hands have been doing the same small motion for a while. Undo it.",
                "Typing is a thousand tiny clenches. Time to unclench them.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Circle both wrists slowly, five times each direction.",
                        "Loosen your wrists in slow circles — both ways.",
                    ),
                ),
                holdSec = 10,
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Press your palms together at chest height, fingers up. Lower gently for the stretch.",
                        "Prayer-hands at your chest, then press your palms down slowly.",
                    ),
                ),
                holdSec = 10,
                poseAsset = "pose_wrist_prayer",
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Spread your fingers wide, hold, then give them a little shake out.",
                        "Fan your fingers out fully. Hold a beat, then shake your hands loose.",
                    ),
                ),
                holdSec = 10,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Lighter grip, looser wrists. Notice it before the next click.",
                "That's what your hands feel like without the low hum of tension.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — a lighter touch on the keys this round.",
                "Go on. Your wrists vote for doing this again in an hour.",
            ),
        ),
    )

    // ------------------------------------------------------------ Fern

    val FERN = SessionScript(
        id = "fern_v1",
        displayName = "Fern",
        modality = Modality.AMBIENT,
        evidence = Evidence(
            grade = 4,
            citation = "Kaplan, 1995, Journal of Environmental Psychology",
            finding = "Attention Restoration Theory: brief, undemanding noticing of one's " +
                "surroundings lets directed attention recover from fatigue, independent of " +
                "any particular setting.",
        ),
        arrivalDefault = CopyPool(
            listOf(
                "No wrong way to do this one. Just look around for a second.",
                "The smallest possible reset — good for whenever nothing else fits.",
            ),
        ),
        guide = listOf(
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Find one green thing — a plant, a screen icon, anything. Just look at it.",
                        "Pick one small living or growing thing nearby. Rest your eyes there.",
                    ),
                ),
                dwellSec = 20,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Now notice one sound you weren't paying attention to.",
                        "Let one background sound come into focus, then fade again.",
                    ),
                ),
                dwellSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "That's it. Small counts.",
                "Nothing dramatic — just a few seconds of actually noticing.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it, whenever you're ready.",
                "Go on. That was enough.",
            ),
        ),
    )

    // ------------------------------------------------------------ The Climb

    val THE_CLIMB = SessionScript(
        id = "climb_v1",
        displayName = "The Climb",
        modality = Modality.MOVE,
        evidence = Evidence(
            grade = 4,
            citation = "Randolph & O'Connor, 2017, Physiology & Behavior",
            finding = "A brief bout of stair climbing increased self-reported energy more " +
                "than a standard dose of caffeine, with no crash afterward.",
        ),
        requirements = setOf(Requirement.STAIRS, Requirement.MOVE_SPACE),
        arrivalDefault = CopyPool(
            listOf(
                "Better than coffee, and it's already in the building.",
                "Empty tank? This refills it faster than sitting still ever will.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "One flight up, one flight down. Steady pace, not a sprint.",
                        "Take the stairs — up and back. Let your breathing pick up a little.",
                    ),
                ),
                holdSec = 60,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Feel your heart rate. That's real energy, not borrowed.",
                        "Notice the lift. No crash is coming for this one.",
                    ),
                ),
                dwellSec = 10,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "That's your own body's energy, not a stimulant's.",
                "Legs a little warm, head a lot clearer.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it, actually awake this time.",
                "Go on. That'll carry you further than a cup would have.",
            ),
        ),
    )

    // ------------------------------------------------------------ Step Outside

    val STEP_OUTSIDE = SessionScript(
        id = "step_outside_v1",
        displayName = "Step Outside",
        modality = Modality.AMBIENT,
        evidence = Evidence(
            grade = 3,
            citation = "Berman, Jonides & Kaplan, 2008, Psychological Science",
            finding = "A short walk through a natural setting improved attention and mood " +
                "more than an equivalent walk through an urban setting — the outdoor air " +
                "itself is doing real work.",
        ),
        requirements = setOf(Requirement.MOVE_SPACE),
        arrivalDefault = CopyPool(
            listOf(
                "Outside if you can get there fast. A window if you can't.",
                "Different air, different light — your nervous system notices the switch.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Step outside, or right up to a window. Let your eyes go to the farthest point.",
                        "Get outside if you can. Look up, not at a screen.",
                    ),
                ),
                holdSec = 45,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Feel the temperature on your skin. That's not indoor air.",
                        "Notice the light is different out here. Let it register.",
                    ),
                ),
                dwellSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Whatever that was, your head is quieter now.",
                "That's the fastest 'somewhere else' available to you.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back inside — bring some of that quiet with you.",
                "Go on. The outside will still be there next time.",
            ),
        ),
    )

    // ------------------------------------------------------------ Warmth

    val WARMTH = SessionScript(
        id = "warmth_v1",
        displayName = "Warmth",
        modality = Modality.MIND,
        evidence = Evidence(
            grade = 4,
            citation = "Neff, 2003, Self and Identity",
            finding = "Treating yourself with the same kindness you'd offer a friend — " +
                "self-compassion — is linked to lower anxiety and depression than either " +
                "self-criticism or simple self-esteem boosting.",
        ),
        requirements = setOf(Requirement.AUDIO),
        arrivalDefault = CopyPool(
            listOf(
                "Low days don't need fixing — just a hand on the shoulder. This one sits with you.",
                "Nothing to solve here. Just a minute of being kind to yourself.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Put a hand on your chest. Say, silently: this is a hard moment.",
                        "Hand over your heart. However you're doing is allowed right now.",
                    ),
                ),
                holdSec = 30,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "What would you say to a friend feeling this? Say it to yourself instead.",
                        "You'd be gentler with someone else right now. Try being that gentle with you.",
                    ),
                ),
                dwellSec = 20,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "That warmth was real, even if it felt small.",
                "You just practiced being on your own side.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it, a little less alone with it.",
                "Go on. That hand on your shoulder stays with you.",
            ),
        ),
    )

    // ------------------------------------------------------------ Three Good Things

    val THREE_GOOD_THINGS = SessionScript(
        id = "three_good_things_v1",
        displayName = "Three Good Things",
        modality = Modality.MIND,
        evidence = Evidence(
            grade = 4,
            citation = "Seligman, Steen, Park & Peterson, 2005, American Psychologist",
            finding = "Writing down three good things each day, and why they happened, " +
                "produced lasting increases in happiness and drops in depressive symptoms " +
                "in a randomized trial — one of positive psychology's most replicated results.",
        ),
        arrivalDefault = CopyPool(
            listOf(
                "Your attention has been on what's wrong. Let's borrow it back for a second.",
                "Not toxic positivity — just evidence you'd otherwise skip past.",
            ),
        ),
        guide = listOf(
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Name one small good thing from today. Anything — coffee counts.",
                        "One good thing, however tiny. What comes to mind first?",
                    ),
                ),
                dwellSec = 15,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Now a second one. Doesn't need to top the first.",
                        "Another good thing — no ranking required.",
                    ),
                ),
                dwellSec = 15,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "One more. Then notice — you had three, not zero.",
                        "A third. See how fast those were to find?",
                    ),
                ),
                dwellSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "The day had more in it than the hard part.",
                "That's three real things. They were there the whole time.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it, carrying the fuller picture.",
                "Go on. Tomorrow's three are already forming.",
            ),
        ),
    )

    // ------------------------------------------------------------ Mini Unpack

    val MINI_UNPACK = SessionScript(
        id = "mini_unpack_v1",
        displayName = "Mini Unpack",
        modality = Modality.MIND,
        evidence = Evidence(
            grade = 4,
            citation = "Hayes, Strosahl & Wilson, 1999, Acceptance and Commitment Therapy",
            finding = "Naming a thought as 'just a thought' (cognitive defusion) loosens its " +
                "grip faster than trying to argue it away — the plausible mechanism behind " +
                "brief, in-the-moment unhooking practices.",
        ),
        arrivalDefault = CopyPool(
            listOf(
                "One thought's been running on a loop. Let's just name it, not solve it.",
                "You don't have to finish the thought right now. Just set it down for a second.",
            ),
        ),
        guide = listOf(
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Say the thought to yourself, but start with 'I'm noticing I'm thinking…'",
                        "Name it plainly: 'I'm having the thought that…' Just that framing.",
                    ),
                ),
                dwellSec = 20,
            ),
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Picture setting it on a shelf for later. It'll still be there if you need it.",
                        "Put it down for now — not away forever, just for this minute.",
                    ),
                ),
                holdSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "It's a thought, not a fact, not an order. Notice the difference.",
                "Still there if it matters. Lighter either way.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — the thought can wait its turn.",
                "Go on. You don't owe it an answer this second.",
            ),
        ),
    )

    // ------------------------------------------------------------ The Settle

    val THE_SETTLE = SessionScript(
        id = "settle_v1",
        displayName = "The Settle",
        modality = Modality.MIND,
        evidence = Evidence(
            grade = 4,
            citation = "Zeidan, Johnson, Diamond, David & Goolkasian, 2010, Consciousness and Cognition",
            finding = "Brief mindfulness training — as little as a few short sessions — " +
                "improved sustained attention and reduced mental fatigue versus an " +
                "active control.",
        ),
        requirements = setOf(Requirement.AUDIO),
        arrivalByTrigger = mapOf(
            "STRETCH" to CopyPool(
                listOf(
                    "Seven tabs open in your head. Let's close a few.",
                    "That was a lot of switching. One thing now.",
                ),
            ),
        ),
        arrivalDefault = CopyPool(
            listOf(
                "Scattered is a state, not a personality. This settles it.",
                "Your attention's been sprinting. Time to walk it back.",
            ),
        ),
        guide = listOf(
            GuideStep.Breath(
                pattern = BreathPattern(inhaleMs = 4_000, exhaleMs = 4_000),
                cycles = 4,
                cue = CopyPool(
                    listOf(
                        "In for four… out for four. Nothing to get right.",
                        "Slow in… slow out. Just ride the four counts.",
                    ),
                ),
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Name the one thing you'll do next. Just the one.",
                        "Pick the single next thing. Everything else can wait its turn.",
                    ),
                ),
                dwellSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "One thread, not twelve. That's the whole shift.",
                "Notice how much quieter one thing sounds.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — the one thing, first.",
                "Go on. The rest of the list survives being ignored a while longer.",
            ),
        ),
    )

    // ------------------------------------------------------------ The Sweep

    val THE_SWEEP = SessionScript(
        id = "sweep_v1",
        displayName = "The Sweep",
        modality = Modality.MIND,
        evidence = Evidence(
            grade = 4,
            citation = "Ong & Sholtes, 2010, Journal of Clinical Psychology",
            finding = "A mindfulness-based body-scan practice, used as a pre-sleep " +
                "wind-down, reduced the racing-mind arousal that keeps people from " +
                "falling asleep.",
        ),
        requirements = setOf(Requirement.AUDIO),
        arrivalByTrigger = mapOf(
            "WIND_DOWN" to CopyPool(
                listOf(
                    "Still up. Let's talk your body out of standing guard.",
                    "Late one. Let's sweep the tension out before the lights go off.",
                ),
            ),
        ),
        arrivalDefault = CopyPool(
            listOf(
                "A slow pass through the body, head to feet. Nowhere to be.",
                "Nothing to achieve — just noticing, one place at a time.",
            ),
        ),
        guide = listOf(
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Forehead… jaw… shoulders. Let each one soften as you name it.",
                        "Move slowly: forehead, jaw, shoulders. Unclench each in turn.",
                    ),
                ),
                dwellSec = 30,
                dimScreen = true,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Now your hands, your stomach, your legs. Heavy is the goal.",
                        "Hands, stomach, legs — let them get heavy against whatever's under you.",
                    ),
                ),
                dwellSec = 25,
                dimScreen = true,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "Heavier all over. That's the body agreeing to rest.",
                "Nothing left standing guard tonight.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Phone down, face down. That's the whole rest of the plan.",
                "This is the good stopping point. Take it.",
            ),
        ),
    )

    // ------------------------------------------------------------ The Plunge

    val THE_PLUNGE = SessionScript(
        id = "plunge_v1",
        displayName = "The Plunge",
        modality = Modality.CALM,
        evidence = Evidence(
            grade = 3,
            citation = "Panneton, 2013, Physiology",
            finding = "Cold water on the face triggers the mammalian dive reflex — a sharp, " +
                "involuntary jump in vagal (parasympathetic) activity that slows the heart " +
                "and interrupts a flooded, overwhelmed state fast.",
        ),
        requirements = setOf(Requirement.WATER_ACCESS),
        // A jarring, acute reset — not for repeated same-hour use, and not a wind-down move
        // despite being CALM-modality: night still vetoes it explicitly.
        minHoursBetween = 4,
        nightVetoed = true,
        arrivalDefault = CopyPool(
            listOf(
                "Too much, too fast? Your body has an override switch. Use it.",
                "This isn't gentle on purpose — overwhelmed needs a hard reset, not a soft one.",
            ),
        ),
        guide = listOf(
            GuideStep.Move(
                instruction = CopyPool(
                    listOf(
                        "Cold water on your wrists and face. Let it actually be cold.",
                        "Splash cold water on your face, or hold your wrists under the tap.",
                    ),
                ),
                holdSec = 20,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Feel your heart rate drop. That's not in your head — that's real.",
                        "Notice the sudden quiet. Your nervous system just changed gears.",
                    ),
                ),
                dwellSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "The flood just broke. That was your body's own circuit breaker.",
                "Overwhelmed doesn't win every time. That's proof.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — from a lower floor than you were on.",
                "Go on. One thing at a time is possible again.",
            ),
        ),
    )

    // ------------------------------------------------------------ Reach Out

    val REACH_OUT = SessionScript(
        id = "reach_out_v1",
        displayName = "Reach Out",
        modality = Modality.CONNECT,
        evidence = Evidence(
            grade = 4,
            citation = "Sandstrom & Dunn, 2014, Personality and Social Psychology Bulletin",
            finding = "Brief positive contact with another person — even a short message — " +
                "measurably lifted mood and sense of belonging, independent of how close " +
                "the relationship was.",
        ),
        requirements = setOf(Requirement.HAS_CLOSE_PERSON),
        arrivalDefault = CopyPool(
            listOf(
                "Disconnected fixes itself faster with someone else in the loop.",
                "Not a favor to ask — just a hello. Warmth going out, not validation coming in.",
            ),
        ),
        guide = listOf(
            GuideStep.LaunchAction(
                prompt = CopyPool(
                    listOf(
                        "Send one honest line to someone who'd want to hear from you.",
                        "A quick 'thinking of you' — nothing more is required.",
                    ),
                ),
                actionKind = ActionKind.MESSAGE,
                dwellSec = 20,
            ),
            GuideStep.Prompt(
                text = CopyPool(
                    listOf(
                        "Whatever they say back, you already did the part that counts.",
                        "This was for you two, not for a reply. Notice that's already true.",
                    ),
                ),
                dwellSec = 15,
            ),
        ),
        landingClose = CopyPool(
            listOf(
                "You're a little less alone than four minutes ago.",
                "That reached somewhere. It usually does.",
            ),
        ),
        landingBridge = CopyPool(
            listOf(
                "Back to it — that thread stays open now.",
                "Go on. They'll get to it, and so will you.",
            ),
        ),
    )

    // ------------------------------------------------------------ catalog

    private val ALL = listOf(
        THE_SIGH, HORIZON, THE_UNFOLD, EMBER,
        UNCLENCH, THE_LOOP, WRISTS_AND_HANDS, FERN, THE_CLIMB, STEP_OUTSIDE,
        WARMTH, THREE_GOOD_THINGS, MINI_UNPACK, THE_SETTLE, THE_SWEEP, THE_PLUNGE, REACH_OUT,
    )

    fun byId(id: String): SessionScript? = ALL.find { it.id == id }

    /**
     * Sense trigger (a `BreakType.name`) → default script. The future session bandit
     * replaces this table with per-user learned selection; scripts are its arms, which is
     * why [SessionScript.id] is stable. Night (WIND_DOWN) always owns Ember regardless of
     * what else the trigger names.
     */
    fun forBreak(senseTrigger: String?): SessionScript = when (senseTrigger) {
        "WIND_DOWN" -> EMBER
        "STRETCH", "RECOVERY_BREAK" -> THE_UNFOLD
        "EYE_BREAK" -> HORIZON
        else -> THE_SIGH
    }
}
