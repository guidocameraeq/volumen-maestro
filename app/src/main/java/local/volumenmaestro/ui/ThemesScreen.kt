package local.volumenmaestro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import local.volumenmaestro.engine.Protocol
import local.volumenmaestro.overlay.OverlayController
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme
import local.volumenmaestro.overlay.ThemeRegistry
import local.volumenmaestro.settings.OverlayDuration
import kotlin.math.sin

@Composable
fun ThemesScreen(state: EngineState, client: EngineClient) {
    val c = state.config
    var focusedId by remember { mutableStateOf(c.themeId) }
    Columns {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(ThemeRegistry.all, key = { it.id }) { theme ->
                    ThemeCard(
                        theme,
                        selected = theme.id == c.themeId,
                        onFocus = { focusedId = theme.id },
                        onClick = { client.setConfig(c.copy(themeId = theme.id)) },
                    )
                }
            }
        }
        Column(
            Modifier.weight(1.15f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val theme = ThemeRegistry.byId(focusedId)
            Section(theme.name, Modifier.fillMaxWidth()) {
                TvPreview(theme, Modifier.fillMaxWidth(), showSide = c.showSide)
                Hint(theme.description)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(
                        if (theme.id == c.themeId) "Elegido" else "Elegir este tema",
                        { client.setConfig(c.copy(themeId = theme.id)) },
                        selected = theme.id == c.themeId,
                    )
                    PillButton("Probar en pantalla", { client.withId(Protocol.PREVIEW_THEME, theme.id) })
                }
            }
            Section("Panel") {
                Text("Tiempo en pantalla", color = Palette.textDim, fontSize = 15.sp)
                Choices(
                    listOf(OverlayDuration.NORMAL to "3,8 s", OverlayDuration.LONG to "5 s",
                        OverlayDuration.VERY_LONG to "7 s"),
                    c.overlayDuration,
                ) { client.setConfig(c.copy(overlayDuration = it)) }
                ToggleRow("Cartel lateral", c.showSide, { client.setConfig(c.copy(showSide = it)) },
                    "Si lo apagás, vuelve a verse la barrita gris de TCL a la derecha.")
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: Theme, selected: Boolean, onFocus: () -> Unit, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        Modifier
            .scale(if (focused) 1.05f else 1f)
            .background(if (focused) Palette.surfaceHigh else Palette.surface, RoundedCornerShape(16.dp))
            .border(2.dp, if (selected) Palette.accent else Color.Transparent, RoundedCornerShape(16.dp))
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvPreview(theme, Modifier.fillMaxWidth(), showSide = true, animate = focused || selected)
        Text((if (selected) "✓ " else "") + theme.name, color = Palette.text, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/**
 * Una TV de muestra con los dos carteles en su lugar real, dibujados con el mismo código
 * del tema que se usa en la TV. El volumen hace una demo sola.
 */
@Composable
fun TvPreview(theme: Theme, modifier: Modifier, showSide: Boolean, animate: Boolean = true) {
    var t by remember { mutableFloatStateOf(1.5f) }
    if (animate) {
        LaunchedEffect(theme.id) {
            val start = withFrameNanos { it }
            while (true) withFrameNanos { t = (it - start) / 1e9f + 1.5f }
        }
    }
    Canvas(
        modifier
            .aspectRatio(16f / 9f)
            .background(
                Brush.linearGradient(listOf(Color(0xFF1B2333), Color(0xFF0B0E14), Color(0xFF2A1B33))),
                RoundedCornerShape(12.dp),
            ),
    ) {
        val scale = size.width / 1920f
        val demo = demoState(t)
        drawPanel(scale, 480f, 1080f - OverlayController.BOTTOM_MARGIN - OverlayController.BOTTOM_H,
            OverlayController.BOTTOM_W, OverlayController.BOTTOM_H) { canvas, w, h ->
            theme.drawBottom(canvas, w, h, demo)
        }
        if (showSide) {
            drawPanel(scale, OverlayController.SIDE_X.toFloat(), OverlayController.SIDE_Y.toFloat(),
                OverlayController.SIDE_W, OverlayController.SIDE_H) { canvas, w, h ->
                theme.drawSide(canvas, w, h, demo)
            }
        }
    }
}

private fun DrawScope.drawPanel(
    scale: Float, x: Float, y: Float, w: Int, h: Int,
    draw: (android.graphics.Canvas, Int, Int) -> Unit,
) {
    withTransform({
        scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
        translate(x, y)
    }) {
        val canvas = drawContext.canvas.nativeCanvas
        canvas.save()
        canvas.clipRect(0f, 0f, w.toFloat(), h.toFloat())
        draw(canvas, w, h)
        canvas.restore()
    }
}

/** Volumen de demostración: sube, baja y silencia cada tanto. */
private fun demoState(t: Float): OverlayState {
    val cycle = t % 12f
    val muted = cycle in 9f..10.5f
    val level = (0.55f + 0.42f * sin(t * 0.7f)).coerceIn(0f, 1f)
    // Igual que el controlador real: en silencio el nivel animado baja a 0.
    val shown = if (muted) (level * (1f - (cycle - 9f) / 0.25f).coerceIn(0f, 1f)) else level
    return OverlayState(
        level = shown,
        index = (level * 100).toInt(),
        muted = muted,
        timeSec = t,
        appear = 1f,
        sinceChangeSec = if (muted) cycle - 9f else (t % 1.7f),
    )
}
