package app.kairos.data

import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Hatırlatmalar üzerindeki tüm işlemler. Ekranlar ve alıcılar veritabanına doğrudan değil,
 * buradan erişir. Değiştiren her işlem:
 * - alarmları günceller ([alarms]),
 * - "Geri al" için önceki kaydı döndürür.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val alarms: ReminderAlarms = ReminderAlarms.None,
) {
    val open: Flow<List<Reminder>> = dao.observeOpen()
    val completed: Flow<List<Reminder>> = dao.observeCompleted()

    suspend fun get(id: Long): Reminder? = dao.getById(id)

    suspend fun add(
        title: String,
        date: LocalDate,
        minute: Int,
        recurrence: RecurrenceRule? = null,
        leadMinutes: Int? = null,
        important: Boolean = false,
        persistent: Boolean = false,
    ): Reminder {
        val now = clock.millis()
        val draft = Reminder(
            title = title.trim(),
            dueEpochDay = date.toEpochDay(),
            dueMinute = minute,
            recurrenceJson = RecurrenceRule.toJson(recurrence),
            leadMinutes = leadMinutes,
            important = important,
            persistent = persistent,
            createdAt = now,
            updatedAt = now,
        )
        val saved = draft.copy(id = dao.insert(draft))
        alarms.onChanged(saved)
        return saved
    }

    /** Düzenleme. Tarih/saat değişirse eski erteleme geçersiz olur. */
    suspend fun update(edited: Reminder): Reminder? {
        val old = dao.getById(edited.id) ?: return null
        val timeChanged = old.dueEpochDay != edited.dueEpochDay || old.dueMinute != edited.dueMinute
        val saved = edited.copy(
            title = edited.title.trim(),
            snoozedUntil = if (timeChanged) null else edited.snoozedUntil,
            updatedAt = clock.millis(),
        )
        dao.update(saved)
        alarms.onChanged(saved)
        return old
    }

    /**
     * Tamamla. Tekrarlayan hatırlatma bir sonraki tarihine taşınır; kural bittiyse tamamlanır.
     * @return geri almak için önceki kayıt; kayıt yoksa ya da zaten tamamlanmışsa null.
     */
    suspend fun complete(id: Long): Reminder? {
        val old = dao.getById(id) ?: return null
        if (old.isCompleted) return null
        val now = clock.millis()
        val rule = old.recurrence
        val next = rule?.nextDateAfter(old.dueDate)
        val saved = if (rule != null && next != null) {
            old.copy(
                dueEpochDay = next.toEpochDay(),
                recurrenceJson = RecurrenceRule.toJson(rule.afterFire()),
                snoozedUntil = null,
                updatedAt = now,
            )
        } else {
            old.copy(completedAt = now, snoozedUntil = null, updatedAt = now)
        }
        dao.update(saved)
        alarms.onChanged(saved)
        return old
    }

    /** Tamamlanmış bir hatırlatmayı yeniden açık listeye alır. */
    suspend fun reopen(id: Long): Reminder? {
        val old = dao.getById(id) ?: return null
        if (!old.isCompleted) return null
        val saved = old.copy(completedAt = null, updatedAt = clock.millis())
        dao.update(saved)
        alarms.onChanged(saved)
        return old
    }

    /** [untilMillis] anına kadar ertele. */
    suspend fun snooze(id: Long, untilMillis: Long): Reminder? {
        val old = dao.getById(id) ?: return null
        val saved = old.copy(snoozedUntil = untilMillis, updatedAt = clock.millis())
        dao.update(saved)
        alarms.onChanged(saved)
        return old
    }

    suspend fun delete(id: Long): Reminder? {
        val old = dao.getById(id) ?: return null
        dao.delete(old)
        alarms.onRemoved(id)
        return old
    }

    /** Geri al: [previous] kaydı aynen geri yazar (silinmişse yeniden oluşur). */
    suspend fun restore(previous: Reminder) {
        dao.upsert(previous)
        alarms.onChanged(previous)
    }

    suspend fun markNotified(id: Long) = dao.markNotified(id, clock.millis())

    suspend fun countForBrief(day: LocalDate): Int = dao.countOpenOn(day.toEpochDay())
    suspend fun countOverdueBefore(day: LocalDate): Int = dao.countOpenBefore(day.toEpochDay())
    suspend fun openForScheduling(): List<Reminder> = dao.getOpen()

    suspend fun all(): List<Reminder> = dao.getAll()

    /** Yedekten geri yükleme: tüm hatırlatmalar değişir, alarmlar yeniden kurulur. */
    suspend fun replaceAll(reminders: List<Reminder>) {
        dao.getAll().forEach { alarms.onRemoved(it.id) }
        dao.replaceAll(reminders.map { it.copy(id = 0) })
        dao.getOpen().forEach(alarms::onChanged)
    }

    /** Tamamlananları [olderThanDays] günden eskiyse siler. Sadece kullanıcı bu ayarı açarsa çağrılır. */
    suspend fun purgeCompleted(olderThanDays: Int): Int =
        dao.deleteCompletedBefore(clock.millis() - olderThanDays * 24L * 60 * 60 * 1000)
}
