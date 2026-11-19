// Bildirim kanalı ve bildirim kurulumu ProfessorQuantumUniverse/reReminder (a963087) projesindeki
// utils/NotificationHelper.kt dosyasından uyarlanmıştır. Orijinal: Copyright (C) reReminder yazarları — GPL-3.0
package app.kairos.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.kairos.MainActivity
import app.kairos.R
import app.kairos.data.Reminder
import app.kairos.data.settings.AppSettings
import app.kairos.util.TimeText
import java.time.LocalDate

/**
 * Bildirim kanalları ve hatırlatma bildirimleri.
 *
 * Kanallar kullanıcının Android ayarlarından ayrı ayrı sesini/titreşimini değiştirebileceği
 * gruplardır; uygulama hiçbirini zorla açmaz.
 */
object Notifications {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_LEAD = "lead"
    const val CHANNEL_MISSED = "missed"
    const val CHANNEL_BRIEF = "brief"
    const val CHANNEL_BRIEF_SPEAKING = "brief_speaking"

    /** Hatırlatma kimlikleriyle çakışmasın diye negatif. */
    const val ID_BRIEF = -1
    const val ID_BRIEF_SPEAKING = -2

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Kurduğunuz hatırlatmalar, zamanı geldiğinde."
                },
                NotificationChannel(CHANNEL_LEAD, "Önceden hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Bir hatırlatmadan önce istediğiniz ek bildirim."
                },
                NotificationChannel(CHANNEL_MISSED, "Kaçırılanlar", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Telefon kapalıyken zamanı geçen hatırlatmalar. Ses çıkarmaz."
                },
                NotificationChannel(CHANNEL_BRIEF, "Günlük özet", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Sesli özet okunamadığında (ör. telefon sessizdeyken) yazılı hâli."
                    setSound(null, null)
                },
                NotificationChannel(CHANNEL_BRIEF_SPEAKING, "Sesli okuma", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Özet okunurken birkaç saniye görünen gösterge."
                    setSound(null, null)
                    setShowBadge(false)
                },
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    enum class Style { DUE, LEAD, MISSED }

    /**
     * Hatırlatma bildirimini gösterir. Bildirim kimliği hatırlatma kimliğidir: aynı hatırlatmanın
     * önceden ve asıl bildirimi üst üste binmez, biri diğerinin yerine geçer.
     */
    fun showReminder(
        context: Context,
        reminder: Reminder,
        style: Style,
        settings: AppSettings,
        today: LocalDate,
        nowMinute: Int,
    ) {
        if (!canPost(context)) return
        val use24 = settings.appearance.use24Hour
        val whenText = TimeText.dayAndTime(reminder.dueDate, reminder.dueMinute, today, use24)
        val text = when (style) {
            Style.DUE -> whenText
            Style.LEAD -> "${TimeText.duration(reminder.leadMinutes ?: 0)} sonra · ${TimeText.time(reminder.dueMinute, use24)}"
            Style.MISSED -> "Kaçırıldı · $whenText"
        }
        val channel = when (style) {
            Style.DUE -> CHANNEL_REMINDERS
            Style.LEAD -> CHANNEL_LEAD
            Style.MISSED -> CHANNEL_MISSED
        }
        val snooze = settings.notifications.snoozeMinutes
        val quiet = settings.notifications.let { n -> n.quietHoursEnabled && inWindow(nowMinute, n.quietStartMinute, n.quietEndMinute) }

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (style == Style.MISSED) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSilent(quiet || style == Style.MISSED)
            .setContentIntent(openApp(context, reminder.id))
            .addAction(0, "Tamamlandı", ActionReceiver.intent(context, reminder.id, ActionReceiver.ACTION_COMPLETE))
            .addAction(
                0, "${TimeText.duration(snooze)} ertele",
                ActionReceiver.intent(context, reminder.id, ActionReceiver.ACTION_SNOOZE, snooze),
            )
            .addAction(0, "1 saat", ActionReceiver.intent(context, reminder.id, ActionReceiver.ACTION_SNOOZE, 60))
        if (style != Style.MISSED) builder.setWhen(System.currentTimeMillis()).setShowWhen(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId(reminder.id), builder.build())
        } catch (_: SecurityException) {
            // İzin tam bu sırada geri alındıysa sessizce geç; hatırlatma listede gecikmiş görünür.
        }
    }

    /** Özetin yazılı hâli: telefon sessizken ya da Türkçe ses yokken. Ses çıkarmaz. */
    fun showBrief(context: Context, text: String, missingVoice: Boolean = false) {
        if (!canPost(context)) return
        val builder = NotificationCompat.Builder(context, CHANNEL_BRIEF)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Günlük özet")
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (missingVoice) "$text\n\nTelefonunuzda Türkçe ses yüklü değil. Ayarlar > Metin okuma bölümünden yükleyebilirsiniz." else text,
                ),
            )
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, 0))
        try {
            NotificationManagerCompat.from(context).notify(ID_BRIEF, builder.build())
        } catch (_: SecurityException) {
        }
    }

    /** Özet okunurken ön plan servisinin göstergesi. */
    fun briefSpeaking(context: Context) = NotificationCompat.Builder(context, CHANNEL_BRIEF_SPEAKING)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Günlük özet okunuyor")
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setSilent(true)
        .setOngoing(true)
        .build()

    fun cancel(context: Context, reminderId: Long) =
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId))

    fun notificationId(reminderId: Long): Int = (reminderId % Int.MAX_VALUE).toInt()

    /** [start, end) aralığı; gece yarısını aşan aralıkları (23:00–07:00) da kapsar. */
    fun inWindow(minute: Int, start: Int, end: Int): Boolean =
        if (start <= end) minute in start until end else minute >= start || minute < end

    private fun openApp(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("kairos://reminder/$reminderId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
