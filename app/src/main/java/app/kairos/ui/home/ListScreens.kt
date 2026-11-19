package app.kairos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.kairos.data.settings.SwipeAction
import app.kairos.ui.components.EmptyNote
import app.kairos.ui.components.Hairline
import app.kairos.ui.components.Label
import app.kairos.ui.theme.Kairos

@Composable
private fun ScreenHeader(title: String, subtitle: String?) {
    val c = Kairos.colors
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = c.ink, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
    }
}

/** Yakında: Yarın / Bu hafta / Daha sonra. */
@Composable
fun UpcomingScreen(
    sections: List<SectionUi>,
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    onComplete: (Long) -> Unit,
    onSwipe: (Long, SwipeAction) -> Unit,
    contentPadding: PaddingValues,
    onOpen: (Long) -> Unit = {},
) {
    val total = sections.sumOf { it.rows.size }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item("header") { ScreenHeader("Yakında", if (total == 0) null else "$total hatırlatma") }
        if (sections.isEmpty()) {
            item("empty") {
                EmptyNote(
                    "Önümüzdeki günler için hatırlatma yok.",
                    "Hatırlatma eklerken gün olarak Yarın ya da başka bir tarih seçebilirsiniz.",
                    Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        sections.forEach { section ->
            item("s-${section.title}") { Label(section.title, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)) }
            items(section.rows, key = { "u${it.id}" }) { row ->
                SwipeRow(swipeRight, swipeLeft, onAction = { onSwipe(row.id, it) }) {
                    TimelineRow(row, onCheck = { onComplete(row.id) }, modifier = Modifier.padding(horizontal = 20.dp), onOpen = { onOpen(row.id) })
                }
            }
            item("e-${section.title}") {
                Hairline(Modifier.padding(start = 90.dp, end = 20.dp))
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Tamamlananlar: en yeni üstte, her satırda "Geri aç". */
@Composable
fun CompletedScreen(rows: List<RowUi>, onReopen: (Long) -> Unit, contentPadding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item("header") { ScreenHeader("Tamamlanan", if (rows.isEmpty()) null else "${rows.size} hatırlatma") }
        if (rows.isEmpty()) {
            item("empty") {
                EmptyNote(
                    "Henüz tamamlanan hatırlatma yok.",
                    "Tamamladığınız hatırlatmalar burada durur; isterseniz geri açabilirsiniz.",
                    Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        items(rows, key = { "c${it.id}" }) { row ->
            CompletedRow(row, onReopen = { onReopen(row.id) }, modifier = Modifier.padding(horizontal = 20.dp))
        }
        item("end") { Spacer(Modifier.height(24.dp)) }
    }
}
