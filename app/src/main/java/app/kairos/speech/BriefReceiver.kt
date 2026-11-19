package app.kairos.speech

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import app.kairos.KairosApp
import app.kairos.notification.Notifications
import java.time.LocalDateTime
import kotlinx.coroutines.launch

/**
 * Özet saati geldi: bugünkü sayıyı hesaplar, kullanıcının seçimine göre okur, yazar ya da
 * hiçbir şey yapmaz; ardından bir sonraki günü kurar.
 */
class BriefReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val graph = KairosApp.graph(context)
        val pending = goAsync()
        graph.scope.launch {
            try {
                val settings = graph.settings.current()
                val brief = settings.brief
                if (!brief.enabled) return@launch // kapatıldıysa sessizce geç; alarm zaten iptal

                val now = LocalDateTime.now(graph.clock)
                val today = now.toLocalDate()
                val sentence = BriefText.sentence(
                    todayCount = graph.repository.countForBrief(today),
                    overdueCount = graph.repository.countOverdueBefore(today),
                    minuteOfDay = now.hour * 60 + now.minute,
                    brief = brief,
                )
                when (BriefPlanner.decide(sentence, isPhoneSilent(context), brief.whenSilentMode)) {
                    BriefAction.SPEAK -> if (!DailyBriefService.start(context, sentence!!, brief.channel, brief.speechRate)) {
                        Notifications.showBrief(context, sentence)
                    }
                    BriefAction.NOTIFY -> Notifications.showBrief(context, sentence!!)
                    BriefAction.NOTHING -> Unit
                }
            } catch (e: Exception) {
                Log.e("BriefReceiver", "Günlük özet çalıştırılamadı", e)
            } finally {
                runCatching { graph.briefScheduler.schedule(graph.settings.current().brief) }
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "app.kairos.action.DAILY_BRIEF"

        /** Zil sessiz/titreşimde ya da Rahatsız Etmeyin açıksa. */
        fun isPhoneSilent(context: Context): Boolean {
            val ringer = context.getSystemService(AudioManager::class.java)?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            val filter = context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
                ?: NotificationManager.INTERRUPTION_FILTER_ALL
            return ringer != AudioManager.RINGER_MODE_NORMAL ||
                (filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
        }
    }
}
