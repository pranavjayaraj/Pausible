package com.reset.repository.data.checkin

/** In-memory DAO mirroring the SQL semantics for repository unit tests. */
class FakeCheckInPropensityDao : CheckInPropensityDao {

    val rows = mutableListOf<CheckInPropensityEntity>()
    private var nextId = 1L

    override suspend fun insert(entity: CheckInPropensityEntity): Long {
        val id = nextId++
        rows += entity.copy(id = id)
        return id
    }

    override suspend fun markAlternateTaken(id: Long) {
        val index = rows.indexOfFirst { it.id == id }
        if (index >= 0) rows[index] = rows[index].copy(alternateTaken = true)
    }

    override suspend fun markCompleted(id: Long, scriptId: String, atMs: Long) {
        val index = rows.indexOfFirst { it.id == id }
        if (index >= 0) rows[index] = rows[index].copy(completedScriptId = scriptId, completedAtMs = atMs)
    }

    override suspend fun completedSince(sinceMs: Long): List<CheckInPropensityEntity> =
        rows.filter { it.completedAtMs != null && it.completedAtMs >= sinceMs }
}
