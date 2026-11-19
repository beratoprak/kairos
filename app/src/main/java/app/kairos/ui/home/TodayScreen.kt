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

/** Ana ekran: Gecikmiş (kartlar) ve Bugün (saat sütunlu liste). */
@Composable
fun TodayScreen(
    ui: TodayUi,
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    onComplete: (Long) -> Unit,
    onSwipe: (Long, SwipeAction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onOpen: (Long) -> Unit = {},
    banner: (@Composable () -> Unit)? = null,
) {
    val c = Kairos.colors
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        item("header") {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(ui.dateLabel)
                Text("Bugün", style = MaterialTheme.typography.headlineLarge, color = c.ink, modifier = Modifier.semantics { heading() })
                Text(ui.summary, style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
            }
        }
        if (banner != null) item("banner") { Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) { banner() } }

        if (ui.isEmpty) {
            item("empty") {
                EmptyNote(
                    title = "Bugün için hatırlatma yok.",
                    hint = "Yeni bir hatırlatma için aşağıdaki düğmeyi kullanın.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        if (ui.overdue.isNotEmpty()) {
            item("overdue-label") { Label("Gecikmiş", color = c.overdue, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp)) }
            items(ui.overdue, key = { "o${it.id}" }) { row ->
                SwipeRow(swipeRight, swipeLeft, onAction = { onSwipe(row.id, it) }) {
                    OverdueCard(row, onCheck = { onComplete(row.id) }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), onOpen = { onOpen(row.id) })
                }
            }
            item("overdue-gap") { Spacer(Modifier.height(16.dp)) }
        }

        if (ui.today.isNotEmpty()) {
            item("today-label") { Label("Bugün", modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)) }
            items(ui.today, key = { "t${it.id}" }) { row ->
                if (row.isDone) {
                    TimelineRow(row, onCheck = null, modifier = Modifier.padding(horizontal = 20.dp))
                } else {
                    SwipeRow(swipeRight, swipeLeft, onAction = { onSwipe(row.id, it) }) {
                        TimelineRow(row, onCheck = { onComplete(row.id) }, modifier = Modifier.padding(horizontal = 20.dp), onOpen = { onOpen(row.id) })
                    }
                }
            }
            item("today-end") { Hairline(Modifier.padding(start = 90.dp, end = 20.dp)) }
        }
        item("bottom") { Spacer(Modifier.height(24.dp)) }
    }
}
