package app.kairos.ui.editor

import app.kairos.data.MonthlyMode
import app.kairos.data.settings.AppSettings
import app.kairos.data.settings.NewReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EditorLogicTest {
    private val now = LocalDateTime.parse("2026-11-19T10:30") // Perşembe
    private val today = now.toLocalDate()
    private fun withLast(minute: Int?) = AppSettings(newReminder = NewReminder(lastUsedMinute = minute))

    @Test fun newDraftHasEverythingOptionalOff() {
        val d = EditorLogic.newDraft(now, AppSettings())
        assertTrue(d.isNew)
        assertNull(d.recurrence)
        assertNull(d.leadMinutes)
        assertFalse(d.important)
        assertFalse(d.canSave)
    }

    @Test fun newDraftUsesLastUsedNoon() {
        val d = EditorLogic.newDraft(now, withLast(720))
        assertEquals(today, d.date)
        assertEquals(720, d.minute)
        assertTrue(d.fromLastUsed)
    }

    @Test fun lastUsedPassedTodayGivesNextFullHour() {
        val d = EditorLogic.newDraft(LocalDateTime.parse("2026-11-19T15:10"), withLast(720))
        assertEquals(16 * 60, d.minute)
        assertFalse(d.fromLastUsed)
    }

    @Test fun switchingToTomorrowRestoresLastUsedWhenTimeUntouched() {
        val late = LocalDateTime.parse("2026-11-19T15:10")
        val d = EditorLogic.newDraft(late, withLast(720))
        val tomorrow = EditorLogic.withDate(d, today.plusDays(1), late, withLast(720))
        assertEquals(720, tomorrow.minute)
    }

    @Test fun touchedTimeIsKeptWhenDayChanges() {
        val d = EditorLogic.withMinute(EditorLogic.newDraft(now, withLast(720)), 17 * 60)
        assertEquals(17 * 60, EditorLogic.withDate(d, today.plusDays(1), now, withLast(720)).minute)
    }

    @Test fun touchedPastTimeBumpsWhenMovedToToday() {
        val d = EditorLogic.withMinute(EditorLogic.newDraft(now, withLast(720)).copy(date = today.plusDays(1)), 9 * 60)
        val back = EditorLogic.withDate(d, today, now, withLast(720))
        assertEquals(11 * 60, back.minute)
    }

    @Test fun dayChips() {
        assertEquals(listOf("Bugün", "Yarın", "Cumartesi"), EditorLogic.dayChips(today).map { it.label })
        val friday = LocalDate.parse("2026-11-20")
        assertEquals("Pazartesi", EditorLogic.dayChips(friday)[2].label)
        assertEquals(LocalDate.parse("2026-11-23"), EditorLogic.dayChips(friday)[2].date)
    }

    @Test fun repeatOptionsStartWithNoneAndPinMonthlyDay() {
        val options = EditorLogic.repeatOptions(LocalDate.parse("2026-11-30"))
        assertEquals("Tekrar yok", options.first().label)
        assertNull(options.first().rule)
        assertEquals(listOf("Tekrar yok", "Her gün", "Hafta içi", "Her pazartesi", "Her ayın 30'u", "Her yıl"), options.map { it.label })
        assertEquals(MonthlyMode.ByDayOfMonth(30), options[4].rule!!.monthlyMode)
    }

    @Test fun leadLabels() {
        assertEquals("Yok", EditorLogic.leadLabel(null))
        assertEquals("15 dk önce", EditorLogic.leadLabel(15))
        assertEquals("1 saat önce", EditorLogic.leadLabel(60))
        assertEquals("1 gün önce", EditorLogic.leadLabel(1440))
    }

    @Test fun rulerSnapsToFiveMinutesWithinTheDay() {
        assertEquals(725, RulerMath.snapped(720f, 6f))
        assertEquals(720, RulerMath.snapped(720f, 2f))
        assertEquals(0, RulerMath.snapped(10f, -500f))
        assertEquals(23 * 60 + 55, RulerMath.snapped(1400f, 500f))
        assertEquals(2, RulerMath.tickKind(600))
        assertEquals(1, RulerMath.tickKind(630))
        assertEquals(0, RulerMath.tickKind(615))
    }

    @Test fun pastWarning() {
        assertTrue(EditorLogic.isInPast(EditorLogic.newDraft(now, AppSettings()).copy(minute = 600), now))
        assertFalse(EditorLogic.isInPast(EditorLogic.newDraft(now, AppSettings()).copy(minute = 660), now))
    }
}
