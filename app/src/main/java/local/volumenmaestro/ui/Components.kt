package local.volumenmaestro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import local.volumenmaestro.engine.Level

object Palette {
    val background = Color(0xFF0D1017)
    val surface = Color(0xFF171B26)
    val surfaceHigh = Color(0xFF222838)
    val accent = Color(0xFF8B5CF6)
    val accentSoft = Color(0xFF3B2A6B)
    val text = Color(0xFFF2F3F7)
    val textDim = Color(0xFFA3A9B8)
    val ok = Color(0xFF34D399)
    val warn = Color(0xFFFBBF24)
    val error = Color(0xFFF87171)

    fun of(level: Level) = when (level) {
        Level.OK -> ok
        Level.WARN -> warn
        Level.ERROR -> error
    }
}

/** Tarjeta con título para agrupar opciones. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(Palette.surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

/**
 * Botón propio para TV: se agranda y se ilumina con el foco. [selected] lo marca como opción elegida.
 * Usa foundation (clickable responde al OK del control).
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    primary: Boolean = false,
    danger: Boolean = false,
    /** false para botones angostos (los días): la selección se ve igual por el borde y el color. */
    check: Boolean = true,
    horizontalPadding: Dp = 22.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val base = when {
        danger -> Color(0xFF4A1D24)
        primary || selected -> Palette.accentSoft
        else -> Palette.surfaceHigh
    }
    val bg = when {
        focused && danger -> Palette.error
        focused -> Palette.accent
        else -> base
    }
    Box(
        modifier
            .scale(if (focused) 1.06f else 1f)
            .background(bg, RoundedCornerShape(50))
            .then(if (selected && !focused) Modifier.border(2.dp, Palette.accent, RoundedCornerShape(50)) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (if (selected && check) "✓ " else "") + text,
            color = Palette.text,
            fontSize = 16.sp,
            fontWeight = if (selected || primary) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Fila de opciones excluyentes. */
@Composable
fun <T> Choices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (value, label) ->
            PillButton(label, onClick = { onSelect(value) }, selected = value == selected)
        }
    }
}

/** − valor + */
@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        PillButton("−", onMinus)
        Text(value, color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        PillButton("+", onPlus)
    }
}

/** Interruptor con texto, para usar con el control. */
@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, detail: String? = null) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (focused) Palette.surfaceHigh else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Palette.text, fontSize = 17.sp)
            if (detail != null) Text(detail, color = Palette.textDim, fontSize = 14.sp)
        }
        Box(
            Modifier
                .size(width = 56.dp, height = 30.dp)
                .background(if (checked) Palette.accent else Palette.surfaceHigh, RoundedCornerShape(50))
                .padding(4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(22.dp).background(Color.White, CircleShape))
        }
    }
}

@Composable
fun StatusDot(color: Color, size: Int = 14) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}

@Composable
fun Gap(height: Int = 12) = Spacer(Modifier.padding(top = height.dp))

@Composable
fun Hint(text: String) = Text(text, color = Palette.textDim, fontSize = 15.sp)
