package local.volumenmaestro.audio

import local.volumenmaestro.settings.Config
import local.volumenmaestro.settings.CurveType
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

/**
 * Convierte el índice de volumen de la TV (0..max) en la ganancia del volumen maestro (0..1).
 * En el máximo siempre da 1.0: la app nunca amplifica, solo atenúa.
 */
object VolumeCurve {
    const val SILENCE_DB = -80f
    private const val AMPLIFIER_RANGE_DB = 60f
    private const val AMPLIFIER_BEND = 0.6f

    fun gain(index: Int, max: Int, muted: Boolean, config: Config): Float {
        if (muted) return 0f
        val x = (index.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
        if (x == 0f && config.zeroIsSilence) return 0f
        // "Mínimo de la curva": el 0 suena como el primer punto.
        val xx = if (x == 0f) 1f / max.coerceAtLeast(1) else x
        return when (config.curve) {
            CurveType.SMOOTH -> xx * xx
            CurveType.EVEN_DB -> dbToGain((xx - 1f) * 100f * config.dbPerPoint)
            CurveType.AMPLIFIER -> {
                // Pendiente baja en el medio (control fino) y alta en los extremos.
                val bent = xx + AMPLIFIER_BEND * sin(2 * PI * xx).toFloat() / (2 * PI).toFloat()
                dbToGain(-AMPLIFIER_RANGE_DB * (1f - bent))
            }
        }
    }

    fun dbToGain(db: Float): Float = if (db <= SILENCE_DB) 0f else 10f.pow(db / 20f)

    fun gainToDb(gain: Float): Float = if (gain <= 0f) SILENCE_DB else (20f * log10(gain)).coerceAtLeast(SILENCE_DB)
}
