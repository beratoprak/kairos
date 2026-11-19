package app.kairos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Bir hatırlatma.
 *
 * Tarih ve saat "yerel duvar saati" olarak tutulur ([dueEpochDay] + [dueMinute]): 12:00'ye kurulan
 * hatırlatma, saat dilimi ya da yaz saati değişse de 12:00'de çalar. Erteleme ise gerçek bir an
 * olduğu için epoch milisaniye ([snoozedUntil]) olarak tutulur.
 *
 * Opsiyonel her şey varsayılan olarak kapalıdır (KURALLAR §1.2): tekrar yok, önceden hatırlatma yok,
 * önem normal, ısrarcı mod kapalı.
 */
@Entity(
    tableName = "reminders",
    indices = [Index("completed_at"), Index("due_epoch_day", "due_minute")],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "due_epoch_day") val dueEpochDay: Long,
    /** Günün dakikası, 0..1439 (12:00 = 720). */
    @ColumnInfo(name = "due_minute") val dueMinute: Int,
    /** Null = tekrar yok (varsayılan). */
    @ColumnInfo(name = "recurrence") val recurrenceJson: String? = null,
    /** Kaç dakika önce ek bildirim; null = yok (varsayılan). */
    @ColumnInfo(name = "lead_minutes") val leadMinutes: Int? = null,
    val important: Boolean = false,
    /** Israrcı mod: tamamlanana kadar tekrar bildir. Varsayılan kapalı. */
    val persistent: Boolean = false,
    @ColumnInfo(name = "snoozed_until") val snoozedUntil: Long? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    /** Son bildirim gösterildiği an. Yeniden başlatma sonrası aynı hatırlatma iki kez bildirilmesin diye. */
    @ColumnInfo(name = "last_notified_at") val lastNotifiedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    val dueDate: LocalDate get() = LocalDate.ofEpochDay(dueEpochDay)
    val dueTime: LocalTime get() = LocalTime.of(dueMinute / 60, dueMinute % 60)
    val dueDateTime: LocalDateTime get() = LocalDateTime.of(dueDate, dueTime)
    val recurrence: RecurrenceRule? get() = RecurrenceRule.fromJson(recurrenceJson)
    val isCompleted: Boolean get() = completedAt != null
    val isRecurring: Boolean get() = recurrenceJson != null

    init {
        require(dueMinute in 0 until MINUTES_PER_DAY) { "dueMinute 0..1439 olmalı: $dueMinute" }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        fun minuteOf(time: LocalTime) = time.hour * 60 + time.minute
    }
}
