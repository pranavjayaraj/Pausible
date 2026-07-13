package com.reset.sense.store

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DecisionEntity::class],
    // v2: DecisionEntity.schemaVersion (training rows are schema-stamped)
    // v3: propensity trail (appliedThreshold, explorationEpsilon, explored)
    version = 3,
    exportSchema = true,
)
abstract class SenseDatabase : RoomDatabase() {
    abstract fun decisionLogDao(): DecisionLogDao

    companion object {
        const val NAME = "sense_store.db"
    }
}
