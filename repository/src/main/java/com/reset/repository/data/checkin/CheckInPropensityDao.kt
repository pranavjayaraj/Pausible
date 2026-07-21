package com.reset.repository.data.checkin

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CheckInPropensityDao {

    @Insert
    suspend fun insert(entity: CheckInPropensityEntity): Long

    @Query("UPDATE checkin_propensity SET alternateTaken = 1 WHERE id = :id")
    suspend fun markAlternateTaken(id: Long)

    @Query(
        "UPDATE checkin_propensity SET completedScriptId = :scriptId, completedAtMs = :atMs " +
            "WHERE id = :id",
    )
    suspend fun markCompleted(id: Long, scriptId: String, atMs: Long)

    /** Completed rows since [sinceMs] — the selector's freshness-tiebreak input. */
    @Query("SELECT * FROM checkin_propensity WHERE completedAtMs IS NOT NULL AND completedAtMs >= :sinceMs")
    suspend fun completedSince(sinceMs: Long): List<CheckInPropensityEntity>
}
