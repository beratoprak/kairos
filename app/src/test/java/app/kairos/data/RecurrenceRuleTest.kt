package app.kairos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceRuleTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun daily() =
        assertEquals(d("2026-11-20"), RecurrenceRule(RecurrenceUnit.DAY).nextDateAfter(d("2026-11-19")))

    @Test fun everyThreeDays() =
        assertEquals(d("2026-11-22"), RecurrenceRule(RecurrenceUnit.DAY, interval = 3).nextDateAfter(d("2026-11-19")))

    @Test fun weeklySameWeekday() =
        assertEquals(d("2026-11-26"), RecurrenceRule(RecurrenceUnit.WEEK).nextDateAfter(d("2026-11-19")))

    @Test fun weekdaysOnly_fridayJumpsToMonday() {
        val weekdays = RecurrenceRule(RecurrenceUnit.WEEK, daysOfWeek = (1..5).toSet())
        // 2026-11-20 Cuma → 2026-11-23 Pazartesi
        assertEquals(d("2026-11-23"), weekdays.nextDateAfter(d("2026-11-20")))
        // Çarşamba → Perşembe (aynı hafta)
        assertEquals(d("2026-11-19"), weekdays.nextDateAfter(d("2026-11-18")))
    }

    @Test fun everyOtherWeekOnMondayAndThursday() {
        val rule = RecurrenceRule(RecurrenceUnit.WEEK, interval = 2, daysOfWeek = setOf(1, 4))
        // Pazartesi 2026-11-16 → aynı haftanın Perşembesi
        assertEquals(d("2026-11-19"), rule.nextDateAfter(d("2026-11-16")))
        // Perşembe 2026-11-19 → iki hafta sonraki Pazartesi
        assertEquals(d("2026-11-30"), rule.nextDateAfter(d("2026-11-19")))
    }

    @Test fun monthlyOn31stClampsToShortMonthsWithoutDrifting() {
        val rule = RecurrenceRule(RecurrenceUnit.MONTH, monthlyMode = MonthlyMode.ByDayOfMonth(31))
        val feb = rule.nextDateAfter(d("2027-01-31"))
        assertEquals(d("2027-02-28"), feb)
        assertEquals(d("2027-03-31"), rule.nextDateAfter(feb!!))
    }

    @Test fun lastFridayOfMonth() {
        val rule = RecurrenceRule(RecurrenceUnit.MONTH, monthlyMode = MonthlyMode.ByNthWeekday(ordinal = 5, isoWeekday = 5))
        assertEquals(d("2026-12-25"), rule.nextDateAfter(d("2026-11-27")))
    }

    @Test fun yearlyOnLeapDay() =
        assertEquals(d("2029-02-28"), RecurrenceRule(RecurrenceUnit.YEAR).nextDateAfter(d("2028-02-29")))

    @Test fun endsOnDate() {
        val rule = RecurrenceRule(RecurrenceUnit.DAY, endKind = RecurrenceEndKind.ON_DATE, endEpochDay = d("2026-11-20").toEpochDay())
        assertEquals(d("2026-11-20"), rule.nextDateAfter(d("2026-11-19")))
        assertNull(rule.nextDateAfter(d("2026-11-20")))
    }

    @Test fun endsAfterCount() {
        var rule = RecurrenceRule(RecurrenceUnit.DAY, endKind = RecurrenceEndKind.AFTER_COUNT, endCount = 2)
        rule = rule.afterFire()
        assertEquals(1, rule.endCount)
        rule = rule.afterFire()
        assertNull(rule.nextDateAfter(d("2026-11-19")))
    }

    @Test fun afterCountWithoutCountBecomesEndless() =
        assertEquals(RecurrenceEndKind.NEVER, RecurrenceRule(RecurrenceUnit.DAY, endKind = RecurrenceEndKind.AFTER_COUNT).sanitized().endKind)

    @Test fun jsonRoundTrip() {
        val rule = RecurrenceRule(RecurrenceUnit.MONTH, monthlyMode = MonthlyMode.ByNthWeekday(2, 3))
        assertEquals(rule, RecurrenceRule.fromJson(RecurrenceRule.toJson(rule)))
        assertNull(RecurrenceRule.fromJson(null))
        assertNull(RecurrenceRule.fromJson("bozuk"))
    }
}
