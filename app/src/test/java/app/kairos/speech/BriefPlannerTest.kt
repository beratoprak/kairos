package app.kairos.speech

import app.kairos.data.settings.DailyBrief
import app.kairos.data.settings.WhenSilentMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class BriefPlannerTest {
    private fun at(s: String) = LocalDateTime.parse(s)
    private val on = DailyBrief(enabled = true) // 08:00, her gün

    @Test fun offByDefaultMeansNoAlarm() = assertNull(BriefPlanner.nextAt(at("2026-11-19T07:00"), DailyBrief()))

    @Test fun laterTodayOrTomorrow() {
        assertEquals(at("2026-11-19T08:00"), BriefPlanner.nextAt(at("2026-11-19T07:59"), on))
        assertEquals(at("2026-11-20T08:00"), BriefPlanner.nextAt(at("2026-11-19T08:00"), on))
    }

    @Test fun weekdaysOnlySkipsWeekend() {
        // 2026-11-20 Cuma 09:00 → Pazartesi 08:00
        assertEquals(at("2026-11-23T08:00"), BriefPlanner.nextAt(at("2026-11-20T09:00"), on.copy(days = (1..5).toSet())))
    }

    @Test fun weekendHasItsOwnTime() {
        val b = on.copy(weekendDifferent = true, weekendMinute = 10 * 60)
        assertEquals(at("2026-11-21T10:00"), BriefPlanner.nextAt(at("2026-11-20T09:00"), b))
    }

    @Test fun noDaysSelectedMeansNoAlarm() = assertNull(BriefPlanner.nextAt(at("2026-11-19T07:00"), on.copy(days = emptySet())))

    @Test fun decisions() {
        assertEquals(BriefAction.SPEAK, BriefPlanner.decide("x", phoneSilent = false, WhenSilentMode.NOTIFY_ONLY))
        assertEquals(BriefAction.NOTIFY, BriefPlanner.decide("x", phoneSilent = true, WhenSilentMode.NOTIFY_ONLY))
        assertEquals(BriefAction.SPEAK, BriefPlanner.decide("x", phoneSilent = true, WhenSilentMode.SPEAK_ANYWAY))
        assertEquals(BriefAction.NOTHING, BriefPlanner.decide("x", phoneSilent = true, WhenSilentMode.SKIP))
        assertEquals(BriefAction.NOTHING, BriefPlanner.decide(null, phoneSilent = false, WhenSilentMode.SPEAK_ANYWAY))
    }
}
