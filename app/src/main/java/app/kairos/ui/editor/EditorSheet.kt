package app.kairos.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.kairos.data.settings.AppSettings
import app.kairos.ui.components.Hairline
import app.kairos.ui.components.KairosIcons
import app.kairos.ui.components.Label
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.TimeStyles
import app.kairos.util.TimeText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Sayfanın içinde açılan seçim pencereleri. */
private enum class Picker { NONE, DATE, TIME, REPEAT, LEAD }

/**
 * Hatırlatma ekleme/düzenleme alt sayfası. Durumu çağıran tutar ([draft]); sayfa sadece
 * değişiklikleri bildirir. Böylece içerik ekran görüntüsü testinde tek başına çizilebilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(
    draft: EditorDraft,
    settings: AppSettings,
    now: LocalDateTime,
    onChange: (EditorDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Kairos.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = c.scrim.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp,
        dragHandle = { Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.line)) },
    ) {
        EditorContent(draft, settings, now, onChange, onSave, onDelete, onDismiss, autoFocus = draft.isNew)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorContent(
    draft: EditorDraft,
    settings: AppSettings,
    now: LocalDateTime,
    onChange: (EditorDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    autoFocus: Boolean = false,
) {
    val c = Kairos.colors
    val use24 = settings.appearance.use24Hour
    val today = now.toLocalDate()
    var picker by remember { mutableStateOf(Picker.NONE) }
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // --- Metin ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label("Ne hatırlatılsın?")
            BasicTextField(
                value = draft.title,
                onValueChange = { onChange(draft.copy(title = it)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (draft.canSave) onSave() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Hatırlatma metni" },
                decorationBox = { inner ->
                    Column {
                        Box(Modifier.padding(vertical = 8.dp)) {
                            if (draft.title.isEmpty()) Text("Örneğin: Sözleşmeyi hukuka ilet", style = MaterialTheme.typography.titleLarge, color = c.inkFaint)
                            inner()
                        }
                        Hairline(color = c.ink, thickness = 1.5.dp)
                    }
                },
            )
        }

        // --- Gün ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("Gün")
            val chips = EditorLogic.dayChips(today)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { chip ->
                    Chip(chip.label, selected = draft.date == chip.date) { onChange(EditorLogic.withDate(draft, chip.date, now, settings)) }
                }
                val other = chips.none { it.date == draft.date }
                Chip(if (other) TimeText.day(draft.date, today) else "Tarih", selected = other, icon = true) { picker = Picker.DATE }
            }
        }

        // --- Saat ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Label("Saat")
                if (draft.fromLastUsed) Text("Son seçtiğiniz saat", style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    TimeText.time(draft.minute, use24),
                    style = TimeStyles.Display,
                    color = c.ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClickLabel = "Saati yazarak seç") { picker = Picker.TIME },
                )
                Text(EditorLogic.longDate(draft.date), style = MaterialTheme.typography.bodyMedium, color = c.inkMuted, modifier = Modifier.padding(bottom = 10.dp))
            }
            TimeRuler(draft.minute, { onChange(EditorLogic.withMinute(draft, it)) }, use24)
            val parts = settings.newReminder.dayParts
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sabah" to parts.morning, "Öğle" to parts.noon, "Akşam" to parts.evening).forEach { (name, m) ->
                    Chip("$name ${TimeText.time(m, use24)}", selected = draft.minute == m, modifier = Modifier.weight(1f)) {
                        onChange(EditorLogic.withMinute(draft, m))
                    }
                }
            }
            if (EditorLogic.isInPast(draft, now)) {
                Text("Bu zaman geçti. Kaydederseniz gecikmiş olarak görünür.", style = MaterialTheme.typography.bodySmall, color = c.overdue)
            }
        }

        // --- Seçenekler (hepsi varsayılan kapalı) ---
        Column {
            Hairline()
            OptionRow("Tekrar", EditorLogic.repeatLabel(draft)) { picker = Picker.REPEAT }
            Hairline()
            OptionRow("Önceden hatırlat", EditorLogic.leadLabel(draft.leadMinutes)) { picker = Picker.LEAD }
            Hairline()
            OptionRow("Önem", if (draft.important) "Önemli" else "Normal") { onChange(draft.copy(important = !draft.important)) }
            Hairline()
        }

        // --- Düğmeler ---
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (draft.isNew) {
                SecondaryButton("Vazgeç", onCancel)
            } else {
                SecondaryButton("Sil", onDelete, color = c.overdue)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (draft.canSave) c.primary else c.line)
                    .clickable(enabled = draft.canSave, role = Role.Button, onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                Text("Kaydet", style = MaterialTheme.typography.labelLarge, color = if (draft.canSave) c.onPrimary else c.inkMuted)
            }
        }
    }

    when (picker) {
        Picker.DATE -> DateDialog(draft.date, today, onPick = { onChange(EditorLogic.withDate(draft, it, now, settings)); picker = Picker.NONE }, onDismiss = { picker = Picker.NONE })
        Picker.TIME -> TimeDialog(draft.minute, use24, onPick = { onChange(EditorLogic.withMinute(draft, it)); picker = Picker.NONE }, onDismiss = { picker = Picker.NONE })
        Picker.REPEAT -> ChoiceDialog(
            title = "Tekrar",
            options = EditorLogic.repeatOptions(draft.date).map { it.label to it.rule },
            selected = draft.recurrence,
            onPick = { onChange(draft.copy(recurrence = it)); picker = Picker.NONE },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.LEAD -> ChoiceDialog(
            title = "Önceden hatırlat",
            options = EditorLogic.leadOptions.map { EditorLogic.leadLabel(it) to it },
            selected = draft.leadMinutes,
            onPick = { onChange(draft.copy(leadMinutes = it)); picker = Picker.NONE },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.NONE -> Unit
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, icon: Boolean = false, onClick: () -> Unit) {
    val c = Kairos.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) c.primary else Color.Transparent)
            .border(1.dp, if (selected) c.primary else c.line, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon) Icon(KairosIcons.Upcoming, contentDescription = null, tint = if (selected) c.onPrimary else c.ink, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) c.onPrimary else c.ink, maxLines = 1)
    }
}

@Composable
private fun OptionRow(label: String, value: String, onClick: () -> Unit) {
    val c = Kairos.colors
    Row(
        Modifier.fillMaxWidth().height(52.dp).clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = c.ink)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, color: Color = Kairos.colors.ink) {
    val c = Kairos.colors
    Box(
        Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = color) }
}

/** Seçeneklerden birini seçtiren sade pencere (Tekrar, Önceden hatırlat). */
@Composable
internal fun <T> ChoiceDialog(title: String, options: List<Pair<String, T>>, selected: T, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    val c = Kairos.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = {
            Column {
                options.forEachIndexed { i, (label, value) ->
                    if (i > 0) Hairline()
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clickable(role = Role.RadioButton) { onPick(value) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = c.ink)
                        if (value == selected) Icon(KairosIcons.Check, contentDescription = "Seçili", tint = c.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat", color = c.ink) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = Kairos.colors
    val toUtc = { d: LocalDate -> d.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = toUtc(initial),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= toUtc(today)
            override fun isSelectableYear(year: Int) = year >= today.year
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = c.surface,
        titleContentColor = c.inkMuted,
        headlineContentColor = c.ink,
        weekdayContentColor = c.inkMuted,
        dayContentColor = c.ink,
        disabledDayContentColor = c.inkFaint,
        selectedDayContainerColor = c.primary,
        selectedDayContentColor = c.onPrimary,
        todayContentColor = c.ink,
        todayDateBorderColor = c.accent,
        yearContentColor = c.ink,
        selectedYearContainerColor = c.primary,
        selectedYearContentColor = c.onPrimary,
        dividerColor = c.line,
        navigationContentColor = c.ink,
        subheadContentColor = c.inkMuted,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        tonalElevation = 0.dp,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text("Seç", color = c.ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = c.inkMuted) } },
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false, title = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDialog(initial: Int, use24: Boolean, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Kairos.colors
    val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = use24)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        title = { Text("Saat", style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.ground,
                    clockDialSelectedContentColor = c.onPrimary,
                    clockDialUnselectedContentColor = c.ink,
                    selectorColor = c.primary,
                    containerColor = c.surface,
                    periodSelectorBorderColor = c.line,
                    periodSelectorSelectedContainerColor = c.primary,
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = c.onPrimary,
                    periodSelectorUnselectedContentColor = c.ink,
                    timeSelectorSelectedContainerColor = c.primary,
                    timeSelectorUnselectedContainerColor = c.ground,
                    timeSelectorSelectedContentColor = c.onPrimary,
                    timeSelectorUnselectedContentColor = c.ink,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text("Seç", color = c.ink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = c.inkMuted) } },
    )
}
