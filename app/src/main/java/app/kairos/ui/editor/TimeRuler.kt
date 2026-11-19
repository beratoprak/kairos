package app.kairos.ui.editor

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.kairos.data.Reminder
import app.kairos.ui.theme.Kairos
import app.kairos.ui.theme.TimeStyles
import app.kairos.util.TimeText
import kotlin.math.roundToInt

/**
 * Saat cetveli hesapları. Saf mantık; birim testle doğrulanır.
 * Cetvel 5 dakikalık adımlarla ilerler; 00:00 ile 23:55 arasında sınırlıdır.
 */
object RulerMath {
    const val STEP = 5
    const val MAX = Reminder.MINUTES_PER_DAY - STEP

    /** [startMinute]'tan [dragMinutes] kadar kaydırınca varılan, adıma oturtulmuş dakika. */
    fun snapped(startMinute: Float, dragMinutes: Float): Int =
        (((startMinute + dragMinutes) / STEP).roundToInt() * STEP).coerceIn(0, MAX)

    /** Bölme tipi: saat başı (uzun), yarım (orta), çeyrek (kısa). */
    fun tickKind(minute: Int): Int = when {
        minute % 60 == 0 -> 2
        minute % 30 == 0 -> 1
        else -> 0
    }
}

/**
 * KAİROS'un imza bileşeni (KURALLAR §2.8): kol saati kadranı gibi bölmeli, kaydırmalı saat cetveli.
 * Ortadaki pirinç ibre seçili anı gösterir; cetvel parmağın altında kayar. Her çeyrek saatte hafif
 * bir titreşim verilir. Ekran okuyucu için kaydırıcı (slider) olarak tanımlıdır.
 */
@Composable
fun TimeRuler(minute: Int, onMinute: (Int) -> Unit, use24Hour: Boolean, modifier: Modifier = Modifier) {
    val c = Kairos.colors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val measurer: TextMeasurer = rememberTextMeasurer()
    val pxPerMinute = with(density) { 2.4.dp.toPx() } // 1 saat = 144 dp
    val current by rememberUpdatedState(minute)
    // Sürükleme sırasında kesirli konum; bırakınca 5 dakikaya oturur.
    var fine by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (fine.isNaN()) minute.toFloat() else fine

    val drag = rememberDraggableState { deltaPx ->
        val base = if (fine.isNaN()) current.toFloat() else fine
        val next = (base - deltaPx / pxPerMinute).coerceIn(0f, RulerMath.MAX.toFloat())
        fine = next
        val snapped = RulerMath.snapped(next, 0f)
        if (snapped != current) {
            if (snapped % 15 == 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onMinute(snapped)
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.ground)
            .draggable(drag, Orientation.Horizontal, onDragStopped = { fine = Float.NaN })
            .semantics {
                contentDescription = "Saat cetveli"
                stateDescription = TimeText.time(minute, use24Hour)
                progressBarRangeInfo = ProgressBarRangeInfo(minute.toFloat(), 0f..RulerMath.MAX.toFloat(), RulerMath.MAX / RulerMath.STEP - 1)
                setProgress { v -> onMinute(RulerMath.snapped(v, 0f)); true }
            },
    ) {
        val center = size.width / 2f
        val top = 14.dp.toPx()
        val halfSpan = (center / pxPerMinute).toInt() + 15
        val first = ((shown.toInt() - halfSpan) / 15) * 15
        // Cetvel bir çizimdir: rakamlar yazı boyutu ayarıyla büyümez, ibreyle çakışmaz.
        val labelStyle = TimeStyles.Ruler.copy(fontSize = 15.dp.toSp())

        var m = first
        while (m <= shown + halfSpan) {
            if (m in 0..RulerMath.MAX + RulerMath.STEP) {
                val x = center + (m - shown) * pxPerMinute
                val kind = RulerMath.tickKind(m)
                val len = when (kind) { 2 -> 24.dp; 1 -> 14.dp; else -> 9.dp }.toPx()
                drawLine(
                    color = if (kind == 2) c.ink else c.inkFaint,
                    start = Offset(x, top),
                    end = Offset(x, top + len),
                    strokeWidth = if (kind == 2) 1.5.dp.toPx() else 1.dp.toPx(),
                )
                if (kind == 2 && m < Reminder.MINUTES_PER_DAY) {
                    val hour = m / 60
                    val text = if (use24Hour) "%02d".format(hour) else (if (hour % 12 == 0) 12 else hour % 12).toString()
                    val isCurrentHour = hour == minute / 60
                    val layout = measurer.measure(text, labelStyle.copy(color = if (isCurrentHour) c.ink else c.inkMuted))
                    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, top + 32.dp.toPx()))
                }
            }
            m += 15
        }

        // Pirinç ibre ve üstündeki üçgen. Saat rakamlarının hizasında kesilir; rakam okunur kalır.
        val gapTop = top + 28.dp.toPx()
        val gapBottom = top + 54.dp.toPx()
        drawLine(c.accent, Offset(center, 0f), Offset(center, gapTop), strokeWidth = 2.dp.toPx())
        drawLine(c.accent, Offset(center, gapBottom), Offset(center, size.height), strokeWidth = 2.dp.toPx())
        val tri = Path().apply {
            moveTo(center - 5.dp.toPx(), 0f)
            lineTo(center + 5.dp.toPx(), 0f)
            lineTo(center, 7.dp.toPx())
            close()
        }
        drawPath(tri, c.accent)
    }
}
