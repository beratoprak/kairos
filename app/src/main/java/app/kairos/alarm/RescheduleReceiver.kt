// Bu dosya ProfessorQuantumUniverse/reReminder (a963087) projesindeki service/BootReceiver.kt
// dosyasından uyarlanmıştır. Orijinal: Copyright (C) reReminder yazarları — GPL-3.0
//
// Alınanlar: ele alınan yayın listesi, kapalıyken kaçırılan hatırlatmalar için 12 saatlik pencere ve
// sessiz "kaçırıldı" bildirimi.
// KAİROS değişiklikleri: aynı hatırlatma daha önce bildirildiyse tekrar bildirilmez (last_notified_at);
// veriler şifreyle korunan alanda olduğundan kilit açılmadan önceki açılış (LOCKED_BOOT_COMPLETED)
// dinlenmez — yeniden kurma, kullanıcı kilidi ilk kez açınca yapılır (KURALLAR §3.5).
package app.kairos.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.kairos.KairosApp
import app.kairos.notification.Notifications
import java.time.LocalDateTime
import kotlinx.coroutines.launch

/**
 * Android bekleyen alarmları sildiğinde ya da geçersiz kıldığında hepsini yeniden kurar:
 * yeniden başlatma (kullanıcı kilidi ilk kez açınca), uygulama güncellemesi, saat veya saat dilimi
 * değişimi, kesin alarm izninin geri verilmesi. Kilit açılana kadar zamanı geçenler tek seferlik,
 * sessiz "Kaçırıldı" bildirimiyle gösterilir.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) return
        val graph = KairosApp.graph(context)
        val pending = goAsync()
        graph.scope.launch {
            try {
                Notifications.createChannels(context)
                val open = graph.repository.openForScheduling()
                graph.scheduler.syncAll(open)
                graph.briefScheduler.schedule(graph.settings.current().brief)
                if (action in BOOT) notifyMissedWhileOff(context, graph, open)
            } catch (e: Exception) {
                Log.e(TAG, "$action sonrası alarmlar kurulamadı", e)
            } finally {
                pending.finish()
            }
        }
    }

    /** Telefon kapalıyken zamanı geçen hatırlatmalar: tek seferlik, sessiz bildirim. */
    private suspend fun notifyMissedWhileOff(context: Context, graph: app.kairos.AppGraph, open: List<app.kairos.data.Reminder>) {
        val nowMillis = graph.clock.millis()
        val now = LocalDateTime.now(graph.clock)
        val settings = graph.settings.current()
        open.filter { r ->
            val due = AlarmPlanner.dueMillis(r, graph.clock.zone)
            due in (nowMillis - MISSED_WINDOW_MILLIS) until nowMillis &&
                (r.snoozedUntil ?: 0) <= nowMillis &&
                (r.lastNotifiedAt ?: 0) < due
        }.forEach { r ->
            Notifications.showReminder(context, r, Notifications.Style.MISSED, settings, now.toLocalDate(), now.hour * 60 + now.minute)
            graph.repository.markNotified(r.id)
        }
    }

    private companion object {
        const val TAG = "RescheduleReceiver"
        const val MISSED_WINDOW_MILLIS = 12 * 60 * 60_000L
        const val EXACT_ALARM_PERMISSION_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        val BOOT = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
        val HANDLED = BOOT + setOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            EXACT_ALARM_PERMISSION_CHANGED,
        )
    }
}
