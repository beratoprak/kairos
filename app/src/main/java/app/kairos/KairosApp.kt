package app.kairos

import app.kairos.update.UpdateManager
import android.app.Application
import android.content.Context
import app.kairos.alarm.AlarmScheduler
import app.kairos.data.KairosDatabase
import app.kairos.data.ReminderRepository
import app.kairos.data.settings.SettingsStore
import app.kairos.notification.Notifications
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import app.kairos.speech.BriefScheduler

/** Uygulama genelinde tek örnek nesneler. Ekranlar, alıcılar ve servisler buradan alır. */
class AppGraph(context: Context) {
    private val app = context.applicationContext
    val clock: Clock = Clock.systemDefaultZone()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settings: SettingsStore = SettingsStore.create(app)
    val database: KairosDatabase = KairosDatabase.create(app)
    val scheduler = AlarmScheduler(app, clock)
    val repository = ReminderRepository(database.reminders(), clock, scheduler)
    val briefScheduler = BriefScheduler(app, clock)
    val updates = UpdateManager(app, scope, BuildConfig.UPDATE_REPO, BuildConfig.VERSION_NAME)
}

class KairosApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        // Açılışta alarmları eşitle: başka bir yoldan kaybolmuş alarm varsa geri gelsin.
        graph.scope.launch {
            graph.scheduler.syncAll(graph.repository.openForScheduling())
            // Sadece kullanıcı "Tamamlananları sil" ayarını açtıysa (varsayılan: hiçbir zaman).
            graph.settings.current().list.purgeCompletedAfterDays?.let { graph.repository.purgeCompleted(it) }
        }
        // Özet ayarı her değiştiğinde (açma, kapama, saat, günler) alarmı yeniden kur.
        graph.scope.launch {
            graph.settings.settings.map { it.brief }.distinctUntilChanged().collect { graph.briefScheduler.schedule(it) }
        }
    }

    companion object {
        fun graph(context: Context): AppGraph = (context.applicationContext as KairosApp).graph
    }
}
