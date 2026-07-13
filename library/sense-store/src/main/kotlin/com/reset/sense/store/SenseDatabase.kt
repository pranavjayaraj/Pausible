package com.reset.sense.store

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DecisionEntity::class],
    version = 2, // v2: DecisionEntity.schemaVersion (training rows are schema-stamped)
    exportSchema = true,
)
abstract class SenseDatabase : RoomDatabase() {
    abstract fun decisionLogDao(): DecisionLogDao

    companion object {
        const val NAME = "sense_store.db"
    }
}
