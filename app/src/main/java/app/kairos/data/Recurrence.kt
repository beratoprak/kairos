// Bu dosya bikram-agarwal/Remember (80d099c) projesindeki
// app/src/main/java/dev/bikram/remember/data/Recurrence.kt dosyasından uyarlanmıştır.
// Orijinal: Copyright (C) Remember yazarları — GPL-3.0
//
// KAİROS değişiklikleri:
// - Milisaniye yerine tarih (LocalDate) üzerinde çalışır; saat ayrı tutulduğu için yaz saati ve
//   saat dilimi değişimleri hatırlatma saatini kaydırmaz.
// - Saatlik tekrar (HOUR) çıkarıldı: KAİROS aralıklı "her X dakikada bir" hatırlatıcısı değildir.
// - Haftanın günleri java.util.Calendar yerine ISO DayOfWeek (Pazartesi = 1) ile tutulur.
// - Bitiş tarihi epoch milisaniye yerine epoch gün olarak tutulur.
package app.kairos.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Serializable
enum class RecurrenceUnit { DAY, WEEK, MONTH, YEAR }

@Serializable
enum class RecurrenceEndKind { NEVER, ON_DATE, AFTER_COUNT }

/** Aylık tekrarın alt kuralı: ayın belirli günü ya da ayın N'inci haftanın-günü. */
@Serializable
sealed class MonthlyMode {
    /** Ayın [day]'inci günü (1..31). Ay daha kısaysa ayın son günü kullanılır. */
    @Serializable
    @SerialName("day_of_month")
    data class ByDayOfMonth(val day: Int) : MonthlyMode()

    /**
     * Ayın [ordinal]'inci [isoWeekday]'i. ordinal: 1..4 = birinci..dördüncü, 5 = son.
     * isoWeekday: 1 = Pazartesi … 7 = Pazar.
     */
    @Serializable
    @SerialName("nth_weekday")
    data class ByNthWeekday(val ordinal: Int, val isoWeekday: Int) : MonthlyMode()
}

/**
 * Bir hatırlatmanın tekrar kuralı. Hatırlatmada kural yoksa (null) hatırlatma tekrarlamaz;
 * bu varsayılandır (KURALLAR §1.3).
 */
@Serializable
data class RecurrenceRule(
    val unit: RecurrenceUnit,
    val interval: Int = 1,
    /** Sadece WEEK için. ISO gün numaraları (1 = Pazartesi). Boşsa ilk tarihin günü. */
    val daysOfWeek: Set<Int> = emptySet(),
    /** Sadece MONTH için. Null ise ilk tarihin ay günü. */
    val monthlyMode: MonthlyMode? = null,
    val endKind: RecurrenceEndKind = RecurrenceEndKind.NEVER,
    /** ON_DATE için son geçerli gün (epoch gün, dahil). */
    val endEpochDay: Long? = null,
    /** AFTER_COUNT için kalan tekrar sayısı; her tamamlamada bir azalır. */
    val endCount: Int? = null,
) {
    /**
     * [after] tarihinden SONRAKİ ilk tekrar tarihi. Kural tükendiyse null.
     * Sayıya göre bitiş repository'de, tamamlama sırasında [afterFire] ile işlenir.
     */
    fun nextDateAfter(after: LocalDate): LocalDate? {
        if (interval < 1) return null
        if (endKind == RecurrenceEndKind.AFTER_COUNT && (endCount ?: 0) <= 0) return null
        val next: LocalDate = when (unit) {
            RecurrenceUnit.DAY -> after.plusDays(interval.toLong())
            RecurrenceUnit.WEEK ->
                if (daysOfWeek.isEmpty()) after.plusWeeks(interval.toLong())
                else nextWeeklyWithDays(after) ?: return null
            RecurrenceUnit.MONTH -> when (val mode = monthlyMode) {
                null -> after.plusMonths(interval.toLong())
                is MonthlyMode.ByDayOfMonth -> {
                    if (mode.day !in 1..31) return null
                    val month = after.plusMonths(interval.toLong())
                    month.withDayOfMonth(minOf(mode.day, month.lengthOfMonth()))
                }
                is MonthlyMode.ByNthWeekday -> {
                    if (mode.ordinal !in 1..5 || mode.isoWeekday !in 1..7) return null
                    nthWeekdayOf(after.plusMonths(interval.toLong()), mode.ordinal, DayOfWeek.of(mode.isoWeekday))
                }
            }
            RecurrenceUnit.YEAR -> after.plusYears(interval.toLong())
        }
        if (!next.isAfter(after)) return null
        if (endKind == RecurrenceEndKind.ON_DATE && endEpochDay != null && next.toEpochDay() > endEpochDay) return null
        return next
    }

    /**
     * Seçili haftanın günleri için bir sonraki tarih. [interval] ISO haftaları sayar:
     * [after]'ın haftası 0'dır; aynı hafta içindeki sonraki seçili gün de geçerlidir.
     */
    private fun nextWeeklyWithDays(after: LocalDate): LocalDate? {
        if (daysOfWeek.any { it !in 1..7 }) return null
        val monday = after.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return daysOfWeek.map { iso ->
            var candidate = monday.plusDays((iso - 1).toLong())
            if (!candidate.isAfter(after)) candidate = candidate.plusWeeks(interval.toLong())
            candidate
        }.minOrNull()
    }

    /** AFTER_COUNT için sayaç olmadan kayıt gelirse sonsuz tekrara döner (tek seferlik sanılmaz). */
    fun sanitized(): RecurrenceRule =
        if (endKind == RecurrenceEndKind.AFTER_COUNT && endCount == null) copy(endKind = RecurrenceEndKind.NEVER)
        else this

    /** Bir tekrar tamamlandıktan sonraki kural (sayaç varsa bir azalır). */
    fun afterFire(): RecurrenceRule =
        if (endKind == RecurrenceEndKind.AFTER_COUNT && endCount != null) copy(endCount = (endCount - 1).coerceAtLeast(0))
        else this

    companion object {
        private val json = Json {
            classDiscriminator = "type"
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        fun toJson(rule: RecurrenceRule?): String? = rule?.let { json.encodeToString(serializer(), it.sanitized()) }

        fun fromJson(value: String?): RecurrenceRule? {
            if (value.isNullOrBlank()) return null
            return runCatching { json.decodeFromString(serializer(), value).sanitized() }.getOrNull()
        }
    }
}

/** [monthDate]'in ayındaki [ordinal]'inci [weekday] (5 = son). */
private fun nthWeekdayOf(monthDate: LocalDate, ordinal: Int, weekday: DayOfWeek): LocalDate =
    if (ordinal == 5) monthDate.with(TemporalAdjusters.lastInMonth(weekday))
    else monthDate.with(TemporalAdjusters.dayOfWeekInMonth(ordinal, weekday))
