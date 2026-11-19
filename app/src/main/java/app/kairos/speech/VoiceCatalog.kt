package app.kairos.speech

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Ayarlar'daki ses seçicisinde bir satır. */
data class VoiceOption(val name: String, val label: String, val detail: String)

/**
 * Telefondaki Türkçe sesler. Telefonun varsayılan motoru (ör. Samsung'unki) çoğu zaman robotik
 * okur; Google'ın konuşma motoru kuruluysa o kullanılır ve en yüksek kaliteli ses seçilir.
 */
object VoiceCatalog {
    const val GOOGLE_ENGINE = "com.google.android.tts"

    fun isGoogleEngineInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(GOOGLE_ENGINE, 0) }.isSuccess

    /** Google motoru kuruluysa onu, değilse telefonun varsayılanını (null) kullan. */
    fun preferredEngine(context: Context): String? = GOOGLE_ENGINE.takeIf { isGoogleEngineInstalled(context) }

    /** Kurulu (indirilmiş) Türkçe sesler, en kaliteliden başlayarak; aynı kalitede cihazdaki ses önce. */
    fun turkishVoices(engine: TextToSpeech): List<Voice> =
        runCatching { engine.voices.orEmpty() }.getOrDefault(emptySet())
            .filter { it.locale.language == "tr" }
            .filterNot { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features.orEmpty() }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.isNetworkConnectionRequired }.thenBy { it.name })

    /** Kullanıcının seçtiği ses varsa o, yoksa en kaliteli Türkçe ses. */
    fun pick(engine: TextToSpeech, preferredName: String?): Voice? {
        val voices = turkishVoices(engine)
        return voices.firstOrNull { it.name == preferredName } ?: voices.firstOrNull()
    }

    /** Ayarlar için ses listesi. Motor 5 sn içinde yanıt vermezse boş liste. */
    suspend fun load(context: Context): List<VoiceOption> = withTimeoutOrNull(5_000L) {
        suspendCancellableCoroutine { cont ->
            var tts: TextToSpeech? = null
            tts = TextToSpeech(context.applicationContext, { status ->
                val engine = tts
                val list = if (status == TextToSpeech.SUCCESS && engine != null) {
                    turkishVoices(engine).mapIndexed { i, v ->
                        VoiceOption(
                            name = v.name,
                            label = "Ses ${i + 1}",
                            detail = buildList {
                                add(qualityText(v.quality))
                                if (v.isNetworkConnectionRequired) add("internet gerekir")
                            }.joinToString(" · "),
                        )
                    }
                } else emptyList()
                runCatching { engine?.shutdown() }
                if (cont.isActive) cont.resume(list)
            }, preferredEngine(context))
            cont.invokeOnCancellation { runCatching { tts?.shutdown() } }
        }
    } ?: emptyList()

    private fun qualityText(q: Int) = when {
        q >= Voice.QUALITY_VERY_HIGH -> "Çok yüksek kalite"
        q >= Voice.QUALITY_HIGH -> "Yüksek kalite"
        q >= Voice.QUALITY_NORMAL -> "Normal kalite"
        else -> "Düşük kalite"
    }

    /** Daha doğal ses: Google'ın motoru yoksa Play Store sayfası, varsa ses verisi indirme ekranı. */
    fun installIntents(context: Context): List<Intent> =
        if (!isGoogleEngineInstalled(context)) listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_ENGINE")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_ENGINE")),
        ) else listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(GOOGLE_ENGINE),
            Intent("com.android.settings.TTS_SETTINGS"),
        )
}
