package app.kairos.snooze

import app.kairos.data.settings.AppSettings
import app.kairos.notification.Notifications
import app.kairos.util.TimeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SnoozePresetsTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test fun morningOffersAllFour() {
        val p = SnoozePresets.build(LocalDateTime.parse("2026-11-19T10:00"), zone, AppSettings())
        assertEquals(listOf("Birazdan", "1 saat sonra", "Bu akşam", "Yarın sabah"), p.map { it.title })
        assertEquals("10 dk · 10:10", p[0].detail)
        assertEquals("19:00", p[2].detail)
        assertEquals("09:00", p[3].detail)
    }

    @Test fun eveningHidesThisEvening() {
        val p = SnoozePresets.build(LocalDateTime.parse("2026-11-19T18:45"), zone, AppSettings())
        assertFalse(p.any { it.title == "Bu akşam" })
    }

    @Test fun timeTextFormats() {
        assertEquals("12:00", TimeText.time(720))
        assertEquals("12:00 ÖS", TimeText.time(720, use24Hour = false))
        assertEquals("12:30 ÖÖ", TimeText.time(30, use24Hour = false))
        val today = LocalDate.parse("2026-11-19")
        assertEquals("Bugün", TimeText.day(today, today))
        assertEquals("Yarın", TimeText.day(today.plusDays(1), today))
        assertEquals("Pazartesi", TimeText.day(today.plusDays(4), today))
        assertEquals("1 Aralık", TimeText.day(LocalDate.parse("2026-12-01"), today))
        assertEquals("1 sa 30 dk", TimeText.duration(90))
    }

    @Test fun quietHoursWindowCrossesMidnight() {
        assertTrue(Notifications.inWindow(23 * 60 + 30, 23 * 60, 7 * 60))
        assertTrue(Notifications.inWindow(6 * 60, 23 * 60, 7 * 60))
        assertFalse(Notifications.inWindow(7 * 60, 23 * 60, 7 * 60))
        assertFalse(Notifications.inWindow(12 * 60, 23 * 60, 7 * 60))
    }
}
