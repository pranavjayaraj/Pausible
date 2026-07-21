package com.reset.repository.data.checkin

import androidx.room.Database
import androidx.room.RoomDatabase

/** Separate from `sense_store.db` on purpose — the `sense-*` modules stay untouched. */
@Database(entities = [CheckInPropensityEntity::class], version = 1, exportSchema = true)
abstract class CheckInPropensityDatabase : RoomDatabase() {
    abstract fun checkInPropensityDao(): CheckInPropensityDao

    companion object {
        const val NAME = "checkin_propensity.db"
    }
}
