package app.kairos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kairos.data.RecurrenceRule
import app.kairos.data.Reminder
import app.kairos.data.ReminderRepository
import app.kairos.data.settings.AppSettings
import app.kairos.ui.editor.EditorDraft
import app.kairos.ui.editor.EditorLogic
import app.kairos.data.settings.SettingsStore
import app.kairos.snooze.SnoozePreset
import app.kairos.snooze.SnoozePresets
import app.kairos.util.TimeText
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ekranın altında beliren mesaj. [previous] varsa "Geri al" düğmesi gösterilir. */
data class UndoMessage(val text: String, val previous: Reminder?)

class HomeViewModel(
    private val repository: ReminderRepository,
    private val settings: SettingsStore,
    private val clock: Clock,
) : ViewModel() {

    /** Her dakika başında yeniden hesap: "38 dk sonra" ve gecikmiş geçişleri güncel kalsın. */
    private val minuteTicks = flow {
        while (true) {
            emit(Unit)
            delay(60_000L - clock.millis() % 60_000L)
        }
    }

    val ui: StateFlow<HomeUi?> = combine(repository.open, repository.completed, settings.settings, minuteTicks) { open, done, s, _ ->
        HomeUiBuilder.build(open, done, s, LocalDateTime.now(clock), clock.zone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _undo = MutableSharedFlow<UndoMessage>(extraBufferCapacity = 1)
    val undo: SharedFlow<UndoMessage> = _undo

    fun complete(id: Long) = act { repository.complete(id)?.let { prev ->
        val next = repository.get(id)
        val text = if (next != null && !next.isCompleted) "Sonraki: ${TimeText.day(next.dueDate, LocalDateTime.now(clock).toLocalDate())}" else "Tamamlandı"
        UndoMessage(text, prev)
    } }

    fun reopen(id: Long) = act { repository.reopen(id)?.let { UndoMessage("Listeye geri alındı", it) } }

    fun delete(id: Long) = act { repository.delete(id)?.let { UndoMessage("Silindi", it) } }

    fun snooze(id: Long, untilMillis: Long) = act {
        repository.snooze(id, untilMillis)?.let {
            val t = Instant.ofEpochMilli(untilMillis).atZone(clock.zone).toLocalDateTime()
            val use24 = settings.current().appearance.use24Hour
            UndoMessage("Ertelendi · ${TimeText.dayAndTime(t.toLocalDate(), t.hour * 60 + t.minute, LocalDateTime.now(clock).toLocalDate(), use24)}", it)
        }
    }

    fun undo(message: UndoMessage) = viewModelScope.launch { message.previous?.let { repository.restore(it) } }

    suspend fun snoozeOptions(): List<SnoozePreset> =
        SnoozePresets.build(LocalDateTime.now(clock), clock.zone, settings.current())

    // --- Ekleme / düzenleme ---

    val appSettings: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun now(): LocalDateTime = LocalDateTime.now(clock)

    suspend fun newDraft(): EditorDraft = EditorLogic.newDraft(now(), settings.current())

    suspend fun editDraft(id: Long): EditorDraft? = repository.get(id)?.let(EditorLogic::editDraft)

    /** Kaydet. Seçilen saat bir sonraki eklemede varsayılan olur (KURALLAR §1.7). */
    fun save(d: EditorDraft) = act {
        if (!d.canSave) return@act null
        val previous = if (d.isNew) {
            repository.add(d.title, d.date, d.minute, d.recurrence, d.leadMinutes, d.important)
            null
        } else {
            val base = d.original ?: repository.get(d.id) ?: return@act null
            repository.update(
                base.copy(
                    title = d.title, dueEpochDay = d.date.toEpochDay(), dueMinute = d.minute,
                    recurrenceJson = RecurrenceRule.toJson(d.recurrence), leadMinutes = d.leadMinutes, important = d.important,
                ),
            )
        }
        settings.rememberUsedMinute(d.minute)
        val use24 = settings.current().appearance.use24Hour
        UndoMessage("Kaydedildi · ${TimeText.dayAndTime(d.date, d.minute, now().toLocalDate(), use24)}", previous)
    }

    private fun act(block: suspend () -> UndoMessage?) = viewModelScope.launch {
        block()?.let { _undo.tryEmit(it) }
    }
}
