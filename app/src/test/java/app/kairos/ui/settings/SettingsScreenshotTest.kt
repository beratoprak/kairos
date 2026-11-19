package app.kairos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.kairos.data.settings.AppSettings
import app.kairos.data.settings.Appearance
import app.kairos.data.settings.DailyBrief
import app.kairos.ui.theme.Kairos
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

/** Ayarlar ekranının tamamı (uzun ekran) → `app/screenshots/ayarlar_*.png`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h2600dp-xxhdpi")
class SettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String, s: AppSettings) {
        compose.setContent {
            KairosTheme(s.appearance.family, s.appearance.mode) {
                Box(Modifier.fillMaxSize().background(Kairos.colors.ground)) {
                    SettingsScreen(s, {}, PaddingValues(), onPreviewBrief = {}, appVersion = "0.1.0")
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun varsayilan() = shot("ayarlar_tam_varsayilan", AppSettings())
    @Test fun ozetAcik() = shot("ayarlar_tam_ozet_acik", AppSettings(brief = DailyBrief(enabled = true)))
    @Test fun tasKoyu() = shot("ayarlar_tam_tas_koyu", AppSettings(appearance = Appearance(ThemeFamily.TAS, ThemeMode.DARK)))
}
