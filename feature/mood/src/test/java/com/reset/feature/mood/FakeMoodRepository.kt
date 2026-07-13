package com.reset.feature.mood

import com.reset.model.domain.mood.MoodRepository

/** In-memory [MoodRepository] for Mood ViewModel tests. */
class FakeMoodRepository : MoodRepository {

    /** Levels handed to [recordMood], in call order. */
    val recordedMoods = mutableListOf<Int>()

    override suspend fun recordMood(level: Int) {
        recordedMoods += level
    }
}
