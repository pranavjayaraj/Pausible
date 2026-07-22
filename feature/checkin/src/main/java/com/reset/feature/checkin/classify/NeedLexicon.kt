package com.reset.feature.checkin.classify

import com.reset.model.domain.checkin.NeedState

/**
 * The classifier's tunable data: per-need weighted term sets + the crisis set + the small
 * function words (negation, intensifiers, contractions) the scorer needs. Kept as plain
 * Kotlin so it's trivially unit-testable and editable; a localized asset/JSON pack can
 * replace it later without touching [LexiconNeedStateClassifier].
 *
 * Terms are matched against normalized, stemmed tokens/bigrams, so store them stemmed-ish
 * and lowercase (e.g. "ache" catches "aching"/"aches" after the light stemmer). Multi-word
 * phrases are matched as adjacent-token bigrams.
 */
internal object NeedLexicon {

    const val STRONG = 1.0
    const val MEDIUM = 0.6

    /** Weighted terms per need. STRONG = names the state directly; MEDIUM = related/adjacent. */
    val TERMS: Map<NeedState, Map<String, Double>> = mapOf(
        NeedState.BODY_TENSION to mapOf(
            "neck" to STRONG, "shoulder" to STRONG, "back" to STRONG, "stiff" to STRONG,
            "sore" to MEDIUM, "ache" to MEDIUM, "achy" to MEDIUM, "knot" to MEDIUM,
            "tight" to MEDIUM, "tens" to MEDIUM, "crick" to MEDIUM, "hunch" to MEDIUM,
            "spasm" to MEDIUM, "clench" to MEDIUM, "pinch" to MEDIUM, "throb" to MEDIUM,
            "seized up" to MEDIUM, "stiffness" to STRONG, "posture" to MEDIUM,
        ),
        NeedState.HAND_STRAIN to mapOf(
            "wrist" to STRONG, "hand" to STRONG, "forearm" to STRONG, "finger" to STRONG,
            "typ" to MEDIUM, "cramp" to MEDIUM, "carpal" to MEDIUM, "mouse" to MEDIUM,
            "keyboard" to MEDIUM, "thumb" to MEDIUM, "knuckle" to MEDIUM, "tendon" to MEDIUM,
        ),
        NeedState.EYE_STRAIN to mapOf(
            "eye" to STRONG, "blurry" to STRONG, "blur" to STRONG, "screen headache" to STRONG,
            "dry eye" to STRONG, "squint" to MEDIUM, "strain" to MEDIUM, "vision" to MEDIUM,
            "eyestrain" to STRONG, "sore eyes" to STRONG, "tired eyes" to STRONG,
            "watery" to MEDIUM, "glare" to MEDIUM, "twitch" to MEDIUM, "headache" to MEDIUM,
        ),
        NeedState.WOUND_UP to mapOf(
            "stress" to STRONG, "anxious" to STRONG, "anxiety" to STRONG, "tense" to STRONG,
            "angry" to STRONG, "anger" to STRONG, "frustrat" to STRONG, "irritat" to STRONG,
            "wound up" to STRONG, "on edge" to STRONG, "frazzled" to MEDIUM, "panicky" to MEDIUM,
            "nervous" to MEDIUM, "worked up" to MEDIUM, "keyed up" to MEDIUM, "racing" to MEDIUM,
            "annoyed" to MEDIUM, "fuming" to MEDIUM, "furious" to MEDIUM, "livid" to MEDIUM,
            "fed up" to MEDIUM, "wound-up" to STRONG, "grumpy" to MEDIUM, "cranky" to MEDIUM,
            "irritable" to STRONG, "snappy" to MEDIUM, "agitat" to STRONG, "edgy" to MEDIUM,
            "uptight" to MEDIUM, "peeved" to MEDIUM, "vexed" to MEDIUM, "riled" to MEDIUM,
            "seething" to MEDIUM, "pissed" to MEDIUM, "overwrought" to MEDIUM, "jittery" to MEDIUM,
            "het up" to MEDIUM, "high strung" to MEDIUM, "wound tight" to STRONG,
        ),
        NeedState.SCATTERED to mapOf(
            "focus" to STRONG, "distract" to STRONG, "scattered" to STRONG, "concentrate" to STRONG,
            "foggy" to MEDIUM, "fog" to MEDIUM, "all over the place" to MEDIUM, "jump" to MEDIUM,
            "mush" to MEDIUM, "cant think" to MEDIUM, "unfocused" to STRONG,
            "spacey" to MEDIUM, "spaced out" to STRONG, "zoned" to MEDIUM, "zoning" to MEDIUM,
            "wander" to MEDIUM, "flighty" to MEDIUM, "scatty" to MEDIUM, "brain fog" to STRONG,
        ),
        NeedState.DRAINED to mapOf(
            "tired" to STRONG, "exhaust" to STRONG, "sleepy" to STRONG, "drain" to STRONG,
            "no energy" to STRONG, "wiped" to STRONG, "sluggish" to MEDIUM, "flat" to MEDIUM,
            "worn out" to MEDIUM, "knackered" to MEDIUM, "burnt out" to MEDIUM, "lethargic" to MEDIUM,
            "fatigu" to STRONG, "weary" to STRONG, "drowsy" to MEDIUM, "depleted" to MEDIUM,
            "zonked" to MEDIUM, "burnout" to MEDIUM, "no fuel" to MEDIUM, "running on empty" to MEDIUM,
        ),
        NeedState.LOW_MOOD to mapOf(
            "low" to STRONG, "sad" to STRONG, "down" to STRONG, "blue" to STRONG,
            "unhappy" to STRONG, "meh" to MEDIUM, "blah" to MEDIUM, "discouraged" to MEDIUM,
            "grey" to MEDIUM, "gray" to MEDIUM, "glum" to MEDIUM, "deflated" to MEDIUM,
            "depress" to STRONG, "miserable" to STRONG, "gloomy" to STRONG, "gloom" to MEDIUM,
            "downhearted" to STRONG, "tearful" to STRONG, "in tears" to STRONG, "crying" to MEDIUM,
            "hollow" to MEDIUM, "empty inside" to STRONG, "melancholy" to MEDIUM, "dispirited" to MEDIUM,
            "heavy hearted" to MEDIUM, "sorrow" to MEDIUM, "wretched" to MEDIUM,
        ),
        NeedState.STUCK_ON_A_THOUGHT to mapOf(
            "overthink" to STRONG, "ruminat" to STRONG, "cant stop thinking" to STRONG,
            "spiral" to STRONG, "replay" to MEDIUM, "obsess" to MEDIUM, "stuck on" to MEDIUM,
            "worry" to MEDIUM, "worried about" to MEDIUM, "in my head" to MEDIUM,
            "brooding" to MEDIUM, "dwelling" to MEDIUM, "fixat" to STRONG, "circling" to MEDIUM,
            "wont stop" to MEDIUM, "keep thinking" to STRONG, "preoccupied" to MEDIUM,
            "cant shake" to MEDIUM, "haunt" to MEDIUM,
        ),
        NeedState.OVERWHELMED to mapOf(
            "overwhelm" to STRONG, "too much" to STRONG, "drowning" to STRONG, "cant cope" to STRONG,
            "crushed" to MEDIUM, "buried" to MEDIUM, "falling apart" to MEDIUM, "swamped" to MEDIUM,
            "overload" to STRONG, "maxed out" to STRONG, "cant handle" to STRONG, "breaking point" to STRONG,
            "snowed under" to MEDIUM, "stretched thin" to MEDIUM, "no bandwidth" to MEDIUM,
        ),
        NeedState.DISCONNECTED to mapOf(
            "lonely" to STRONG, "alone" to STRONG, "isolat" to STRONG, "disconnect" to STRONG,
            "out of touch" to MEDIUM, "no one" to MEDIUM, "left out" to MEDIUM, "distant" to MEDIUM,
            "cut off" to MEDIUM, "nobody" to MEDIUM, "invisible" to MEDIUM, "unseen" to MEDIUM,
            "withdrawn" to MEDIUM, "detach" to MEDIUM, "adrift" to MEDIUM, "abandoned" to MEDIUM,
            "friendless" to STRONG, "by myself" to MEDIUM, "reclusive" to MEDIUM,
        ),
        NeedState.CANT_WIND_DOWN to mapOf(
            "cant sleep" to STRONG, "cant switch off" to STRONG, "wired" to STRONG, "insomnia" to STRONG,
            "still scrolling" to MEDIUM, "wide awake" to MEDIUM, "cant settle" to MEDIUM,
            "cant relax" to STRONG, "cant unwind" to STRONG, "buzzing" to MEDIUM, "tossing" to MEDIUM,
            "nod off" to MEDIUM, "overtired" to MEDIUM, "unwind" to MEDIUM,
        ),
        NeedState.RESTLESS to mapOf(
            "restless" to STRONG, "cooped up" to STRONG, "stir crazy" to STRONG, "fidget" to STRONG,
            "bored" to STRONG, "boredom" to STRONG, "boring" to STRONG, "understimulated" to STRONG,
            "sat too long" to MEDIUM, "been sitting" to MEDIUM, "antsy" to MEDIUM, "cant sit still" to MEDIUM,
            "nothing to do" to MEDIUM, "twiddling" to MEDIUM, "pent up" to MEDIUM, "cabin fever" to STRONG,
            "stagnant" to MEDIUM, "stuck indoors" to MEDIUM,
        ),
    )

