package app.kairos

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.lifecycleScope
import app.kairos.backup.BackupFile
import app.kairos.backup.BackupFormatException
import app.kairos.backup.BackupManager
import app.kairos.backup.RestoreDialog
import app.kairos.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.kairos.reliability.Reliability
import app.kairos.reliability.ReliabilityActions
import app.kairos.reliability.ReliabilityBanner
import app.kairos.reliability.ReliabilityStatus
import app.kairos.speech.BriefSpeaker
import app.kairos.speech.SpeakResult
import app.kairos.ui.home.HomeRoute
import app.kairos.ui.home.HomeViewModel
import app.kairos.ui.onboarding.OnboardingScreen
import app.kairos.ui.settings.SettingsScreen
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.KairosTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** İzin durumu; kullanıcı Android ayarlarından dönünce (onResume) yenilenir. */
    private val reliability = mutableStateOf<ReliabilityStatus?>(null)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshReliability()
        // Kalıcı reddedildiyse sistem penceresi artık çıkmaz: ayar sayfasını aç.
        if (!it && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            Reliability.startFirstAvailable(this, Reliability.notificationIntents(this))
        }
    }

    private val reliabilityActions by lazy {
        ReliabilityActions(
            fixNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reliability.notificationsAllowed(this) &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Reliability.startFirstAvailable(this, Reliability.notificationIntents(this))
                }
            },
            fixExactAlarms = { Reliability.startFirstAvailable(this, Reliability.exactAlarmIntents(this)) },
            fixBattery = { Reliability.startFirstAvailable(this, Reliability.batteryIntents(this)) },
            openGuide = { reliability.value?.let { Reliability.startFirstAvailable(this, listOf(Reliability.guideIntent(it))) } },
        )
    }

    // --- Yedekleme ---
    private val backup by lazy { BackupManager(this, KairosApp.graph(this)) }
    private val pendingRestore = mutableStateOf<Pair<BackupFile, Int>?>(null)

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val message = runCatching { backup.export(uri) }
                .fold({ "Yedek kaydedildi · $it hatırlatma" }, { "Yedek kaydedilemedi." })
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { backup.read(uri) }
                val current = KairosApp.graph(this@MainActivity).repository.all().size
                pendingRestore.value = file to current
            } catch (e: BackupFormatException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Dosya okunamadı.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmRestore(file: BackupFile) {
        pendingRestore.value = null
        lifecycleScope.launch {
            val message = runCatching { backup.restore(file) }
                .fold({ "Yedek geri yüklendi · ${file.reminders.size} hatırlatma" }, { "Geri yükleme tamamlanamadı. Yedek dosyası olduğu gibi duruyor; tekrar deneyebilirsiniz." })
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshReliability()
    }

    private fun refreshReliability() {
        reliability.value = Reliability.status(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = KairosApp.graph(this)
        val store = graph.settings
        val homeFactory = viewModelFactory { initializer { HomeViewModel(graph.repository, store, graph.clock) } }
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

        setContent {
            // Ayar okunana kadar hiçbir şey çizilmez; açılış penceresinin zemini görünür.
            // Böylece kullanıcının seçtiği tema yerine bir anlığına başka tema yanıp sönmez.
            val settings: AppSettings? by store.settings.collectAsState(initial = null)
            val current = settings ?: return@setContent
            val status = reliability.value ?: Reliability.status(this)
            val scope = rememberCoroutineScope()

            KairosTheme(family = current.appearance.family, mode = current.appearance.mode) {
                val dark = Kairos.colors.isDark
                LaunchedEffect(dark) {
                    val bars = if (dark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }

                if (!current.onboarding.completed) {
                    OnboardingScreen(
                        status = status,
                        actions = reliabilityActions,
                        briefMinute = current.brief.minute,
                        use24Hour = current.appearance.use24Hour,
                        onStart = { wantsBrief ->
                            scope.launch {
                                store.update {
                                    it.copy(
                                        brief = it.brief.copy(enabled = wantsBrief),
                                        onboarding = it.onboarding.copy(completed = true, briefQuestionAsked = true),
                                    )
                                }
                            }
                        },
                    )
                    return@KairosTheme
                }

                val showBanner = status.blocking && current.onboarding.dismissedReliability != status.signature
                HomeRoute(
                    vm = viewModel(factory = homeFactory),
                    banner = if (!showBanner) null else {
                        {
                            ReliabilityBanner(
                                status,
                                onFix = if (!status.notificationsAllowed) reliabilityActions.fixNotifications else reliabilityActions.fixExactAlarms,
                                onDismiss = {
                                    scope.launch { store.update { it.copy(onboarding = it.onboarding.copy(dismissedReliability = status.signature)) } }
                                },
                            )
                        }
                    },
                    settingsContent = { padding ->
                        SettingsScreen(
                            settings = current,
                            onUpdate = { change -> scope.launch { store.update(change) } },
                            contentPadding = padding,
                            onOpenSoundSettings = ::openNotificationSettings,
                            onPreviewBrief = { text -> previewBrief(text, current) },
                            appVersion = version,
                            reliability = status,
                            reliabilityActions = reliabilityActions,
                            onExport = { exportLauncher.launch(backup.suggestedName()) },
                            onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        )
                    },
                )
                pendingRestore.value?.let { (file, count) ->
                    RestoreDialog(file, count, graph.clock.zone, onConfirm = { confirmRestore(file) }, onDismiss = { pendingRestore.value = null })
                }
            }
        }
    }

    private var previewSpeaker: BriefSpeaker? = null

    /** Ayarlar > "Dinle": örnek cümleyi seçili ses ve hızla hemen okur. */
    private fun previewBrief(text: String, settings: AppSettings) {
        if (previewSpeaker != null) return
        previewSpeaker = BriefSpeaker(this).also { s ->
            s.speak(text, settings.brief.channel, settings.brief.speechRate) { result ->
                previewSpeaker = null
                if (result == SpeakResult.NO_TURKISH_VOICE) {
                    Toast.makeText(this, "Telefonunuzda Türkçe ses yüklü değil. Android ayarlarında Metin okuma bölümünden yükleyebilirsiniz.", Toast.LENGTH_LONG).show()
                    runCatching { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                } else if (result == SpeakResult.ENGINE_ERROR) {
                    Toast.makeText(this, "Metin okuma motoru yanıt vermedi.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Android'in bu uygulamaya ait bildirim ayarları: kanal başına ses ve titreşim orada seçilir. */
    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        runCatching { startActivity(intent) }
    }
}
