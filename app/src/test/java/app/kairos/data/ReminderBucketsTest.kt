package app.kairos.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReminderBucketsTest {
    // 2026-11-19 Perşembe
    private val today = LocalDate.parse("2026-11-19")
    private var nextId = 1L

    private fun r(dayOffset: Long, minute: Int, snoozedUntil: Long? = null) = Reminder(
        id = nextId++, title = "x", dueEpochDay = today.toEpochDay() + dayOffset, dueMinute = minute,
        snoozedUntil = snoozedUntil, createdAt = 0, updatedAt = 0,
    )

    @Test fun splitsIntoScreens() {
        val yesterday = r(-1, 17 * 60)
        val passedToday = r(0, 9 * 60)
        val laterToday = r(0, 15 * 60)
        val tomorrow = r(1, 12 * 60)
        val saturday = r(2, 10 * 60)
        val sunday = r(3, 10 * 60)
        val nextMonday = r(4, 10 * 60)
        val b = ReminderBuckets.split(
            listOf(nextMonday, laterToday, yesterday, sunday, tomorrow, passedToday, saturday),
            today, nowMinute = 11 * 60, nowMillis = 1_000,
        )
        assertEquals(listOf(yesterday, passedToday), b.overdue)
        assertEquals(listOf(laterToday), b.today)
        assertEquals(listOf(tomorrow), b.tomorrow)
        assertEquals(listOf(saturday, sunday), b.thisWeek)
        assertEquals(listOf(nextMonday), b.later)
    }

    @Test fun snoozedReminderIsNotOverdue() {
        val snoozed = r(0, 9 * 60, snoozedUntil = 5_000)
        val b = ReminderBuckets.split(listOf(snoozed), today, nowMinute = 11 * 60, nowMillis = 1_000)
        assertEquals(emptyList<Reminder>(), b.overdue)
        assertEquals(listOf(snoozed), b.today)
    }

    @Test fun onSundayEverythingAfterTomorrowIsLater() {
        val sunday = LocalDate.parse("2026-11-22")
        val tue = Reminder(id = 1, title = "x", dueEpochDay = sunday.toEpochDay() + 2, dueMinute = 600, createdAt = 0, updatedAt = 0)
        val b = ReminderBuckets.split(listOf(tue), sunday, nowMinute = 0, nowMillis = 0)
        assertEquals(listOf(tue), b.later)
    }
}
