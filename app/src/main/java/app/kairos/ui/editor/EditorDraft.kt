package app.kairos.ui.editor

import app.kairos.data.MonthlyMode
import app.kairos.data.RecurrenceRule
import app.kairos.data.RecurrenceUnit
import app.kairos.data.Reminder
import app.kairos.data.TimeSuggestion
import app.kairos.data.settings.AppSettings
import app.kairos.util.RecurrenceText
import app.kairos.util.TimeText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

/**
 * Ekleme/düzenleme sayfasının durumu. Değişmez veri; her dokunuş yeni bir taslak üretir.
 * Opsiyonel her şey varsayılan kapalıdır (KURALLAR §1.2–1.3).
 */
data class EditorDraft(
    /** 0 = yeni hatırlatma. */
    val id: Long = 0,
    val title: String = "",
    val date: LocalDate,
    val minute: Int,
    val recurrence: RecurrenceRule? = null,
    val leadMinutes: Int? = null,
    val important: Boolean = false,
    /** Kullanıcı saate dokundu mu? Dokunmadıysa gün değişince saat yeniden önerilir. */
    val timeTouched: Boolean = false,
    /** Açılışta önerilen saat "son seçtiğiniz saat" mi? Saatin yanındaki açıklama için. */
    val fromLastUsed: Boolean = false,
    /** Düzenlenen kaydın kendisi (kaydederken diğer alanları korumak için). */
    val original: Reminder? = null,
) {
    val isNew get() = id == 0L
    val canSave get() = title.isNotBlank()
}

/** Gün kısayolu: "Bugün", "Yarın", "Cumartesi". */
data class DayChip(val label: String, val date: LocalDate)

/** Tekrar seçenekleri listesindeki bir satır. */
data class RepeatOption(val label: String, val rule: RecurrenceRule?)

object EditorLogic {

    /** Yeni hatırlatma: son kullanılan saat (ya da sabah dilimi), gün bugün; saat geçtiyse sonraki tam saat. */
    fun newDraft(now: LocalDateTime, settings: AppSettings): EditorDraft {
        val slot = TimeSuggestion.suggest(now.toLocalDate(), now, settings.newReminder)
        val fromLast = settings.newReminder.lastUsedMinute != null && slot.minute == TimeSuggestion.preferredMinute(settings.newReminder)
        return EditorDraft(
            date = slot.date,
            minute = slot.minute,
            leadMinutes = settings.newReminder.defaultLeadMinutes,
            fromLastUsed = fromLast,
        )
    }

    fun editDraft(r: Reminder) = EditorDraft(
        id = r.id, title = r.title, date = r.dueDate, minute = r.dueMinute, recurrence = r.recurrence,
        leadMinutes = r.leadMinutes, important = r.important, timeTouched = true, original = r,
    )

    /** Gün değişti. Saate dokunulmadıysa öneri yeniden hesaplanır; dokunulduysa sadece geçmişe düşmesi engellenir. */
    fun withDate(d: EditorDraft, date: LocalDate, now: LocalDateTime, settings: AppSettings): EditorDraft {
        if (!d.timeTouched) {
            val slot = TimeSuggestion.suggest(date, now, settings.newReminder)
            return d.copy(date = slot.date, minute = slot.minute, fromLastUsed = d.fromLastUsed && slot.minute == d.minute)
        }
        val nowMinute = now.hour * 60 + now.minute
        if (date == now.toLocalDate() && d.minute <= nowMinute) {
            val next = (now.hour + 1) * 60
            return if (next < Reminder.MINUTES_PER_DAY) d.copy(date = date, minute = next) else d.copy(date = date.plusDays(1))
        }
        return d.copy(date = date)
    }

    fun withMinute(d: EditorDraft, minute: Int) =
        d.copy(minute = minute.coerceIn(0, Reminder.MINUTES_PER_DAY - 1), timeTouched = true, fromLastUsed = false)

    /** Seçilen gün/saat geçmişte mi? Kaydet düğmesinin altında uyarı için. */
    fun isInPast(d: EditorDraft, now: LocalDateTime) = d.date.atTime(d.minute / 60, d.minute % 60) <= now

    /**
     * Gün çipleri: Bugün, Yarın ve üçüncü olarak hafta içiyse "Cumartesi" (hafta sonu),
     * cuma–pazar arasıysa gelecek "Pazartesi".
     */
    fun dayChips(today: LocalDate): List<DayChip> {
        val third = when (today.dayOfWeek) {
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            else -> today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
        }
        return listOf(
            DayChip("Bugün", today),
            DayChip("Yarın", today.plusDays(1)),
            DayChip(third.dayOfWeek.getDisplayName(TextStyle.FULL, TimeText.TR), third),
        )
    }

    /** Hazır tekrar seçenekleri; ilki her zaman "Tekrar yok". Aylıkta gün her zaman sabitlenir (kayma olmasın). */
    fun repeatOptions(date: LocalDate): List<RepeatOption> {
        val weekly = RecurrenceRule(RecurrenceUnit.WEEK, daysOfWeek = setOf(date.dayOfWeek.value))
        val monthly = RecurrenceRule(RecurrenceUnit.MONTH, monthlyMode = MonthlyMode.ByDayOfMonth(date.dayOfMonth))
        val rules = listOf(
            RecurrenceRule(RecurrenceUnit.DAY),
            RecurrenceRule(RecurrenceUnit.WEEK, daysOfWeek = (1..5).toSet()),
            weekly,
            monthly,
            RecurrenceRule(RecurrenceUnit.YEAR),
        )
        return listOf(RepeatOption("Tekrar yok", null)) + rules.map { RepeatOption(RecurrenceText.describe(it, date), it) }
    }

    fun repeatLabel(d: EditorDraft) = d.recurrence?.let { RecurrenceText.describe(it, d.date) } ?: "Yok"

    val leadOptions: List<Int?> = listOf(null, 5, 15, 30, 60, 24 * 60)

    fun leadLabel(minutes: Int?) = when (minutes) {
        null -> "Yok"
        24 * 60 -> "1 gün önce"
        else -> "${TimeText.duration(minutes)} önce"
    }

    /** Büyük saatin altındaki tarih: "Perşembe, 20 Kasım". */
    fun longDate(date: LocalDate) =
        "${date.dayOfWeek.getDisplayName(TextStyle.FULL, TimeText.TR)}, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, TimeText.TR)}"
}
