package app.kairos.backup

import android.app.AlarmManager
import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.kairos.KairosApp
import app.kairos.data.RecurrenceRule
import app.kairos.data.RecurrenceUnit
import app.kairos.ui.theme.ThemeFamily
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val graph get() = KairosApp.graph(app)

    private fun expectError(text: String, contains: String) {
        try {
            BackupCodec.decode(text)
            fail("hata bekleniyordu")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!, e.message!!.contains(contains))
        }
    }

    @Test fun rejectsWrongOrBrokenFiles() {
        expectError("rastgele metin", "okunamadı")
        expectError("""{"format":"baska","version":1,"exportedAt":0,"reminders":[]}""", "Kairos yedeği değil")
        expectError("""{"format":"kairos-backup","version":99,"exportedAt":0,"reminders":[]}""", "daha yeni")
        expectError(
            """{"format":"kairos-backup","version":1,"exportedAt":0,"reminders":[{"title":"x","dueEpochDay":1,"dueMinute":5000,"createdAt":0,"updatedAt":0}]}""",
            "geçersiz",
        )
    }

    @Test fun fullRoundTripRestoresRemindersSettingsAndAlarms() = runBlocking {
        val tomorrow = LocalDate.now(graph.clock).plusDays(1)
        graph.repository.add("Sözleşme", tomorrow, 720, important = true)
        graph.repository.add("İlaç", tomorrow, 1110, recurrence = RecurrenceRule(RecurrenceUnit.WEEK))
        val done = graph.repository.add("Bitti", tomorrow, 600)
        graph.repository.complete(done.id)
        graph.settings.update { it.copy(appearance = it.appearance.copy(family = ThemeFamily.BORDO), brief = it.brief.copy(enabled = true, minute = 7 * 60 + 30)) }

        val manager = BackupManager(app, graph)
        val file = File(app.cacheDir, manager.suggestedName())
        assertEquals(3, manager.export(Uri.fromFile(file)))
        assertTrue(file.readText().contains("\"format\": \"kairos-backup\""))

        // Telefon değişti / veri bozuldu: her şeyi farklılaştır.
        graph.repository.replaceAll(emptyList())
        graph.settings.update { it.copy(appearance = it.appearance.copy(family = ThemeFamily.TAS), brief = it.brief.copy(enabled = false)) }

        manager.restore(manager.read(Uri.fromFile(file)))

        val all = graph.repository.all()
        assertEquals(setOf("Sözleşme", "İlaç", "Bitti"), all.map { it.title }.toSet())
        assertTrue(all.single { it.title == "Sözleşme" }.important)
        assertEquals(RecurrenceRule(RecurrenceUnit.WEEK), all.single { it.title == "İlaç" }.recurrence)
        assertTrue(all.single { it.title == "Bitti" }.isCompleted)

        val s = graph.settings.current()
        assertEquals(ThemeFamily.BORDO, s.appearance.family)
        assertTrue(s.brief.enabled)
        assertEquals(7 * 60 + 30, s.brief.minute)
        assertTrue("ilk açılış tekrar gösterilmemeli", s.onboarding.completed)

        // 2 açık hatırlatma + 1 özet alarmı
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertEquals(3, alarms.size)
    }
}
