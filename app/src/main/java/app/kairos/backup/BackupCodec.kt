package app.kairos.backup

import app.kairos.data.Reminder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Yedek dosyasının biçimi. İnsan tarafından okunabilir JSON; sürüm alanı ileride biçim değişirse
 * eski yedeklerin de açılabilmesi içindir.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val reminders: List<BackupReminder>,
    /** Ayarlar: anahtar → değer (bool, sayı, metin ya da metin listesi). */
    val settings: Map<String, JsonElement> = emptyMap(),
) {
    companion object {
        const val FORMAT = "kairos-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupReminder(
    val title: String,
    val dueEpochDay: Long,
    val dueMinute: Int,
    val recurrence: String? = null,
    val leadMinutes: Int? = null,
    val important: Boolean = false,
    val persistent: Boolean = false,
    val snoozedUntil: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Yedekte geçerli olmayan bir şey bulundu. Mesaj kullanıcıya gösterilir. */
class BackupFormatException(message: String) : Exception(message)

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(reminders: List<Reminder>, settings: Map<String, Any>, exportedAt: Long): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                exportedAt = exportedAt,
                reminders = reminders.map { it.toBackup() },
                settings = settings.mapValues { (_, v) -> v.toJson() },
            ),
        )

    /** @throws BackupFormatException dosya KAİROS yedeği değilse ya da bozuksa */
    fun decode(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupFormatException("Bu dosya okunamadı. Kairos yedeği olduğundan emin olun.")
        }
        if (file.format != BackupFile.FORMAT) throw BackupFormatException("Bu dosya bir Kairos yedeği değil.")
        if (file.version > BackupFile.VERSION) throw BackupFormatException("Bu yedek uygulamanın daha yeni bir sürümüyle alınmış. Önce uygulamayı güncelleyin.")
        file.reminders.forEach {
            if (it.dueMinute !in 0 until Reminder.MINUTES_PER_DAY || it.title.isBlank()) {
                throw BackupFormatException("Yedekte geçersiz bir hatırlatma var.")
            }
        }
        return file
    }

    fun BackupReminder.toReminder() = Reminder(
        title = title, dueEpochDay = dueEpochDay, dueMinute = dueMinute, recurrenceJson = recurrence,
        leadMinutes = leadMinutes, important = important, persistent = persistent, snoozedUntil = snoozedUntil,
        completedAt = completedAt, createdAt = createdAt, updatedAt = updatedAt,
    )

    private fun Reminder.toBackup() = BackupReminder(
        title = title, dueEpochDay = dueEpochDay, dueMinute = dueMinute, recurrence = recurrenceJson,
        leadMinutes = leadMinutes, important = important, persistent = persistent, snoozedUntil = snoozedUntil,
        completedAt = completedAt, createdAt = createdAt, updatedAt = updatedAt,
    )

    private fun Any.toJson(): JsonElement = when (this) {
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Set<*> -> JsonArray(map { JsonPrimitive(it.toString()) })
        else -> JsonPrimitive(toString())
    }

    /** Ayar değerini geri çevirir: true/false, tam sayı, ondalık, metin ya da metin kümesi. */
    fun settingValue(e: JsonElement): Any? = when (e) {
        is JsonArray -> e.map { it.jsonPrimitive.content }.toSet()
        is JsonPrimitive -> when {
            e.isString -> e.content
            e.booleanOrNull != null -> e.booleanOrNull
            e.intOrNull != null && !e.content.contains('.') -> e.intOrNull
            else -> e.floatOrNull
        }
        is JsonObject -> null
    }
}
