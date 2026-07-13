package com.reset.model.domain.mood

/** Persistence boundary for the post-session mood log. */
interface MoodRepository {

    /** Logs the mood the user reported after a session, on a 0–100 scale. */
    suspend fun recordMood(level: Int)
}
