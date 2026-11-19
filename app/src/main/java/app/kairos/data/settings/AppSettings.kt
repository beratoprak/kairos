package app.kairos.data.settings

import app.kairos.ui.theme.ThemeFamily
import app.kairos.ui.theme.ThemeMode
import java.time.DayOfWeek

/**
 * Uygulamanın tüm ayarları. Buradaki varsayılanlar KURALLAR §1'in kodudur:
 * kullanıcı açmadıkça hiçbir opsiyonel davranış çalışmaz. `AppSettingsDefaultsTest` bunu denetler.
 */
data class AppSettings(
    val appearance: Appearance = Appearance(),
    val brief: DailyBrief = DailyBrief(),
    val newReminder: NewReminder = NewReminder(),
    val notifications: Notifications = Notifications(),
    val list: ListBehavior = ListBehavior(),
    val onboarding: Onboarding = Onboarding(),
    val updates: Updates = Updates(),
)

/** Uygulama içi güncelleme. Proje sahibinin isteğiyle açılışta denetim varsayılan açık. */
data class Updates(val checkOnLaunch: Boolean = true)

/** Görünüm. */
data class Appearance(
    val family: ThemeFamily = ThemeFamily.Default,
    val mode: ThemeMode = ThemeMode.Default,
    val use24Hour: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
)

/** Günlük sesli özet: "Bugün N hatırlatmanız var." İçerik asla okunmaz. */
data class DailyBrief(
    /** VARSAYILAN KAPALI. Sadece kullanıcı açar (ilk açılış sorusu ya da Ayarlar). */
    val enabled: Boolean = false,
    val minute: Int = 8 * 60,
    /** ISO gün numaraları (1 = Pazartesi). Varsayılan her gün; özet kapalı olduğu sürece etkisizdir. */
    val days: Set<Int> = (1..7).toSet(),
    /** Hafta sonu için ayrı saat. Varsayılan kapalı. */
    val weekendDifferent: Boolean = false,
    val weekendMinute: Int = 10 * 60,
    /** "…ayrıca dünden kalan N hatırlatmanız var." Varsayılan kapalı. */
    val includeOverdue: Boolean = false,
    val whenNone: WhenNone = WhenNone.STAY_SILENT,
    val whenSilentMode: WhenSilentMode = WhenSilentMode.NOTIFY_ONLY,
    val channel: AudioChannel = AudioChannel.NOTIFICATION,
    /** 0.5..2.0; 1.0 normal. */
    val speechRate: Float = 1.0f,
    /** Konuşan ses (motorun ses adı); null = otomatik, en kaliteli Türkçe ses. */
    val voiceName: String? = null,
)

enum class WhenNone { STAY_SILENT, SAY_NONE }

/** Telefon sessiz / rahatsız etmeyin modundayken. */
enum class WhenSilentMode { NOTIFY_ONLY, SPEAK_ANYWAY, SKIP }

enum class AudioChannel { NOTIFICATION, MEDIA, ALARM }

/** Yeni hatırlatma eklerken. */
data class NewReminder(
    val timeMode: DefaultTimeMode = DefaultTimeMode.LAST_USED,
    /** FIXED modunda kullanılan saat. */
    val fixedMinute: Int = 9 * 60,
    /** Son seçilen saat; hiç seçilmediyse null (o zaman sabah dilimi önerilir). */
    val lastUsedMinute: Int? = null,
    val dayParts: DayParts = DayParts(),
    /** Önceden hatırlatma varsayılanı; null = yok. */
    val defaultLeadMinutes: Int? = null,
)

enum class DefaultTimeMode { LAST_USED, FIXED }

/** Ekleme sayfasındaki kısayollar; kullanıcı saatlerini değiştirebilir. */
data class DayParts(
    val morning: Int = 9 * 60,
    val noon: Int = 12 * 60,
    val evening: Int = 19 * 60,
    val night: Int = 22 * 60,
)

/** Hatırlatma bildirimleri. */
data class Notifications(
    val snoozeMinutes: Int = 10,
    /** Israrcı mod varsayılanı (yeni hatırlatmalarda). Varsayılan kapalı. */
    val persistentByDefault: Boolean = false,
    val persistentIntervalMinutes: Int = 10,
    /** Sessiz saatler: bu aralıkta bildirimler sessizce biriktirilir. Varsayılan kapalı. */
    val quietHoursEnabled: Boolean = false,
    val quietStartMinute: Int = 23 * 60,
    val quietEndMinute: Int = 7 * 60,
    /**
     * Hatırlatma sesinin düzeyi. LOUD: telefonun alarm ses düzeyinden çalar (genelde en yüksek);
     * telefon sessiz/titreşimdeyken yine sessiz kalır. Proje sahibinin isteğiyle varsayılan LOUD.
     */
    val volume: ReminderVolume = ReminderVolume.LOUD,
)

enum class ReminderVolume { NORMAL, LOUD }

/** Liste davranışı. */
data class ListBehavior(
    val swipeRight: SwipeAction = SwipeAction.COMPLETE,
    val swipeLeft: SwipeAction = SwipeAction.SNOOZE,
    /** Tamamlananları N gün sonra sil; null = hiç silme (varsayılan). */
    val purgeCompletedAfterDays: Int? = null,
)

enum class SwipeAction { NONE, COMPLETE, SNOOZE, DELETE }

data class Onboarding(
    val completed: Boolean = false,
    /** İlk açılışta sesli özet sorusu soruldu mu? Bir kez sorulur. */
    val briefQuestionAsked: Boolean = false,
    /** Bugün ekranındaki izin uyarısını kullanıcı hangi sorun birleşimi için kapattı. */
    val dismissedReliability: String? = null,
)
