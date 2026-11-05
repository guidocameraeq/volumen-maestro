package local.volumenmaestro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import local.volumenmaestro.engine.Protocol
import local.volumenmaestro.settings.Config
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TvStats(val memAvailKb: Long, val memTotalKb: Long, val dataFreeKb: Long, val uptimeSec: Long)

@Composable
fun OptimizeScreen(state: EngineState, client: EngineClient) {
    var stats by remember { mutableStateOf<TvStats?>(null) }
    var optimizing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var confirmReboot by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            client.send(Protocol.GET_STATS) { b ->
                if (b.containsKey(Protocol.K_MEM_TOTAL)) stats = TvStats(
                    b.getLong(Protocol.K_MEM_AVAIL), b.getLong(Protocol.K_MEM_TOTAL),
                    b.getLong(Protocol.K_DATA_FREE), b.getLong(Protocol.K_UPTIME),
                )
            }
            delay(5_000)
        }
    }

    Columns {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val s = stats
                StatCard(Modifier.weight(1f), "Memoria libre",
                    s?.let { "${it.memAvailKb / 1024} MB" } ?: "…",
                    s?.let { "de ${String.format(Locale("es"), "%.1f", it.memTotalKb / 1048576f)} GB" } ?: "",
                    s?.let { it.memAvailKb.toFloat() / it.memTotalKb.coerceAtLeast(1) })
                StatCard(Modifier.weight(1f), "Espacio libre",
                    s?.let { String.format(Locale("es"), "%.1f GB", it.dataFreeKb / 1048576f) } ?: "…", "", null)
                val days = (s?.uptimeSec ?: 0) / 86400
                StatCard(Modifier.weight(1f), "Sin reiniciar",
                    s?.let { if (days >= 1) "$days días" else "${it.uptimeSec / 3600} h" } ?: "…",
                    if (days >= 7) "Conviene reiniciar" else "", null, warn = days >= 7)
            }
            Section("Optimizar ahora") {
                Hint("Libera la caché de todas las apps (solo archivos temporales) y cierra las que quedaron en segundo plano. " +
                    "No toca lo que estás usando, ni Alexa, ni el volumen.")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(if (optimizing) "Optimizando…" else "Optimizar ahora", {
                        if (optimizing) return@PillButton
                        optimizing = true
                        result = null
                        client.send(Protocol.OPTIMIZE) { b ->
                            optimizing = false
                            result = if (b.getBoolean(Protocol.K_OK)) "✓ Liberaste ${b.getLong(Protocol.K_FREED_STORAGE)} MB de espacio " +
                                "y ${b.getLong(Protocol.K_FREED_MEMORY)} MB de memoria."
                            else "No se pudo optimizar. Mirá el estado en Inicio."
                        }
                    }, primary = true)
                    PillButton(if (confirmReboot) "¿Seguro? Apretá de nuevo" else "Reiniciar ahora", {
                        if (confirmReboot) client.send(Protocol.REBOOT_NOW) else confirmReboot = true
                    }, danger = confirmReboot)
                }
                result?.let { Text(it, color = Palette.ok, fontSize = 16.sp) }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            RebootSection(state, client)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String, detail: String, fraction: Float?, warn: Boolean = false) {
    Column(
        modifier.background(Palette.surface, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Palette.textDim, fontSize = 13.sp, maxLines = 1)
        Text(value, color = if (warn) Palette.warn else Palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (detail.isNotEmpty()) Text(detail, color = if (warn) Palette.warn else Palette.textDim, fontSize = 14.sp)
        if (fraction != null) {
            Box(Modifier.fillMaxWidth().height(8.dp).background(Palette.surfaceHigh, RoundedCornerShape(50))) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(8.dp).background(Palette.accent, RoundedCornerShape(50)))
            }
        }
    }
}

private val DAYS = listOf(
    DayOfWeek.MONDAY to "L", DayOfWeek.TUESDAY to "M", DayOfWeek.WEDNESDAY to "M", DayOfWeek.THURSDAY to "J",
    DayOfWeek.FRIDAY to "V", DayOfWeek.SATURDAY to "S", DayOfWeek.SUNDAY to "D",
)

/** Paso de la hora del reinicio, en minutos. */
private const val TIME_STEP = 30

private fun nextRebootText(nextMillis: Long, c: Config): String {
    if (nextMillis <= 0) return "Elegí al menos un día."
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(nextMillis).atZone(zone)
    val today = LocalDate.now(zone)
    val hour = String.format("%02d:%02d", c.rebootHour, c.rebootMinute)
    val day = when (start.toLocalDate()) {
        today -> "Esta noche"
        today.minusDays(1) -> "Esta noche" // la noche de ayer sigue hasta las 6
        today.plusDays(1) -> "Mañana"
        else -> DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es")).format(start).replaceFirstChar { it.uppercase() }
    }
    return "Próximo: $day, si la apagás desde las $hour"
}

@Composable
private fun RebootSection(state: EngineState, client: EngineClient) {
    val c = state.config
    fun set(new: Config) = client.setConfig(new)
    Section("Reinicio al apagar", Modifier.fillMaxWidth()) {
        ToggleRow("Reiniciar al apagar de noche", c.rebootEnabled, { set(c.copy(rebootEnabled = it)) },
            "Con 2 GB de memoria, reiniciar seguido es lo que más se nota.")
        if (c.rebootEnabled) {
            Text("Días", color = Palette.textDim, fontSize = 15.sp)
            // Siete botones iguales que ocupan el ancho: con el ✓ no entraban y se cortaban el sábado y el domingo.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAYS.forEach { (day, label) ->
                    val on = day in c.rebootDays
                    PillButton(label, {
                        set(c.copy(rebootDays = if (on) c.rebootDays - day else c.rebootDays + day))
                    }, Modifier.weight(1f), selected = on, check = false, horizontalPadding = 0.dp)
                }
            }
            Text("Desde las", color = Palette.textDim, fontSize = 15.sp)
            fun shift(minutes: Int) {
                val t = ((c.rebootHour * 60 + c.rebootMinute) / TIME_STEP * TIME_STEP + minutes + 24 * 60) % (24 * 60)
                set(c.copy(rebootHour = t / 60, rebootMinute = t % 60))
            }
            Stepper(String.format("%02d:%02d", c.rebootHour, c.rebootMinute), { shift(-TIME_STEP) }, { shift(TIME_STEP) })
            Text("Cuántas veces", color = Palette.textDim, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Cada día elegido", { set(c.copy(rebootOncePerWeek = false)) },
                    Modifier.weight(1f), selected = !c.rebootOncePerWeek, horizontalPadding = 8.dp)
                PillButton("Una por semana", { set(c.copy(rebootOncePerWeek = true)) },
                    Modifier.weight(1f), selected = c.rebootOncePerWeek, horizontalPadding = 8.dp)
            }
            Text(nextRebootText(state.nextReboot, c), color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Hint("Se reinicia en el momento, la primera vez que la apagás después de esa hora (hasta las 6 de la " +
                "mañana), y queda apagada. De día, apagarla y prenderla no hace nada.")
        }
    }
}
