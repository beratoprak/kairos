// İlk açılış akışının yapısı (izin adımları, durumun geri dönüşte yenilenmesi) ProfessorQuantumUniverse/reReminder
// (a963087) projesindeki ui/setup/SetupScreen.kt dosyasından esinlenmiştir. Orijinal: GPL-3.0
package app.kairos.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.kairos.reliability.ReliabilityActions
import app.kairos.reliability.ReliabilityChecklist
import app.kairos.reliability.ReliabilityStatus
import app.kairos.ui.components.Label
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.theme.BarlowSemiCondensed
import app.kairos.ui.theme.Kairos
import app.kairos.util.TimeText

/**
 * İlk açılış. Tek ekran, hiçbir şey zorunlu değil (KURALLAR §1.1):
 * izinler istenir ama "Başla" her zaman çalışır; sesli özet sorusunun varsayılan cevabı "Hayır"dır
 * ve bu soru bir daha sorulmaz (KURALLAR §1.4).
 *
 * @param onStart kullanıcının özet cevabıyla çağrılır (true = her sabah oku)
 */
@Composable
fun OnboardingScreen(
    status: ReliabilityStatus,
    actions: ReliabilityActions,
    briefMinute: Int,
    use24Hour: Boolean,
    onStart: (wantsBrief: Boolean) -> Unit,
) {
    val c = Kairos.colors
    var wantsBrief by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.ground).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "KAİROS",
                    fontFamily = BarlowSemiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 40.sp,
                    letterSpacing = 0.14.em,
                    color = c.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Hatırlatmalarınızın zamanında gelmesi için telefonunuzda birkaç ayar gerekiyor. İsterseniz bunları daha sonra Ayarlar'dan da yapabilirsiniz.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.inkMuted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("İzinler", modifier = Modifier.padding(start = 4.dp))
                val shape = RoundedCornerShape(12.dp)
                ReliabilityChecklist(status, actions, Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.line, shape))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("Günlük sesli özet", modifier = Modifier.padding(start = 4.dp))
                Text(
                    "Her sabah, o gün kaç hatırlatmanız olduğunu sesli söyleyelim mi? Hatırlatmaların içeriği okunmaz. Saati ve günleri Ayarlar'dan değiştirebilir ya da istediğiniz zaman kapatabilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.ink,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Hayır", selected = !wantsBrief, Modifier.weight(1f)) { wantsBrief = false }
                    Choice("Evet, ${TimeText.timeWithLocative(briefMinute, use24Hour)}", selected = wantsBrief, Modifier.weight(1f)) { wantsBrief = true }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton("Başla", onClick = { onStart(wantsBrief) }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Kairos.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) c.primary else Color.Transparent)
            .border(1.dp, if (selected) c.primary else c.line, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onPrimary else c.ink)
    }
}
