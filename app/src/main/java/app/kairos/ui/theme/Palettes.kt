package app.kairos.ui.theme

/**
 * KAİROS renk tabloları (KURALLAR.md §2).
 *
 * Değerler 0xAARRGGBB biçiminde saf Long'dur: hem Compose teması hem JVM birim testleri
 * (kontrast ve mor yasağı) aynı tabloyu okur. Gradyan yoktur; her token tek bir düz renktir.
 */
data class Palette(
    /** Ekran zemini. */
    val ground: Long,
    /** Kart, alt sayfa, liste satırı zemini. */
    val surface: Long,
    /** Ana metin. */
    val ink: Long,
    /** İkincil metin (açıklama, etiket). */
    val inkMuted: Long,
    /** Tamamlanan öğeler, cetvelin ince bölmeleri. Okunması gereken metin için kullanılmaz. */
    val inkFaint: Long,
    /** 1 px ayırıcı çizgiler ve kenarlıklar. */
    val line: Long,
    /** Birincil düğme ve seçili durum zemini. */
    val primary: Long,
    /** [primary] üzerindeki metin. */
    val onPrimary: Long,
    /** Pirinç ibre, sıradaki hatırlatma çizgisi, seçili sekme işareti. */
    val accent: Long,
    /** Gecikmiş hatırlatmalar. Kırmızı yerine temanın koyu toprak tonu. */
    val overdue: Long,
    /** Açılır panelin arkasındaki karartma (düz, yarı saydam değil). */
    val scrim: Long,
)

enum class ThemeFamily(val id: String, val displayName: String, val light: Palette, val dark: Palette) {
    BANKER(
        id = "banker",
        displayName = "Banker",
        light = Palette(
            ground = 0xFFEEF0EC, surface = 0xFFFAFBF8, ink = 0xFF17201B, inkMuted = 0xFF4F5953,
            inkFaint = 0xFF8A938C, line = 0xFFD5DAD3, primary = 0xFF1F4A38, onPrimary = 0xFFF3F4EF,
            accent = 0xFF9A7536, overdue = 0xFF7B2D2B, scrim = 0xFF59605B,
        ),
        dark = Palette(
            ground = 0xFF121815, surface = 0xFF1A221E, ink = 0xFFE6EAE5, inkMuted = 0xFFA3ADA6,
            inkFaint = 0xFF66706A, line = 0xFF2B352F, primary = 0xFF2F6B52, onPrimary = 0xFFEEF2EE,
            accent = 0xFFC9A35E, overdue = 0xFFD98579, scrim = 0xFF070A08,
        ),
    ),
    LACIVERT(
        id = "lacivert",
        displayName = "Lacivert",
        light = Palette(
            ground = 0xFFE8EBEF, surface = 0xFFF7F8FA, ink = 0xFF121A26, inkMuted = 0xFF4A5464,
            inkFaint = 0xFF8790A0, line = 0xFFD2D8E0, primary = 0xFF1C2C44, onPrimary = 0xFFEEF1F5,
            accent = 0xFF8A5532, overdue = 0xFF8E2F2A, scrim = 0xFF545C68,
        ),
        dark = Palette(
            ground = 0xFF0F151F, surface = 0xFF172030, ink = 0xFFE4E8EF, inkMuted = 0xFF9EA8B8,
            inkFaint = 0xFF5F6A7C, line = 0xFF263246, primary = 0xFF3A5378, onPrimary = 0xFFF1F4F8,
            accent = 0xFFC08A62, overdue = 0xFFE08A80, scrim = 0xFF05080C,
        ),
    ),
    BORDO(
        id = "bordo",
        displayName = "Bordo",
        light = Palette(
            ground = 0xFFEFECEB, surface = 0xFFFAF8F7, ink = 0xFF1E1716, inkMuted = 0xFF5C5150,
            inkFaint = 0xFF968B89, line = 0xFFDDD6D4, primary = 0xFF5C1E22, onPrimary = 0xFFF5EEEE,
            accent = 0xFF5F6B4E, overdue = 0xFFA0522D, scrim = 0xFF5E5654,
        ),
        dark = Palette(
            ground = 0xFF171112, surface = 0xFF211819, ink = 0xFFEDE6E5, inkMuted = 0xFFADA2A0,
            inkFaint = 0xFF6E6260, line = 0xFF352828, primary = 0xFF8E3A40, onPrimary = 0xFFF7EEEE,
            accent = 0xFF93A07F, overdue = 0xFFD9906A, scrim = 0xFF0A0707,
        ),
    ),
    TAS(
        id = "tas",
        displayName = "Taş",
        light = Palette(
            ground = 0xFFF4F4F2, surface = 0xFFFBFBFA, ink = 0xFF0E0E0D, inkMuted = 0xFF4D4D4A,
            inkFaint = 0xFF94948F, line = 0xFFD9D9D6, primary = 0xFF0E0E0D, onPrimary = 0xFFF4F4F2,
            accent = 0xFF4D4D4A, overdue = 0xFF6E2B26, scrim = 0xFF5A5A57,
        ),
        dark = Palette(
            ground = 0xFF111111, surface = 0xFF1A1A19, ink = 0xFFF0F0EE, inkMuted = 0xFFA6A6A2,
            inkFaint = 0xFF66665F, line = 0xFF2C2C2A, primary = 0xFFF0F0EE, onPrimary = 0xFF111111,
            accent = 0xFFA6A6A2, overdue = 0xFFD98A82, scrim = 0xFF050505,
        ),
    ),
    GRAFIT(
        id = "grafit",
        displayName = "Grafit",
        light = Palette(
            ground = 0xFFECECE8, surface = 0xFFF8F8F5, ink = 0xFF1B1D1B, inkMuted = 0xFF555A55,
            inkFaint = 0xFF92968F, line = 0xFFD4D6D1, primary = 0xFF2A2D2A, onPrimary = 0xFFF2F0E8,
            accent = 0xFF8E6E2E, overdue = 0xFF8E3A30, scrim = 0xFF5A5C59,
        ),
        dark = Palette(
            ground = 0xFF141614, surface = 0xFF1D201E, ink = 0xFFE7E8E3, inkMuted = 0xFF9DA39D,
            inkFaint = 0xFF6E756F, line = 0xFF2E332F, primary = 0xFFC9A35E, onPrimary = 0xFF141614,
            accent = 0xFFC9A35E, overdue = 0xFFD98579, scrim = 0xFF060706,
        ),
    );

    companion object {
        val Default = BANKER
        fun fromId(id: String?): ThemeFamily = entries.firstOrNull { it.id == id } ?: Default
    }
}

/** Açık / Koyu / Telefonla aynı. */
enum class ThemeMode(val id: String, val displayName: String) {
    LIGHT("light", "Açık"),
    DARK("dark", "Koyu"),
    SYSTEM("system", "Telefonla aynı");

    companion object {
        val Default = SYSTEM
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: Default
    }
}
