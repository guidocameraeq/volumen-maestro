package local.volumenmaestro.overlay

import android.graphics.Canvas
import local.volumenmaestro.overlay.themes.AcuaticoTheme
import local.volumenmaestro.overlay.themes.ArcadeTheme
import local.volumenmaestro.overlay.themes.EspacialTheme
import local.volumenmaestro.overlay.themes.MedievalTheme
import local.volumenmaestro.overlay.themes.NeonTheme
import local.volumenmaestro.overlay.themes.OndasTheme
import local.volumenmaestro.overlay.themes.RetroTheme
import local.volumenmaestro.overlay.themes.SableTheme
import local.volumenmaestro.overlay.themes.SteampunkTheme
import local.volumenmaestro.overlay.themes.TerminalTheme
import local.volumenmaestro.overlay.themes.VidrioTheme

/** Lo que cada tema necesita para dibujar un cuadro. */
data class OverlayState(
    /** Nivel animado 0..1 (se acerca suavemente al real). */
    val level: Float,
    /** Número a mostrar (0..100). */
    val index: Int,
    val muted: Boolean,
    /** Reloj de animación en segundos desde que apareció el cartel. */
    val timeSec: Float,
    /** 0..1: entrada (sube a 1) y salida (baja a 0). */
    val appear: Float,
    /** Segundos desde el último cambio de volumen (para chispas, destellos, etc.). */
    val sinceChangeSec: Float,
    /** Mensaje en lugar del volumen, por ejemplo "Volumen Maestro: APAGADO". */
    val message: String? = null,
)

/**
 * Un tema dibuja los dos carteles. Reglas para todos:
 * - El fondo es opaco desde el primer cuadro: tapa los carteles de TCL aunque appear sea 0.
 * - Solo dibuja dentro de (0,0)-(w,h).
 * - No guarda estado entre cuadros: todo sale de OverlayState (así la vista previa y la TV coinciden).
 */
interface Theme {
    val id: String
    val name: String
    val description: String
    fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState)
    fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState)
}

object ThemeRegistry {
    /** El orden es el de la grilla de la pestaña Temas. Para sumar un tema, agregalo acá. */
    val all: List<Theme> = listOf(
        SableTheme,
        NeonTheme,
        MedievalTheme,
        RetroTheme,
        VidrioTheme,
        ArcadeTheme,
        EspacialTheme,
        OndasTheme,
        SteampunkTheme,
        AcuaticoTheme,
        TerminalTheme,
        ClassicTheme,
    )

    fun byId(id: String): Theme = all.firstOrNull { it.id == id } ?: ClassicTheme
}
