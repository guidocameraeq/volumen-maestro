package local.volumenmaestro.recientes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import kotlinx.coroutines.delay
import local.volumenmaestro.engine.Protocol
import local.volumenmaestro.ui.EngineClient
import local.volumenmaestro.ui.Palette
import local.volumenmaestro.ui.PillButton

/**
 * El panel de apps abiertas: una franja abajo, sobre lo que se estaba viendo. La lista la arma el
 * motor ([AppPanel]) y llega en el intent; abrir una app lo hace esta pantalla, cerrar pide al motor.
 */
class RecentsActivity : ComponentActivity() {
    private lateinit var client: EngineClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = EngineClient(applicationContext)
        val packages = intent.getStringArrayListExtra(EXTRA_APPS).orEmpty()
        val current = intent.getStringExtra(EXTRA_CURRENT)
        val apps = packages.mapNotNull { load(this, it) }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Palette.accent)) {
                Panel(
                    initial = apps,
                    current = current,
                    focusIndex = if (current != null && apps.firstOrNull()?.pkg == current) 1 else 0,
                    onOpen = ::open,
                    onClose = { client.withId(Protocol.CLOSE_APP, it) },
                    onCloseAll = { list ->
                        client.send(Protocol.CLOSE_ALL, Bundle().apply { putStringArrayList(Protocol.K_APPS, ArrayList(list)) })
                        finish()
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        client.start()
    }

    override fun onStop() {
        client.stop()
        super.onStop()
        finish() // si se va a otra app, el panel no queda abierto detrás
    }

    private fun open(pkg: String) {
        val launch = packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            // Igual que el inicio: si la app sigue abierta, vuelve donde estaba.
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        }
        finish()
    }

    companion object {
        const val EXTRA_APPS = "apps"
        const val EXTRA_CURRENT = "current"
    }
}

private class PanelApp(val pkg: String, val label: String, val image: ImageBitmap?, val isSnapshot: Boolean)

private fun load(context: Context, pkg: String): PanelApp? {
    val pm = context.packageManager
    val info = try {
        pm.getApplicationInfo(pkg, 0)
    } catch (e: Exception) {
        return null
    }
    val snapshot = Snapshots.load(context, pkg)
    val image = snapshot ?: (pm.getApplicationBanner(info) ?: pm.getApplicationIcon(info)).let { d ->
        val w = d.intrinsicWidth.takeIf { it > 0 } ?: 320
        val h = d.intrinsicHeight.takeIf { it > 0 } ?: 180
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b ->
            d.setBounds(0, 0, w, h)
            d.draw(Canvas(b))
        }
    }
    return PanelApp(pkg, pm.getApplicationLabel(info).toString(), image.asImageBitmap(), snapshot != null)
}

@Composable
private fun Panel(
    initial: List<PanelApp>,
    current: String?,
    focusIndex: Int,
    onOpen: (String) -> Unit,
    onClose: (String) -> Unit,
    onCloseAll: (List<String>) -> Unit,
) {
    val apps = remember { mutableStateListOf<PanelApp>().apply { addAll(initial) } }
    val firstFocus = remember { FocusRequester() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Transparent, 1f to Color(0xB3000000))),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, bottom = 28.dp)
                .background(Color(0xF0171B26), RoundedCornerShape(20.dp))
                .padding(start = 24.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Apps abiertas", color = Palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(16.dp))
                Text(
                    if (apps.isEmpty()) "" else "OK abre · mantené OK para cerrar",
                    color = Palette.textDim, fontSize = 13.sp, modifier = Modifier.weight(1f),
                )
                if (apps.isNotEmpty()) PillButton("Limpiar todo", { onCloseAll(apps.map { it.pkg }) }, danger = true)
            }
            if (apps.isEmpty()) {
                Text("No hay apps abiertas", color = Palette.textDim, fontSize = 17.sp)
                Spacer(Modifier.height(12.dp))
                PillButton("Volver", { onOpen("") }, Modifier.focusRequester(firstFocus))
                Spacer(Modifier.height(10.dp))
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    // Lugar para que la tarjeta enfocada crezca sin cortarse contra el borde.
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                ) {
                    itemsIndexed(apps, key = { _, a -> a.pkg }) { i, app ->
                        AppCard(
                            app,
                            onScreen = app.pkg == current,
                            modifier = if (i == focusIndex.coerceAtMost(apps.lastIndex)) Modifier.focusRequester(firstFocus) else Modifier,
                            onOpen = { onOpen(app.pkg) },
                            onClose = {
                                if (app.pkg !in RecentApps.PROTECTED) {
                                    onClose(app.pkg)
                                    apps.remove(app)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
    // El foco inicial va a la app anterior. Se reintenta unos cuadros: al principio la fila
    // todavía no está armada y Compose le da el foco a la primera tarjeta.
    LaunchedEffect(Unit) {
        repeat(5) {
            delay(40)
            try {
                firstFocus.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }
}

@Composable
private fun AppCard(app: PanelApp, onScreen: Boolean, modifier: Modifier, onOpen: () -> Unit, onClose: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val longPressed = remember { booleanArrayOf(false) }
    Column(
        modifier
            .width(184.dp)
            .scale(if (focused) 1.08f else 1f)
            .onPreviewKeyEvent { e ->
                val k = e.nativeKeyEvent
                if (k.keyCode != KeyEvent.KEYCODE_DPAD_CENTER && k.keyCode != KeyEvent.KEYCODE_ENTER) return@onPreviewKeyEvent false
                when {
                    k.action == KeyEvent.ACTION_DOWN && k.repeatCount == 0 -> { longPressed[0] = false; false }
                    k.action == KeyEvent.ACTION_DOWN && k.isLongPress -> { longPressed[0] = true; onClose(); true }
                    k.action == KeyEvent.ACTION_DOWN -> true // repeticiones mientras se mantiene
                    else -> longPressed[0] // al soltar después de mantener, no abre
                }
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.surfaceHigh)
                .border(2.dp, if (focused) Palette.accent else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (app.image != null) {
                Image(
                    app.image, contentDescription = app.label,
                    modifier = if (app.isSnapshot) Modifier.fillMaxSize() else Modifier.size(128.dp, 72.dp),
                    contentScale = if (app.isSnapshot) ContentScale.Crop else ContentScale.Fit,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                app.label, color = if (focused) Palette.text else Palette.textDim, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            if (onScreen) Text("  · en pantalla", color = Palette.textDim, fontSize = 12.sp, maxLines = 1)
        }
    }
}
