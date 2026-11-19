package app.kairos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.ThemeFamily
import app.kairos.ui.theme.ThemeMode

/**
 * Ayarlar > Görünüm > Tema. Her tema, açık ve koyu hâlini birlikte gösteren küçük bir
 * örnekle seçilir; altında Açık / Koyu / Telefonla aynı seçimi bulunur.
 */
@Composable
fun ThemePicker(
    selectedFamily: ThemeFamily,
    selectedMode: ThemeMode,
    onFamily: (ThemeFamily) -> Unit,
    onMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Kairos.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeFamily.entries.forEach { family ->
                val selected = family == selectedFamily
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .selectable(selected = selected, role = Role.RadioButton) { onFamily(family) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Swatch(family, selected)
                    Text(
                        text = family.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) c.ink else c.inkMuted,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(1.dp, c.line, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .selectableGroup(),
        ) {
            ThemeMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(if (selected) c.primary else Color.Transparent)
                        .selectable(selected = selected, role = Role.RadioButton) { onMode(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) c.onPrimary else c.ink,
                    )
                }
            }
        }
    }
}

/** Sol yarı açık, sağ yarı koyu: temanın iki hâli tek bakışta görünür. */
@Composable
private fun Swatch(family: ThemeFamily, selected: Boolean) {
    val c = Kairos.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            // Seçili değilken de belirgin kenar: koyu zeminde koyu Taş/Grafit örnekleri kaybolmasın.
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.inkFaint, CircleShape)
            .padding(4.dp)
            .clip(CircleShape),
    ) {
        Row(Modifier.size(40.dp)) {
            Box(Modifier.weight(1f).height(40.dp).background(Color(family.light.primary)))
            Box(Modifier.weight(1f).height(40.dp).background(Color(family.dark.ground)))
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(family.light.accent)),
        )
    }
}
