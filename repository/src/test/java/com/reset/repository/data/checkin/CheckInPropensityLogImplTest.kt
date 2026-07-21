package com.reset.repository.data.checkin

import com.reset.model.domain.checkin.GateOutcome
import com.reset.model.domain.checkin.GateReason
import com.reset.model.domain.checkin.InputMethod
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.SessionSelection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInPropensityLogImplTest {

    private val dao = FakeCheckInPropensityDao()
    private val log = CheckInPropensityLogImpl(dao)

    private val selection = SessionSelection(
        primaryScriptId = "unfold_v1",
        alternateScriptId = "unclench_v1",
        shortlist = listOf(
            GateOutcome("unfold_v1", null),
            GateOutcome("unclench_v1", null),
            GateOutcome("loop_v1", GateReason.NIGHT_VETOED),
        ),
    )

    @Test
    fun `logs a row with input method, need, and the full shortlist trace`() = runTest {
        val id = log.logSelection(NeedState.BODY_TENSION, InputMethod.CHIP, selection, nowMs = 1_000L)

        val row = dao.rows.single { it.id == id }
        assertEquals("BODY_TENSION", row.needState)
        assertEquals("CHIP", row.inputMethod)
        assertEquals("unfold_v1", row.offeredScriptId)
        assertEquals("unclench_v1", row.alternateScriptId)
        assertTrue(row.shortlistTrace.contains("unfold_v1:OK"))
        assertTrue(row.shortlistTrace.contains("unclench_v1:OK"))
        assertTrue(row.shortlistTrace.contains("loop_v1:NIGHT_VETOED"))
        assertEquals(false, row.alternateTaken)
        assertNull(row.completedScriptId)
    }

    @Test
    fun `records the alternate being taken`() = runTest {
        val id = log.logSelection(NeedState.WOUND_UP, InputMethod.CHIP, selection, nowMs = 1_000L)
        log.recordAlternateTaken(id)

        assertTrue(dao.rows.single { it.id == id }.alternateTaken)
    }

    @Test
    fun `records completion and surfaces it back as a recent completion`() = runTest {
        val id = log.logSelection(NeedState.BODY_TENSION, InputMethod.CHIP, selection, nowMs = 1_000L)
        log.recordCompleted(id, "unfold_v1", nowMs = 5_000L)

        val recent = log.recentCompletions(sinceMs = 0L)
        assertEquals(
            listOf("unfold_v1" to NeedState.BODY_TENSION),
            recent.map { it.scriptId to it.needState },
        )
        assertEquals(5_000L, recent.single().completedAtMs)
    }

    @Test
    fun `an uncompleted row never surfaces as a recent completion`() = runTest {
        log.logSelection(NeedState.BODY_TENSION, InputMethod.CHIP, selection, nowMs = 1_000L)

        assertTrue(log.recentCompletions(sinceMs = 0L).isEmpty())
    }

    @Test
    fun `recentCompletions respects the since bound`() = runTest {
        val id = log.logSelection(NeedState.DRAINED, InputMethod.CHIP, selection, nowMs = 1_000L)
        log.recordCompleted(id, "climb_v1", nowMs = 10_000L)

        assertTrue(log.recentCompletions(sinceMs = 20_000L).isEmpty())
        assertEquals(1, log.recentCompletions(sinceMs = 5_000L).size)
    }
}
