package app.kairos.speech

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.kairos.data.settings.DailyBrief
import java.time.Clock
import java.time.LocalDateTime

/**
 * Günlük özet alarmı. Tek bir alarm vardır: her çalışmadan sonra bir sonraki gün kurulur.
 * Özet kapatılırsa alarm iptal edilir; uygulama kendiliğinden hiçbir şey söylemez (KURALLAR §1.4).
 */
class BriefScheduler(context: Context, private val clock: Clock) {
    private val context = context.applicationContext
    private val alarmManager = this.context.getSystemService(AlarmManager::class.java)

    /** @return kurulan an (test ve ayar ekranı için); özet kapalıysa null. */
    fun schedule(brief: DailyBrief): LocalDateTime? {
        val pending = pendingIntent()
        alarmManager.cancel(pending)
        val next = BriefPlanner.nextAt(LocalDateTime.now(clock), brief) ?: return null
        val at = next.atZone(clock.zone).toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
        return next
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, BriefReceiver::class.java).setAction(BriefReceiver.ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
