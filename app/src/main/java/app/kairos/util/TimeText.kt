package app.kairos.util

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Türkçe saat ve gün metinleri. Ekranlar ve bildirimler aynı biçimi kullanır. */
object TimeText {
    val TR: Locale = Locale.forLanguageTag("tr-TR")

    /** 720 → "12:00"; 12 saat modunda "12:00 ÖS". */
    fun time(minuteOfDay: Int, use24Hour: Boolean = true): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        if (use24Hour) return "%02d:%02d".format(h, m)
        val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
        return "%d:%02d %s".format(h12, m, if (h < 12) "ÖÖ" else "ÖS")
    }

    /** "Bugün", "Yarın", "Dün", bu hafta içiyse gün adı ("Cuma"), değilse "20 Kasım". */
    fun day(date: LocalDate, today: LocalDate): String = when (date.toEpochDay() - today.toEpochDay()) {
        0L -> "Bugün"
        1L -> "Yarın"
        -1L -> "Dün"
        in 2L..6L -> date.dayOfWeek.getDisplayName(TextStyle.FULL, TR)
        else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, TR)}" +
            if (date.year != today.year) " ${date.year}" else ""
    }

    /** "Bugün, 12:00" */
    fun dayAndTime(date: LocalDate, minuteOfDay: Int, today: LocalDate, use24Hour: Boolean = true) =
        "${day(date, today)}, ${time(minuteOfDay, use24Hour)}"

    /**
     * "08:00'de", "09:00'da", "15:30'da", "13:00'te": saatin okunuşundaki son kelimeye göre
     * bulunma eki (ünlü uyumu ve sert ünsüz benzeşmesi).
     */
    fun timeWithLocative(minuteOfDay: Int, use24Hour: Boolean = true): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val spoken = if (m != 0) m else if (use24Hour) h else (h % 12).let { if (it == 0) 12 else it }
        val suffix = when (spoken % 10) {
            1, 2, 7, 8 -> "de"
            3, 4, 5 -> "te"
            6, 9 -> "da"
            else -> when (spoken) { 20, 50 -> "de"; 40 -> "ta"; else -> "da" } // 0, 10, 30: sıfır, on, otuz
        }
        return "${time(minuteOfDay, use24Hour)}'$suffix"
    }

    /** 10 → "10 dk", 60 → "1 saat", 90 → "1 sa 30 dk" */
    fun duration(minutes: Int): String = when {
        minutes < 60 -> "$minutes dk"
        minutes % 60 == 0 -> "${minutes / 60} saat"
        else -> "${minutes / 60} sa ${minutes % 60} dk"
    }
}
