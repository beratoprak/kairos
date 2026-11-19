// TTS yaşam döngüsü, ses kanalı seçimi ve geçici ses odağı yaklaşımı
// ProfessorQuantumUniverse/reReminder (a963087) projesindeki utils/AlertPlayer.kt dosyasından uyarlanmıştır.
// Orijinal: Copyright (C) reReminder yazarları — GPL-3.0
package app.kairos.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import app.kairos.data.settings.AudioChannel
import app.kairos.util.TimeText

/** Okumanın sonucu. */
enum class SpeakResult { DONE, NO_TURKISH_VOICE, ENGINE_ERROR }

/**
 * Tek bir cümleyi Türkçe sesle okur ve kendini kapatır. Okurken diğer sesleri (müzik vb.)
 * geçici olarak kısar; bitince geri bırakır. Her okuma için yeni bir örnek kullanılır.
 */
class BriefSpeaker(context: Context) {
    private val context = context.applicationContext
    private val audio = this.context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var focus: AudioFocusRequest? = null
    private var finished = false

    fun speak(text: String, channel: AudioChannel, rate: Float, onFinished: (SpeakResult) -> Unit) {
        val finish = { result: SpeakResult ->
            main.post {
                if (!finished) {
                    finished = true
                    release()
                    onFinished(result)
                }
            }
            Unit
        }
        // Motor hiç yanıt vermezse bile en geç 30 sn'de kapan.
        main.postDelayed({ finish(SpeakResult.ENGINE_ERROR) }, 30_000L)

        tts = TextToSpeech(context) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) return@TextToSpeech finish(SpeakResult.ENGINE_ERROR)
            val lang = engine.setLanguage(TimeText.TR)
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                return@TextToSpeech finish(SpeakResult.NO_TURKISH_VOICE)
            }
            val attrs = attributes(channel)
            engine.setAudioAttributes(attrs)
            engine.setSpeechRate(rate)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finish(SpeakResult.DONE)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish(SpeakResult.ENGINE_ERROR)
                override fun onError(utteranceId: String?, errorCode: Int) = finish(SpeakResult.ENGINE_ERROR)
            })
            requestFocus(attrs)
            val params = Bundle().apply { putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, stream(channel)) }
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE) != TextToSpeech.SUCCESS) {
                finish(SpeakResult.ENGINE_ERROR)
            }
        }
    }

    private fun release() {
        main.removeCallbacksAndMessages(null)
        focus?.let { runCatching { audio?.abandonAudioFocusRequest(it) } }
        focus = null
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
    }

    private fun requestFocus(attrs: AudioAttributes) {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .build()
        focus = request
        runCatching { audio?.requestAudioFocus(request) }
    }

    companion object {
        private const val UTTERANCE = "kairos-brief"

        fun attributes(channel: AudioChannel): AudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(
                when (channel) {
                    AudioChannel.NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION
                    AudioChannel.MEDIA -> AudioAttributes.USAGE_MEDIA
                    AudioChannel.ALARM -> AudioAttributes.USAGE_ALARM
                },
            )
            .build()

        fun stream(channel: AudioChannel) = when (channel) {
            AudioChannel.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            AudioChannel.MEDIA -> AudioManager.STREAM_MUSIC
            AudioChannel.ALARM -> AudioManager.STREAM_ALARM
        }
    }
}
