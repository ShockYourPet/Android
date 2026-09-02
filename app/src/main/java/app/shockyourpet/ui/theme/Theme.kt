package app.shockyourpet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppThemeStyle(val storageKey: String, val displayName: String, val isDark: Boolean) {
    Blush("blush", "Blush", false),
    RoseTea("rose_tea", "Rose Tea", false),
    RoseDusk("rose_dusk", "Rose Dusk", true),
    BerryNight("berry_night", "Berry Night", true);

    companion object {
        fun fromStorageKey(value: String?): AppThemeStyle =
            entries.firstOrNull { it.storageKey == value } ?: Blush
    }
}

val Berry = Color(0xFF9E315D)
val BerryDark = Color(0xFF6E1F40)
val RibbonPink = Color(0xFFE85F96)
val Blush = Color(0xFFFFD7E5)
val PowderPink = Color(0xFFFFECF3)
val Cream = Color(0xFFFFF9F5)
val Cocoa = Color(0xFF4D2937)
val Sage = Color(0xFF6E8D75)
val Cherry = Color(0xFFC63D5B)

private val BlushColorScheme = lightColorScheme(
    primary = Berry,
    onPrimary = Color.White,
    primaryContainer = Blush,
    onPrimaryContainer = BerryDark,
    secondary = RibbonPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1EC),
    onSecondaryContainer = BerryDark,
    tertiary = Sage,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDEBDD),
    onTertiaryContainer = Color(0xFF29432F),
    error = Cherry,
    onError = Color.White,
    errorContainer = Color(0xFFFFD9DF),
    onErrorContainer = Color(0xFF76152B),
    background = PowderPink,
    onBackground = Cocoa,
    surface = Cream,
    onSurface = Cocoa,
    surfaceVariant = Color(0xFFFFE7EF),
    onSurfaceVariant = Color(0xFF765464),
    outline = Color(0xFFD99AB2),
    outlineVariant = Color(0xFFF0C8D7),
    scrim = BerryDark,
)

private val RoseTeaColorScheme = lightColorScheme(
    primary = Color(0xFF7B4057),
    onPrimary = Color(0xFFF8F0ED),
    primaryContainer = Color(0xFFC9A9B2),
    onPrimaryContainer = Color(0xFF472532),
    secondary = Color(0xFF965E6D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1B7BB),
    onSecondaryContainer = Color(0xFF472A33),
    tertiary = Color(0xFF66725F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC2C9BA),
    onTertiaryContainer = Color(0xFF293326),
    error = Color(0xFF983D4E),
    onError = Color.White,
    errorContainer = Color(0xFFD7B5BA),
    onErrorContainer = Color(0xFF59232E),
    background = Color(0xFFC8BBB8),
    onBackground = Color(0xFF3E2B32),
    surface = Color(0xFFDDD1CC),
    onSurface = Color(0xFF3E2B32),
    surfaceVariant = Color(0xFFC0AFB0),
    onSurfaceVariant = Color(0xFF604B52),
    outline = Color(0xFF9C747F),
    outlineVariant = Color(0xFFB69EA3),
    scrim = Color(0xFF35232A),
)

private val RoseDuskColorScheme = darkColorScheme(
    primary = Color(0xFFF2A9C1),
    onPrimary = Color(0xFF54263A),
    primaryContainer = Color(0xFF694456),
    onPrimaryContainer = Color(0xFFFFD8E5),
    secondary = Color(0xFFD996AA),
    onSecondary = Color(0xFF4B2634),
    secondaryContainer = Color(0xFF60424E),
    onSecondaryContainer = Color(0xFFFFD8E2),
    tertiary = Color(0xFFB9C8AE),
    onTertiary = Color(0xFF293425),
    tertiaryContainer = Color(0xFF465142),
    onTertiaryContainer = Color(0xFFDDE8D5),
    error = Color(0xFFFFB0BC),
    onError = Color(0xFF64162A),
    background = Color(0xFF30262B),
    onBackground = Color(0xFFF5E7EB),
    surface = Color(0xFF40343A),
    onSurface = Color(0xFFF5E7EB),
    surfaceVariant = Color(0xFF514149),
    onSurfaceVariant = Color(0xFFF0DDE4),
    outline = Color(0xFFC49BAA),
    outlineVariant = Color(0xFF806570),
    scrim = Color(0xFF160F13),
)

private val BerryNightColorScheme = darkColorScheme(
    primary = Color(0xFFFF9FC2),
    onPrimary = Color(0xFF54142F),
    primaryContainer = Color(0xFF6D2946),
    onPrimaryContainer = Color(0xFFFFD9E6),
    secondary = Color(0xFFFF82B1),
    onSecondary = Color(0xFF53152E),
    secondaryContainer = Color(0xFF55263A),
    onSecondaryContainer = Color(0xFFFFD7E4),
    tertiary = Color(0xFFAED4B5),
    onTertiary = Color(0xFF183821),
    tertiaryContainer = Color(0xFF304A37),
    onTertiaryContainer = Color(0xFFD2F0D7),
    error = Color(0xFFFF9BAB),
    onError = Color(0xFF640D24),
    background = Color(0xFF160F14),
    onBackground = Color(0xFFFFE8F0),
    surface = Color(0xFF241923),
    onSurface = Color(0xFFFFE8F0),
    surfaceVariant = Color(0xFF35242E),
    onSurfaceVariant = Color(0xFFFFDCE9),
    outline = Color(0xFFD38CA8),
    outlineVariant = Color(0xFF704A5B),
    scrim = Color.Black,
)

private val PreppyTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 52.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.5.sp),
)

private val PreppyShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun ShockYourPetTheme(
    style: AppThemeStyle = AppThemeStyle.Blush,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when (style) {
            AppThemeStyle.Blush -> BlushColorScheme
            AppThemeStyle.RoseTea -> RoseTeaColorScheme
            AppThemeStyle.RoseDusk -> RoseDuskColorScheme
            AppThemeStyle.BerryNight -> BerryNightColorScheme
        },
        typography = PreppyTypography,
        shapes = PreppyShapes,
        content = content,
    )
}
