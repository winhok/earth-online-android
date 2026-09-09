package xyz.winhok.earthonline

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.winhok.earthonline.core.ThemeMode
import xyz.winhok.earthonline.core.NarrativeLocale
import xyz.winhok.earthonline.core.NarrativeRegistry
import xyz.winhok.earthonline.core.NarrativeScheme
import xyz.winhok.earthonline.core.NarrativeSystemId
import xyz.winhok.earthonline.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this, EarthViewModel.Factory(application as EarthApplication))[EarthViewModel::class.java]
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            val mode = state.world.player.theme
            val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
            val systemId = state.narrativeSystemId
            DisposableEffect(dark, systemId) {
                // App theme can differ from the device theme; keep system-bar icons readable.
                val lightNavigation = if (systemId == NarrativeSystemId.CULTIVATION) {
                    Color.rgb(234, 244, 236)
                } else {
                    Color.rgb(247, 250, 244)
                }
                val darkNavigation = if (systemId == NarrativeSystemId.CULTIVATION) {
                    Color.rgb(7, 26, 22)
                } else {
                    Color.rgb(16, 23, 19)
                }
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(lightNavigation, darkNavigation) { dark },
                )
                onDispose { }
            }
            val registry = remember { NarrativeRegistry.builtIns() }
            val narrative = remember(dark, systemId) {
                RegistryNarrativePresenter(
                    registry = registry,
                    systemId = systemId,
                    locale = NarrativeLocale.ZH_CN,
                    scheme = if (dark) NarrativeScheme.DARK else NarrativeScheme.LIGHT,
                )
            }
            EarthTheme(mode, systemId) {
                NarrativeHost(narrative) { EarthApp(model, state) }
            }
        }
    }
}
