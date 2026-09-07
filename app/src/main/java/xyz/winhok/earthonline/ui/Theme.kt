package xyz.winhok.earthonline.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.ThemeMode

private val Night = darkColorScheme(
    primary = Color(0xFF81D9B3), onPrimary = Color(0xFF053826),
    primaryContainer = Color(0xFF1D4B37), onPrimaryContainer = Color(0xFFC6F9DC),
    secondary = Color(0xFFE4C285), onSecondary = Color(0xFF3F2D08),
    secondaryContainer = Color(0xFF524322), onSecondaryContainer = Color(0xFFFCE5B4),
    tertiary = Color(0xFFA9CDDD), background = Color(0xFF101713), onBackground = Color(0xFFE2E9E1),
    surface = Color(0xFF101713), onSurface = Color(0xFFE2E9E1),
    surfaceVariant = Color(0xFF263B2F), onSurfaceVariant = Color(0xFFBDCCC0),
    outline = Color(0xFF87978B), outlineVariant = Color(0xFF3C4D41),
)
private val Day = lightColorScheme(
    primary = Color(0xFF176B47), onPrimary = Color.White,
    primaryContainer = Color(0xFFC7EFD8), onPrimaryContainer = Color(0xFF092F1D),
    secondary = Color(0xFF775B22), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAE5B5), onSecondaryContainer = Color(0xFF302207),
    background = Color(0xFFF7FAF4), surface = Color(0xFFF7FAF4), onSurface = Color(0xFF17251C),
    onBackground = Color(0xFF17251C), surfaceVariant = Color(0xFFE2EBDF), onSurfaceVariant = Color(0xFF405345),
)
@Composable
fun EarthTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val typography = Typography()
    MaterialTheme(
        colorScheme = if (dark) Night else Day,
        typography = typography.copy(
            headlineLarge = typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            titleLarge = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}
