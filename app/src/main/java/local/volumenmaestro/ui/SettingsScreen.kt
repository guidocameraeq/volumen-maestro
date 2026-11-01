package local.volumenmaestro.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import local.volumenmaestro.BuildConfig
import local.volumenmaestro.engine.Protocol
import local.volumenmaestro.settings.ShortcutMode

@Composable
fun SettingsScreen(state: EngineState, client: EngineClient) {
    val c = state.config
    var backupMsg by remember { mutableStateOf<String?>(null) }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }
    var confirmUninstall by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            client.send(Protocol.GET_LOG) { b -> log = b.getStringArrayList(Protocol.K_LOG) ?: emptyList() }
            delay(3_000)
        }
    }
    // Lo que el motor detecta mientras aprende un botón se guarda como atajo.
    LaunchedEffect(state.learnedKey) {
        if (state.learnedKey != 0 && state.learnedKey != c.shortcutKey) client.setConfig(c.copy(shortcutKey = state.learnedKey))
    }

    Columns {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Atajo del control") {
                Text(
                    if (c.shortcutKey == 0) "Sin atajo" else "Botón: ${keyName(c.shortcutKey)}",
                    color = Palette.text, fontSize = 18.sp,
                )
                when {
                    state.learning -> Text("Apretá el botón que querés usar…", color = Palette.warn, fontSize = 16.sp)
                    state.rejectedKey != 0 -> Text("El botón ${keyName(state.rejectedKey)} no se puede usar. Probá con otro.",
                        color = Palette.error, fontSize = 15.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(if (state.learning) "Cancelar" else "Elegir botón", {
                        client.send(if (state.learning) Protocol.STOP_LEARN else Protocol.START_LEARN)
                    }, primary = true)
                    if (c.shortcutKey != 0) PillButton("Quitar atajo", { client.setConfig(c.copy(shortcutKey = 0)) })
                }
                Choices(
                    listOf(ShortcutMode.LONG_PRESS to "Mantener 2 s", ShortcutMode.SINGLE_PRESS to "Toque simple"),
                    c.shortcutMode,
                ) { client.setConfig(c.copy(shortcutMode = it)) }
                Hint(if (c.shortcutMode == ShortcutMode.LONG_PRESS)
                    "Mantenelo apretado 2 segundos para prender o apagar Volumen Maestro."
                else "Cada toque prende o apaga Volumen Maestro.")
                Hint("El botón igual hace lo suyo en la TV, así que conviene uno que no uses (por ejemplo Info).")
            }
            Section("Botón TCL Home") {
                ToggleRow("Panel de apps abiertas", c.appPanel, { client.setConfig(c.copy(appPanel = it)) },
                    "Un toque abre las apps abiertas; dos toques vuelven a la app anterior.")
            }
            Section("Copia de seguridad") {
                Hint("Cada cambio se guarda solo en /sdcard/VolumenMaestro. Si reinstalás la app, te ofrece restaurarla.")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("Exportar ahora", {
                        client.send(Protocol.EXPORT_BACKUP) { backupMsg = if (it.getBoolean(Protocol.K_OK)) "✓ Copia guardada" else "No se pudo guardar" }
                    })
                    PillButton("Importar", {
                        client.send(Protocol.IMPORT_BACKUP) { backupMsg = if (it.getBoolean(Protocol.K_OK)) "✓ Ajustes importados" else "No encontré una copia" }
                    })
                }
                backupMsg?.let { Text(it, color = Palette.ok, fontSize = 15.sp) }
            }
            Section("Desinstalar") {
                Hint("Deja la TV como venía (Dolby, salida digital en Auto, volumen al 100 %), cancela el reinicio " +
                    "programado y apaga el servicio. Después abre el desinstalador de Android.")
                PillButton(if (confirmUninstall) "¿Seguro? Apretá de nuevo" else "Desinstalar limpio", {
                    if (!confirmUninstall) {
                        confirmUninstall = true
                    } else {
                        client.send(Protocol.PREPARE_UNINSTALL) {
                            context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }, danger = true)
            }
            Hint("Volumen Maestro ${BuildConfig.VERSION_NAME} · hecho para esta TCL")
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Section("Registro", Modifier.fillMaxWidth()) {
                LazyColumn(
                    Modifier.fillMaxWidth().height(560.dp)
                        .background(Palette.background, RoundedCornerShape(12.dp)).padding(12.dp),
                ) {
                    items(log) { line ->
                        Text(line, color = Palette.textDim, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/** Nombres de los códigos crudos del kernel (getevent). Info del TCL RC813 manda 0xC3. */
private val KEY_NAMES = mapOf(
    0xC3 to "Info",
    0x166 to "Info",
    0x192 to "CH +",
    0x193 to "CH −",
    0x16D to "Guía",
    0x179 to "TV",
    0x182 to "Entrada",
    0x8B to "Menú",
    0x18E to "Rojo",
    0x18F to "Verde",
    0x190 to "Amarillo",
    0x191 to "Azul",
)

fun keyName(code: Int): String = KEY_NAMES[code] ?: "botón 0x%02X".format(code)
