// Erteleme seçenekleri fikri bikram-agarwal/Remember (80d099c) projesindeki reminders/ReminderPresets.kt
// dosyasından uyarlanmıştır ("soon / later today / this evening / tomorrow morning").
// Orijinal: Copyright (C) Remember yazarları — GPL-3.0
package app.kairos.snooze

import app.kairos.data.settings.AppSettings
import app.kairos.util.TimeText
import java.time.LocalDateTime
import java.time.ZoneId

/** Ertele menüsündeki bir seçenek: "Bu akşam · 19:00". */
data class SnoozePreset(val title: String, val detail: String, val untilMillis: Long)

/**
 * İnsan diliyle erteleme seçenekleri. Saatler kullanıcının ayarladığı gün dilimlerinden gelir.
 * Anlamsız seçenekler gösterilmez (ör. akşam 20:00'de "Bu akşam").
 */
object SnoozePresets {
    private const val MIN_GAP_MINUTES = 30

    fun build(now: LocalDateTime, zone: ZoneId, settings: AppSettings): List<SnoozePreset> {
        val use24 = settings.appearance.use24Hour
        val parts = settings.newReminder.dayParts
        val nowMinute = now.hour * 60 + now.minute
        fun millis(t: LocalDateTime) = t.atZone(zone).toInstant().toEpochMilli()
        fun atMinute(dayOffset: Long, minute: Int) =
            now.toLocalDate().plusDays(dayOffset).atTime(minute / 60, minute % 60)

        val soon = settings.notifications.snoozeMinutes
        return buildList {
            val soonAt = now.plusMinutes(soon.toLong())
            add(SnoozePreset("Birazdan", "${TimeText.duration(soon)} · ${TimeText.time(soonAt.hour * 60 + soonAt.minute, use24)}", millis(soonAt)))

            val hourAt = now.plusHours(1)
            add(SnoozePreset("1 saat sonra", TimeText.time(hourAt.hour * 60 + hourAt.minute, use24), millis(hourAt)))

            if (parts.evening - nowMinute >= MIN_GAP_MINUTES) {
                add(SnoozePreset("Bu akşam", TimeText.time(parts.evening, use24), millis(atMinute(0, parts.evening))))
            }
            add(SnoozePreset("Yarın sabah", TimeText.time(parts.morning, use24), millis(atMinute(1, parts.morning))))
        }
    }
}
