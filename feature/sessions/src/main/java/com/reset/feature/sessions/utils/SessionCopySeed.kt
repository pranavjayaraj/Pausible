package com.reset.feature.sessions.utils

import java.util.Calendar

/**
 * A day-stable seed for [com.reset.feature.sessions.content.CopyPool] rotation: two
 * sessions on the same calendar day start from the same line, while the caller's
 * completed-count still walks the pool forward within the day.
 */
object SessionCopySeed {
    fun today(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
}
