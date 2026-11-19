package app.kairos.backup

import android.content.Context
import android.net.Uri
import app.kairos.AppGraph
import app.kairos.backup.BackupCodec.toReminder
import java.time.LocalDate

/** Kullanıcının seçtiği dosyaya yedek yazar ya da oradan okur. Hesap/sunucu yok (KURALLAR §3.7). */
class BackupManager(private val context: Context, private val graph: AppGraph) {

    /** Önerilen dosya adı: kairos-yedek-2026-11-19.json */
    fun suggestedName(): String = "kairos-yedek-${LocalDate.now(graph.clock)}.json"

    /** @return yedeklenen hatırlatma sayısı */
    suspend fun export(uri: Uri): Int {
        val reminders = graph.repository.all()
        val text = BackupCodec.encode(reminders, graph.settings.exportRaw(), graph.clock.millis())
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Dosya açılamadı")
        out.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
        return reminders.size
    }

    /** Okur ve doğrular, ama henüz hiçbir şeyi değiştirmez (onaydan önce). */
    fun read(uri: Uri): BackupFile {
        val input = context.contentResolver.openInputStream(uri) ?: throw BackupFormatException("Dosya açılamadı.")
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return BackupCodec.decode(text)
    }

    /** Onaydan sonra: mevcut hatırlatmalar ve ayarlar yedektekilerle değişir. */
    suspend fun restore(file: BackupFile) {
        graph.repository.replaceAll(file.reminders.map { it.toReminder() })
        graph.settings.importRaw(file.settings.mapValues { (_, v) -> BackupCodec.settingValue(v) })
        graph.briefScheduler.schedule(graph.settings.current().brief)
    }
}
