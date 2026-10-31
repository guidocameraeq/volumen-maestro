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

private enum class Frequency(val label: String, val days: Set<DayOfWeek>?) {
    DAILY("Todos los días", DayOfWeek.entries.toSet()),
    WEEKLY("1 vez por semana", setOf(DayOfWeek.MONDAY)),
    TWICE("2 veces por semana", setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
    CUSTOM("Personalizado", null),
}

private fun frequencyOf(days: Set<DayOfWeek>) =
    Frequency.entries.firstOrNull { it.days == days } ?: Frequency.CUSTOM

@Composable
private fun RebootSection(state: EngineState, client: EngineClient) {
    val c = state.config
    fun set(new: Config) = client.setConfig(new)
    Section("Reinicio programado", Modifier.fillMaxWidth()) {
        ToggleRow("Reiniciar automáticamente", c.rebootEnabled, { set(c.copy(rebootEnabled = it)) },
            "Con 2 GB de memoria, reiniciar seguido es lo que más se nota.")
        if (c.rebootEnabled) {
            Text("Frecuencia", color = Palette.textDim, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Frequency.entries.forEach { f ->
                    PillButton(f.label, { f.days?.let { set(c.copy(rebootDays = it)) } },
                        selected = frequencyOf(c.rebootDays) == f)
                }
            }
            Text("Días", color = Palette.textDim, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DAYS.forEach { (day, label) ->
                    val on = day in c.rebootDays
                    PillButton(label, {
                        set(c.copy(rebootDays = if (on) c.rebootDays - day else c.rebootDays + day))
                    }, selected = on)
                }
            }
            Text("Hora", color = Palette.textDim, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Stepper(String.format("%02d h", c.rebootHour),
                    { set(c.copy(rebootHour = (c.rebootHour + 23) % 24)) },
                    { set(c.copy(rebootHour = (c.rebootHour + 1) % 24)) })
                Stepper(String.format("%02d min", c.rebootMinute),
                    { set(c.copy(rebootMinute = (c.rebootMinute + 55) % 60)) },
                    { set(c.copy(rebootMinute = (c.rebootMinute + 5) % 60)) })
            }
            Text(
                if (state.nextReboot > 0) "Próximo reinicio: " + DateTimeFormatter
                    .ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", Locale("es"))
                    .format(Instant.ofEpochMilli(state.nextReboot).atZone(ZoneId.systemDefault()))
                else "Elegí al menos un día.",
                color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            )
            Hint("Si a esa hora estás usando la TV, espera hasta 3 horas a que la apagues. " +
                "Después del reinicio, si la pantalla se prende sola, la vuelve a apagar.")
        }
    }
}
