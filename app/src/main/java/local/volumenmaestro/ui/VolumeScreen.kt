package local.volumenmaestro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import local.volumenmaestro.audio.VolumeCurve
import local.volumenmaestro.settings.Config
import local.volumenmaestro.settings.CurveType
import local.volumenmaestro.settings.Transition
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VolumeScreen(state: EngineState, client: EngineClient) {
    val c = state.config
    fun set(new: Config) = client.setConfig(new)
    Columns {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Tipo de curva") {
                Choices(
                    listOf(CurveType.EVEN_DB to "Pareja", CurveType.SMOOTH to "Suave", CurveType.AMPLIFIER to "Ampli"),
                    c.curve,
                ) { set(c.copy(curve = it)) }
                Hint(
                    when (c.curve) {
                        CurveType.EVEN_DB -> "Cada punto baja siempre lo mismo; elegí cuánto en \"Salto por punto\"."
                        CurveType.SMOOTH -> "Cambia poco arriba y más rápido abajo."
                        CurveType.AMPLIFIER -> "Tipo amplificador: fina en el medio, donde más se escucha, y gruesa en los extremos."
                    },
                )
            }
            if (c.curve == CurveType.EVEN_DB) {
                Section("Salto por punto") {
                    Stepper(
                        String.format(Locale("es"), "%.1f dB", c.dbPerPoint),
                        onMinus = { set(c.copy(dbPerPoint = step(c.dbPerPoint, -0.1f))) },
                        onPlus = { set(c.copy(dbPerPoint = step(c.dbPerPoint, +0.1f))) },
                    )
                    Hint("De 100 a 1 baja ${(c.dbPerPoint * 99).roundToInt()} dB en total. Más alto = más recorrido.")
                }
            }
            Section("Volumen 0") {
                Choices(listOf(true to "Silencio total", false to "Mínimo de la curva"), c.zeroIsSilence) {
                    set(c.copy(zeroIsSilence = it))
                }
            }
            Section("Transición") {
                Choices(
                    listOf(Transition.OFF to "Apagada", Transition.FAST to "Rápida", Transition.SMOOTH to "Suave"),
                    c.transition,
                ) { set(c.copy(transition = it)) }
                Hint("Cuánto tarda el sonido en deslizarse al nivel nuevo.")
            }
            Section("Mantener apretado") {
                Text("✓ Mantener apretado el volumen sube o baja rápido: arranca despacio y acelera.",
                    color = Palette.ok, fontSize = 16.sp)
                Hint("La velocidad la maneja la TV; cuánto suena cada paso lo decide la curva de arriba.")
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Section("Curva", Modifier.fillMaxWidth()) {
                CurveGraph(c, state.index, state.max)
                val db = VolumeCurve.gainToDb(VolumeCurve.gain(state.index, state.max, state.muted, c))
                Text(
                    if (db <= VolumeCurve.SILENCE_DB) "Ahora (${state.index}): silencio"
                    else "Ahora (${state.index}): ${String.format(Locale("es"), "%.1f", db)} dB · ${percentLoudness(db)}",
                    color = Palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                )
                Hint("Usá los botones de volumen del control para escuchar la curva mientras la ajustás. " +
                    "En 100 la señal sale completa: lo fuerte del máximo lo pone el equipo de audio.")
            }
        }
    }
}

private fun step(v: Float, delta: Float) =
    ((v + delta) * 10).roundToInt().div(10f).coerceIn(Config.MIN_DB_PER_POINT, Config.MAX_DB_PER_POINT)

/** Cada −10 dB se escucha como la mitad de volumen. */
private fun percentLoudness(db: Float): String {
    val pct = (Math.pow(2.0, db / 10.0) * 100).roundToInt()
    return "se escucha al $pct %"
}

@Composable
private fun CurveGraph(config: Config, index: Int, max: Int) {
    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val minDb = -80f
        fun x(i: Int) = size.width * i / 100f
        fun y(db: Float) = size.height * (db.coerceAtLeast(minDb) / minDb)
        for (db in listOf(-20f, -40f, -60f)) {
            drawLine(Color(0xFF2B3142), Offset(0f, y(db)), Offset(size.width, y(db)), 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        }
        val path = Path()
        for (i in 0..100) {
            val db = VolumeCurve.gainToDb(VolumeCurve.gain(i, 100, false, config))
            if (i == 0) path.moveTo(x(i), y(db)) else path.lineTo(x(i), y(db))
        }
        drawPath(path, Palette.accent, style = Stroke(width = 6f))
        val cur = (index * 100f / max.coerceAtLeast(1)).roundToInt()
        val curDb = VolumeCurve.gainToDb(VolumeCurve.gain(cur, 100, false, config))
        drawCircle(Color.White, 12f, Offset(x(cur), y(curDb)))
    }
}
