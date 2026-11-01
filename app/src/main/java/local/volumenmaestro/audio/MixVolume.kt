package local.volumenmaestro.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer

/**
 * Un ecualizador plano sobre la mezcla general (sesión 0) que se hace cargo del volumen maestro.
 *
 * TCL engancha sus efectos de sonido (DTS, Dolby, ecualizadores) a la mezcla al prender la TV.
 * Con efectos en la mezcla, Android redondea el volumen maestro a 0 o 1 (por debajo de 0,5 = mudo),
 * salvo que uno de esos efectos controle el volumen. El ecualizador de Android sí lo controla:
 * recibe el volumen maestro, lo aplica él y la mezcla queda en 1. Ver ADR-013.
 */
class MixVolume(private val onLog: (String) -> Unit) {
    private var eq: Equalizer? = null

    /** Lo crea si falta o si murió (el servidor de audio se reinició). */
    fun ensure() {
        val current = eq
        if (current != null && current.setEnabled(true) == AudioEffect.SUCCESS) return
        release()
        eq = try {
            Equalizer(PRIORITY, 0).apply {
                for (band in 0 until numberOfBands) setBandLevel(band.toShort(), 0)
                enabled = true
            }.also { onLog(if (current == null) "Control de volumen en la mezcla activo" else "Control de volumen en la mezcla recreado") }
        } catch (e: RuntimeException) {
            onLog("No se pudo crear el control de volumen en la mezcla: $e")
            null
        }
    }

    fun release() {
        eq?.release()
        eq = null
    }

    private companion object {
        const val PRIORITY = 1000
    }
}
