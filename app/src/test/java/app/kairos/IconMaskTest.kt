package app.kairos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Uyarlanabilir uygulama simgesinin daire, squircle ve kare maskelerde önizlemesi, tek renkli
 * tema katmanı ve durum çubuğu simgesi → `app/screenshots/ikon_maskeler.png`.
 * Android simgenin dış 18 dp'lik kısmını maskeyle kırpar; görünür alan 72/108'dir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w600dp-h220dp-xxhdpi")
class IconMaskTest {
    @get:Rule val compose = createComposeRule()

    @Test fun masks() {
        compose.setContent {
            Row(
                Modifier.background(Color(0xFFF7F7F6)).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf<Shape>(CircleShape, RoundedCornerShape(30), RectangleShape).forEach { shape -> Masked(shape) }
                // Tek renkli (temalı) simge: sistem rengiyle boyanır.
                Box(Modifier.size(96.dp).clip(CircleShape).background(Color(0xFFDDE3EA)), contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(R.drawable.ic_launcher_monochrome), null,
                        Modifier.size(144.dp), colorFilter = ColorFilter.tint(Color(0xFF2B3440)),
                    )
                }
                // Durum çubuğu simgesi (24 dp) koyu çubuk üzerinde.
                Box(Modifier.size(48.dp).background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_notification), null, Modifier.size(24.dp))
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/ikon_maskeler.png")
    }

    @androidx.compose.runtime.Composable
    private fun Masked(shape: Shape) {
        // 108 dp katmanın orta 72 dp'si görünür: 96 dp gösterim için katman 144 dp çizilir.
        Box(Modifier.size(96.dp).clip(shape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(144.dp))
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(144.dp))
        }
    }
}
