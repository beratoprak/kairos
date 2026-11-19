package app.kairos.ui.home

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import app.kairos.KairosApp
import app.kairos.ui.editor.EditorLogic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Proje sahibinin örneği (KURALLAR §1.7): yarın için 12:00'ye hatırlatma eklenir;
 * bir sonraki eklemede saat varsayılan olarak 12:00 gelir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LastUsedTimeFlowTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val graph get() = KairosApp.graph(app)

    private fun waitUntil(what: String, condition: () -> Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("Beklenen olmadı: $what")
    }

    @Test fun nextReminderDefaultsToTheTimeJustUsed() {
        val vm = HomeViewModel(graph.repository, graph.settings, graph.clock)
        val first = runBlocking { vm.newDraft() }
        val tomorrow = vm.now().toLocalDate().plusDays(1)

        vm.save(first.copy(title = "Öğle yemeği", date = tomorrow, minute = 12 * 60, timeTouched = true))
        waitUntil("kaydedildi") { runBlocking { graph.repository.openForScheduling() }.isNotEmpty() }
        waitUntil("saat hatırlandı") { runBlocking { graph.settings.current().newReminder.lastUsedMinute } == 12 * 60 }

        val saved = runBlocking { graph.repository.openForScheduling() }.single()
        assertEquals(12 * 60, saved.dueMinute)
        assertEquals(tomorrow, saved.dueDate)
        assertNull("tekrar varsayılan olarak yok", saved.recurrence)

        val next = runBlocking { vm.newDraft() }
        val forTomorrow = EditorLogic.withDate(next, tomorrow, vm.now(), runBlocking { graph.settings.current() })
        assertEquals(12 * 60, forTomorrow.minute)
    }

    @Test fun editingKeepsIdAndUpdatesTime() {
        val vm = HomeViewModel(graph.repository, graph.settings, graph.clock)
        val r = runBlocking { graph.repository.add("Banka", vm.now().toLocalDate().plusDays(2), 600) }
        val d = runBlocking { vm.editDraft(r.id) }!!
        vm.save(d.copy(minute = 630, important = true))
        waitUntil("güncellendi") { runBlocking { graph.repository.get(r.id) }?.dueMinute == 630 }
        assertEquals(true, runBlocking { graph.repository.get(r.id) }!!.important)
    }
}
