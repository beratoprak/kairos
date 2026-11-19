// Bildirim düğmesi işleme yaklaşımı (goAsync + uygulama kapsamında coroutine) bikram-agarwal/Remember
// (80d099c) projesindeki reminders/ActionReceiver.kt dosyasından uyarlanmıştır.
// Orijinal: Copyright (C) Remember yazarları — GPL-3.0
package app.kairos.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.kairos.KairosApp
import kotlinx.coroutines.launch

/** Bildirimdeki "Tamamlandı" ve "Ertele" düğmeleri. Uygulamayı açmadan çalışır. */
class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id <= 0L) return
        val graph = KairosApp.graph(context)
        val pending = goAsync()
        graph.scope.launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> graph.repository.complete(id)
                    ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(EXTRA_MINUTES, graph.settings.current().notifications.snoozeMinutes)
                        graph.repository.snooze(id, graph.clock.millis() + minutes * 60_000L)
                    }
                }
                Notifications.cancel(context, id)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "app.kairos.action.COMPLETE"
        const val ACTION_SNOOZE = "app.kairos.action.SNOOZE"
        private const val EXTRA_REMINDER_ID = "reminder_id"
        private const val EXTRA_MINUTES = "minutes"

        fun intent(context: Context, reminderId: Long, action: String, minutes: Int? = null): PendingIntent {
            val intent = Intent(context, ActionReceiver::class.java).apply {
                this.action = action
                data = Uri.parse("kairos://action/$reminderId/${action.substringAfterLast('.')}/${minutes ?: 0}")
                putExtra(EXTRA_REMINDER_ID, reminderId)
                if (minutes != null) putExtra(EXTRA_MINUTES, minutes)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
