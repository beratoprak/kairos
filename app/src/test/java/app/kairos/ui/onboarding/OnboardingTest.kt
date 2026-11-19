package app.kairos.ui.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import app.kairos.reliability.DkmaVendor
import app.kairos.reliability.ReliabilityActions
import app.kairos.reliability.ReliabilityStatus
import app.kairos.ui.theme.KairosTheme
import app.kairos.ui.theme.ThemeFamily
import app.kairos.ui.theme.ThemeMode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** İlk açılış: varsayılan cevap "Hayır"; izin vermeden de başlanabilir. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class OnboardingTest {
    @get:Rule val compose = createComposeRule()
    private val notReady = ReliabilityStatus(DkmaVendor.SAMSUNG, "Samsung", notificationsAllowed = false, exactAlarmsAllowed = false, batteryUnrestricted = false)

    private fun show(onStart: (Boolean) -> Unit, family: ThemeFamily = ThemeFamily.BANKER, mode: ThemeMode = ThemeMode.LIGHT) =
        compose.setContent {
            KairosTheme(family, mode) { OnboardingScreen(notReady, ReliabilityActions(), 8 * 60, true, onStart) }
        }

    @Test fun startingWithoutAnsweringMeansNoBrief() {
        var answer: Boolean? = null
        show({ answer = it })
        compose.onNodeWithText("Başla").performClick()
        assertEquals(false, answer)
    }

    @Test fun choosingYesTurnsBriefOn() {
        var answer: Boolean? = null
        show({ answer = it })
        compose.onNodeWithText("Evet, 08:00'de").performClick()
        compose.onNodeWithText("Başla").performClick()
        assertEquals(true, answer)
    }

    @Test fun screenshotLight() {
        show({})
        compose.onRoot().captureRoboImage("screenshots/ilk_acilis_banker_acik.png")
    }

    @Test fun screenshotDark() {
        show({}, ThemeFamily.LACIVERT, ThemeMode.DARK)
        compose.onRoot().captureRoboImage("screenshots/ilk_acilis_lacivert_koyu.png")
    }
}
