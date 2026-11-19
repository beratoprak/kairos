package app.kairos.reliability

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.kairos.ui.components.Hairline
import app.kairos.ui.components.KairosIcons
import app.kairos.ui.theme.Kairos

/** İzinleri düzeltmek için ekranların çağırdığı eylemler (MainActivity bağlar). */
data class ReliabilityActions(
    val fixNotifications: () -> Unit = {},
    val fixExactAlarms: () -> Unit = {},
    val fixBattery: () -> Unit = {},
    val openGuide: () -> Unit = {},
)

/**
 * İzin durumu listesi: her satırda ne işe yaradığı ve durumu ("Açık" ya da "Aç" düğmesi).
 * İlk açılışta ve Ayarlar > Güvenilirlik'te aynı liste kullanılır.
 */
@Composable
fun ReliabilityChecklist(status: ReliabilityStatus, actions: ReliabilityActions, modifier: Modifier = Modifier) {
    Column(modifier) {
        CheckRow("Bildirimler", "Hatırlatmalar bildirim olarak görünür.", status.notificationsAllowed, "Aç", actions.fixNotifications)
        Hairline(Modifier.padding(start = 16.dp))
        CheckRow("Tam saatinde alarm", "Hatırlatma dakikası dakikasına gelir.", status.exactAlarmsAllowed, "Aç", actions.fixExactAlarms)
        Hairline(Modifier.padding(start = 16.dp))
        CheckRow(
            "Pil kısıtlaması (önerilir)",
            "Uygulama ayarlarında Pil bölümünden “Kısıtlamasız” seçin.",
            status.batteryUnrestricted, "Ayarlar", actions.fixBattery, strong = false,
        )
        if (status.vendor?.aggressive == true) {
            Hairline(Modifier.padding(start = 16.dp))
            val hasAutostart = Reliability.autostartIntents(status.vendor).isNotEmpty()
            CheckRow(
                "${status.vendorName.ifEmpty { "Bu" }} telefonlar",
                if (hasAutostart) {
                    "Bu telefonlar uygulamaları arka planda durdurur; hatırlatma ancak uygulama açılınca gelir. “Otomatik başlatma”da Kairos'u açın."
                } else {
                    "Bazı üreticiler uygulamaları arka planda durdurur. Telefonunuza özel adımlar için rehbere bakın."
                },
                ok = null, action = if (hasAutostart) "Aç" else "Rehber", onAction = actions.openGuide,
            )
        }
    }
}

/**
 * @param ok true = hazır, false = düzeltilmeli, null = bilgi satırı.
 * @param strong düzeltilmesi gereken zorunlu bir izin mi? İsteğe bağlı satırlar dolu düğmeyle öne çıkarılmaz.
 */
@Composable
private fun CheckRow(title: String, detail: String, ok: Boolean?, action: String, onAction: () -> Unit, strong: Boolean = true) {
    val c = Kairos.colors
    // Büyük yazı boyutunda açıklama ile düğme yan yana sığmaz: düğme açıklamanın altına geçer.
    val stacked = LocalDensity.current.fontScale > 1.3f
    val texts: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
    }
    if (stacked) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            texts(Modifier.fillMaxWidth())
            CheckAction(ok, action, onAction, strong)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        texts(Modifier.weight(1f))
        CheckAction(ok, action, onAction, strong)
    }
}

@Composable
private fun CheckAction(ok: Boolean?, action: String, onAction: () -> Unit, strong: Boolean) {
    val c = Kairos.colors
    run {
        if (ok == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(KairosIcons.Check, contentDescription = null, tint = c.primary.takeIf { !c.isDark } ?: c.accent, modifier = Modifier.size(16.dp))
                Text("Açık", style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
            }
        } else {
            val shape = RoundedCornerShape(10.dp)
            val filled = ok == false && strong
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(shape)
                    .then(if (filled) Modifier.background(c.primary) else Modifier.border(1.dp, c.line, shape))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(action, style = MaterialTheme.typography.labelMedium, color = if (filled) c.onPrimary else c.ink)
            }
        }
    }
}

/**
 * Bugün ekranında, sadece hatırlatmaların hiç görünmemesine yol açan bir durum varsa (bildirim ya da
 * kesin alarm izni kapalı). Kullanıcı kapatabilir; aynı durum için bir daha gösterilmez.
 */
@Composable
fun ReliabilityBanner(status: ReliabilityStatus, onFix: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kairos.colors
    val shape = RoundedCornerShape(12.dp)
    val text = when {
        !status.notificationsAllowed -> "Bildirim izni kapalı. Hatırlatmalar görünmez."
        else -> "“Alarmlar ve hatırlatıcılar” izni kapalı. Hatırlatmalar geç gelebilir; uygulama kapalıyken hiç gelmeyebilir."
    }
    Column(modifier.fillMaxWidth().clip(shape).border(1.dp, c.overdue, shape).padding(start = 14.dp, top = 12.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.ink, modifier = Modifier.padding(end = 14.dp))
        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Aç",
            style = MaterialTheme.typography.labelLarge,
            color = c.overdue,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onFix).padding(horizontal = 12.dp, vertical = 12.dp),
        )
        Text(
            "Kapat",
            style = MaterialTheme.typography.labelMedium,
            color = c.inkMuted,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onDismiss).padding(end = 12.dp, start = 4.dp, top = 12.dp, bottom = 12.dp),
        )
        }
    }
}

/**
 * Hatırlatma kaydedildikten sonra, hatırlatmaların zamanında gelmesini engelleyen bir izin kapalıysa
 * bir kez gösterilir (uygulama her açılışta en fazla bir kez). Zorlamaz: "Şimdi değil" her zaman var.
 */
@Composable
fun PermissionPrompt(status: ReliabilityStatus, actions: ReliabilityActions, onDismiss: () -> Unit) {
    val c = Kairos.colors
    val (title, text, fix) = if (!status.notificationsAllowed) {
        Triple(
            "Bildirimlere izin verin",
            "Bildirim izni kapalı olduğu için hatırlatmalarınız görünmeyecek. İzin verirseniz zamanı gelince bildirim olarak gelir.",
            actions.fixNotifications,
        )
    } else {
        Triple(
            "Hatırlatmalar zamanında gelsin",
            "Telefonunuz şu an hatırlatmaları geciktirebilir; uygulama kapalıyken bildirim gelmeyebilir. Açılan ekranda “Alarmlar ve hatırlatıcılar” iznini açın.",
            actions.fixExactAlarms,
        )
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = c.ink) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { fix(); onDismiss() }) { Text("İzni aç", color = c.ink) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Şimdi değil", color = c.inkMuted) } },
    )
}
