package com.reset.feature.home

import com.reset.model.domain.TimeProvider

/** Deterministic clock for Home ViewModel tests. */
class FakeTimeProvider(
    var epochDay: Long = 1_000L,
    var dayIndex: Int = 0,
    var hour: Int = 10,
    var dayOfMonth: Int = 1,
) : TimeProvider {
    override fun todayEpochDay() = epochDay
    override fun dayOfWeekIndex() = dayIndex
    override fun hourOfDay() = hour
    override fun dayOfMonth() = dayOfMonth
}
