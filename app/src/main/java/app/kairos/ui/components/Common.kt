package app.kairos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.SectionLabel
import app.kairos.util.TimeText

/** Küçük, büyük harfli, aralıklı bölüm etiketi ("GECİKMİŞ"). Türkçe büyük harf kuralıyla (i → İ). */
@Composable
fun Label(text: String, color: Color = Kairos.colors.inkMuted, modifier: Modifier = Modifier) =
    Text(text.uppercase(TimeText.TR), style = SectionLabel, color = color, modifier = modifier)

/** Tam genişlikte birincil düğme ("Hatırlatma ekle", "Kaydet"). Düz renk, gölge yok. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val c = Kairos.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.primary)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.onPrimary)
    }
}

/**
 * Tamamlama dairesi. Boşsa dokununca tamamlar; doluysa içinde onay işareti vardır.
 * Dokunma alanı 44 dp (KURALLAR §2.9), çizilen daire 22 dp.
 */
@Composable
fun CheckCircle(
    checked: Boolean,
    onClick: (() -> Unit)?,
    ringColor: Color = Kairos.colors.ink,
    label: String = "Tamamlandı olarak işaretle",
    size: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val c = Kairos.colors
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Checkbox, onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (checked) c.primary else Color.Transparent)
                .border(1.5.dp, if (checked) c.primary else ringColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(KairosIcons.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(14.dp))
        }
    }
}

/** 1 dp ayırıcı çizgi. */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Kairos.colors.line, thickness: androidx.compose.ui.unit.Dp = 1.dp) =
    Box(modifier.fillMaxWidth().height(thickness).background(color))

/** Boş liste: ne yapılacağını söyleyen kısa yönlendirme. */
@Composable
fun EmptyNote(title: String, hint: String, modifier: Modifier = Modifier) {
    val c = Kairos.colors
    androidx.compose.foundation.layout.Column(modifier.padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
    }
}
