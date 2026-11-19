package app.kairos.ui.home

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kairos.data.settings.SwipeAction
import app.kairos.snooze.SnoozePreset
import app.kairos.ui.editor.EditorDraft
import app.kairos.ui.editor.EditorSheet
import app.kairos.ui.components.Hairline
import app.kairos.ui.components.KairosIcons
import app.kairos.ui.components.Label
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.theme.Kairos

enum class HomeTab(val title: String) { TODAY("Bugün"), UPCOMING("Yakında"), COMPLETED("Tamamlanan"), SETTINGS("Ayarlar") }

/** Ekranın ihtiyaç duyduğu tüm eylemler; ViewModel'e bağlanır, önizlemede boş bırakılır. */
data class HomeActions(
    val complete: (Long) -> Unit = {},
    val reopen: (Long) -> Unit = {},
    val delete: (Long) -> Unit = {},
    val requestSnooze: (Long) -> Unit = {},
    val add: () -> Unit = {},
    val open: (Long) -> Unit = {},
)

@Composable
fun HomeScreen(
    ui: HomeUi,
    tab: HomeTab,
    onTab: (HomeTab) -> Unit,
    actions: HomeActions,
    snackbar: SnackbarHostState,
    settingsContent: @Composable (PaddingValues) -> Unit,
    banner: (@Composable () -> Unit)? = null,
) {
    val c = Kairos.colors
    BackHandler(enabled = tab != HomeTab.TODAY) { onTab(HomeTab.TODAY) }

    val onSwipe: (Long, SwipeAction) -> Unit = { id, action ->
        when (action) {
            SwipeAction.COMPLETE -> actions.complete(id)
            SwipeAction.SNOOZE -> actions.requestSnooze(id)
            SwipeAction.DELETE -> actions.delete(id)
            SwipeAction.NONE -> Unit
        }
    }

    Scaffold(
        containerColor = c.ground,
        contentColor = c.ink,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(10.dp),
                    containerColor = c.ink,
                    contentColor = c.ground,
                    actionColor = c.ground,
                )
            }
        },
        bottomBar = {
            Column(Modifier.background(c.ground).navigationBarsPadding()) {
                if (tab == HomeTab.TODAY || tab == HomeTab.UPCOMING) {
                    PrimaryButton("Hatırlatma ekle", onClick = actions.add, icon = KairosIcons.Plus, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }
                BottomNav(tab, onTab)
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { inner ->
        val padding = PaddingValues(bottom = inner.calculateBottomPadding())
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                HomeTab.TODAY -> TodayScreen(ui.today, ui.swipeRight, ui.swipeLeft, actions.complete, onSwipe, padding, onOpen = actions.open, banner = banner)
                HomeTab.UPCOMING -> UpcomingScreen(ui.upcoming, ui.swipeRight, ui.swipeLeft, actions.complete, onSwipe, padding, onOpen = actions.open)
                HomeTab.COMPLETED -> CompletedScreen(ui.completed, actions.reopen, padding)
                HomeTab.SETTINGS -> settingsContent(padding)
            }
        }
    }
}

@Composable
private fun BottomNav(selected: HomeTab, onTab: (HomeTab) -> Unit) {
    val c = Kairos.colors
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val iconsOnly = config.fontScale > 1.5f && config.screenWidthDp < 400
    Column {
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).selectableGroup()) {
            HomeTab.entries.forEach { t ->
                val on = t == selected
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = on, role = Role.Tab) { onTab(t) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val icon = when (t) {
                        HomeTab.TODAY -> KairosIcons.Today
                        HomeTab.UPCOMING -> KairosIcons.Upcoming
                        HomeTab.COMPLETED -> KairosIcons.Check
                        HomeTab.SETTINGS -> KairosIcons.Settings
                    }
                    // Çok büyük yazı boyutunda ve dar ekranda sekme adları sığmaz: sadece ikon gösterilir,
                    // ad ekran okuyucuya söylenir. Yazı kesilmez, bölünmez.
                    Icon(icon, contentDescription = if (iconsOnly) t.title else null, tint = if (on) c.ink else c.inkMuted, modifier = Modifier.size(22.dp))
                    if (!iconsOnly) BasicText(
                        t.title,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = MaterialTheme.typography.labelSmall.fontSize),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (on) c.ink else c.inkMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(Modifier.width(18.dp).height(2.dp).background(if (on) c.accent else c.ground))
                }
            }
        }
    }
}

/** Ertele seçenekleri alt sayfası: "Birazdan · 10 dk · 12:10" vb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSheet(options: List<SnoozePreset>, onPick: (SnoozePreset) -> Unit, onDismiss: () -> Unit) {
    val c = Kairos.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = c.scrim.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Label("Ertele", modifier = Modifier.padding(bottom = 8.dp))
            options.forEachIndexed { i, o ->
                if (i > 0) Hairline()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onPick(o) }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(o.title, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
                    Text(o.detail, style = MaterialTheme.typography.bodyMedium, color = c.inkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

/** ViewModel'i ekrana bağlar: sekme, geri al mesajı, erteleme sayfası. */
@Composable
fun HomeRoute(
    vm: HomeViewModel,
    settingsContent: @Composable (PaddingValues) -> Unit,
    banner: (@Composable () -> Unit)? = null,
    onSaved: () -> Unit = {},
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val appSettings by vm.appSettings.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(HomeTab.TODAY) }
    val snackbar = remember { SnackbarHostState() }
    var snoozeFor by remember { mutableStateOf<Pair<Long, List<SnoozePreset>>?>(null) }
    var draft by remember { mutableStateOf<EditorDraft?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.undo.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            // Mesaj gösterilirken yeni mesaj gelirse eskisi kapanır; toplama işi beklemez.
            launch {
                val canUndo = msg.previous != null
                val result = snackbar.showSnackbar(
                    msg.text,
                    actionLabel = if (canUndo) "Geri al" else null,
                    duration = if (canUndo) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) vm.undo(msg)
            }
        }
    }

    val current = ui ?: return Box(Modifier.fillMaxSize().background(Kairos.colors.ground))
    HomeScreen(
        ui = current,
        tab = tab,
        onTab = { tab = it },
        actions = HomeActions(
            complete = vm::complete,
            reopen = vm::reopen,
            delete = vm::delete,
            requestSnooze = { id -> scope.launch { snoozeFor = id to vm.snoozeOptions() } },
            add = { scope.launch { draft = vm.newDraft() } },
            open = { id -> scope.launch { draft = vm.editDraft(id) } },
        ),
        snackbar = snackbar,
        settingsContent = settingsContent,
        banner = banner,
    )
    snoozeFor?.let { (id, options) ->
        SnoozeSheet(options, onPick = { vm.snooze(id, it.untilMillis); snoozeFor = null }, onDismiss = { snoozeFor = null })
    }
    val d = draft
    val s = appSettings
    if (d != null && s != null) {
        EditorSheet(
            draft = d,
            settings = s,
            now = vm.now(),
            onChange = { draft = it },
            onSave = { vm.save(d); draft = null; onSaved() },
            onDelete = { vm.delete(d.id); draft = null },
            onDismiss = { draft = null },
        )
    }
}
