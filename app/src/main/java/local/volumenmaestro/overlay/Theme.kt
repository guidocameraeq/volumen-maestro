package local.volumenmaestro.overlay

import android.graphics.Canvas
import local.volumenmaestro.overlay.themes.AcuaticoTheme
import local.volumenmaestro.overlay.themes.ArcadeTheme
import local.volumenmaestro.overlay.themes.CandelabroTheme
import local.volumenmaestro.overlay.themes.EspacialTheme
import local.volumenmaestro.overlay.themes.EspiralTheme
import local.volumenmaestro.overlay.themes.MedievalTheme
import local.volumenmaestro.overlay.themes.NeonTheme
import local.volumenmaestro.overlay.themes.OndasTheme
import local.volumenmaestro.overlay.themes.RetroTheme
import local.volumenmaestro.overlay.themes.SableTheme
import local.volumenmaestro.overlay.themes.SteampunkTheme
import local.volumenmaestro.overlay.themes.TelaranaTheme
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

/** Un grupo de la pestaña Temas. */
class ThemeGroup(val name: String, val hint: String, val themes: List<Theme>)

object ThemeRegistry {
    /** Los grupos y su orden son los de la pestaña Temas. Para sumar un tema, agregalo a su grupo. */
    val groups: List<ThemeGroup> = listOf(
        ThemeGroup("Sobrios", "Pocos elementos, para todos los días.",
            listOf(ClassicTheme, VidrioTheme, OndasTheme, RetroTheme)),
        ThemeGroup("Fantasía", "Mundos con mucha escenografía.",
            listOf(MedievalTheme, EspacialTheme, AcuaticoTheme, SteampunkTheme, SableTheme)),
        ThemeGroup("Gamer y tecno", "Pantallas, píxeles y neón.",
            listOf(NeonTheme, ArcadeTheme, TerminalTheme)),
        ThemeGroup("Noche de brujas", "Oscuros y elegantes.",
            listOf(EspiralTheme, CandelabroTheme, TelaranaTheme)),
    )

    val all: List<Theme> = groups.flatMap { it.themes }

    fun byId(id: String): Theme = all.firstOrNull { it.id == id } ?: ClassicTheme
}
