package app.kairos.util

import app.kairos.data.MonthlyMode
import app.kairos.data.RecurrenceRule
import app.kairos.data.RecurrenceUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/** Tekrar kuralının kısa Türkçe açıklaması: "Her gün", "Hafta içi", "Her perşembe", "Her ayın 15'i". */
object RecurrenceText {

    fun describe(rule: RecurrenceRule, anchor: LocalDate): String {
        val n = rule.interval
        return when (rule.unit) {
            RecurrenceUnit.DAY -> if (n == 1) "Her gün" else "$n günde bir"
            RecurrenceUnit.WEEK -> week(rule, anchor)
            RecurrenceUnit.MONTH -> month(rule, anchor)
            RecurrenceUnit.YEAR -> if (n == 1) "Her yıl" else "$n yılda bir"
        }
    }

    private fun week(rule: RecurrenceRule, anchor: LocalDate): String {
        val days = rule.daysOfWeek.ifEmpty { setOf(anchor.dayOfWeek.value) }
        val prefix = if (rule.interval == 1) "Her" else "${rule.interval} haftada bir"
        return when (days) {
            (1..5).toSet() -> if (rule.interval == 1) "Hafta içi" else "$prefix hafta içi"
            setOf(6, 7) -> if (rule.interval == 1) "Hafta sonu" else "$prefix hafta sonu"
            (1..7).toSet() -> if (rule.interval == 1) "Her gün" else "$prefix her gün"
            else -> "$prefix " + days.sorted().joinToString(", ") { dayName(it) }
        }
    }

    private fun month(rule: RecurrenceRule, anchor: LocalDate): String {
        val prefix = if (rule.interval == 1) "Her ayın" else "${rule.interval} ayda bir,"
        return when (val m = rule.monthlyMode) {
            null -> "$prefix ${ordinalDay(anchor.dayOfMonth)}"
            is MonthlyMode.ByDayOfMonth -> "$prefix ${ordinalDay(m.day)}"
            is MonthlyMode.ByNthWeekday -> {
                val which = when (m.ordinal) { 1 -> "ilk"; 2 -> "ikinci"; 3 -> "üçüncü"; 4 -> "dördüncü"; else -> "son" }
                "$prefix $which ${dayName(m.isoWeekday)}"
            }
        }
    }

    private fun dayName(iso: Int) = DayOfWeek.of(iso).getDisplayName(TextStyle.FULL, TimeText.TR).lowercase(TimeText.TR)

    /** 15 → "15'i", 1 → "1'i", 3 → "3'ü", 6 → "6'sı" (Türkçe ünlü uyumu). */
    fun ordinalDay(day: Int): String {
        val suffix = when (day % 10) {
            1, 5, 8 -> "'i"
            2, 7 -> "'si"
            3, 4 -> "'ü"
            6 -> "'sı"
            9 -> "'u"
            else -> when (day) { 10, 30 -> "'u"; 20 -> "'si"; else -> "'i" }
        }
        return "$day$suffix"
    }
}
