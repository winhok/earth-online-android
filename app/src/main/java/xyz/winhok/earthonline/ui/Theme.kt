package xyz.winhok.earthonline.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.NarrativeSystemId
import xyz.winhok.earthonline.core.ThemeMode

private val EarthNight = darkColorScheme(
    primary = Color(0xFF81D9B3), onPrimary = Color(0xFF053826),
    primaryContainer = Color(0xFF1D4B37), onPrimaryContainer = Color(0xFFC6F9DC),
    secondary = Color(0xFFE4C285), onSecondary = Color(0xFF3F2D08),
    secondaryContainer = Color(0xFF524322), onSecondaryContainer = Color(0xFFFCE5B4),
    tertiary = Color(0xFFA9CDDD), background = Color(0xFF101713), onBackground = Color(0xFFE2E9E1),
    surface = Color(0xFF101713), onSurface = Color(0xFFE2E9E1),
    surfaceVariant = Color(0xFF263B2F), onSurfaceVariant = Color(0xFFBDCCC0),
    outline = Color(0xFF87978B), outlineVariant = Color(0xFF3C4D41),
)
private val EarthDay = lightColorScheme(
    primary = Color(0xFF176B47), onPrimary = Color.White,
    primaryContainer = Color(0xFFC7EFD8), onPrimaryContainer = Color(0xFF092F1D),
    secondary = Color(0xFF775B22), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAE5B5), onSecondaryContainer = Color(0xFF302207),
    background = Color(0xFFF7FAF4), surface = Color(0xFFF7FAF4), onSurface = Color(0xFF17251C),
    onBackground = Color(0xFF17251C), surfaceVariant = Color(0xFFE2EBDF), onSurfaceVariant = Color(0xFF405345),
)
private val CultivationNight = darkColorScheme(
    primary = Color(0xFF63E6AF), onPrimary = Color(0xFF002E20),
    primaryContainer = Color(0xFF123E31), onPrimaryContainer = Color(0xFFB7F5D8),
    secondary = Color(0xFFE2BE70), onSecondary = Color(0xFF3D2D08),
    secondaryContainer = Color(0xFF493A1D), onSecondaryContainer = Color(0xFFF4D99A),
    tertiary = Color(0xFFE96A61), onTertiary = Color(0xFF3D0303),
    tertiaryContainer = Color(0xFF52211E), onTertiaryContainer = Color(0xFFFFDAD5),
    error = Color(0xFFFF8A80), onError = Color(0xFF520009),
    background = Color(0xFF071A16), onBackground = Color(0xFFEAF4EC),
    surface = Color(0xFF0A1F1A), onSurface = Color(0xFFEAF4EC),
    surfaceContainerLowest = Color(0xFF04120F), surfaceContainerLow = Color(0xFF081C17),
    surfaceContainer = Color(0xFF0D241F), surfaceContainerHigh = Color(0xFF132C25),
    surfaceContainerHighest = Color(0xFF19352D),
    surfaceVariant = Color(0xFF17332B), onSurfaceVariant = Color(0xFFB9CCC3),
    outline = Color(0xFF829B90), outlineVariant = Color(0xFF304B41),
)
private val CultivationDay = lightColorScheme(
    primary = Color(0xFF146B4D), onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EBD2), onPrimaryContainer = Color(0xFF062E21),
    secondary = Color(0xFF755A1D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2D99B), onSecondaryContainer = Color(0xFF2C2104),
    tertiary = Color(0xFF9B3B34), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD5), onTertiaryContainer = Color(0xFF3D0303),
    error = Color(0xFFB3261E), onError = Color.White,
    background = Color(0xFFEAF4EC), onBackground = Color(0xFF10241D),
    surface = Color(0xFFF3F8F3), onSurface = Color(0xFF10241D),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFE8F2EA),
    surfaceContainer = Color(0xFFE1EDE5), surfaceContainerHigh = Color(0xFFD9E7DE),
    surfaceContainerHighest = Color(0xFFD1E1D7),
    surfaceVariant = Color(0xFFD5E7DD), onSurfaceVariant = Color(0xFF3C5148),
    outline = Color(0xFF687D73), outlineVariant = Color(0xFFB8CCC1),
)

val LocalNarrativeSystemId = staticCompositionLocalOf { NarrativeSystemId.EARTH_NATIVE }

@Composable
fun EarthTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    narrativeSystemId: NarrativeSystemId = NarrativeSystemId.EARTH_NATIVE,
    content: @Composable () -> Unit,
) {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val cultivation = narrativeSystemId == NarrativeSystemId.CULTIVATION
    val colors = when {
        cultivation && dark -> CultivationNight
        cultivation -> CultivationDay
        dark -> EarthNight
        else -> EarthDay
    }
    val typography = Typography()
    val shapes = if (cultivation) {
        Shapes(
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(18.dp),
        )
    } else {
        Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        )
    }
    CompositionLocalProvider(LocalNarrativeSystemId provides narrativeSystemId) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography.copy(
                headlineLarge = typography.headlineLarge.copy(
                    fontWeight = if (cultivation) FontWeight.ExtraBold else FontWeight.Bold,
                ),
                titleLarge = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            ),
            shapes = shapes,
            content = content,
        )
    }
}
