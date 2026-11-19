package app.kairos.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * KAİROS'un kendi ince çizgili ikonları (24×24, 1.8 kalınlık). Hazır Material ikon paketi
 * kullanılmaz; her ikon tasarım taslağındaki çizimle aynıdır. Renk, Icon'un tint'inden gelir.
 */
object KairosIcons {
    private fun icon(name: String, stroke: Float = 1.8f, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    /** Bugün: saat kadranı. */
    val Today = icon("today") {
        moveTo(20f, 12f); arcTo(8f, 8f, 0f, true, true, 4f, 12f); arcTo(8f, 8f, 0f, true, true, 20f, 12f); close()
        moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15f, 14f)
    }

    /** Yakında: takvim yaprağı. */
    val Upcoming = icon("upcoming") {
        moveTo(6f, 5f); lineTo(18f, 5f); arcTo(2f, 2f, 0f, false, true, 20f, 7f); lineTo(20f, 18f)
        arcTo(2f, 2f, 0f, false, true, 18f, 20f); lineTo(6f, 20f); arcTo(2f, 2f, 0f, false, true, 4f, 18f)
        lineTo(4f, 7f); arcTo(2f, 2f, 0f, false, true, 6f, 5f); close()
        moveTo(4f, 10f); lineTo(20f, 10f)
        moveTo(9f, 3f); lineTo(9f, 7f)
        moveTo(15f, 3f); lineTo(15f, 7f)
    }

    /** Tamamlanan / tamamlandı işareti. */
    val Check = icon("check") { moveTo(5f, 12f); lineTo(9.5f, 16.5f); lineTo(19f, 7f) }

    /** Ayarlar: iki sürgü. */
    val Settings = icon("settings") {
        moveTo(4f, 7f); lineTo(14f, 7f); moveTo(18f, 7f); lineTo(20f, 7f)
        moveTo(4f, 17f); lineTo(8f, 17f); moveTo(12f, 17f); lineTo(20f, 17f)
        moveTo(18f, 7f); arcTo(2f, 2f, 0f, true, true, 14f, 7f); arcTo(2f, 2f, 0f, true, true, 18f, 7f)
        moveTo(12f, 17f); arcTo(2f, 2f, 0f, true, true, 8f, 17f); arcTo(2f, 2f, 0f, true, true, 12f, 17f)
    }

    val Plus = icon("plus", stroke = 2f) { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }

    /** Önemli: zil. */
    val Bell = icon("bell") {
        moveTo(12f, 3f); lineTo(12f, 5f)
        moveTo(5f, 15f); lineTo(5f, 11f); arcTo(7f, 7f, 0f, false, true, 19f, 11f); lineTo(19f, 15f)
        lineTo(21f, 18f); lineTo(3f, 18f); close()
        moveTo(10f, 21f); lineTo(14f, 21f)
    }

    /** Tekrar: dönen ok. */
    val Repeat = icon("repeat") {
        moveTo(4f, 12f); arcTo(8f, 8f, 0f, false, true, 18f, 6.5f)
        moveTo(18f, 3f); lineTo(18f, 7f); lineTo(14f, 7f)
        moveTo(20f, 12f); arcTo(8f, 8f, 0f, false, true, 6f, 17.5f)
        moveTo(6f, 21f); lineTo(6f, 17f); lineTo(10f, 17f)
    }

    /** Ertele: kum saati yerine ileri saat. */
    val Snooze = icon("snooze") {
        moveTo(19f, 12f); arcTo(7f, 7f, 0f, true, true, 12f, 5f)
        moveTo(12f, 8f); lineTo(12f, 12f); lineTo(14.5f, 13.5f)
        moveTo(16f, 3f); lineTo(20f, 3f); lineTo(16f, 8f); lineTo(20f, 8f)
    }

    /** Sil: çöp kutusu. */
    val Trash = icon("trash") {
        moveTo(4f, 7f); lineTo(20f, 7f)
        moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
        moveTo(6f, 7f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 7f)
    }

    /** Dinle: oynat üçgeni. */
    val Play = icon("play") { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() }

    /** Geri aç (tamamlananı listeye döndür). */
    val Undo = icon("undo") {
        moveTo(9f, 14f); lineTo(4f, 9f); lineTo(9f, 4f)
        moveTo(4f, 9f); lineTo(15f, 9f); arcTo(5f, 5f, 0f, false, true, 15f, 19f); lineTo(11f, 19f)
    }
}
