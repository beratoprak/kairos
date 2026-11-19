package app.kairos.ui.settings

import app.kairos.data.settings.AudioChannel
import app.kairos.data.settings.DefaultTimeMode
import app.kairos.data.settings.SwipeAction
import app.kairos.data.settings.WhenNone
import app.kairos.data.settings.WhenSilentMode
import app.kairos.util.TimeText
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Ayarlar ekranındaki seçeneklerin Türkçe adları. Tek yerde; ekran ve testler aynısını kullanır. */
object SettingsText {

    fun days(days: Set<Int>): String = when (days) {
        (1..7).toSet() -> "Her gün"
        (1..5).toSet() -> "Hafta içi"
        setOf(6, 7) -> "Hafta sonu"
        emptySet<Int>() -> "Hiçbir gün"
        else -> days.sorted().joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, TimeText.TR) }
    }

    fun dayName(iso: Int): String = DayOfWeek.of(iso).getDisplayName(TextStyle.FULL, TimeText.TR)

    fun whenNone(v: WhenNone) = when (v) {
        WhenNone.STAY_SILENT -> "Sessiz kal"
        WhenNone.SAY_NONE -> "“Hatırlatmanız yok” de"
    }

    fun whenSilent(v: WhenSilentMode) = when (v) {
        WhenSilentMode.NOTIFY_ONLY -> "Sadece bildirim"
        WhenSilentMode.SPEAK_ANYWAY -> "Yine de konuş"
        WhenSilentMode.SKIP -> "Hiçbir şey yapma"
    }

    fun channel(v: AudioChannel) = when (v) {
        AudioChannel.NOTIFICATION -> "Bildirim sesi"
        AudioChannel.MEDIA -> "Medya sesi"
        AudioChannel.ALARM -> "Alarm sesi"
    }

    val rates = listOf(0.8f to "Yavaş", 1.0f to "Normal", 1.25f to "Hızlı")
    fun rate(v: Float) = rates.minByOrNull { kotlin.math.abs(it.first - v) }!!.second

    fun timeMode(v: DefaultTimeMode) = when (v) {
        DefaultTimeMode.LAST_USED -> "Son kullanılan"
        DefaultTimeMode.FIXED -> "Sabit saat"
    }

    fun swipe(v: SwipeAction) = when (v) {
        SwipeAction.COMPLETE -> "Tamamla"
        SwipeAction.SNOOZE -> "Ertele"
        SwipeAction.DELETE -> "Sil"
        SwipeAction.NONE -> "Hiçbir şey"
    }

    fun purge(days: Int?) = if (days == null) "Hiçbir zaman" else "$days gün sonra"
    val purgeOptions: List<Int?> = listOf(null, 7, 30, 90)

    val snoozeOptions = listOf(5, 10, 15, 30, 60)
}
