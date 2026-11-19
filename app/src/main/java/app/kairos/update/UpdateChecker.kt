package app.kairos.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** GitHub'da yayınlanmış, telefondakinden yeni bir sürüm. */
data class AvailableUpdate(val versionName: String, val apkUrl: String, val notes: String, val sizeBytes: Long)

@Serializable
internal data class GhRelease(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GhAsset> = emptyList(),
)

@Serializable
internal data class GhAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
)

/**
 * Yeni sürüm denetimi (KURALLAR §3.7 istisnası): sadece GitHub'daki son sürümün adına bakılır.
 * Hiçbir hatırlatma, ayar ya da kişisel veri gönderilmez; istek kimliği genel "Kairos-Android"tır.
 */
object UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true }

    /** "v0.1.2" / "0.1.2" → [0, 1, 2]. Anlaşılmayan parça 0 sayılır. */
    fun parse(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V").split('.', '-', '+')
            .take(3).map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
            .let { it + List(3 - it.size) { 0 } }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate); val b = parse(current)
        for (i in 0 until 3) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }

    /** GitHub yanıtından güncellemeyi çıkarır; daha yeni değilse, taslaksa ya da APK yoksa null. */
    internal fun fromRelease(body: String, currentVersion: String): AvailableUpdate? {
        val release = json.decodeFromString(GhRelease.serializer(), body)
        if (release.draft || release.prerelease) return null
        if (!isNewer(release.tag, currentVersion)) return null
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return null
        if (!apk.url.startsWith("https://")) return null
        return AvailableUpdate(release.tag.removePrefix("v"), apk.url, release.body.orEmpty().trim(), apk.size)
    }

    /** Ağ isteği; çağıran arka planda çalıştırır. Bağlantı yoksa ya da hata varsa istisna fırlatır. */
    fun check(repo: String, currentVersion: String): AvailableUpdate? {
        val conn = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "Kairos-Android")
        try {
            if (conn.responseCode == 404) return null // henüz yayınlanmış sürüm yok
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            return fromRelease(text, currentVersion)
        } finally {
            conn.disconnect()
        }
    }
}
