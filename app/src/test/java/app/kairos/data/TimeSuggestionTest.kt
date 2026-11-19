package app.kairos.data

import app.kairos.data.settings.DefaultTimeMode
import app.kairos.data.settings.NewReminder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TimeSuggestionTest {
    private val today = LocalDate.parse("2026-11-19")
    private val tomorrow = today.plusDays(1)
    private fun at(h: Int, m: Int = 0) = LocalDateTime.of(today, java.time.LocalTime.of(h, m))
    private val noon = 12 * 60

    @Test fun neverUsed_suggestsMorningPart() =
        assertEquals(SuggestedSlot(tomorrow, 9 * 60), TimeSuggestion.suggest(tomorrow, at(8), NewReminder()))

    @Test fun lastUsedNoon_isOfferedForTomorrow() =
        assertEquals(SuggestedSlot(tomorrow, noon), TimeSuggestion.suggest(tomorrow, at(15), NewReminder(lastUsedMinute = noon)))

    @Test fun lastUsedNoon_isOfferedForTodayWhileStillAhead() =
        assertEquals(SuggestedSlot(today, noon), TimeSuggestion.suggest(today, at(10, 30), NewReminder(lastUsedMinute = noon)))

    @Test fun todayAndPreferredPassed_nextFullHour() =
        assertEquals(SuggestedSlot(today, 16 * 60), TimeSuggestion.suggest(today, at(15, 10), NewReminder(lastUsedMinute = noon)))

    @Test fun exactlyAtPreferredMinute_countsAsPassed() =
        assertEquals(SuggestedSlot(today, 13 * 60), TimeSuggestion.suggest(today, at(12, 0), NewReminder(lastUsedMinute = noon)))

    @Test fun lateNight_movesToTomorrowKeepingPreferred() =
        assertEquals(SuggestedSlot(tomorrow, noon), TimeSuggestion.suggest(today, at(23, 20), NewReminder(lastUsedMinute = noon)))

    @Test fun fixedMode_ignoresLastUsed() =
        assertEquals(
            SuggestedSlot(tomorrow, 8 * 60 + 30),
            TimeSuggestion.suggest(tomorrow, at(9), NewReminder(timeMode = DefaultTimeMode.FIXED, fixedMinute = 8 * 60 + 30, lastUsedMinute = noon)),
        )
}
