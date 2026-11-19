// Bu dosya ProfessorQuantumUniverse/reReminder (a963087) projesindeki utils/Reliability.kt
// dosyasından uyarlanmıştır. Orijinal: Copyright (C) reReminder yazarları — GPL-3.0
//
// Alınanlar: üretici listesi (dontkillmyapp.com), önek eşleşmesi, sorun imzası, "ilk çalışan
// ekranı aç" yaklaşımı. KAİROS değişiklikleri: bildirim izni de denetlenir; pil için
// REQUEST_IGNORE_BATTERY_OPTIMIZATIONS izni KULLANILMAZ (Google Play kısıtlaması), uygulamanın
// ayar sayfası açılır.
package app.kairos.reliability

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.Locale

/** dontkillmyapp.com'da sayfası olan üretici. [aggressive]: izinler tam olsa bile uygulamayı durdurabilir. */
enum class DkmaVendor(val slug: String, val aggressive: Boolean, val keys: List<String>) {
    XIAOMI("xiaomi", true, listOf("xiaomi", "redmi", "poco", "blackshark")),
    HUAWEI("huawei", true, listOf("huawei", "honor")),
    ONEPLUS("oneplus", true, listOf("oneplus")),
    OPPO("oppo", true, listOf("oppo")),
    VIVO("vivo", true, listOf("vivo", "iqoo")),
    REALME("realme", true, listOf("realme")),
    MEIZU("meizu", true, listOf("meizu")),
    SAMSUNG("samsung", true, listOf("samsung")),
    ASUS("asus", true, listOf("asus")),
    LENOVO("lenovo", true, listOf("lenovo")),
    BLACKVIEW("blackview", true, listOf("blackview")),
    WIKO("wiko", true, listOf("wiko")),
    UNIHERTZ("unihertz", true, listOf("unihertz")),
    TECNO("tecno", true, listOf("tecno")),
    ULEFONE("ulefone", true, listOf("ulefone")),
    SONY("sony", false, listOf("sony")),
    NOKIA("nokia", false, listOf("nokia", "hmd")),
    HTC("htc", false, listOf("htc")),
    MOTOROLA("motorola", false, listOf("motorola", "moto")),
    GOOGLE("google", false, listOf("google")),
}

/** Hatırlatmanın zamanında gelmesini engelleyebilecek tek bir durum. */
enum class ReliabilityIssue { NOTIFICATIONS, EXACT_ALARMS, BATTERY, VENDOR }

data class ReliabilityStatus(
    val vendor: DkmaVendor?,
    val vendorName: String,
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val issues: List<ReliabilityIssue> = buildList {
        if (!notificationsAllowed) add(ReliabilityIssue.NOTIFICATIONS)
        if (!exactAlarmsAllowed) add(ReliabilityIssue.EXACT_ALARMS)
        if (!batteryUnrestricted) add(ReliabilityIssue.BATTERY)
        if (vendor?.aggressive == true) add(ReliabilityIssue.VENDOR)
    }

    /** Hatırlatmaların hiç görünmemesine yol açan durumlar (Bugün ekranında uyarı gerektirir). */
    val blocking: Boolean get() = !notificationsAllowed || !exactAlarmsAllowed

    /** Bu sorun birleşimi için kapatma imzası: yeni bir sorun çıkarsa uyarı bir kez daha gösterilir. */
    val signature: String get() = (listOf(vendor?.slug ?: "-") + issues.map { it.name }).joinToString("|")

    val guideUrl: String get() = Reliability.guideUrl(vendor)
}

object Reliability {
    private const val BASE_URL = "https://dontkillmyapp.com"

    /** Önek eşleşmesi: "mi" alt dizesi Micromax'ı Xiaomi sanmasın. Test edilebilsin diye parametreli. */
    fun vendorOf(manufacturer: String?, brand: String?): DkmaVendor? {
        val candidates = listOfNotNull(manufacturer?.normalized(), brand?.normalized()).filter { it.isNotEmpty() }
        return DkmaVendor.entries.firstOrNull { v -> v.keys.any { key -> candidates.any { it.startsWith(key) } } }
    }

    fun guideUrl(vendor: DkmaVendor?) = "$BASE_URL/${vendor?.slug ?: "general"}"

    /** Kullanıcının bildiği yazım: "xiaomi" → "Xiaomi"; "HONOR", "OnePlus" olduğu gibi kalır. */
    fun vendorNameOf(manufacturer: String?): String {
        val raw = manufacturer?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        return if (raw == raw.lowercase(Locale.ROOT)) raw.replaceFirstChar { it.titlecase(Locale.ROOT) } else raw
    }

    fun status(context: Context): ReliabilityStatus = ReliabilityStatus(
        vendor = vendorOf(Build.MANUFACTURER, Build.BRAND),
        vendorName = vendorNameOf(Build.MANUFACTURER),
        notificationsAllowed = notificationsAllowed(context),
        exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
    )

    fun notificationsAllowed(context: Context): Boolean {
        val runtime = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtime && androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun self(context: Context) = Uri.parse("package:${context.packageName}")

    /** Bu uygulamanın "tam saatinde alarm" anahtarı; yoksa uygulama ayarları. */
    fun exactAlarmIntents(context: Context): List<Intent> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        return listOf(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(self(context)),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(self(context)),
        )
    }

    /** Pil: önce bu uygulamanın ayar sayfası (Pil > Kısıtlamasız), yoksa tüm uygulamalar listesi. */
    fun batteryIntents(context: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(self(context)),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    )

    /** Bildirim izni kalıcı reddedildiyse: uygulamanın bildirim ayarları. */
    fun notificationIntents(context: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(self(context)),
    )

    fun guideIntent(status: ReliabilityStatus) = Intent(Intent.ACTION_VIEW, Uri.parse(status.guideUrl))

    /**
     * Uygulamaları arka planda durduran üreticilerin "otomatik başlatma" ekranları. Bu izin
     * kapalıyken uygulama kapalıyken alarm gelmez, ancak uygulama açılınca gelir. Ekran adları
     * sürümden sürüme değiştiği için birkaç yol denenir; hiçbiri yoksa rehber açılır.
     */
    fun autostartIntents(vendor: DkmaVendor?): List<Intent> {
        fun c(pkg: String, cls: String) = Intent().setComponent(android.content.ComponentName(pkg, cls))
        return when (vendor) {
            DkmaVendor.XIAOMI -> listOf(
                c("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            )
            DkmaVendor.OPPO, DkmaVendor.REALME, DkmaVendor.ONEPLUS -> listOf(
                c("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                c("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                c("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            )
            DkmaVendor.VIVO -> listOf(
                c("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                c("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            )
            DkmaVendor.HUAWEI -> listOf(
                c("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                c("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            )
            else -> emptyList()
        }
    }

    /** Cihazın açabildiği ilk ekranı açar. Üreticiler bu ekranları sık kaldırdığından tek denemeye güvenilmez. */
    fun startFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }

    private fun String.normalized() = lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
}
