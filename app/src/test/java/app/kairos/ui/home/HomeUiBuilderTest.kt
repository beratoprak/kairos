package app.kairos.ui.home

import app.kairos.data.MonthlyMode
import app.kairos.data.RecurrenceRule
import app.kairos.data.RecurrenceUnit
import app.kairos.data.Reminder
import app.kairos.data.settings.AppSettings
import app.kairos.util.RecurrenceText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HomeUiBuilderTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val now = LocalDateTime.parse("2026-11-19T11:22") // Perşembe
    private val today = now.toLocalDate()
    private fun ms(dt: LocalDateTime) = dt.atZone(zone).toInstant().toEpochMilli()
    private var id = 1L
    private fun r(day: Long, minute: Int, title: String = "x", rule: RecurrenceRule? = null, done: LocalDateTime? = null, important: Boolean = false) =
        Reminder(id = id++, title = title, dueEpochDay = today.toEpochDay() + day, dueMinute = minute,
            recurrenceJson = RecurrenceRule.toJson(rule), important = important,
            completedAt = done?.let(::ms), createdAt = 0, updatedAt = 0)

    @Test fun todayScreenMatchesDesign() {
        val overdue = r(-1, 17 * 60, "Muhasebeciyi ara")
        val done = r(0, 9 * 60 + 30, "Notları gönder", done = now.minusHours(1))
        val lunch = r(0, 12 * 60, "Öğle yemeği")
        val contract = r(0, 15 * 60, "Sözleşme", important = true)
        val pharmacy = r(0, 18 * 60 + 30, "Eczane", rule = RecurrenceRule(RecurrenceUnit.WEEK))
        val ui = HomeUiBuilder.build(listOf(overdue, lunch, contract, pharmacy), listOf(done), AppSettings(), now, zone)

        assertEquals("Perşembe, 19 Kasım", ui.today.dateLabel)
        assertEquals("3 hatırlatma · 1 gecikmiş", ui.today.summary)
        assertEquals("Dün, 17:00", ui.today.overdue.single().meta)
        assertEquals(listOf("09:30", "12:00", "15:00", "18:30"), ui.today.today.map { it.time })
        assertTrue(ui.today.today[0].isDone)
        assertTrue(ui.today.today[1].isNext)
        assertEquals("Sıradaki · 38 dk sonra", ui.today.today[1].meta)
        assertTrue(ui.today.today[2].important)
        assertEquals("Her perşembe", ui.today.today[3].meta)
    }

    @Test fun emptyAndAllDoneSummaries() {
        assertEquals("Bugün için hatırlatma yok", HomeUiBuilder.build(emptyList(), emptyList(), AppSettings(), now, zone).today.summary)
        val done = r(0, 9 * 60, done = now.minusMinutes(5))
        assertEquals("Bugünkü hatırlatmalar tamamlandı", HomeUiBuilder.build(emptyList(), listOf(done), AppSettings(), now, zone).today.summary)
    }

    @Test fun upcomingSections() {
        val ui = HomeUiBuilder.build(listOf(r(1, 600), r(2, 600), r(10, 600)), emptyList(), AppSettings(), now, zone)
        assertEquals(listOf("Yarın", "Bu hafta", "Daha sonra"), ui.upcoming.map { it.title })
        assertEquals("Cumartesi", ui.upcoming[1].rows.single().meta)
        assertEquals("29 Kasım", ui.upcoming[2].rows.single().meta)
    }

    @Test fun twelveHourTimesSortCorrectly() {
        val s = AppSettings().let { it.copy(appearance = it.appearance.copy(use24Hour = false)) }
        val ui = HomeUiBuilder.build(listOf(r(0, 13 * 60), r(0, 12 * 60 + 30)), emptyList(), s, now, zone)
        assertEquals(listOf("12:30 ÖS", "1:00 ÖS"), ui.today.today.map { it.time })
    }

    @Test fun completedTodayFromAnotherDayIsNotInToday() {
        val oldDue = r(-3, 600, done = now.minusMinutes(1))
        val ui = HomeUiBuilder.build(emptyList(), listOf(oldDue), AppSettings(), now, zone)
        assertFalse(ui.today.today.any { it.id == oldDue.id })
        assertEquals(1, ui.completed.size)
        assertTrue(ui.completed.single().meta!!.startsWith("Tamamlandı · Bugün"))
    }

    @Test fun recurrenceTexts() {
        val thu = LocalDate.parse("2026-11-19")
        fun d(rule: RecurrenceRule) = RecurrenceText.describe(rule, thu)
        assertEquals("Her gün", d(RecurrenceRule(RecurrenceUnit.DAY)))
        assertEquals("3 günde bir", d(RecurrenceRule(RecurrenceUnit.DAY, 3)))
        assertEquals("Hafta içi", d(RecurrenceRule(RecurrenceUnit.WEEK, daysOfWeek = (1..5).toSet())))
        assertEquals("Her pazartesi, perşembe", d(RecurrenceRule(RecurrenceUnit.WEEK, daysOfWeek = setOf(4, 1))))
        assertEquals("2 haftada bir cuma", d(RecurrenceRule(RecurrenceUnit.WEEK, 2, setOf(5))))
        assertEquals("Her ayın 15'i", d(RecurrenceRule(RecurrenceUnit.MONTH, monthlyMode = MonthlyMode.ByDayOfMonth(15))))
        assertEquals("Her ayın 3'ü", d(RecurrenceRule(RecurrenceUnit.MONTH, monthlyMode = MonthlyMode.ByDayOfMonth(3))))
        assertEquals("Her ayın son cuma", d(RecurrenceRule(RecurrenceUnit.MONTH, monthlyMode = MonthlyMode.ByNthWeekday(5, 5))))
        assertEquals("Her yıl", d(RecurrenceRule(RecurrenceUnit.YEAR)))
        assertEquals(listOf("1'i", "2'si", "6'sı", "9'u", "10'u", "20'si", "30'u", "31'i"), listOf(1, 2, 6, 9, 10, 20, 30, 31).map(RecurrenceText::ordinalDay))
    }
}
