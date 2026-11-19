package app.kairos.ui.home

import app.kairos.data.Reminder
import app.kairos.data.ReminderBuckets
import app.kairos.data.settings.AppSettings
import app.kairos.data.settings.SwipeAction
import app.kairos.util.RecurrenceText
import app.kairos.util.TimeText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle

/** Listede tek satır. Ekranlar sadece bunu çizer; hesap [HomeUiBuilder]'dadır. */
data class RowUi(
    val id: Long,
    val title: String,
    /** Saat sütunu ("12:00"). */
    val time: String,
    /** Başlığın altındaki açıklama ("Sıradaki · 38 dk sonra", "Her perşembe"). */
    val meta: String? = null,
    val isNext: Boolean = false,
    val isDone: Boolean = false,
    val important: Boolean = false,
    val overdue: Boolean = false,
)

data class SectionUi(val title: String, val rows: List<RowUi>)

data class TodayUi(
    /** "ÇARŞAMBA, 19 KASIM" için kaynak metin (büyük harfe ekran çevirir). */
    val dateLabel: String,
    val summary: String,
    val overdue: List<RowUi>,
    val today: List<RowUi>,
) {
    val isEmpty get() = overdue.isEmpty() && today.isEmpty()
}

data class HomeUi(
    val today: TodayUi,
    val upcoming: List<SectionUi>,
    val completed: List<RowUi>,
    val swipeRight: SwipeAction = SwipeAction.COMPLETE,
    val swipeLeft: SwipeAction = SwipeAction.SNOOZE,
)

/** Veritabanı kayıtlarını ekran satırlarına çevirir. Saf fonksiyon; birim testle doğrulanır. */
object HomeUiBuilder {

    fun build(
        open: List<Reminder>,
        completed: List<Reminder>,
        settings: AppSettings,
        now: LocalDateTime,
        zone: ZoneId,
    ): HomeUi {
        val use24 = settings.appearance.use24Hour
        val today = now.toLocalDate()
        val nowMinute = now.hour * 60 + now.minute
        val nowMillis = now.atZone(zone).toInstant().toEpochMilli()
        val b = ReminderBuckets.split(open, today, nowMinute, nowMillis, settings.appearance.firstDayOfWeek)

        fun recurrenceOf(r: Reminder) = r.recurrence?.let { RecurrenceText.describe(it, r.dueDate) }
        fun snoozeOf(r: Reminder) = r.snoozedUntil?.takeIf { it > nowMillis }?.let {
            val t = Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
            "Ertelendi · ${TimeText.time(t.hour * 60 + t.minute, use24)}"
        }
        fun join(vararg parts: String?) = parts.filterNotNull().joinToString(" · ").ifEmpty { null }

        // --- Bugün ---
        val nextId = b.today.firstOrNull()?.id
        val openToday = b.today.map { r ->
            val isNext = r.id == nextId
            val until = if (isNext) "Sıradaki · ${TimeText.duration((r.dueMinute - nowMinute).coerceAtLeast(1))} sonra" else null
            RowUi(
                id = r.id, title = r.title, time = TimeText.time(r.dueMinute, use24),
                meta = join(snoozeOf(r) ?: until, recurrenceOf(r)),
                isNext = isNext, important = r.important,
            )
        }
        val doneToday = completed
            .filter { it.dueEpochDay == today.toEpochDay() && it.completedAt?.let { c -> dayOf(c, zone) } == today }
            .map { r -> RowUi(id = r.id, title = r.title, time = TimeText.time(r.dueMinute, use24), isDone = true) }
        // Saat sırasıyla; aynı saatte tamamlanan önce.
        val todayRows = (openToday + doneToday).sortedWith(compareBy({ it.time.toSortKey() }, { !it.isDone }))

        val overdueRows = b.overdue.map { r ->
            RowUi(
                id = r.id, title = r.title, time = TimeText.time(r.dueMinute, use24),
                meta = join(TimeText.dayAndTime(r.dueDate, r.dueMinute, today, use24), recurrenceOf(r)),
                important = r.important, overdue = true,
            )
        }

        val summary = when {
            openToday.isEmpty() && overdueRows.isEmpty() && doneToday.isEmpty() -> "Bugün için hatırlatma yok"
            openToday.isEmpty() && overdueRows.isEmpty() -> "Bugünkü hatırlatmalar tamamlandı"
            else -> buildString {
                append("${openToday.size} hatırlatma")
                if (overdueRows.isNotEmpty()) append(" · ${overdueRows.size} gecikmiş")
            }
        }
        val dateLabel = "${today.dayOfWeek.getDisplayName(TextStyle.FULL, TimeText.TR)}, " +
            "${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.FULL, TimeText.TR)}"

        // --- Yakında ---
        fun upcomingRow(r: Reminder, showDay: Boolean) = RowUi(
            id = r.id, title = r.title, time = TimeText.time(r.dueMinute, use24),
            meta = join(if (showDay) TimeText.day(r.dueDate, today) else null, snoozeOf(r), recurrenceOf(r)),
            important = r.important,
        )
        val upcoming = listOfNotNull(
            b.tomorrow.takeIf { it.isNotEmpty() }?.let { SectionUi("Yarın", it.map { r -> upcomingRow(r, false) }) },
            b.thisWeek.takeIf { it.isNotEmpty() }?.let { SectionUi("Bu hafta", it.map { r -> upcomingRow(r, true) }) },
            b.later.takeIf { it.isNotEmpty() }?.let { SectionUi("Daha sonra", it.map { r -> upcomingRow(r, true) }) },
        )

        // --- Tamamlanan ---
        val completedRows = completed.map { r ->
            val at = r.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
            RowUi(
                id = r.id, title = r.title, time = TimeText.time(r.dueMinute, use24), isDone = true,
                meta = at?.let { "Tamamlandı · ${TimeText.dayAndTime(it.toLocalDate(), it.hour * 60 + it.minute, today, use24)}" },
            )
        }

        return HomeUi(
            today = TodayUi(dateLabel, summary, overdueRows, todayRows),
            upcoming = upcoming,
            completed = completedRows,
            swipeRight = settings.list.swipeRight,
            swipeLeft = settings.list.swipeLeft,
        )
    }

    private fun dayOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** "9:05 ÖÖ" gibi 12 saat metinlerini de doğru sıralamak için. */
    private fun String.toSortKey(): Int {
        val pm = endsWith("ÖS"); val am = endsWith("ÖÖ")
        val (h, m) = substringBefore(' ').split(':').map { it.toInt() }
        val h24 = when { pm && h != 12 -> h + 12; am && h == 12 -> 0; else -> h }
        return h24 * 60 + m
    }
}
