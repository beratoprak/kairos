// Alarm kurma yaklaşımı ProfessorQuantumUniverse/reReminder (a963087) projesindeki
// service/ReminderScheduler.kt dosyasından uyarlanmıştır. Orijinal: Copyright (C) reReminder yazarları — GPL-3.0
//
// Alınanlar: setExactAndAllowWhileIdle ile Doze'a dayanıklı alarm; izin yoksa ya da reddedilirse
// hatırlatmayı düşürmek yerine kesin olmayan alarma geri dönüş; data URI ile birbirinden ayrışan
// PendingIntent'ler; aynı girdiyle tekrar çalıştırıldığında aynı sonucu veren senkronizasyon.
// Çıkarılanlar: aralıklı tekrar ızgarası, ana anahtar, alarm stili.
package app.kairos.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import app.kairos.data.Reminder
import app.kairos.data.ReminderAlarms
import java.time.Clock

class AlarmScheduler(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ReminderAlarms {
    private val context = context.applicationContext
    private val alarmManager = this.context.getSystemService(AlarmManager::class.java)

    /** Bir hatırlatmanın alarmlarını günceller: eskileri iptal eder, gerekenleri kurar. */
    override fun onChanged(reminder: Reminder) {
        cancel(reminder.id)
        AlarmPlanner.plan(reminder, clock.millis(), clock.zone).forEach(::set)
    }

    override fun onRemoved(reminderId: Long) = cancel(reminderId)

    /** Açılış, yeniden başlatma, saat/saat dilimi değişimi sonrası: tüm açık hatırlatmalar. */
    fun syncAll(open: List<Reminder>) = open.forEach(::onChanged)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun set(alarm: PlannedAlarm) {
        val pending = pendingIntent(alarm.reminderId, alarm.kind, alarm.atMillis)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.atMillis, pending)
            } else {
                // İzin kapalı: hatırlatmayı düşürme, birkaç dakika sapmayla da olsa çalsın.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.atMillis, pending)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Kesin alarm reddedildi (${alarm.reminderId}), kesin olmayana geçiliyor", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.atMillis, pending)
        }
    }

    private fun cancel(reminderId: Long) {
        AlarmKind.entries.forEach { kind -> alarmManager.cancel(pendingIntent(reminderId, kind, 0L)) }
    }

    private fun pendingIntent(reminderId: Long, kind: AlarmKind, atMillis: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            // Extras filterEquals'ta yok sayılır; her hatırlatma+tür için ayrı URI gerekir.
            data = Uri.parse("kairos://reminder/$reminderId/${kind.name.lowercase()}")
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_KIND, kind.name)
            putExtra(EXTRA_AT, atMillis)
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_FIRE = "app.kairos.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_AT = "at"
    }
}
