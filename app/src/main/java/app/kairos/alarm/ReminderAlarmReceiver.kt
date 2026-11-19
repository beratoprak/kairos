// goAsync + uyanık tutma yaklaşımı ProfessorQuantumUniverse/reReminder (a963087) projesindeki
// service/AlarmReceiver.kt dosyasından uyarlanmıştır. Orijinal: Copyright (C) reReminder yazarları — GPL-3.0
package app.kairos.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import app.kairos.KairosApp
import app.kairos.notification.Notifications
import java.time.LocalDateTime
import kotlinx.coroutines.launch

/** Kurulan alarm çaldığında: hatırlatma hâlâ geçerliyse bildirimi gösterir. */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val kind = runCatching { AlarmKind.valueOf(intent.getStringExtra(AlarmScheduler.EXTRA_KIND).orEmpty()) }.getOrNull()
        val at = intent.getLongExtra(AlarmScheduler.EXTRA_AT, 0L)
        if (kind == null) return
        if (kind != AlarmKind.TEST && id <= 0L) return

        val graph = KairosApp.graph(context)
        val pending = goAsync()
        if (kind == AlarmKind.TEST) {
            graph.scope.launch {
                try {
                    Notifications.showTest(context, graph.settings.current())
                } finally {
                    pending.finish()
                }
            }
            return
        }
        val wakeLock = context.getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kairos:reminder")
            ?.apply { acquire(10_000L) }

        graph.scope.launch {
            try {
                val reminder = graph.repository.get(id) ?: return@launch
                // Düzenlendi, tamamlandı ya da ertelendiyse bu eski alarm artık istenmiyor.
                if (!AlarmPlanner.isStillWanted(reminder, kind, at, graph.clock.zone)) return@launch

                val now = LocalDateTime.now(graph.clock)
                Notifications.showReminder(
                    context = context,
                    reminder = reminder,
                    style = if (kind == AlarmKind.LEAD) Notifications.Style.LEAD else Notifications.Style.DUE,
                    settings = graph.settings.current(),
                    today = now.toLocalDate(),
                    nowMinute = now.hour * 60 + now.minute,
                )
                if (kind == AlarmKind.DUE) graph.repository.markNotified(id)
            } catch (e: Exception) {
                Log.e("ReminderAlarm", "Hatırlatma $id gösterilemedi", e)
            } finally {
                runCatching { if (wakeLock?.isHeld == true) wakeLock.release() }
                pending.finish()
            }
        }
    }
}
