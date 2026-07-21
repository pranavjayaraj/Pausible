package com.reset.feature.sessions

import com.reset.model.domain.TimeProvider

/** Deterministic clock for Session ViewModel tests. */
class FakeTimeProvider(
    var millis: Long = 1_700_000_000_000L,
    var epochDay: Long = 1_000L,
    var dayIndex: Int = 0,
    var hour: Int = 10,
    var dayOfMonth: Int = 1,
) : TimeProvider {
    override fun nowMillis() = millis
    override fun todayEpochDay() = epochDay
    override fun dayOfWeekIndex() = dayIndex
    override fun hourOfDay() = hour
    override fun dayOfMonth() = dayOfMonth
}
