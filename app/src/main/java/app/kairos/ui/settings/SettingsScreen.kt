package app.kairos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.kairos.data.settings.AppSettings
import app.kairos.data.settings.AudioChannel
import app.kairos.data.settings.DefaultTimeMode
import app.kairos.data.settings.SwipeAction
import app.kairos.data.settings.WhenNone
import app.kairos.data.settings.WhenSilentMode
import app.kairos.reliability.ReliabilityActions
import app.kairos.reliability.ReliabilityChecklist
import app.kairos.reliability.ReliabilityStatus
import app.kairos.speech.BriefText
import app.kairos.ui.components.Hairline
import app.kairos.ui.components.KairosIcons
import app.kairos.ui.components.Label
import app.kairos.ui.editor.ChoiceDialog
import app.kairos.ui.editor.EditorLogic
import app.kairos.ui.editor.TimeDialog
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.TimeStyles
import app.kairos.util.TimeText
import java.time.DayOfWeek

/** Açık olan pencere: hangi ayar değiştiriliyor. */
private sealed interface Edit {
    data class Time(val title: String, val minute: Int, val apply: (AppSettings, Int) -> AppSettings) : Edit
    data class Choice<T>(val title: String, val options: List<Pair<String, T>>, val selected: T, val apply: (AppSettings, T) -> AppSettings) : Edit
    data object BriefDays : Edit
}

