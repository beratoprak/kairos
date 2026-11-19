package app.kairos.data

import app.kairos.data.settings.DefaultTimeMode
import app.kairos.data.settings.NewReminder
import java.time.LocalDate
import java.time.LocalDateTime

/** Ekleme sayfasının açılışta göstereceği gün ve saat. */
data class SuggestedSlot(val date: LocalDate, val minute: Int)

/**
 * Yeni hatırlatma için akıllı varsayılan (KURALLAR §1.7).
 *
 * 1. Saat: "Son kullanılan" modunda son kaydedilen saat (hiç yoksa sabah dilimi);
 *    "Sabit saat" modunda ayarlardaki saat.
 * 2. Seçilen gün bugünse ve o saat geçmişse bir sonraki tam saat önerilir.
 * 3. Bugün için tam saat kalmadıysa (23:00'ten sonra) gün yarına geçer, saat korunur.
 */
object TimeSuggestion {

    fun preferredMinute(settings: NewReminder): Int = when (settings.timeMode) {
        DefaultTimeMode.LAST_USED -> settings.lastUsedMinute ?: settings.dayParts.morning
        DefaultTimeMode.FIXED -> settings.fixedMinute
    }

    fun suggest(date: LocalDate, now: LocalDateTime, settings: NewReminder): SuggestedSlot {
        val preferred = preferredMinute(settings)
        val today = now.toLocalDate()
        if (date != today) return SuggestedSlot(date, preferred)

        val nowMinute = now.hour * 60 + now.minute
        if (preferred > nowMinute) return SuggestedSlot(date, preferred)

        val nextFullHour = (now.hour + 1) * 60
        return if (nextFullHour < Reminder.MINUTES_PER_DAY) SuggestedSlot(date, nextFullHour)
        else SuggestedSlot(date.plusDays(1), preferred)
    }
}
