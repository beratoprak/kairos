package app.kairos.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import app.kairos.data.settings.AppSettings
import app.kairos.data.settings.NewReminder
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
import java.time.LocalDateTime

/** Ekleme sayfasının içeriği (alt sayfa çerçevesi olmadan) → `app/screenshots/`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class EditorScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val now = LocalDateTime.parse("2026-11-19T10:30")
    private val settings = AppSettings(newReminder = NewReminder(lastUsedMinute = 720))

    private fun shot(name: String, family: ThemeFamily, mode: ThemeMode, draft: EditorDraft) {
        compose.setContent {
            KairosTheme(family, mode) {
                Box(Modifier.fillMaxSize().background(Kairos.colors.surface).padding(top = 24.dp)) {
                    EditorContent(draft, settings, now, {}, {}, {}, {})
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    private val filled = EditorLogic.withDate(EditorLogic.newDraft(now, settings), now.toLocalDate().plusDays(1), now, settings)
        .copy(title = "Sözleşme taslağını hukuka ilet")

    @Test fun ekleBankerAcik() = shot("ekle_banker_acik", ThemeFamily.BANKER, ThemeMode.LIGHT, filled)
    @Test fun ekleGrafitKoyu() = shot("ekle_grafit_koyu", ThemeFamily.GRAFIT, ThemeMode.DARK, filled)
    @Test fun ekleBos() = shot("ekle_bos_lacivert_acik", ThemeFamily.LACIVERT, ThemeMode.LIGHT, EditorLogic.newDraft(now, settings))
}
