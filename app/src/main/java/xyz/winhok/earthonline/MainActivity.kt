package xyz.winhok.earthonline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import xyz.winhok.earthonline.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this, EarthViewModel.Factory(application as EarthApplication))[EarthViewModel::class.java]
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            EarthTheme(state.world.player.theme) { EarthApp(model, state) }
        }
    }
}
