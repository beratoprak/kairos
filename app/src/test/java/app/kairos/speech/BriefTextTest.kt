package app.kairos.speech

import app.kairos.data.settings.DailyBrief
import app.kairos.data.settings.WhenNone
import app.kairos.ui.settings.SettingsText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** KURALLAR §1.5: özet sadece sayı söyler; içerik asla okunmaz. */
class BriefTextTest {
    private val eight = 8 * 60

    @Test fun countOnly() =
        assertEquals("Günaydın. Bugün 4 hatırlatmanız var.", BriefText.sentence(4, 1, eight, DailyBrief(enabled = true)))

    @Test fun overdueOnlyWhenAsked() =
        assertEquals(
            "Günaydın. Bugün 4 hatırlatmanız var. Ayrıca önceki günlerden 1 hatırlatmanız var.",
            BriefText.sentence(4, 1, eight, DailyBrief(enabled = true, includeOverdue = true)),
        )

    @Test fun nothingDueStaysSilentByDefault() =
        assertNull(BriefText.sentence(0, 3, eight, DailyBrief(enabled = true)))

    @Test fun nothingDueCanBeSaid() =
        assertEquals("Günaydın. Bugün hatırlatmanız yok.", BriefText.sentence(0, 0, eight, DailyBrief(enabled = true, whenNone = WhenNone.SAY_NONE)))

    @Test fun greetingFollowsTheHour() {
        assertEquals("İyi günler", BriefText.greeting(13 * 60))
        assertEquals("İyi akşamlar", BriefText.greeting(20 * 60))
        assertEquals("İyi akşamlar", BriefText.greeting(2 * 60))
    }

    @Test fun neverContainsATitle() {
        val s = BriefText.sentence(2, 2, eight, DailyBrief(enabled = true, includeOverdue = true))!!
        assertFalse(s.contains(":"))
    }

    @Test fun dayLabels() {
        assertEquals("Her gün", SettingsText.days((1..7).toSet()))
        assertEquals("Hafta içi", SettingsText.days((1..5).toSet()))
        assertEquals("Hafta sonu", SettingsText.days(setOf(6, 7)))
        assertEquals("Pzt, Çar, Cum", SettingsText.days(setOf(5, 1, 3)))
        assertEquals("Normal", SettingsText.rate(1.0f))
    }
}
