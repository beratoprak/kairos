package app.kairos.update

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.kairos.ui.theme.Kairos

/** Ayarlar'daki "Güncellemeler" satırının değeri. */
fun updateStatusText(state: UpdateState): String = when (state) {
    UpdateState.Idle -> "Denetle"
    UpdateState.Checking -> "Denetleniyor…"
    UpdateState.UpToDate -> "Güncel"
    is UpdateState.Available -> "Yeni sürüm: ${state.update.versionName}"
    is UpdateState.Downloading -> "İndiriliyor %${state.percent}"
    is UpdateState.Failed -> state.message
}

/** Bugün ekranının üstünde: yeni sürüm hazırsa. "Sonra" denirse bu açılışta tekrar gösterilmez. */
@Composable
fun UpdateBanner(state: UpdateState, onUpdate: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kairos.colors
    val shape = RoundedCornerShape(12.dp)
    val (title, detail) = when (state) {
        is UpdateState.Available -> "Yeni sürüm hazır: ${state.update.versionName}" to state.update.notes.lineSequence().firstOrNull { it.isNotBlank() }
        is UpdateState.Downloading -> "Güncelleme indiriliyor… %${state.percent}" to null
        is UpdateState.Failed -> state.message to null
        else -> return
    }
    Column(modifier.fillMaxWidth().clip(shape).border(1.dp, c.accent, shape).padding(start = 14.dp, top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.padding(end = 14.dp))
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = c.inkMuted, modifier = Modifier.padding(end = 14.dp, top = 2.dp))
        if (state !is UpdateState.Downloading) {
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Sonra", style = MaterialTheme.typography.labelMedium, color = c.inkMuted,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onLater).padding(12.dp),
                )
                Text(
                    "Güncelle", style = MaterialTheme.typography.labelLarge, color = c.ink,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onUpdate).padding(12.dp),
                )
            }
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}