    /** High-recall crisis set — checked before anything else. Coarse on purpose. */
    val CRISIS: Set<String> = setOf(
        "kill myself", "suicid", "end it all", "ending it all", "want to die", "wanna die", "end my life",
        "hurt myself", "harm myself", "self harm", "cut myself", "no reason to live",
        "better off dead", "dont want to be here", "want to disappear", "cant go on",
    )

    /**
     * Words that suppress the terms in their short forward window. Deliberately excludes
     * "no", "cant", "dont" — in this domain those are idiomatic parts of the complaint
     * ("no energy", "cant focus", "cant sleep"), not negators, and those idioms are already
     * captured as phrase terms. Only clean negators remain, so "not stressed" / "not tired"
     * suppress correctly without breaking the "cant X" states.
     */
    val NEGATIONS: Set<String> = setOf("not", "never", "isnt", "arent", "without")

    /** Words that amplify the term immediately after them. */
    val INTENSIFIERS: Set<String> = setOf(
        "really", "so", "very", "super", "extremely", "totally", "absolutely",
        "incredibly", "insanely", "ridiculously", "seriously", "genuinely",
    )
    const val INTENSIFIER_FACTOR = 1.5

    /** How far a negation/intensifier reaches forward (in tokens). */
    const val MODIFIER_WINDOW = 3
}
