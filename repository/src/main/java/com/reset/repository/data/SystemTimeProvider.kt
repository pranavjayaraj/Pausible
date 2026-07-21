package com.reset.repository.data

import com.reset.model.domain.TimeProvider
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Device-clock [TimeProvider]; the only place that touches [Calendar]. */
class SystemTimeProvider @Inject constructor() : TimeProvider {

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun todayEpochDay(): Long {
        val calendar = Calendar.getInstance()
        val midnightOffsetMs =
            calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)
        return TimeUnit.MILLISECONDS.toDays(calendar.timeInMillis + midnightOffsetMs)
    }

    override fun dayOfWeekIndex(): Int {
        // Calendar counts SUNDAY=1..SATURDAY=7; WeeklyFocus is Monday-first.
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return (day + 5) % 7
    }

    override fun hourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    override fun dayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
}
