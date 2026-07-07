package com.reset.repository.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HomeRepositoryImplTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Deterministic clock the tests can advance to cross day boundaries. */
    private class FakeTimeProvider(
        var epochDay: Long = 1_000L,
        var dayIndex: Int = 0,
    ) : TimeProvider {
        override fun todayEpochDay() = epochDay
        override fun dayOfWeekIndex() = dayIndex
        override fun hourOfDay() = 10
        override fun dayOfMonth() = 1
    }

    private fun TestScope.repository(
        time: FakeTimeProvider = FakeTimeProvider(),
    ): HomeRepositoryImpl {
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "afk_${System.nanoTime()}.preferences_pb")
        }
        return HomeRepositoryImpl(store, time, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun `exposes defaults when nothing persisted`() = runTest {
        val repo = repository()

        val prefs = repo.preferences.first()
        assertEquals(HomePreferences.DEFAULT_DURATION_MIN, prefs.durationMin)
        assertTrue(prefs.remindersEnabled)

        val stats = repo.stats.first()
        assertEquals(0, stats.sessions)
        assertTrue(stats.isFirstSit)

        assertTrue(repo.presets.first().isEmpty())
    }

    @Test
    fun `persists duration and reminder preference`() = runTest {
        val repo = repository()

        repo.setDuration(3)
        repo.setRemindersEnabled(false)

        val prefs = repo.preferences.first()
        assertEquals(3, prefs.durationMin)
        assertFalse(prefs.remindersEnabled)
    }

    @Test
    fun `records focus sessions into counts and today's weekly bucket`() = runTest {
        val time = FakeTimeProvider(epochDay = 1_000L, dayIndex = 2)
        val repo = repository(time)

        repo.recordFocusSession(25)
        repo.recordFocusSession(10)

        val stats = repo.stats.first()
        assertEquals(2, stats.sessions)
        assertEquals(35, stats.totalMin)

        val weekly = repo.weeklyFocus.first()
        assertEquals(35, weekly.minutesPerDay[2])
        assertEquals(2, weekly.todayIndex)
    }

    @Test
    fun `weekly buckets from a previous week read as zero`() = runTest {
        val time = FakeTimeProvider(epochDay = 1_000L, dayIndex = 3)
        val repo = repository(time)

        repo.recordFocusSession(30)
        // Same weekday next week: the stale bucket must not leak through.
        time.epochDay += 7

        assertEquals(0, repo.weeklyFocus.first().minutesPerDay[3])
    }

    @Test
    fun `consecutive-day activity grows the streak and a gap resets it`() = runTest {
        val time = FakeTimeProvider(epochDay = 1_000L)
        val repo = repository(time)

        repo.recordBreak()
        assertEquals(1, repo.stats.first().streak)

        time.epochDay = 1_001L
        repo.recordBreak()
        assertEquals(2, repo.stats.first().streak)

        // Same-day activity keeps the streak.
        repo.recordFocusSession(5)
        assertEquals(2, repo.stats.first().streak)

        time.epochDay = 1_005L
        repo.recordBreak()
        assertEquals(1, repo.stats.first().streak)
    }

    @Test
    fun `saves presets newest-last, replaces by name and keeps only the cap`() = runTest {
        val repo = repository()
        fun preset(name: String, minutes: Int = 15) = SessionPreset(
            name = name,
            durationMin = minutes,
            soundKey = "rain",
            paceSec = 4,
            intentKey = "focus",
            remindKey = "off",
        )

        repo.savePreset(preset("morning", minutes = 10))
        repo.savePreset(preset("evening"))
        repo.savePreset(preset("morning", minutes = 20))

        val replaced = repo.presets.first()
        assertEquals(listOf("evening", "morning"), replaced.map { it.name })
        assertEquals(20, replaced.last().durationMin)

        repeat(SessionPreset.MAX_PRESETS) { repo.savePreset(preset("extra$it")) }
        val capped = repo.presets.first()
        assertEquals(SessionPreset.MAX_PRESETS, capped.size)
        assertEquals("extra0", capped.first().name)
    }
}
