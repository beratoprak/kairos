package app.kairos.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.remember
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kairos.data.settings.SwipeAction
import app.kairos.ui.components.CheckCircle
import app.kairos.ui.components.Hairline
import app.kairos.ui.components.KairosIcons
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.TimeStyles

/**
 * Bugün ve Yakında listelerindeki satır: solda saat sütunu, sağda üstte ince çizgi.
 * Sıradaki hatırlatmanın çizgisi pirinç renginde ve 2 dp'dir.
 */
@Composable
fun TimelineRow(row: RowUi, onCheck: (() -> Unit)?, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    val c = Kairos.colors
    val timeWidth = timeColumnWidth(row.time)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(c.ground)
            .then(if (onOpen != null) Modifier.clickable(onClickLabel = "Düzenle", onClick = onOpen) else Modifier),
    ) {
        // Başlığa en az ~180 dp kalmıyorsa (dar ekran, büyük yazı) saat başlığın üstüne alınır;
        // kelimeler ortadan bölünmez.
        if (maxWidth - timeWidth - 58.dp < 180.dp) {
            StackedRow(row, onCheck)
        } else {
            SideBySideRow(row, onCheck, timeWidth)
        }
    }
}

@Composable
private fun StackedRow(row: RowUi, onCheck: (() -> Unit)?) {
    val c = Kairos.colors
    Column {
        Hairline(color = if (row.isNext) c.accent else c.line, thickness = if (row.isNext) 2.dp else 1.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(checked = row.isDone, onClick = onCheck, label = if (row.isDone) "Tamamlandı" else "Tamamlandı olarak işaretle")
            Text(
                row.time,
                style = TimeStyles.Row,
                color = if (row.isDone) c.inkFaint else c.ink,
                textDecoration = if (row.isDone) TextDecoration.LineThrough else null,
            )
        }
        RowText(row, Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 12.dp))
    }
}

@Composable
private fun SideBySideRow(row: RowUi, onCheck: (() -> Unit)?, timeWidth: Dp) {
    val c = Kairos.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            row.time,
            style = TimeStyles.Row,
            maxLines = 1,
            softWrap = false,
            color = if (row.isDone) c.inkFaint else c.ink,
            textDecoration = if (row.isDone) TextDecoration.LineThrough else null,
            textAlign = TextAlign.End,
            modifier = Modifier.width(timeWidth).padding(top = 14.dp),
        )
        Column(Modifier.weight(1f)) {
            Hairline(color = if (row.isNext) c.accent else c.line, thickness = if (row.isNext) 2.dp else 1.dp)
            Row(
                Modifier.padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Row(Modifier.padding(start = 0.dp).width(44.dp)) {
                    CheckCircle(checked = row.isDone, onClick = onCheck, label = if (row.isDone) "Tamamlandı" else "Tamamlandı olarak işaretle")
                }
                RowText(row, Modifier.weight(1f).padding(top = 10.dp, bottom = 10.dp, start = 2.dp))
            }
        }
    }
}

@Composable
private fun RowText(row: RowUi, modifier: Modifier = Modifier) {
    val c = Kairos.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            row.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (row.isDone) c.inkFaint else c.ink,
            // Başlık kesilmez; uzunsa alt satırlara geçer.
        )
        if (row.meta != null || row.important) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (row.important) Icon(KairosIcons.Bell, contentDescription = "Önemli", tint = if (row.overdue) c.overdue else c.inkMuted, modifier = Modifier.size(14.dp))
                Text(
                    row.meta ?: "Önemli",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.overdue) c.overdue else c.inkMuted,
                )
            }
        }
    }
}

/** Gecikmiş hatırlatma: kart içinde, halka ve açıklama gecikmiş renginde. */
@Composable
fun OverdueCard(row: RowUi, onCheck: () -> Unit, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    val c = Kairos.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .then(if (onOpen != null) Modifier.clickable(onClickLabel = "Düzenle", onClick = onOpen) else Modifier)
            .border(1.dp, c.line, shape)
            .padding(start = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked = false, onClick = onCheck, ringColor = c.overdue, size = 24.dp)
        RowText(row, Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp))
    }
}

/** Tamamlanan listesindeki satır: "Geri aç" düğmesiyle. */
@Composable
fun CompletedRow(row: RowUi, onReopen: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kairos.colors
    Column(modifier.fillMaxWidth().background(c.ground)) {
        Hairline()
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(checked = true, onClick = null, label = "Tamamlandı")
            RowText(row, Modifier.weight(1f).padding(vertical = 12.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onReopen)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(KairosIcons.Undo, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(16.dp))
                Text("Geri aç", style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
            }
        }
    }
}

/**
 * Kaydırma: sağa ve sola hangi işin yapılacağı Ayarlar'dan seçilir (varsayılan sağ = tamamla,
 * sol = ertele). Satır her zaman yerine döner; işi veri değişikliği yapar (geri al güvenli kalır).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRow(
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    onAction: (SwipeAction) -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onAction(swipeRight)
                SwipeToDismissBoxValue.EndToStart -> onAction(swipeLeft)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
        positionalThreshold = { it * 0.35f },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = swipeRight != SwipeAction.NONE,
        enableDismissFromEndToStart = swipeLeft != SwipeAction.NONE,
        backgroundContent = {
            // Satır dururken arka plan çizilmez; sadece kaydırma yönündeki eylem görünür.
            when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> SwipeBackground(swipeRight, alignStart = true)
                SwipeToDismissBoxValue.EndToStart -> SwipeBackground(swipeLeft, alignStart = false)
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
    ) {
        // Satırın zemini tüm genişliği kaplar; kaydırma arka planı kenarlardan sızmaz.
        Box(Modifier.fillMaxWidth().background(Kairos.colors.ground)) { content() }
    }
}

@Composable
private fun RowScope.SwipeBackground(action: SwipeAction, alignStart: Boolean) {
    val c = Kairos.colors
    val (bg, fg, icon, label) = when (action) {
        SwipeAction.COMPLETE -> Quad(c.primary, c.onPrimary, KairosIcons.Check, "Tamamla")
        SwipeAction.SNOOZE -> Quad(c.ink, c.ground, KairosIcons.Snooze, "Ertele")
        SwipeAction.DELETE -> Quad(c.overdue, c.ground, KairosIcons.Trash, "Sil")
        SwipeAction.NONE -> Quad(Color.Transparent, Color.Transparent, null, "")
    }
    Row(
        Modifier.fillMaxSize().background(bg).padding(horizontal = 20.dp),
        horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Text("  $label", style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}

private data class Quad(val bg: Color, val fg: Color, val icon: ImageVector?, val label: String)

/**
 * Saat sütununun genişliği: o yazı boyutunda en geniş saat metni ("00:00" ya da "12:00 ÖS")
 * kadar. Böylece büyük yazı boyutunda saat kesilmez, satırlar yine aynı hizada kalır.
 */
@Composable
private fun timeColumnWidth(sample: String): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widest = if (sample.contains('Ö')) "12:00 ÖS" else "00:00"
    return remember(widest, density) {
        with(density) { measurer.measure(widest, TimeStyles.Row).size.width.toDp() }
    }.coerceAtLeast(56.dp)
}
