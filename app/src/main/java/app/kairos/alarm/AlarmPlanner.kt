package app.kairos.alarm

import app.kairos.data.Reminder
import java.time.ZoneId

enum class AlarmKind { DUE, LEAD }

/** Kurulacak tek bir alarm. */
data class PlannedAlarm(val reminderId: Long, val kind: AlarmKind, val atMillis: Long)

/**
 * Bir hatırlatma için hangi alarmların kurulacağını hesaplar. Saf fonksiyon: Android'e bağımlı
 * değildir, birim testle doğrulanır.
 *
 * - Ertelenmişse: sadece erteleme anında bir DUE alarmı.
 * - Değilse: zamanı gelecekteyse DUE alarmı; önceden hatırlatma açıksa (varsayılan kapalı) ve
 *   o an da gelecekteyse ayrıca LEAD alarmı.
 * - Zamanı geçmiş hatırlatma için alarm kurulmaz; listede "Gecikmiş" olarak görünür.
 */
object AlarmPlanner {
    private const val MINUTE = 60_000L

    fun plan(reminder: Reminder, nowMillis: Long, zone: ZoneId): List<PlannedAlarm> {
        if (reminder.isCompleted) return emptyList()
        val dueMillis = dueMillis(reminder, zone)
        val snooze = reminder.snoozedUntil?.takeIf { it > nowMillis }
        if (snooze != null) return listOf(PlannedAlarm(reminder.id, AlarmKind.DUE, snooze))

        return buildList {
            val lead = reminder.leadMinutes
            if (lead != null && lead > 0) {
                val leadAt = dueMillis - lead * MINUTE
                if (leadAt > nowMillis) add(PlannedAlarm(reminder.id, AlarmKind.LEAD, leadAt))
            }
            if (dueMillis > nowMillis) add(PlannedAlarm(reminder.id, AlarmKind.DUE, dueMillis))
        }
    }

    /** Yerel duvar saatinden gerçek ana. Yaz saati boşluğuna düşen saat ileri kaydırılır. */
    fun dueMillis(reminder: Reminder, zone: ZoneId): Long =
        reminder.dueDateTime.atZone(zone).toInstant().toEpochMilli()

    /**
     * Çalan bir alarm hâlâ geçerli mi? Kurulduktan sonra hatırlatma düzenlendiyse, tamamlandıysa
     * ya da ertelendiyse eski alarm sessizce yok sayılır.
     */
    fun isStillWanted(reminder: Reminder, kind: AlarmKind, firedAtMillis: Long, zone: ZoneId): Boolean =
        plan(reminder, firedAtMillis - MINUTE, zone).any { it.kind == kind && kotlin.math.abs(it.atMillis - firedAtMillis) < MINUTE }
}
