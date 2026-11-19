package app.kairos.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import app.kairos.KairosApp
import app.kairos.data.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * Uçtan uca: hatırlatma eklenir → alarm kurulur → alarm çalar → bildirim görünür →
 * "Tamamlandı" düğmesi → hatırlatma tamamlanır, bildirim kalkar, alarm iptal edilir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderFlowTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val graph get() = KairosApp.graph(app)
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))

    @Before fun grant() = shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    /** Alıcılar işi arka planda yapar; sonucu kısa bir süre bekle. */
    private fun waitUntil(what: String, condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("Beklenen olmadı: $what")
    }

    private fun addInOneHour(): Reminder = runBlocking {
        val at = LocalDateTime.now(graph.clock).plusHours(1).withSecond(0).withNano(0)
        graph.repository.add("Sözleşmeyi gönder", at.toLocalDate(), at.hour * 60 + at.minute)
    }

    @Test fun addingSchedulesAnExactAlarm() {
        val r = addInOneHour()
        val scheduled = alarms.scheduledAlarms
        assertEquals(1, scheduled.size)
        assertEquals(AlarmPlanner.dueMillis(r, graph.clock.zone), scheduled.single().triggerAtTime)
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled.single().type)
    }

    @Test fun firingShowsNotificationAndCompleteActionFinishesIt() {
        val r = addInOneHour()
        val fire = shadowOf(alarms.scheduledAlarms.single().operation).savedIntent
        app.sendBroadcast(Intent(fire))

        waitUntil("bildirim") { notifications.allNotifications.isNotEmpty() }
        val n = notifications.allNotifications.single()
        assertEquals("Sözleşmeyi gönder", shadowOf(n).contentTitle)
        assertEquals(listOf("Tamamlandı", "10 dk ertele", "1 saat"), n.actions.map { it.title.toString() })
        waitUntil("bildirildi işareti") { runBlocking { graph.repository.get(r.id)?.lastNotifiedAt } != null }

        val complete = shadowOf(n.actions[0].actionIntent).savedIntent
        app.sendBroadcast(Intent(complete))
        waitUntil("tamamlandı") { runBlocking { graph.repository.get(r.id)?.isCompleted } == true }
        waitUntil("bildirim kalktı") { notifications.allNotifications.isEmpty() }
        assertTrue("alarm iptal edilmeli", alarms.scheduledAlarms.isEmpty())
    }

    @Test fun snoozeActionReschedules() {
        val r = addInOneHour()
        app.sendBroadcast(Intent(shadowOf(alarms.scheduledAlarms.single().operation).savedIntent))
        waitUntil("bildirim") { notifications.allNotifications.isNotEmpty() }

        val snooze = shadowOf(notifications.allNotifications.single().actions[1].actionIntent).savedIntent
        app.sendBroadcast(Intent(snooze))
        waitUntil("ertelendi") { runBlocking { graph.repository.get(r.id)?.snoozedUntil } != null }
        val until = runBlocking { graph.repository.get(r.id)!!.snoozedUntil!! }
        assertEquals(until, alarms.scheduledAlarms.single().triggerAtTime)
    }

    @Test fun staleAlarmAfterEditShowsNothing() {
        val r = addInOneHour()
        val oldFire = Intent(shadowOf(alarms.scheduledAlarms.single().operation).savedIntent)
        runBlocking { graph.repository.update(r.copy(dueMinute = (r.dueMinute + 30) % (24 * 60))) }
        app.sendBroadcast(oldFire)
        Thread.sleep(300)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(notifications.allNotifications.isEmpty())
    }

    /** Telefon kapalı/kilitliyken zamanı geçen hatırlatma, kilit açılınca bir kez ve sessizce bildirilir. */
    @Test fun missedWhileOffIsShownOnceAfterUnlock() {
        val past = LocalDateTime.now(graph.clock).minusHours(2).withSecond(0).withNano(0)
        val r = runBlocking { graph.repository.add("Kaçan toplantı", past.toLocalDate(), past.hour * 60 + past.minute) }

        app.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(app.packageName))
        waitUntil("kaçırıldı bildirimi") { notifications.allNotifications.any { shadowOf(it).contentTitle == "Kaçan toplantı" } }
        val n = notifications.allNotifications.single()
        assertEquals("missed", n.channelId)
        assertTrue(shadowOf(n).contentText.toString().startsWith("Kaçırıldı"))
        waitUntil("bildirildi işareti") { runBlocking { graph.repository.get(r.id)?.lastNotifiedAt } != null }

        // İkinci yeniden başlatmada aynı hatırlatma tekrar bildirilmez.
        app.getSystemService(NotificationManager::class.java).cancelAll()
        app.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(app.packageName))
        Thread.sleep(300)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(notifications.allNotifications.isEmpty())
    }

    @Test fun bootReschedulesEverything() {
        addInOneHour()
        addInOneHour()
        val am = app.getSystemService(AlarmManager::class.java)
        alarms.scheduledAlarms.toList().forEach { am.cancel(it.operation!!) }
        assertTrue(alarms.scheduledAlarms.isEmpty())

        app.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(app.packageName))
        waitUntil("yeniden kuruldu") { alarms.scheduledAlarms.size == 2 }
        assertNotNull(alarms.scheduledAlarms.first().operation)
    }
}
