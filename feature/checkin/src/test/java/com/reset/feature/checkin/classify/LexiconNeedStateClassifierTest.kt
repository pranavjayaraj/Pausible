package com.reset.feature.checkin.classify

import com.reset.feature.checkin.classify.NeedStateClassifier.Result
import com.reset.model.domain.checkin.NeedState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LexiconNeedStateClassifierTest {

    private val classifier = LexiconNeedStateClassifier()

    private suspend fun need(text: String): NeedState =
        (classifier.classify(text) as Result.Confident).needState

    // ------------------------------------------------------------ clear cases

    @Test
    fun `clear single-need utterances map to their state`() = runTest {
        assertEquals(NeedState.BODY_TENSION, need("my neck is killing me"))
        assertEquals(NeedState.WOUND_UP, need("I'm so stressed right now"))
        assertEquals(NeedState.DRAINED, need("completely exhausted, no energy left"))
        assertEquals(NeedState.EYE_STRAIN, need("my eyes are so tired and blurry"))
        assertEquals(NeedState.HAND_STRAIN, need("my wrists ache from typing"))
        assertEquals(NeedState.STUCK_ON_A_THOUGHT, need("I can't stop thinking about it"))
        assertEquals(NeedState.LOW_MOOD, need("just feeling really low today"))
        assertEquals(NeedState.DISCONNECTED, need("I feel so lonely lately"))
        assertEquals(NeedState.OVERWHELMED, need("everything is too much, I'm drowning"))
    }

    @Test
    fun `anger maps to wound-up`() = runTest {
        assertEquals(NeedState.WOUND_UP, need("I am a little angry"))
        assertEquals(NeedState.WOUND_UP, need("so frustrated right now"))
        assertEquals(NeedState.WOUND_UP, need("this is really irritating"))
    }

    @Test
    fun `mad-like substrings do not false-positive`() = runTest {
        // "made" must not trigger anger via a naive "mad" substring.
        assertEquals(Result.NoMatch, classifier.classify("I made a sandwich"))
    }

    @Test
    fun `boredom maps to restless`() = runTest {
        assertEquals(NeedState.RESTLESS, need("bored"))
        assertEquals(NeedState.RESTLESS, need("so bored right now"))
        assertEquals(NeedState.RESTLESS, need("this is boring"))
        // "before" must not false-match a "bore" substring.
        assertEquals(Result.NoMatch, classifier.classify("the meeting before this one"))
    }

    // ------------------------------------------------------------ expanded vocabulary sweep

    @Test
    fun `expanded emotion vocabulary maps correctly`() = runTest {
        // WOUND_UP anger/stress family
        assertEquals(NeedState.WOUND_UP, need("feeling really grumpy"))
        assertEquals(NeedState.WOUND_UP, need("so irritable today"))
        assertEquals(NeedState.WOUND_UP, need("I'm agitated"))
        assertEquals(NeedState.WOUND_UP, need("uptight and edgy"))
        assertEquals(NeedState.WOUND_UP, need("completely fed up"))
        // DRAINED
        assertEquals(NeedState.DRAINED, need("so much fatigue"))
        assertEquals(NeedState.DRAINED, need("weary and drowsy"))
        assertEquals(NeedState.DRAINED, need("total burnout"))
        // LOW_MOOD
        assertEquals(NeedState.LOW_MOOD, need("feeling depressed"))
        assertEquals(NeedState.LOW_MOOD, need("miserable and gloomy"))
        assertEquals(NeedState.LOW_MOOD, need("I feel empty inside"))
        // SCATTERED
        assertEquals(NeedState.SCATTERED, need("totally spaced out"))
        assertEquals(NeedState.SCATTERED, need("my brain is mush, zoned out"))
        // STUCK
        assertEquals(NeedState.STUCK_ON_A_THOUGHT, need("keep thinking about it, brooding"))
        assertEquals(NeedState.STUCK_ON_A_THOUGHT, need("so preoccupied"))
        // OVERWHELMED
        assertEquals(NeedState.OVERWHELMED, need("maxed out, at breaking point"))
        assertEquals(NeedState.OVERWHELMED, need("snowed under"))
        assertEquals(NeedState.OVERWHELMED, need("I am feeling overburdened"))
        assertEquals(NeedState.OVERWHELMED, need("so weighed down, too much on my plate"))
        // DISCONNECTED
        assertEquals(NeedState.DISCONNECTED, need("I feel invisible and cut off"))
        assertEquals(NeedState.DISCONNECTED, need("nobody around, so withdrawn"))
        // CANT_WIND_DOWN
        assertEquals(NeedState.CANT_WIND_DOWN, need("can't relax, wide awake, tossing"))
        assertEquals(NeedState.CANT_WIND_DOWN, need("can't unwind"))
        // RESTLESS
        assertEquals(NeedState.RESTLESS, need("cabin fever, pent up"))
        // BODY_TENSION / HAND / EYE additions
        assertEquals(NeedState.BODY_TENSION, need("shoulders all clenched and throbbing"))
        assertEquals(NeedState.HAND_STRAIN, need("my thumb and knuckles hurt from the keyboard"))
        assertEquals(NeedState.EYE_STRAIN, need("sore eyes and a headache from the screen"))
    }

    // ------------------------------------------------------------ text-only needs

    @Test
    fun `the five text-only needs are reachable from text alone`() = runTest {
        // No chip produces these (see ChipIdTest) — the lexicon is their only Tier-1 route.
        assertEquals(NeedState.CONFUSED_LOST, need("I have no idea what I'm doing here"))
        assertEquals(NeedState.CONFUSED_LOST, need("completely blocked and going in circles"))
        assertEquals(NeedState.TASK_PARALYSIS, need("I can't start this thing"))
        assertEquals(NeedState.TASK_PARALYSIS, need("I keep putting it off"))
        assertEquals(NeedState.SENSORY_OVERLOAD, need("this office is so loud I can't hear myself think"))
        assertEquals(NeedState.SENSORY_OVERLOAD, need("too many notifications, completely overstimulated"))
        assertEquals(NeedState.BIOLOGICAL_DEPLETION, need("I forgot to eat lunch"))
        assertEquals(NeedState.BIOLOGICAL_DEPLETION, need("starving and dehydrated"))
        assertEquals(NeedState.ACCOMPLISHED_FLOW, need("in the zone, crushing it today"))
        assertEquals(NeedState.ACCOMPLISHED_FLOW, need("really productive day, I'm proud of that"))
    }

    @Test
    fun `the positive need does not swallow ordinary work words`() = runTest {
        // "flow" is a phrase term ("in flow"/"flow state") for exactly this reason: the bare
        // token also lives inside workflow/overflow/flowed. Asserted as "not this need" rather
        // than NoMatch because those words separately collide with LOW_MOOD's "low" substring
        // — a pre-existing scorer issue this need must not add to.
        for (text in listOf("my workflow is broken", "the deploy flowed through", "the buffer overflowed")) {
            val result = classifier.classify(text)
            assertTrue(
                "'$text' should never read as ACCOMPLISHED_FLOW, was $result",
                result !is Result.Confident || result.needState != NeedState.ACCOMPLISHED_FLOW,
            )
        }
    }

    @Test
    fun `deliberately-avoided substrings never false-positive`() = runTest {
        // Each right-hand word CONTAINS an emotion term we chose NOT to add for exactly
        // this reason. If someone re-adds the bare root, one of these flips and fails.
        assertEquals(Result.NoMatch, classifier.classify("it's frigid outside"))        // rigid
        assertEquals(Result.NoMatch, classifier.classify("give me a phone number"))     // numb
        assertEquals(Result.NoMatch, classifier.classify("please present the slides"))  // resent
        assertEquals(Result.NoMatch, classifier.classify("walking across the street"))  // cross
        assertEquals(Result.NoMatch, classifier.classify("a crystal glass"))            // cry
        assertEquals(Result.NoMatch, classifier.classify("a dozen eggs"))               // doze
    }

    @Test
    fun `curated roots catch inflections`() = runTest {
        // "typ" → typing, "stress" → stressed, "drain" → drained
        assertEquals(NeedState.HAND_STRAIN, need("been typing all day"))
        assertEquals(NeedState.WOUND_UP, need("work is stressful"))
        assertEquals(NeedState.DRAINED, need("feeling drained"))
    }

    @Test
    fun `metaphor caught by medium terms`() = runTest {
        assertEquals(NeedState.SCATTERED, need("my brain is mush"))
    }

    // ------------------------------------------------------------ negation & intensifier

    @Test
    fun `negation suppresses the negated term, another need still wins`() = runTest {
        // "not stressed" suppressed → neck carries it
        assertEquals(NeedState.BODY_TENSION, need("not stressed but my neck hurts"))
    }

    @Test
    fun `idiomatic cant is not treated as negation`() = runTest {
        // "cant focus" must be SCATTERED, not a negated focus
        assertEquals(NeedState.SCATTERED, need("i cant focus at all"))
        assertEquals(NeedState.DRAINED, need("i have no energy"))
    }

    // ------------------------------------------------------------ ambiguity

    @Test
    fun `two competing needs return ambiguous`() = runTest {
        // No intensifier on either side → a genuine tie the follow-up must resolve.
        val r = classifier.classify("stressed and wiped out")
        assertTrue(r is Result.Ambiguous)
        r as Result.Ambiguous
        assertEquals(setOf(NeedState.WOUND_UP, NeedState.DRAINED), setOf(r.top, r.second))
    }

    // ------------------------------------------------------------ escapes

    @Test
    fun `out-of-scope complaint is NoMatch, never a forced guess`() = runTest {
        assertEquals(Result.NoMatch, classifier.classify("my stomach hurts"))
        assertEquals(Result.NoMatch, classifier.classify("what's the weather today"))
        assertEquals(Result.NoMatch, classifier.classify("asdfghjkl"))
        assertEquals(Result.NoMatch, classifier.classify(""))
    }

    @Test
    fun `crisis language short-circuits to Crisis, before any need scoring`() = runTest {
        assertEquals(Result.Crisis, classifier.classify("I want to disappear"))
        assertEquals(Result.Crisis, classifier.classify("thinking about ending it all"))
        // Even alongside a scoreable need, crisis wins.
        assertEquals(Result.Crisis, classifier.classify("so stressed I want to die"))
    }
}
