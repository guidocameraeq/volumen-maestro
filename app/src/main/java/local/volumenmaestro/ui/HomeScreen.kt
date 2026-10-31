package local.volumenmaestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import local.volumenmaestro.engine.Level
import local.volumenmaestro.engine.Protocol
import local.volumenmaestro.overlay.ThemeRegistry

@Composable
fun HomeScreen(state: EngineState, client: EngineClient) {
    var busy by remember { mutableStateOf(false) }
    Columns {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.backupFound) {
                Section("Encontré tus ajustes guardados") {
                    Hint("Hay una copia de seguridad de una instalación anterior. ¿La restauro?")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton("Restaurar", { client.send(Protocol.RESTORE_BACKUP) }, primary = true)
                        PillButton("No, empezar de cero", { client.send(Protocol.DISMISS_BACKUP) })
                    }
                }
            }
            Section(if (state.enabled) "Volumen Maestro está ENCENDIDO" else "Volumen Maestro está APAGADO") {
                Text(
                    if (state.enabled) "El control de la TV maneja el volumen de todo, en todas las apps. " +
                        "El audio sale en estéreo PCM por la fibra (sin Dolby)."
                    else "La TV está como venía: las apps mandan Dolby y la fibra se lo pasa al equipo. " +
                        "El volumen se maneja con el control del equipo de audio.",
                    color = Palette.textDim, fontSize = 16.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillButton(
                        if (state.enabled) "Apagar y dejar la TV como venía" else "Encender",
                        { client.setEnabled(!state.enabled) },
                        primary = true,
                    )
                }
                Hint("Al cambiar de modo se cierra Netflix para que tome el ajuste. Al prender la TV vuelve a quedar encendido.")
            }
            Section("Ahora") {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Fact("Volumen", if (state.muted) "Silencio" else "${state.index} de ${state.max}")
                    Fact("Tema", ThemeRegistry.byId(state.config.themeId).name)
                    Fact("Conexión", if (state.connected) "Conectada" else if (state.waitingAuth) "Esperando permiso" else "Sin conexión")
                }
            }
        }
        Column(Modifier.weight(1.1f).fillMaxHeight()) {
            Section("Estado", Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val worst = state.health.maxByOrNull { it.level.ordinal }?.level ?: Level.OK
                    StatusDot(Palette.of(worst), 18)
                    Text(
                        when (worst) {
                            Level.OK -> "Todo en orden"
                            Level.WARN -> "Hay cosas para revisar"
                            Level.ERROR -> "Hay un problema"
                        },
                        color = Palette.text, fontSize = 17.sp, modifier = Modifier.weight(1f),
                    )
                    PillButton(if (busy) "Reparando…" else "Reparar todo", {
                        busy = true
                        client.send(Protocol.REPAIR_ALL) { busy = false }
                    })
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.health, key = { it.id }) { check ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            StatusDot(Palette.of(check.level))
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(check.title, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(check.detail, color = Palette.textDim, fontSize = 14.sp)
                            }
                            if (check.level != Level.OK && check.fixLabel != null) {
                                PillButton(check.fixLabel, { client.withId(Protocol.FIX, check.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label, color = Palette.textDim, fontSize = 14.sp)
        Text(value, color = Palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}
