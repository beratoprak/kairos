package app.kairos.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Açık hatırlatmaları ekran gruplarına ayırır.
 * - Bugün ekranı: [overdue] (önceki günler + bugünün saati geçmiş olanlar) ve [today]
 * - Yakında ekranı: [tomorrow], [thisWeek] (haftanın kalan günleri), [later]
 */
data class ReminderBuckets(
    val overdue: List<Reminder>,
    val today: List<Reminder>,
    val tomorrow: List<Reminder>,
    val thisWeek: List<Reminder>,
    val later: List<Reminder>,
) {
    companion object {
        /**
         * @param nowMinute şu anki günün dakikası; bugünün saati geçmiş olanlar gecikmiş sayılır.
         * @param firstDayOfWeek haftanın ilk günü (ayarlardan; varsayılan Pazartesi).
         * Ertelenmiş (snoozedUntil gelecekte) bir hatırlatma gecikmiş sayılmaz.
         */
        fun split(
            open: List<Reminder>,
            today: LocalDate,
            nowMinute: Int,
            nowMillis: Long,
            firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        ): ReminderBuckets {
            val todayDay = today.toEpochDay()
            val tomorrowDay = todayDay + 1
            val weekEnd = today
                .with(TemporalAdjusters.nextOrSame(firstDayOfWeek.minus(1)))
                .toEpochDay()
            val sorted = open.filter { !it.isCompleted }.sortedWith(compareBy({ it.dueEpochDay }, { it.dueMinute }, { it.id }))

            fun isSnoozed(r: Reminder) = (r.snoozedUntil ?: 0) > nowMillis
            fun isOverdue(r: Reminder) = !isSnoozed(r) &&
                (r.dueEpochDay < todayDay || (r.dueEpochDay == todayDay && r.dueMinute < nowMinute))

            val overdue = sorted.filter(::isOverdue)
            val rest = sorted - overdue.toSet()
            return ReminderBuckets(
                overdue = overdue,
                today = rest.filter { it.dueEpochDay <= todayDay },
                tomorrow = rest.filter { it.dueEpochDay == tomorrowDay },
                thisWeek = rest.filter { it.dueEpochDay in (tomorrowDay + 1)..weekEnd },
                later = rest.filter { it.dueEpochDay > maxOf(weekEnd, tomorrowDay) },
            )
        }
    }
}
