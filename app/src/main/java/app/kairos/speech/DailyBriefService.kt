// Kısa ön plan servisi yaklaşımı ProfessorQuantumUniverse/reReminder (a963087) projesindeki
// service/AlertSoundService.kt dosyasından uyarlanmıştır. Orijinal: Copyright (C) reReminder yazarları — GPL-3.0
//
// Android, arka planda başlayan sesi kısabilir; okuma birkaç saniyelik ön plan servisi olarak yapılır.
// Kesin alarm iznine sahip uygulamalar "systemExempted" tipini kullanabilir.
package app.kairos.speech

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.kairos.data.settings.AudioChannel
import app.kairos.notification.Notifications

class DailyBriefService : Service() {
    private var speaker: BriefSpeaker? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT)
        runCatching {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else 0
            ServiceCompat.startForeground(this, Notifications.ID_BRIEF_SPEAKING, Notifications.briefSpeaking(this), type)
        }.onFailure { Log.w(TAG, "Ön plana geçilemedi; ses kısılabilir", it) }

        if (text.isNullOrBlank() || speaker != null) {
            if (speaker == null) stop()
            return START_NOT_STICKY
        }
        val channel = runCatching { AudioChannel.valueOf(intent.getStringExtra(EXTRA_CHANNEL).orEmpty()) }.getOrDefault(AudioChannel.NOTIFICATION)
        val rate = intent.getFloatExtra(EXTRA_RATE, 1.0f)

        speaker = BriefSpeaker(this).also { s ->
            s.speak(text, channel, rate) { result ->
                // Türkçe ses yoksa ya da motor çalışmadıysa: söylenemeyen cümle bildirim olarak kalır.
                if (result != SpeakResult.DONE) Notifications.showBrief(this, text, missingVoice = result == SpeakResult.NO_TURKISH_VOICE)
                stop()
            }
        }
        return START_NOT_STICKY
    }

    private fun stop() {
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    companion object {
        private const val TAG = "DailyBriefService"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_CHANNEL = "channel"
        private const val EXTRA_RATE = "rate"

        /** @return başlatılamadıysa false (çağıran bildirime geri döner). */
        fun start(context: Context, text: String, channel: AudioChannel, rate: Float): Boolean {
            val intent = Intent(context, DailyBriefService::class.java)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_CHANNEL, channel.name)
                .putExtra(EXTRA_RATE, rate)
            return runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Log.w(TAG, "Özet servisi başlatılamadı", it) }
                .isSuccess
        }
    }
}