/**
 * Ayarlar. Her davranışın bir anahtarı vardır (KURALLAR §1.1); opsiyonel her şey varsayılan kapalıdır.
 * Henüz çalışmayan bir özelliğin ayarı gösterilmez.
 *
 * @param onUpdate ayarı değiştirir: `onUpdate { it.copy(...) }`
 * @param onOpenSoundSettings Android'in bu uygulamaya ait bildirim sesi/titreşim ayarları
 * @param onPreviewBrief "Dinle": özet cümlesini şimdi sesli okur (Adım 8'de bağlanır; null ise düğme gizli)
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    contentPadding: PaddingValues,
    onOpenSoundSettings: () -> Unit = {},
    onPreviewBrief: ((String) -> Unit)? = null,
    appVersion: String = "",
    reliability: ReliabilityStatus? = null,
    reliabilityActions: ReliabilityActions = ReliabilityActions(),
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
) {
    val c = Kairos.colors
    val use24 = settings.appearance.use24Hour
    var edit by remember { mutableStateOf<Edit?>(null) }
    fun t(m: Int) = TimeText.time(m, use24)

    // Hatırlatmaları engelleyen bir durum varsa güvenilirlik bölümü en üstte, yoksa sonda durur.
    val reliabilityOnTop = reliability?.blocking == true
    val reliabilitySection: @Composable () -> Unit = {
        if (reliability != null) {
            Section("Güvenilirlik") { ReliabilityChecklist(reliability, reliabilityActions) }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item("title") {
            Text(
                "Ayarlar",
                style = MaterialTheme.typography.headlineLarge,
                color = c.ink,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp).semantics { heading() },
            )
        }
        if (reliabilityOnTop) item("reliability-top") { reliabilitySection() }

        // --- Günlük sesli özet ---
        item("brief") {
            val b = settings.brief
            Section("Günlük sesli özet") {
                SwitchRow(
                    "Her gün sesli özet",
                    "Seçtiğiniz saatte kaç hatırlatmanız olduğu söylenir. İçerik okunmaz.",
                    b.enabled,
                ) { on -> onUpdate { it.copy(brief = it.brief.copy(enabled = on)) } }
                if (b.enabled) {
                    Divider()
                    ValueRow("Saat", t(b.minute), timeValue = true) {
                        edit = Edit.Time("Özet saati", b.minute) { s, m -> s.copy(brief = s.brief.copy(minute = m)) }
                    }
                    Divider()
                    ValueRow("Günler", SettingsText.days(b.days)) { edit = Edit.BriefDays }
                    Divider()
                    SwitchRow("Hafta sonu farklı saat", null, b.weekendDifferent) { on ->
                        onUpdate { it.copy(brief = it.brief.copy(weekendDifferent = on)) }
                    }
                    if (b.weekendDifferent) {
                        Divider()
                        ValueRow("Hafta sonu saati", t(b.weekendMinute), timeValue = true) {
                            edit = Edit.Time("Hafta sonu saati", b.weekendMinute) { s, m -> s.copy(brief = s.brief.copy(weekendMinute = m)) }
                        }
                    }
                    Divider()
                    val sample = BriefText.sentence(4, 1, b.minute, b) ?: ""
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Duyacağınız cümle (örnek)", style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("“$sample”", style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
                            if (onPreviewBrief != null) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, c.line, RoundedCornerShape(10.dp))
                                        .clickable(role = Role.Button, onClickLabel = "Örneği dinle") { onPreviewBrief(sample) },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(KairosIcons.Play, contentDescription = "Örneği dinle", tint = c.ink, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                    Divider()
                    SwitchRow("Gecikmişleri de söyle", "Önceki günlerden kalanların sayısı eklenir.", b.includeOverdue) { on ->
                        onUpdate { it.copy(brief = it.brief.copy(includeOverdue = on)) }
                    }
                    Divider()
                    ValueRow("Hatırlatma yoksa", SettingsText.whenNone(b.whenNone)) {
                        edit = Edit.Choice("Hatırlatma yoksa", WhenNone.entries.map { SettingsText.whenNone(it) to it }, b.whenNone) { s, v ->
                            s.copy(brief = s.brief.copy(whenNone = v))
                        }
                    }
                    Divider()
                    ValueRow("Telefon sessizdeyken", SettingsText.whenSilent(b.whenSilentMode)) {
                        edit = Edit.Choice("Telefon sessizdeyken", WhenSilentMode.entries.map { SettingsText.whenSilent(it) to it }, b.whenSilentMode) { s, v ->
                            s.copy(brief = s.brief.copy(whenSilentMode = v))
                        }
                    }
                    Divider()
                    ValueRow("Ses", SettingsText.channel(b.channel)) {
                        edit = Edit.Choice("Hangi ses düzeyiyle okunsun?", AudioChannel.entries.map { SettingsText.channel(it) to it }, b.channel) { s, v ->
                            s.copy(brief = s.brief.copy(channel = v))
                        }
                    }
                    Divider()
                    ValueRow("Konuşma hızı", SettingsText.rate(b.speechRate)) {
                        edit = Edit.Choice("Konuşma hızı", SettingsText.rates.map { it.second to it.first }, SettingsText.rates.minBy { kotlin.math.abs(it.first - b.speechRate) }.first) { s, v ->
                            s.copy(brief = s.brief.copy(speechRate = v))
                        }
                    }
                }
            }
        }

        // --- Yeni hatırlatma ---
        item("new") {
            val n = settings.newReminder
            Section("Yeni hatırlatma") {
                ValueRow("Varsayılan saat", SettingsText.timeMode(n.timeMode)) {
                    edit = Edit.Choice("Yeni hatırlatmada saat", DefaultTimeMode.entries.map { SettingsText.timeMode(it) to it }, n.timeMode) { s, v ->
                        s.copy(newReminder = s.newReminder.copy(timeMode = v))
                    }
                }
                if (n.timeMode == DefaultTimeMode.FIXED) {
                    Divider()
                    ValueRow("Sabit saat", t(n.fixedMinute), timeValue = true) {
                        edit = Edit.Time("Sabit saat", n.fixedMinute) { s, m -> s.copy(newReminder = s.newReminder.copy(fixedMinute = m)) }
                    }
                }
                Divider()
                val p = n.dayParts
                listOf(
                    Triple("Sabah", p.morning) { s: AppSettings, m: Int -> s.copy(newReminder = s.newReminder.copy(dayParts = s.newReminder.dayParts.copy(morning = m))) },
                    Triple("Öğle", p.noon) { s: AppSettings, m: Int -> s.copy(newReminder = s.newReminder.copy(dayParts = s.newReminder.dayParts.copy(noon = m))) },
                    Triple("Akşam", p.evening) { s: AppSettings, m: Int -> s.copy(newReminder = s.newReminder.copy(dayParts = s.newReminder.dayParts.copy(evening = m))) },
                    Triple("Gece", p.night) { s: AppSettings, m: Int -> s.copy(newReminder = s.newReminder.copy(dayParts = s.newReminder.dayParts.copy(night = m))) },
                ).forEachIndexed { i, (name, minute, apply) ->
                    if (i > 0) Divider()
                    ValueRow(name, t(minute), timeValue = true) { edit = Edit.Time(name, minute, apply) }
                }
                Divider()
                ValueRow("Önceden hatırlat", EditorLogic.leadLabel(n.defaultLeadMinutes)) {
                    edit = Edit.Choice("Önceden hatırlat (varsayılan)", EditorLogic.leadOptions.map { EditorLogic.leadLabel(it) to it }, n.defaultLeadMinutes) { s, v ->
                        s.copy(newReminder = s.newReminder.copy(defaultLeadMinutes = v))
                    }
                }
            }
        }

        // --- Bildirimler ---
        item("notifications") {
            val n = settings.notifications
            Section("Bildirimler") {
                ValueRow("Erteleme süresi", TimeText.duration(n.snoozeMinutes)) {
                    edit = Edit.Choice("Erteleme süresi", SettingsText.snoozeOptions.map { TimeText.duration(it) to it }, n.snoozeMinutes) { s, v ->
                        s.copy(notifications = s.notifications.copy(snoozeMinutes = v))
                    }
                }
                Divider()
                SwitchRow("Sessiz saatler", "Bu aralıkta bildirimler ses çıkarmadan gelir.", n.quietHoursEnabled) { on ->
                    onUpdate { it.copy(notifications = it.notifications.copy(quietHoursEnabled = on)) }
                }
                if (n.quietHoursEnabled) {
                    Divider()
                    ValueRow("Başlangıç", t(n.quietStartMinute), timeValue = true) {
                        edit = Edit.Time("Sessiz saatler başlangıcı", n.quietStartMinute) { s, m -> s.copy(notifications = s.notifications.copy(quietStartMinute = m)) }
                    }
                    Divider()
                    ValueRow("Bitiş", t(n.quietEndMinute), timeValue = true) {
                        edit = Edit.Time("Sessiz saatler bitişi", n.quietEndMinute) { s, m -> s.copy(notifications = s.notifications.copy(quietEndMinute = m)) }
                    }
                }
                Divider()
                ValueRow("Ses ve titreşim", "Android ayarları", onClick = onOpenSoundSettings)
            }
        }

        // --- Liste ---
        item("list") {
            val l = settings.list
            Section("Liste") {
                ValueRow("Sağa kaydırınca", SettingsText.swipe(l.swipeRight)) {
                    edit = Edit.Choice("Sağa kaydırınca", SwipeAction.entries.map { SettingsText.swipe(it) to it }, l.swipeRight) { s, v ->
                        s.copy(list = s.list.copy(swipeRight = v))
                    }
                }
                Divider()
                ValueRow("Sola kaydırınca", SettingsText.swipe(l.swipeLeft)) {
                    edit = Edit.Choice("Sola kaydırınca", SwipeAction.entries.map { SettingsText.swipe(it) to it }, l.swipeLeft) { s, v ->
                        s.copy(list = s.list.copy(swipeLeft = v))
                    }
                }
                Divider()
                ValueRow("Tamamlananları sil", SettingsText.purge(l.purgeCompletedAfterDays)) {
                    edit = Edit.Choice("Tamamlananları sil", SettingsText.purgeOptions.map { SettingsText.purge(it) to it }, l.purgeCompletedAfterDays) { s, v ->
                        s.copy(list = s.list.copy(purgeCompletedAfterDays = v))
                    }
                }
            }
        }

        // --- Görünüm ---
        item("appearance") {
            val a = settings.appearance
            Section("Görünüm") {
                Column(Modifier.padding(16.dp)) {
                    ThemePicker(
                        a.family, a.mode,
                        onFamily = { f -> onUpdate { it.copy(appearance = it.appearance.copy(family = f)) } },
                        onMode = { m -> onUpdate { it.copy(appearance = it.appearance.copy(mode = m)) } },
                    )
                }
                Divider()
                ValueRow("Saat biçimi", if (a.use24Hour) "24 saat (14:30)" else "12 saat (2:30 ÖS)") {
                    edit = Edit.Choice("Saat biçimi", listOf("24 saat (14:30)" to true, "12 saat (2:30 ÖS)" to false), a.use24Hour) { s, v ->
                        s.copy(appearance = s.appearance.copy(use24Hour = v))
                    }
                }
                Divider()
                ValueRow("Haftanın ilk günü", SettingsText.dayName(a.firstDayOfWeek.value)) {
                    edit = Edit.Choice("Haftanın ilk günü", listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).map { SettingsText.dayName(it.value) to it }, a.firstDayOfWeek) { s, v ->
                        s.copy(appearance = s.appearance.copy(firstDayOfWeek = v))
                    }
                }
            }
        }

        if (!reliabilityOnTop) item("reliability-bottom") { reliabilitySection() }

        // --- Veri ---
        item("data") {
            Section("Veri") {
                ValueRow("Yedeği dışa aktar", "Dosyaya kaydet", onClick = onExport)
                Divider()
                ValueRow("Yedekten geri yükle", "Dosya seç", onClick = onImport)
            }
            Text(
                "Yedek; hatırlatmaları ve ayarları içeren bir dosyadır. Nereye kaydedileceğini siz seçersiniz; hiçbir sunucuya gönderilmez.",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )
        }

        // --- Hakkında ---
        item("about") {
            Section("Hakkında") {
                ValueRow("Sürüm", appVersion.ifEmpty { "—" }, onClick = null)
                Divider()
                ValueRow("Lisans", "Açık kaynak · GPL-3.0", onClick = null)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // --- Pencereler ---
    when (val e = edit) {
        is Edit.Time -> TimeDialog(e.minute, use24, onPick = { m -> onUpdate { e.apply(it, m) }; edit = null }, onDismiss = { edit = null })
        is Edit.Choice<*> -> ChoiceEdit(e, onUpdate) { edit = null }
        Edit.BriefDays -> DaysDialog(settings.brief.days, onPick = { d -> onUpdate { it.copy(brief = it.brief.copy(days = d)) }; edit = null }, onDismiss = { edit = null })
        null -> Unit
    }
}

@Composable
private fun <T> ChoiceEdit(e: Edit.Choice<T>, onUpdate: ((AppSettings) -> AppSettings) -> Unit, close: () -> Unit) {
    ChoiceDialog(e.title, e.options, e.selected, onPick = { v -> onUpdate { e.apply(it, v) }; close() }, onDismiss = close)
}

/** Bölüm: etiket ve altında ince çizgili kart. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Kairos.colors
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(title, modifier = Modifier.padding(start = 4.dp))
        Column(Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.line, shape), content = content)
    }
}

@Composable
private fun Divider() = Hairline(Modifier.padding(start = 16.dp))

@Composable
private fun ValueRow(title: String, value: String, timeValue: Boolean = false, onClick: (() -> Unit)?) {
    val c = Kairos.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
        Text(value, style = if (timeValue) TimeStyles.Value else MaterialTheme.typography.bodyMedium, color = if (timeValue) c.ink else c.inkMuted)
    }
}

/** Açma/kapama satırı. Tüm satır dokunulabilir; ekran okuyucu için "anahtar" rolündedir. */
@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Kairos.colors
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
        KairosSwitch(checked)
    }
}

