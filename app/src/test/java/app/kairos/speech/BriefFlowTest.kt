package app.kairos.speech

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import app.kairos.KairosApp
import app.kairos.data.settings.DailyBrief
import app.kairos.data.settings.WhenSilentMode
import app.kairos.notification.Notifications
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/** Günlük sesli özet, gerçek Android bileşenleriyle uçtan uca. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BriefFlowTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val graph get() = KairosApp.graph(app)
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))

    @Before fun setUp() = shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    private fun waitUntil(what: String, condition: () -> Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("Beklenen olmadı: $what")
    }

    private fun briefAlarms(): List<ShadowAlarmManager.ScheduledAlarm> = alarms.scheduledAlarms.filter {
        shadowOf(it.operation).savedIntent.action == BriefReceiver.ACTION
    }

    private fun enable(transform: (DailyBrief) -> DailyBrief = { it }) =
        runBlocking { graph.settings.update { it.copy(brief = transform(it.brief.copy(enabled = true))) } }

    private fun addToday(n: Int) = runBlocking {
        val today = java.time.LocalDate.now(graph.clock)
        repeat(n) { graph.repository.add("Gizli içerik $it", today, 23 * 60 + 59) }
    }

    private fun fire() = app.sendBroadcast(Intent(app, BriefReceiver::class.java).setAction(BriefReceiver.ACTION))

    @Test fun nothingIsScheduledUnlessTheUserTurnsItOn() {
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(200)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(briefAlarms().isEmpty())
    }

    @Test fun turningOnSchedulesAndTurningOffCancels() {
        enable()
        waitUntil("özet alarmı kuruldu") { briefAlarms().size == 1 }
        runBlocking { graph.settings.update { it.copy(brief = it.brief.copy(enabled = false)) } }
        waitUntil("özet alarmı iptal") { briefAlarms().isEmpty() }
    }

    @Test fun silentPhoneGetsAWrittenSummaryWithoutContent() {
        enable()
        addToday(2)
        app.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_VIBRATE
        fire()
        waitUntil("yazılı özet") { notifications.allNotifications.any { shadowOf(it).contentText?.contains("Bugün 2 hatırlatmanız var.") == true } }
        val n = notifications.allNotifications.single { shadowOf(it).contentTitle == "Günlük özet" }
        assertTrue("içerik okunmamalı", shadowOf(n).contentText?.contains("Gizli") == false)
        assertNull("okuma servisi başlamamalı", shadowOf(app).peekNextStartedService())
    }

    @Test fun normalPhoneStartsSpeaking() {
        enable()
        addToday(3)
        fire()
        waitUntil("okuma servisi") { shadowOf(app).peekNextStartedService() != null }
        val started = shadowOf(app).nextStartedService
        assertEquals(DailyBriefService::class.java.name, started.component?.className)
        assertEquals("Günaydın. Bugün 3 hatırlatmanız var.".substringAfter(". "), started.getStringExtra("text")!!.substringAfter(". "))
    }

    @Test fun silentModeSkipDoesNothing() {
        enable { it.copy(whenSilentMode = WhenSilentMode.SKIP) }
        addToday(1)
        app.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_SILENT
        fire()
        Thread.sleep(300)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(notifications.allNotifications.none { it.channelId == Notifications.CHANNEL_BRIEF })
        assertNull(shadowOf(app).peekNextStartedService())
    }

    @Test fun nothingDueStaysSilentAndReschedulesTomorrow() {
        enable()
        waitUntil("özet alarmı kuruldu") { briefAlarms().size == 1 }
        fire()
        Thread.sleep(300)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(shadowOf(app).peekNextStartedService())
        assertTrue(notifications.allNotifications.none { it.channelId == Notifications.CHANNEL_BRIEF })
        waitUntil("ertesi gün yeniden kuruldu") { briefAlarms().size == 1 }
    }
}
