package app.kairos.reliability

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
            CheckRow(
                "${status.vendorName.ifEmpty { "Bu" }} telefonlar",
                "Bazı üreticiler uygulamaları arka planda durdurur. Telefonunuza özel adımlar için rehbere bakın.",
                ok = null, action = "Rehber", onAction = actions.openGuide,
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
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
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
        else -> "Tam saatinde alarm izni kapalı. Hatırlatmalar birkaç dakika gecikebilir."
    }
    Row(
        modifier.fillMaxWidth().clip(shape).border(1.dp, c.overdue, shape).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.ink, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
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
