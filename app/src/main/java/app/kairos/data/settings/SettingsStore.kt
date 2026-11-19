package app.kairos.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import app.kairos.ui.theme.ThemeFamily
import app.kairos.ui.theme.ThemeMode
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Kullanıcı ayarlarının tek kaynağı. Uygulama kapanıp açılsa ya da güncellense de korunur.
 * Kayıtlı olmayan her anahtar [AppSettings] varsayılanına düşer.
 */
class SettingsStore(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map(::read).distinctUntilChanged()
    val appearance: Flow<Appearance> = settings.map { it.appearance }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    /** Ayarları değiştir: `store.update { it.copy(brief = it.brief.copy(enabled = true)) }` */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    /** Yedek için: kayıtlı tüm ayarlar, anahtar adı → değer. */
    suspend fun exportRaw(): Map<String, Any> = store.data.first().asMap().mapKeys { it.key.name }

    /**
     * Yedekten geri yükleme: ayarların tamamı yedektekiyle değişir. Tanınmayan değerler yok sayılır.
     * Kullanıcı zaten uygulamanın içinde olduğundan ilk açılış ekranı tekrar gösterilmez.
     */
    suspend fun importRaw(values: Map<String, Any?>) {
        store.edit { p ->
            p.clear()
            values.forEach { (name, v) ->
                when (v) {
                    is Boolean -> p[booleanPreferencesKey(name)] = v
                    is Int -> p[intPreferencesKey(name)] = v
                    is Float -> p[floatPreferencesKey(name)] = v
                    is String -> p[stringPreferencesKey(name)] = v
                    is Set<*> -> p[stringSetPreferencesKey(name)] = v.map { it.toString() }.toSet()
                }
            }
            val s = read(p)
            write(p, s.copy(onboarding = s.onboarding.copy(completed = true, briefQuestionAsked = true)))
        }
    }

    suspend fun setThemeFamily(family: ThemeFamily) = update { it.copy(appearance = it.appearance.copy(family = family)) }
    suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(appearance = it.appearance.copy(mode = mode)) }

    /** Yeni hatırlatma kaydedilince seçilen saat bir sonraki eklemede varsayılan olur. */
    suspend fun rememberUsedMinute(minute: Int) =
        update { it.copy(newReminder = it.newReminder.copy(lastUsedMinute = minute)) }

    private object K {
        val THEME_FAMILY = stringPreferencesKey("theme_family")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_24H = booleanPreferencesKey("use_24h")
        val FIRST_DAY = intPreferencesKey("first_day_of_week")

        val BRIEF_ENABLED = booleanPreferencesKey("brief_enabled")
        val BRIEF_MINUTE = intPreferencesKey("brief_minute")
        val BRIEF_DAYS = stringSetPreferencesKey("brief_days")
        val BRIEF_WEEKEND_DIFFERENT = booleanPreferencesKey("brief_weekend_different")
        val BRIEF_WEEKEND_MINUTE = intPreferencesKey("brief_weekend_minute")
        val BRIEF_INCLUDE_OVERDUE = booleanPreferencesKey("brief_include_overdue")
        val BRIEF_WHEN_NONE = stringPreferencesKey("brief_when_none")
        val BRIEF_WHEN_SILENT = stringPreferencesKey("brief_when_silent")
        val BRIEF_CHANNEL = stringPreferencesKey("brief_channel")
        val BRIEF_RATE = floatPreferencesKey("brief_speech_rate")

        val NEW_TIME_MODE = stringPreferencesKey("new_time_mode")
        val NEW_FIXED_MINUTE = intPreferencesKey("new_fixed_minute")
        val NEW_LAST_MINUTE = intPreferencesKey("new_last_minute")
        val PART_MORNING = intPreferencesKey("part_morning")
        val PART_NOON = intPreferencesKey("part_noon")
        val PART_EVENING = intPreferencesKey("part_evening")
        val PART_NIGHT = intPreferencesKey("part_night")
        val NEW_LEAD = intPreferencesKey("new_lead_minutes")

        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val PERSISTENT_DEFAULT = booleanPreferencesKey("persistent_default")
        val PERSISTENT_INTERVAL = intPreferencesKey("persistent_interval")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")

        val SWIPE_RIGHT = stringPreferencesKey("swipe_right")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left")
        val PURGE_DAYS = intPreferencesKey("purge_completed_days")

        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val BRIEF_ASKED = booleanPreferencesKey("brief_question_asked")
        val DISMISSED_RELIABILITY = stringPreferencesKey("dismissed_reliability")
    }

    companion object {
        /**
         * Ayarlar da Android'in normal (kullanıcı şifresiyle korunan) alanındadır (KURALLAR §3.5).
         * Tek örneği `AppGraph` tutar (aynı dosyaya iki DataStore açılamaz); doğrudan çağırmayın.
         */
        fun create(context: Context): SettingsStore {
            val app = context.applicationContext
            return SettingsStore(PreferenceDataStoreFactory.create { app.preferencesDataStoreFile("settings") })
        }

        private inline fun <reified E : Enum<E>> enumOr(value: String?, default: E): E =
            value?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default

        internal fun read(p: Preferences): AppSettings {
            val d = AppSettings()
            return AppSettings(
                appearance = Appearance(
                    family = ThemeFamily.fromId(p[K.THEME_FAMILY]),
                    mode = ThemeMode.fromId(p[K.THEME_MODE]),
                    use24Hour = p[K.USE_24H] ?: d.appearance.use24Hour,
                    firstDayOfWeek = p[K.FIRST_DAY]?.takeIf { it in 1..7 }?.let(DayOfWeek::of) ?: d.appearance.firstDayOfWeek,
                ),
                brief = DailyBrief(
                    enabled = p[K.BRIEF_ENABLED] ?: d.brief.enabled,
                    minute = p[K.BRIEF_MINUTE] ?: d.brief.minute,
                    days = p[K.BRIEF_DAYS]?.mapNotNull { it.toIntOrNull() }?.filter { it in 1..7 }?.toSet() ?: d.brief.days,
                    weekendDifferent = p[K.BRIEF_WEEKEND_DIFFERENT] ?: d.brief.weekendDifferent,
                    weekendMinute = p[K.BRIEF_WEEKEND_MINUTE] ?: d.brief.weekendMinute,
                    includeOverdue = p[K.BRIEF_INCLUDE_OVERDUE] ?: d.brief.includeOverdue,
                    whenNone = enumOr(p[K.BRIEF_WHEN_NONE], d.brief.whenNone),
                    whenSilentMode = enumOr(p[K.BRIEF_WHEN_SILENT], d.brief.whenSilentMode),
                    channel = enumOr(p[K.BRIEF_CHANNEL], d.brief.channel),
                    speechRate = (p[K.BRIEF_RATE] ?: d.brief.speechRate).coerceIn(0.5f, 2.0f),
                ),
                newReminder = NewReminder(
                    timeMode = enumOr(p[K.NEW_TIME_MODE], d.newReminder.timeMode),
                    fixedMinute = p[K.NEW_FIXED_MINUTE] ?: d.newReminder.fixedMinute,
                    lastUsedMinute = p[K.NEW_LAST_MINUTE],
                    dayParts = DayParts(
                        morning = p[K.PART_MORNING] ?: d.newReminder.dayParts.morning,
                        noon = p[K.PART_NOON] ?: d.newReminder.dayParts.noon,
                        evening = p[K.PART_EVENING] ?: d.newReminder.dayParts.evening,
                        night = p[K.PART_NIGHT] ?: d.newReminder.dayParts.night,
                    ),
                    defaultLeadMinutes = p[K.NEW_LEAD],
                ),
                notifications = Notifications(
                    snoozeMinutes = p[K.SNOOZE_MINUTES] ?: d.notifications.snoozeMinutes,
                    persistentByDefault = p[K.PERSISTENT_DEFAULT] ?: d.notifications.persistentByDefault,
                    persistentIntervalMinutes = p[K.PERSISTENT_INTERVAL] ?: d.notifications.persistentIntervalMinutes,
                    quietHoursEnabled = p[K.QUIET_ENABLED] ?: d.notifications.quietHoursEnabled,
                    quietStartMinute = p[K.QUIET_START] ?: d.notifications.quietStartMinute,
                    quietEndMinute = p[K.QUIET_END] ?: d.notifications.quietEndMinute,
                ),
                list = ListBehavior(
                    swipeRight = enumOr(p[K.SWIPE_RIGHT], d.list.swipeRight),
                    swipeLeft = enumOr(p[K.SWIPE_LEFT], d.list.swipeLeft),
                    purgeCompletedAfterDays = p[K.PURGE_DAYS],
                ),
                onboarding = Onboarding(
                    completed = p[K.ONBOARDING_DONE] ?: d.onboarding.completed,
                    briefQuestionAsked = p[K.BRIEF_ASKED] ?: d.onboarding.briefQuestionAsked,
                    dismissedReliability = p[K.DISMISSED_RELIABILITY],
                ),
            )
        }

        internal fun write(p: MutablePreferences, s: AppSettings) {
            p[K.THEME_FAMILY] = s.appearance.family.id
            p[K.THEME_MODE] = s.appearance.mode.id
            p[K.USE_24H] = s.appearance.use24Hour
            p[K.FIRST_DAY] = s.appearance.firstDayOfWeek.value

            p[K.BRIEF_ENABLED] = s.brief.enabled
            p[K.BRIEF_MINUTE] = s.brief.minute
            p[K.BRIEF_DAYS] = s.brief.days.map(Int::toString).toSet()
            p[K.BRIEF_WEEKEND_DIFFERENT] = s.brief.weekendDifferent
            p[K.BRIEF_WEEKEND_MINUTE] = s.brief.weekendMinute
            p[K.BRIEF_INCLUDE_OVERDUE] = s.brief.includeOverdue
            p[K.BRIEF_WHEN_NONE] = s.brief.whenNone.name
            p[K.BRIEF_WHEN_SILENT] = s.brief.whenSilentMode.name
            p[K.BRIEF_CHANNEL] = s.brief.channel.name
            p[K.BRIEF_RATE] = s.brief.speechRate

            p[K.NEW_TIME_MODE] = s.newReminder.timeMode.name
            p[K.NEW_FIXED_MINUTE] = s.newReminder.fixedMinute
            s.newReminder.lastUsedMinute?.let { p[K.NEW_LAST_MINUTE] = it } ?: p.remove(K.NEW_LAST_MINUTE)
            p[K.PART_MORNING] = s.newReminder.dayParts.morning
            p[K.PART_NOON] = s.newReminder.dayParts.noon
            p[K.PART_EVENING] = s.newReminder.dayParts.evening
            p[K.PART_NIGHT] = s.newReminder.dayParts.night
            s.newReminder.defaultLeadMinutes?.let { p[K.NEW_LEAD] = it } ?: p.remove(K.NEW_LEAD)

            p[K.SNOOZE_MINUTES] = s.notifications.snoozeMinutes
            p[K.PERSISTENT_DEFAULT] = s.notifications.persistentByDefault
            p[K.PERSISTENT_INTERVAL] = s.notifications.persistentIntervalMinutes
            p[K.QUIET_ENABLED] = s.notifications.quietHoursEnabled
            p[K.QUIET_START] = s.notifications.quietStartMinute
            p[K.QUIET_END] = s.notifications.quietEndMinute

            p[K.SWIPE_RIGHT] = s.list.swipeRight.name
            p[K.SWIPE_LEFT] = s.list.swipeLeft.name
            s.list.purgeCompletedAfterDays?.let { p[K.PURGE_DAYS] = it } ?: p.remove(K.PURGE_DAYS)

            p[K.ONBOARDING_DONE] = s.onboarding.completed
            p[K.BRIEF_ASKED] = s.onboarding.briefQuestionAsked
            s.onboarding.dismissedReliability?.let { p[K.DISMISSED_RELIABILITY] = it } ?: p.remove(K.DISMISSED_RELIABILITY)
        }
    }
}
