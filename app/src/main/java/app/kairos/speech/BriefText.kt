package app.kairos.speech

import app.kairos.data.settings.DailyBrief
import app.kairos.data.settings.WhenNone

/**
 * Günlük sesli özetin cümlesi (KURALLAR §1.5): sadece sayı söylenir, hatırlatma içeriği asla okunmaz.
 * Saf fonksiyon; Ayarlar'daki önizleme ve sesli okuma aynı cümleyi kullanır.
 */
object BriefText {

    /**
     * @return okunacak cümle; söylenecek bir şey yoksa ve kullanıcı "Sessiz kal" seçtiyse null.
     */
    fun sentence(todayCount: Int, overdueCount: Int, minuteOfDay: Int, brief: DailyBrief): String? {
        val overdue = if (brief.includeOverdue) overdueCount else 0
        if (todayCount == 0 && overdue == 0 && brief.whenNone == WhenNone.STAY_SILENT) return null

        return buildString {
            append(greeting(minuteOfDay)).append(". ")
            append(if (todayCount == 0) "Bugün hatırlatmanız yok." else "Bugün $todayCount hatırlatmanız var.")
            if (overdue > 0) append(" Ayrıca önceki günlerden $overdue hatırlatmanız var.")
        }
    }

    fun greeting(minuteOfDay: Int) = when (minuteOfDay / 60) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        else -> "İyi akşamlar"
    }
}
