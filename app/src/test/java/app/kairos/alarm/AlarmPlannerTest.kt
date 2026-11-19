package app.kairos.alarm

import app.kairos.data.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmPlannerTest {
    private val ist = ZoneId.of("Europe/Istanbul")
    private val berlin = ZoneId.of("Europe/Berlin")
    private val day = LocalDate.parse("2026-11-20")
    private fun ms(dt: String, zone: ZoneId = ist) = LocalDateTime.parse(dt).atZone(zone).toInstant().toEpochMilli()
    private fun r(minute: Int = 720, lead: Int? = null, snoozed: Long? = null, done: Long? = null, date: LocalDate = day) =
        Reminder(id = 7, title = "x", dueEpochDay = date.toEpochDay(), dueMinute = minute, leadMinutes = lead,
            snoozedUntil = snoozed, completedAt = done, createdAt = 0, updatedAt = 0)

    @Test fun futureReminderGetsOneDueAlarm() =
        assertEquals(listOf(PlannedAlarm(7, AlarmKind.DUE, ms("2026-11-20T12:00"))), AlarmPlanner.plan(r(), ms("2026-11-20T09:00"), ist))

    @Test fun pastReminderGetsNoAlarm() =
        assertEquals(emptyList<PlannedAlarm>(), AlarmPlanner.plan(r(), ms("2026-11-20T12:01"), ist))

    @Test fun completedReminderGetsNoAlarm() =
        assertEquals(emptyList<PlannedAlarm>(), AlarmPlanner.plan(r(done = 1), ms("2026-11-20T09:00"), ist))

    @Test fun leadIsOffByDefaultAndAddsSecondAlarmWhenSet() {
        assertEquals(1, AlarmPlanner.plan(r(), ms("2026-11-20T09:00"), ist).size)
        assertEquals(
            listOf(PlannedAlarm(7, AlarmKind.LEAD, ms("2026-11-20T11:45")), PlannedAlarm(7, AlarmKind.DUE, ms("2026-11-20T12:00"))),
            AlarmPlanner.plan(r(lead = 15), ms("2026-11-20T09:00"), ist),
        )
    }

    @Test fun leadAlreadyPassedOnlyDueRemains() =
        assertEquals(listOf(AlarmKind.DUE), AlarmPlanner.plan(r(lead = 15), ms("2026-11-20T11:50"), ist).map { it.kind })

    @Test fun snoozeReplacesEverything() {
        val until = ms("2026-11-20T12:10")
        assertEquals(listOf(PlannedAlarm(7, AlarmKind.DUE, until)), AlarmPlanner.plan(r(lead = 15, snoozed = until), ms("2026-11-20T12:00"), ist))
    }

    @Test fun wallClockIsKeptWhenTravelling() {
        // 12:00 hatırlatması Berlin'deyken Berlin saatiyle 12:00'de çalar.
        assertEquals(ms("2026-11-20T12:00", berlin), AlarmPlanner.dueMillis(r(), berlin))
    }

    @Test fun dstGapMovesForwardInsteadOfFailing() {
        // Berlin 2027-03-28 02:00 → 03:00 (02:30 yok)
        val gap = r(minute = 150, date = LocalDate.parse("2027-03-28"))
        assertEquals(ms("2027-03-28T03:30", berlin), AlarmPlanner.dueMillis(gap, berlin))
    }

    @Test fun staleAlarmIsIgnoredAfterEdit() {
        val oldAt = ms("2026-11-20T12:00")
        assertTrue(AlarmPlanner.isStillWanted(r(), AlarmKind.DUE, oldAt, ist))
        assertFalse(AlarmPlanner.isStillWanted(r(minute = 900), AlarmKind.DUE, oldAt, ist))
        assertFalse(AlarmPlanner.isStillWanted(r(done = 1), AlarmKind.DUE, oldAt, ist))
    }
}
