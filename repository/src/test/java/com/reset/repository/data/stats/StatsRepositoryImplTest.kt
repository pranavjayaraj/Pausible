package com.reset.repository.data.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.reset.model.domain.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StatsRepositoryImplTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Deterministic clock the tests can advance to cross day boundaries. */
    private class FakeTimeProvider(
        var epochDay: Long = 1_000L,
        var dayIndex: Int = 0,
    ) : TimeProvider {
        override fun nowMillis() = epochDay * 86_400_000L
        override fun todayEpochDay() = epochDay
        override fun dayOfWeekIndex() = dayIndex
        override fun hourOfDay() = 10
        override fun dayOfMonth() = 1
    }

    private fun TestScope.repository(
        time: FakeTimeProvider = FakeTimeProvider(),
    ): StatsRepositoryImpl {
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "afk_${System.nanoTime()}.preferences_pb")
        }
        return StatsRepositoryImpl(store, time)
    }

    @Test
    fun `exposes defaults when nothing persisted`() = runTest {
        val stats = repository().stats.first()

        assertEquals(0, stats.sessions)
        assertTrue(stats.isFirstSit)
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
    fun `today's break count resets at midnight, unlike the all-time total`() = runTest {
        val time = FakeTimeProvider(epochDay = 1_000L)
        val repo = repository(time)

        repo.recordBreak()
        repo.recordBreak()
        assertEquals(2, repo.todayBreaksTaken.first())
        assertEquals(2, repo.stats.first().breaksTaken)

        time.epochDay = 1_001L
        assertEquals(0, repo.todayBreaksTaken.first())
        assertEquals(2, repo.stats.first().breaksTaken)

        repo.recordBreak()
        assertEquals(1, repo.todayBreaksTaken.first())
        assertEquals(3, repo.stats.first().breaksTaken)
    }
}
