package com.reset.sense.delivery

/** User-configured quiet-hours window, 24h clock; wraps midnight when start > end. */
data class QuietHours(
    val startHour: Int,
    val endHour: Int,
) {
    companion object {
        val DEFAULT = QuietHours(startHour = 22, endHour = 7)
    }
}

/**
 * Seam for the host-owned quiet-hours preference. The sense modules never
 * read host storage directly (SDK rule: config flows in through interfaces);
 * the app binds an implementation backed by its own preferences store.
 * Read once per evaluation tick — cheap, and always current.
 */
fun interface QuietHoursSource {
    suspend fun quietHours(): QuietHours
}
