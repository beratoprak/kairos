package app.kairos.speech

import app.kairos.data.settings.DailyBrief
import app.kairos.data.settings.WhenSilentMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** Özet çaldığında ne yapılacak. */
enum class BriefAction { SPEAK, NOTIFY, NOTHING }

/** Günlük özetin ne zaman ve nasıl çalışacağına karar veren saf mantık. */
object BriefPlanner {

    /** O günün özet saati: hafta sonu ayrı saat açıksa cumartesi-pazar o saat. */
    fun minuteFor(date: LocalDate, brief: DailyBrief): Int =
        if (brief.weekendDifferent && date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) brief.weekendMinute
        else brief.minute

    /** [now]'dan sonraki ilk özet anı. Özet kapalıysa ya da hiç gün seçilmediyse null. */
    fun nextAt(now: LocalDateTime, brief: DailyBrief): LocalDateTime? {
        if (!brief.enabled || brief.days.isEmpty()) return null
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek.value !in brief.days) continue
            val m = minuteFor(date, brief)
            val at = date.atTime(m / 60, m % 60)
            if (at.isAfter(now)) return at
        }
        return null
    }

    /**
     * @param sentence okunacak cümle; null ise söylenecek bir şey yok ("Sessiz kal")
     * @param phoneSilent telefon sessiz/titreşimde ya da Rahatsız Etmeyin açık
     */
    fun decide(sentence: String?, phoneSilent: Boolean, whenSilent: WhenSilentMode): BriefAction = when {
        sentence == null -> BriefAction.NOTHING
        !phoneSilent -> BriefAction.SPEAK
        whenSilent == WhenSilentMode.SPEAK_ANYWAY -> BriefAction.SPEAK
        whenSilent == WhenSilentMode.NOTIFY_ONLY -> BriefAction.NOTIFY
        else -> BriefAction.NOTHING
    }
}
