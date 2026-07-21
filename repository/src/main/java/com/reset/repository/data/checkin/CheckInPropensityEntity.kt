package com.reset.repository.data.checkin

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One check-in's full selection trace: what need was stated, how, what the shortlist looked
 * like (winner, alternate, and every gated-out candidate), and — filled in later — whether
 * the alternate was taken and whether the offered session was completed.
 */
@Entity(
    tableName = "checkin_propensity",
    indices = [Index("timestampMs"), Index("completedAtMs")],
)
data class CheckInPropensityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    /** NeedState name. */
    val needState: String,
    /** InputMethod name. */
    val inputMethod: String,
    val offeredScriptId: String,
    val alternateScriptId: String,
    /** "scriptId:REASON_OR_OK;scriptId:REASON_OR_OK;…", ranked order, one entry per
     *  shortlist candidate — the raw gates-applied trace. */
    val shortlistTrace: String,
    val alternateTaken: Boolean = false,
    val completedScriptId: String? = null,
    val completedAtMs: Long? = null,
)
