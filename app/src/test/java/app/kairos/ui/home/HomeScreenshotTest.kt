package app.kairos.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.kairos.data.RecurrenceRule
import app.kairos.data.RecurrenceUnit
import app.kairos.data.Reminder
import app.kairos.data.settings.AppSettings
import app.kairos.data.settings.Appearance
import app.kairos.ui.settings.SettingsScreen
import app.kairos.ui.theme.KairosTheme
import app.kairos.ui.theme.ThemeFamily
import app.kairos.ui.theme.ThemeMode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Ana ekranların görüntüleri → `app/screenshots/`. Kayıt: `./gradlew recordRoborazziDebug`.
 * Örnek veri tasarım taslağındakiyle aynıdır.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val zone = ZoneId.of("Europe/Istanbul")
    private val now = LocalDateTime.parse("2026-11-19T11:22")
    private val today = now.toLocalDate()
    private var id = 1L
    private fun r(day: Long, minute: Int, title: String, rule: RecurrenceRule? = null, done: Boolean = false, important: Boolean = false) =
        Reminder(id = id++, title = title, dueEpochDay = today.toEpochDay() + day, dueMinute = minute,
            recurrenceJson = RecurrenceRule.toJson(rule), important = important,
            completedAt = if (done) now.minusMinutes(90).atZone(zone).toInstant().toEpochMilli() else null, createdAt = 0, updatedAt = 0)

    private val open = listOf(
        r(-1, 17 * 60, "Muhasebeciyi ara: KDV beyannamesi"),
        r(0, 12 * 60, "Öğle yemeği: Kaan Bey"),
        r(0, 15 * 60, "Sözleşme taslağını hukuka ilet", important = true),
        r(0, 18 * 60 + 30, "Eczaneden ilaçları al", rule = RecurrenceRule(RecurrenceUnit.WEEK)),
        r(1, 9 * 60, "Bütçe sunumunu gözden geçir"),
        r(1, 14 * 60, "Ofis kira ödemesi", rule = RecurrenceRule(RecurrenceUnit.MONTH)),
        r(2, 10 * 60, "Bankayla görüşme"),
        r(12, 11 * 60, "Yıllık sağlık kontrolü"),
    )
    private val completed = listOf(r(0, 9 * 60 + 30, "Satış toplantısı notlarını gönder", done = true))
    private fun ui(settings: AppSettings = AppSettings()) = HomeUiBuilder.build(open, completed, settings, now, zone)

    private fun shot(name: String, family: ThemeFamily, mode: ThemeMode, tab: HomeTab, data: HomeUi = ui()) {
        compose.setContent {
            KairosTheme(family, mode) {
                HomeScreen(data, tab, {}, HomeActions(), SnackbarHostState(), settingsContent = { p ->
                    SettingsScreen(AppSettings(appearance = Appearance(family, mode)), {}, p, appVersion = "0.1.0")
                })
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun bugunBankerAcik() = shot("bugun_banker_acik", ThemeFamily.BANKER, ThemeMode.LIGHT, HomeTab.TODAY)
    @Test fun bugunBankerKoyu() = shot("bugun_banker_koyu", ThemeFamily.BANKER, ThemeMode.DARK, HomeTab.TODAY)
    @Test fun bugunLacivertAcik() = shot("bugun_lacivert_acik", ThemeFamily.LACIVERT, ThemeMode.LIGHT, HomeTab.TODAY)
    @Test fun bugunBordoKoyu() = shot("bugun_bordo_koyu", ThemeFamily.BORDO, ThemeMode.DARK, HomeTab.TODAY)
    @Test fun bugunTasAcik() = shot("bugun_tas_acik", ThemeFamily.TAS, ThemeMode.LIGHT, HomeTab.TODAY)
    @Test fun bugunGrafitKoyu() = shot("bugun_grafit_koyu", ThemeFamily.GRAFIT, ThemeMode.DARK, HomeTab.TODAY)
    @Test fun yakinda() = shot("yakinda_banker_acik", ThemeFamily.BANKER, ThemeMode.LIGHT, HomeTab.UPCOMING)
    @Test fun tamamlanan() = shot("tamamlanan_banker_acik", ThemeFamily.BANKER, ThemeMode.LIGHT, HomeTab.COMPLETED)
    @Test fun ayarlar() = shot("ayarlar_banker_acik", ThemeFamily.BANKER, ThemeMode.LIGHT, HomeTab.SETTINGS)
    @Test fun bos() = shot(
        "bugun_bos_banker_acik", ThemeFamily.BANKER, ThemeMode.LIGHT, HomeTab.TODAY,
        HomeUiBuilder.build(emptyList(), emptyList(), AppSettings(), now, zone),
    )
}