/** Taslaktaki anahtar: açıkken ana renk dolu, kapalıyken ince çerçeveli. */
@Composable
private fun KairosSwitch(checked: Boolean) {
    val c = Kairos.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(48.dp, 28.dp)
            .clip(shape)
            .background(if (checked) c.primary else Color.Transparent)
            .border(1.5.dp, if (checked) c.primary else c.inkFaint, shape),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = if (checked) 23.dp else 4.dp)
                .size(if (checked) 22.dp else 18.dp)
                .clip(CircleShape)
                .background(if (checked) c.onPrimary else c.inkFaint),
        )
    }
}

/** Özet günleri: haftanın her günü ayrı işaretlenir. */
@Composable
private fun DaysDialog(initial: Set<Int>, onPick: (Set<Int>) -> Unit, onDismiss: () -> Unit) {
    val c = Kairos.colors
    var days by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        title = { Text("Özet hangi günler okunsun?", style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = {
            Column {
                (1..7).forEachIndexed { i, iso ->
                    if (i > 0) Hairline()
                    val on = iso in days
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .toggleable(on, role = Role.Checkbox) { days = if (it) days + iso else days - iso },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(SettingsText.dayName(iso), style = MaterialTheme.typography.bodyLarge, color = c.ink)
                        if (on) Icon(KairosIcons.Check, contentDescription = "Seçili", tint = c.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(days) }) { Text("Kaydet", color = c.ink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = c.inkMuted) } },
    )
}
