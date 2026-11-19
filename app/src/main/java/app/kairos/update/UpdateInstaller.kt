package app.kairos.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * İndirilen sürümü Android'in resmî kurulum sistemiyle (PackageInstaller) kurar.
 * Android, yeni APK'nın aynı anahtarla imzalandığını kendisi denetler: imzası farklı bir dosya
 * (sahte ya da bozuk) güncelleme olarak kurulamaz.
 */
object UpdateInstaller {
    private const val ACTION_STATUS = "app.kairos.action.UPDATE_STATUS"

    /** Kairos'un uygulama yükleme izni var mı? (Android 8+: kullanıcı bir kez izin verir.) */
    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** APK'yı uygulamanın önbelleğine indirir. [onProgress] 0..100. Arka planda çağrılır. */
    fun download(context: Context, update: AvailableUpdate, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "kairos-${update.versionName}.apk")
        val conn = URL(update.apkUrl).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("User-Agent", "Kairos-Android")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return file
    }

    /** Kurulumu başlatır; Android kullanıcıya "Güncellensin mi?" onayını gösterir. */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("kairos.apk", 0, apk.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val status = PendingIntent.getBroadcast(
                context, sessionId,
                Intent(context, UpdateStatusReceiver::class.java).setAction(ACTION_STATUS),
                // Durum bilgisini Android doldurur: değiştirilebilir olmalı.
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(status.intentSender)
        }
    }

    /** Kurulum sonucu: onay gerekiyorsa sistem onay ekranını açar; hata olursa kullanıcıya söyler. */
    class UpdateStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    @Suppress("DEPRECATION")
                    val confirm = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                    confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { context.startActivity(it) } }
                }
                PackageInstaller.STATUS_SUCCESS -> Unit // uygulama yenilenir
                else -> {
                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.w("UpdateInstaller", "Kurulum başarısız ($status): $msg")
                    if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                        Toast.makeText(context, "Güncelleme kurulamadı. Daha sonra tekrar deneyin.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
