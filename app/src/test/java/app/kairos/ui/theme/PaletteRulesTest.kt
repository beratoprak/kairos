package app.kairos.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * KURALLAR.md §2'yi kodla denetler: her temanın okunabilirliği (WCAG kontrastı) ve
 * mor/indigo yasağı. Bir renk değiştiğinde bu test kırılırsa kural ihlal edilmiştir.
 */
class PaletteRulesTest {

    private fun channel(v: Long, shift: Int): Double {
        val c = ((v shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Long) =
        0.2126 * channel(c, 16) + 0.7152 * channel(c, 8) + 0.0722 * channel(c, 0)

    private fun contrast(a: Long, b: Long): Double {
        val la = luminance(a); val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** HSL ton (0–360) ve doygunluk (0–1). */
    private fun hueSat(c: Long): Pair<Double, Double> {
        val r = ((c shr 16) and 0xFF) / 255.0
        val g = ((c shr 8) and 0xFF) / 255.0
        val b = (c and 0xFF) / 255.0
        val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val d = mx - mn
        if (d == 0.0) return 0.0 to 0.0
        val l = (mx + mn) / 2
        val s = d / (1 - kotlin.math.abs(2 * l - 1))
        val h = when (mx) {
            r -> 60 * (((g - b) / d) % 6)
            g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }
        return (if (h < 0) h + 360 else h) to s
    }

    private fun allPalettes() = ThemeFamily.entries.flatMap {
        listOf("${it.displayName} açık" to it.light, "${it.displayName} koyu" to it.dark)
    }

    private fun Palette.tokens() = listOf(
        "ground" to ground, "surface" to surface, "ink" to ink, "inkMuted" to inkMuted,
        "inkFaint" to inkFaint, "line" to line, "primary" to primary, "onPrimary" to onPrimary,
        "accent" to accent, "overdue" to overdue, "scrim" to scrim,
    )

    @Test
    fun textIsReadableOnEveryTheme() {
        val failures = mutableListOf<String>()
        fun need(name: String, fg: Long, bg: Long, min: Double) {
            val r = contrast(fg, bg)
            if (r < min) failures += "$name: %.2f < %.1f".format(r, min)
        }
        for ((name, p) in allPalettes()) {
            need("$name ink/ground", p.ink, p.ground, 7.0)
            need("$name ink/surface", p.ink, p.surface, 7.0)
            need("$name inkMuted/ground", p.inkMuted, p.ground, 4.5)
            need("$name inkMuted/surface", p.inkMuted, p.surface, 4.5)
            need("$name onPrimary/primary", p.onPrimary, p.primary, 4.5)
            need("$name overdue/ground", p.overdue, p.ground, 4.5)
            need("$name overdue/surface", p.overdue, p.surface, 4.5)
            // Pirinç ibre ve vurgu çizgileri metin değildir; grafik öğe eşiği 3:1.
            need("$name accent/ground", p.accent, p.ground, 3.0)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun noPurpleOrIndigoAnywhere() {
        val failures = mutableListOf<String>()
        for ((name, p) in allPalettes()) for ((token, c) in p.tokens()) {
            val (h, s) = hueSat(c)
            // Mor/indigo/menekşe aralığı: 245°–330°, belirgin doygunlukla.
            if (h in 245.0..330.0 && s > 0.12) failures += "$name $token #%06X ton=%.0f°".format(c and 0xFFFFFF, h)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun materialDefaultPurpleIsNeverUsed() {
        val banned = setOf(0xFF6750A4, 0xFF6200EE, 0xFF3700B3, 0xFFBB86FC, 0xFFD0BCFF)
        for ((name, p) in allPalettes()) for ((token, c) in p.tokens()) {
            assertTrue("$name $token yasaklı Material moru", c !in banned)
        }
    }
}
