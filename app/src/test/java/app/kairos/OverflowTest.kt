package app.kairos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.TextLayoutResult
import app.kairos.data.RecurrenceRule
import app.kairos.data.RecurrenceUnit
import app.kairos.data.Reminder
import app.kairos.data.settings.AppSettings
import app.kairos.data.settings.DailyBrief
import app.kairos.data.settings.NewReminder
import app.kairos.reliability.DkmaVendor
import app.kairos.reliability.ReliabilityActions
import app.kairos.reliability.ReliabilityStatus
import app.kairos.ui.editor.EditorContent
import app.kairos.ui.editor.EditorLogic
import app.kairos.ui.home.HomeActions
import app.kairos.ui.home.HomeScreen
import app.kairos.ui.home.HomeTab
import app.kairos.ui.home.HomeUiBuilder
import app.kairos.ui.onboarding.OnboardingScreen
import app.kairos.ui.settings.SettingsScreen
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.KairosTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * "Asla taşma olmasın" (proje sahibi): en dar telefon (320 dp) ve en büyük yazı boyutunda (2×)
 * her ekranda hiçbir yazı kesilmemeli, hiçbir öğe ekranın sağından taşmamalı.
 * Görüntüler: `app/screenshots/tasma_*.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h2400dp-xxhdpi", fontScale = 2.0f)
class OverflowTest {
    @get:Rule val compose = createComposeRule()

    private val zone = ZoneId.of("Europe/Istanbul")
    private val now = LocalDateTime.parse("2026-11-19T11:22")
    private val today = now.toLocalDate()
    private val longTitle = "Müşteri toplantısından sonra sözleşme notlarını hukuk ekibine ilet"
    private var id = 1L
    private fun r(day: Long, minute: Int, title: String, rule: RecurrenceRule? = null) = Reminder(
        id = id++, title = title, dueEpochDay = today.toEpochDay() + day, dueMinute = minute,
        recurrenceJson = RecurrenceRule.toJson(rule), important = true, createdAt = 0, updatedAt = 0,
    )
    private val open = listOf(
        r(-1, 17 * 60, longTitle),
        r(0, 12 * 60, longTitle, RecurrenceRule(RecurrenceUnit.WEEK, 2, setOf(1, 4))),
        r(0, 18 * 60 + 30, "Eczane"),
        r(1, 9 * 60, longTitle),
        r(3, 9 * 60, longTitle),
    )

    private fun check(name: String, content: @Composable () -> Unit) {
        compose.setContent { KairosTheme { Box(Modifier.fillMaxSize().background(Kairos.colors.ground)) { content() } } }
        compose.waitForIdle()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val problems = mutableListOf<String>()
        compose.onAllNodes(SemanticsMatcher("her düğüm") { true }, useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
            val b = node.boundsInRoot
            if (b.width > 0 && b.right > root.right + 1f) problems += "sağdan taşıyor: ${node.config} → ${b.right} > ${root.right}"
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.let { get ->
                val results = mutableListOf<TextLayoutResult>()
                get(results)
                results.forEach { t ->
                    // Tek satıra zorlanmış yazı sığmıyorsa sağdan kesilir.
                    if (!t.layoutInput.softWrap && t.didOverflowWidth) problems += "tek satırda kesiliyor: \"${t.layoutInput.text}\""
                    // "…" ile kısaltılmış satır: kullanıcı yazının tamamını göremiyor.
                    if ((0 until t.lineCount).any { t.isLineEllipsized(it) }) problems += "kısaltılmış: \"${t.layoutInput.text}\""
                    // Kelimenin ortasından satır bölünmesi ("toplantısın / dan"): yer yetmiyor demektir.
                    val text = t.layoutInput.text.text
                    for (line in 0 until t.lineCount - 1) {
                        val end = t.getLineEnd(line)
                        if (end in 1 until text.length && text[end - 1].isLetter() && text[end].isLetter()) {
                            problems += "kelime bölünüyor: \"${text.substring(0, end)}|${text.substring(end)}\""
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/tasma_$name.png")
        assertTrue(problems.distinct().joinToString("\n"), problems.isEmpty())
    }

    private fun home(tab: HomeTab) = check(tab.name.lowercase()) {
        HomeScreen(
            HomeUiBuilder.build(open, emptyList(), AppSettings(), now, zone), tab, {}, HomeActions(), SnackbarHostState(),
            settingsContent = { p ->
                SettingsScreen(
                    AppSettings(brief = DailyBrief(enabled = true, weekendDifferent = true)), {}, p, appVersion = "0.1.1",
                    reliability = ReliabilityStatus(DkmaVendor.XIAOMI, "Xiaomi", false, false, false),
                )
            },
        )
    }

    @Test fun bugun() = home(HomeTab.TODAY)
    @Test fun yakinda() = home(HomeTab.UPCOMING)
    @Test fun ayarlar() = home(HomeTab.SETTINGS)

    @Test fun ekle() = check("ekle") {
        val s = AppSettings(newReminder = NewReminder(lastUsedMinute = 720))
        val d = EditorLogic.newDraft(now, s).copy(title = longTitle, recurrence = RecurrenceRule(RecurrenceUnit.WEEK, 2, setOf(1, 4)), leadMinutes = 24 * 60)
        EditorContent(d, s, now, {}, {}, {}, {})
    }

    @Test fun ilkAcilis() = check("ilk_acilis") {
        OnboardingScreen(ReliabilityStatus(DkmaVendor.XIAOMI, "Xiaomi", false, false, false), ReliabilityActions(), 8 * 60, true) {}
    }
}
