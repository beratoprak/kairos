package app.kairos.backup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.kairos.ui.theme.Kairos
import app.kairos.util.TimeText
import java.time.Instant
import java.time.ZoneId

/** Geri yüklemeden önce onay: neyin değişeceği açıkça yazılır; geri alınamaz. */
@Composable
fun RestoreDialog(file: BackupFile, currentCount: Int, zone: ZoneId, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = Kairos.colors
    val at = Instant.ofEpochMilli(file.exportedAt).atZone(zone).toLocalDateTime()
    val date = "${at.dayOfMonth} ${at.month.getDisplayName(java.time.format.TextStyle.FULL, TimeText.TR)} ${at.year}, ${TimeText.time(at.hour * 60 + at.minute)}"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        title = { Text("Yedekten geri yüklensin mi?", style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = {
            Text(
                "Yedek tarihi: $date\nYedekte ${file.reminders.size} hatırlatma var.\n\n" +
                    "Telefondaki $currentCount hatırlatma ve tüm ayarlar yedektekilerle değiştirilecek. Bu işlem geri alınamaz.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.ink,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Geri yükle", color = c.overdue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = c.ink) } },
    )
}
