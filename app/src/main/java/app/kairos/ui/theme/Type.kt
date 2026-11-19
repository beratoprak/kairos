package app.kairos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.kairos.R

/** Schibsted Grotesk (değişken ağırlık): tüm metinler. */
@OptIn(ExperimentalTextApi::class)
private fun schibsted(weight: FontWeight) = Font(
    resId = R.font.schibsted_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val SchibstedGrotesk = FontFamily(
    schibsted(FontWeight.Normal),
    schibsted(FontWeight.Medium),
    schibsted(FontWeight.SemiBold),
    schibsted(FontWeight.Bold),
)

/** Barlow Semi Condensed: saatler ve rakamlar. */
val BarlowSemiCondensed = FontFamily(
    Font(R.font.barlow_semi_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_semi_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semi_condensed_semibold, FontWeight.SemiBold),
)

/** Saat gösterimleri; rakamlar eşit genişlikte (tnum) olduğu için saat değişirken metin kaymaz. */
object TimeStyles {
    private val base = TextStyle(fontFamily = BarlowSemiCondensed, fontFeatureSettings = "tnum")

    /** Ekleme sayfasındaki büyük saat (12:00). */
    val Display = base.copy(fontSize = 76.sp, fontWeight = FontWeight.Medium, lineHeight = 76.sp, letterSpacing = (-0.01).em)
    /** Ayarlardaki saat değeri (08:00). */
    val Value = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium)
    /** Bugün listesindeki saat sütunu. */
    val Row = base.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    /** Cetvel üzerindeki saat numaraları. */
    val Ruler = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal)
}

/** Bölüm etiketleri: küçük, büyük harf, aralıklı ("GECİKMİŞ", "GÜNLÜK SESLİ ÖZET"). */
val SectionLabel = TextStyle(
    fontFamily = SchibstedGrotesk,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    letterSpacing = 0.08.em,
)

private fun s(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = SchibstedGrotesk,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/** Material bileşenlerinin tamamı Schibsted Grotesk kullanır; varsayılan Roboto hiçbir yerde görünmez. */
val KairosTypography = Typography(
    displayLarge = s(57, FontWeight.Bold, 64, -0.02),
    displayMedium = s(45, FontWeight.Bold, 52, -0.02),
    displaySmall = s(36, FontWeight.Bold, 44, -0.02),
    headlineLarge = s(34, FontWeight.Bold, 40, -0.02),
    headlineMedium = s(28, FontWeight.Bold, 34, -0.01),
    headlineSmall = s(22, FontWeight.Bold, 28),
    titleLarge = s(20, FontWeight.SemiBold, 26),
    titleMedium = s(16, FontWeight.Medium, 22),
    titleSmall = s(14, FontWeight.SemiBold, 20),
    bodyLarge = s(16, FontWeight.Normal, 22),
    bodyMedium = s(14, FontWeight.Normal, 20),
    bodySmall = s(13, FontWeight.Normal, 18),
    labelLarge = s(15, FontWeight.SemiBold, 20),
    labelMedium = s(13, FontWeight.Medium, 18),
    labelSmall = s(12, FontWeight.Medium, 16),
)
