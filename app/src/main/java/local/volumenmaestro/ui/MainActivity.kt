package local.volumenmaestro.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme

class MainActivity : ComponentActivity() {
    private lateinit var client: EngineClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = EngineClient(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Palette.accent)) {
                App(client)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        client.start()
    }

    override fun onStop() {
        client.stop()
        super.onStop()
    }
}

private val TABS = listOf("Inicio", "Volumen", "Temas", "Optimizar", "Ajustes")

@Composable
fun App(client: EngineClient) {
    val state by client.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.background)
            .padding(horizontal = 56.dp, vertical = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Volumen Maestro", color = Palette.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !state.running -> "Servicio apagado"
                        state.enabled -> "Encendido · volumen ${if (state.muted) "en silencio" else "${state.index}"}"
                        else -> "Apagado · la TV está como venía"
                    },
                    color = if (state.running && state.enabled) Palette.ok else Palette.textDim,
                    fontSize = 15.sp,
                )
            }
            TabRow(selectedTabIndex = tab) {
                TABS.forEachIndexed { i, title ->
                    key(i) {
                        Tab(selected = tab == i, onFocus = { tab = i }) {
                            Text(title, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().padding(top = 24.dp)) {
            if (!state.running) {
                ServiceOff()
            } else when (tab) {
                0 -> HomeScreen(state, client)
                1 -> VolumeScreen(state, client)
                2 -> ThemesScreen(state, client)
                3 -> OptimizeScreen(state, client)
                else -> SettingsScreen(state, client)
            }
        }
    }
}

@Composable
private fun ServiceOff() {
    Section("El servicio de Volumen Maestro está apagado") {
        Hint("Sin el servicio no hay control de volumen ni carteles.")
        Hint("Activalo en Settings → System → Accessibility → Volumen Maestro, y volvé a esta pantalla.")
    }
}

@Composable
fun Columns(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = Row(
    modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    content = content,
)
