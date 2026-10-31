package local.volumenmaestro.overlay.themes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Arcade 8-bit (mockup 5): diez corazones que se llenan por columnas, contador de tres dígitos,
 * "SILENCIO" titilando y, en el lateral, una torre de bloques con una moneda girando arriba.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object ArcadeTheme : Theme {
    override val id = "arcade"
    override val name = "Arcade 8-bit"
    override val description = "Pixel art de fichín: corazones que se llenan, contador de tres dígitos y torre de bloques."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.30f

    // Paleta tipo NES
    private val NEGRO = 0xFF0D0D1A.toInt()
    private val BLANCO = 0xFFFCFCFC.toInt()
    private val CELESTE = 0xFF3CBCFC.toInt()
    private val ROJO = 0xFFE40058.toInt()
    private val AMARILLO = 0xFFF8B800.toInt()
    private val GRIS_TEXTO = 0xFF7C7C7C.toInt()
    private val GRIS_LLENO = 0xFF8C8C8C.toInt()
    private val GRIS_OSCURO = 0xFF6C6C6C.toInt()
    private val VACIO = 0xFF2C2C4A.toInt()
    private val MONEDA_BORDE = 0xFF7C4A00.toInt()
    private val CRT = Color.argb(6, 255, 255, 255)
    private val BRILLO = Color.argb(115, 255, 255, 255)
    private val SOMBRA = Color.argb(71, 0, 0, 0)

    private val BLOQUES = intArrayOf(
        0xFFE40058.toInt(), 0xFFF87858.toInt(), 0xFFF8B800.toInt(), 0xFFB8F818.toInt(), 0xFF58D854.toInt(),
        0xFF00E8D8.toInt(), 0xFF3CBCFC.toInt(), 0xFF6888FC.toInt(), 0xFF9878F8.toInt(), 0xFFF878F8.toInt(),
    )
    private val CORAZON = arrayOf(".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X...")
    private val PIX_PARLANTE = arrayOf(
        "...X......", "..XX...X..", "XXXX....X.", "XXXX..X.X.", "XXXX..X.X.", "XXXX....X.", "..XX...X..", "...X......",
    )
    private val PIX_MUDO = arrayOf(
        "...X......", "..XX......", "XXXX.X...X", "XXXX..X.X.", "XXXX...X..", "XXXX..X.X.", "..XX.X...X", "...X......",
    )
    private val PAD3 = Array(101) { it.toString().padStart(3, '0') }

    // Pixel art: rectángulos sin antialias.
    private val px = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    private val pixFont: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = pixFont
        textScaleX = 1.3f // las letras monoespaciadas quedan más anchas, como Press Start 2P
    }

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val sc = canvas.save()
        if (w != BW.toInt() || h != BH.toInt()) canvas.scale(w / BW, h / BH)
        val vis = min(s.appear, clamp01(s.timeSec / ENTRADA))
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))

        // Placa de seguridad opaca sobre el cartel del sistema (siempre).
        px.color = NEGRO
        canvas.drawRect(36f, 11f, 924f, 142f, px)

        // La placa crece desde la zona mínima hasta el panel completo.
        val rx = 36f * (1f - p)
        val ry = 11f * (1f - p)
        val rw = 888f + 72f * p
        val rh = 131f + 19f * p
        canvas.clipRect(rx, ry, rx + rw, ry + rh)
        val n = if (p < 0.5f) 6f else 12f
        pixelRect(canvas, rx, ry, rw, rh, n, BLANCO)
        pixelRect(canvas, rx + 6f, ry + 6f, rw - 12f, rh - 12f, 6f, CELESTE)
        pixelRect(canvas, rx + 12f, ry + 12f, rw - 24f, rh - 24f, 6f, NEGRO)
        px.color = CRT
        var y = ry + 2f
        while (y < ry + rh) {
            canvas.drawRect(rx, y, rx + rw, y + 2f, px)
            y += 6f
        }

        if (ap >= 0.12f) {
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, s.timeSec) else drawVolume(canvas, s, ap)
        }
        canvas.restoreToCount(sc)
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float) {
        val mute = s.muted
        val v = level(s)
        text.textAlign = Paint.Align.LEFT
        text.textSize = 25f
        text.color = AMARILLO
        canvas.drawText("VOL", 42f, 56f, text)
        text.textSize = 37f
        text.color = if (mute) GRIS_TEXTO else BLANCO
        canvas.drawText(PAD3[s.index.coerceIn(0, 100)], 42f, 114f, text)

        // Corazones: cada uno vale 10 y se llena por columnas.
        val tope = min(9, max(0, ceil(v * 10f).toInt() - 1))
        for (i in 0 until 10) {
            val pop = easeBack(clamp01(ap * 12f - i))
            if (pop <= 0f) continue
            val fr = clamp01(v * 10f - i)
            val since = s.sinceChangeSec
            val yoff = if (i == tope && since < 0.35f) -sin(since / 0.35f * PI.toFloat()) * 10f else 0f
            val c = canvas.save()
            canvas.translate(180f + i * 64f + 28f, 75f + yoff)
            canvas.scale(pop, pop)
            canvas.translate(-28f, -24f)
            for (fy in CORAZON.indices) {
                val fila = CORAZON[fy]
                for (fx in 0 until 7) {
                    if (fila[fx] != 'X') continue
                    val lleno = (fx + 0.5f) / 7f <= fr
                    var col = if (lleno) (if (mute) GRIS_LLENO else ROJO) else VACIO
                    if (lleno && !mute && fy == 1 && fx == 1) col = BLANCO
                    px.color = col
                    canvas.drawRect(fx * 8f, fy * 8f, fx * 8f + 8f, fy * 8f + 8f, px)
                }
            }
            canvas.restoreToCount(c)
        }

        bitmap(canvas, if (mute) PIX_MUDO else PIX_PARLANTE, 855f, 47f, 7f, if (mute) ROJO else BLANCO)

        if (mute && floor(s.timeSec * 2.5f).toInt() % 2 == 0) {
            px.color = NEGRO
            canvas.drawRect(320f, 52f, 680f, 98f, px)
            text.textAlign = Paint.Align.CENTER
            text.textSize = 28f
            text.color = ROJO
            canvas.drawText("SILENCIO", 500f, 88f, text)
        }
    }

    private fun drawMessage(canvas: Canvas, msg: String, t: Float) {
        text.textAlign = Paint.Align.CENTER
        fitText(msg, 820f, 34f)
        // Sombra de un "pixel" desplazada, como los títulos de los fichines.
        text.color = MONEDA_BORDE
        canvas.drawText(msg, 484f, 92f, text)
        text.color = AMARILLO
        canvas.drawText(msg, 480f, 88f, text)
        // Dos corazoncitos que titilan a los costados.
        if (floor(t * 2f).toInt() % 2 == 0) {
            bitmap(canvas, CORAZON, 40f, 57f, 5f, ROJO)
            bitmap(canvas, CORAZON, 885f, 57f, 5f, ROJO)
        }
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val sc = canvas.save()
        if (w != SW.toInt() || h != SH.toInt()) canvas.scale(w / SW, h / SH)
        px.color = NEGRO
        canvas.drawRect(0f, 0f, SW, SH, px)
        pixelRect(canvas, 0f, 0f, SW, SH, 12f, BLANCO)
        pixelRect(canvas, 6f, 6f, SW - 12f, SH - 12f, 6f, CELESTE)
        pixelRect(canvas, 12f, 12f, SW - 24f, SH - 24f, 6f, NEGRO)

        val vis = min(s.appear, clamp01(s.timeSec / ENTRADA))
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val mute = s.muted
        val v = level(s)
        val t = s.timeSec

        // Torre de bloques: uno por cada 10 de volumen; el de arriba crece de a 6 px.
        val bx = 18f
        val bw = 72f
        val bh = 34f
        val base = 418f
        val nivel = v * 10f
        val maxB = floor(clamp01(ap) * 11f).toInt()
        var tope = base
        for (i in 0 until min(10, maxB)) {
            val fr = clamp01(nivel - i)
            if (fr < 0.05f) break
            val hq = max(6f, (bh * fr / 6f).roundToInt() * 6f)
            val y = base - i * bh - hq
            px.color = NEGRO
            canvas.drawRect(bx, y, bx + bw, y + hq, px)
            px.color = if (mute) (if (i % 2 == 1) GRIS_OSCURO else GRIS_LLENO) else BLOQUES[i]
            canvas.drawRect(bx + 3f, y + 3f, bx + bw - 3f, y + 3f + max(0f, hq - 6f), px)
            if (hq >= 18f) {
                px.color = BRILLO
                canvas.drawRect(bx + 3f, y + 3f, bx + bw - 3f, y + 9f, px)
                px.color = SOMBRA
                canvas.drawRect(bx + 3f, y + hq - 9f, bx + bw - 3f, y + hq - 3f, px)
            }
            tope = y
        }

        val cx = SW / 2f
        if (ap > 0.5f) {
            if (!mute) {
                // Moneda girando: el ancho sigue |cos| en escalones de 6 px.
                val cy = ((tope - 30f + sin(t * 4f) * 4f) / 3f).roundToInt() * 3f
                val mw = max(6f, (abs(cos(t * 5f)) * 30f / 6f).roundToInt() * 6f)
                px.color = MONEDA_BORDE
                canvas.drawRect(cx - mw / 2f - 3f, cy - 18f, cx + mw / 2f + 3f, cy + 18f, px)
                px.color = AMARILLO
                canvas.drawRect(cx - mw / 2f, cy - 15f, cx + mw / 2f, cy + 15f, px)
                if (mw > 12f) {
                    px.color = BLANCO
                    canvas.drawRect(cx - mw / 2f + 3f, cy - 12f, cx - mw / 2f + 9f, cy, px)
                }
            } else if (floor(t * 2f).toInt() % 2 == 0) {
                text.textAlign = Paint.Align.CENTER
                text.textSize = 19f
                text.color = BLANCO
                canvas.drawText("Zz", cx, tope - 20f, text)
            }
        }
        canvas.restoreToCount(sc)
    }

    /** Rectángulo con las esquinas "mordidas" en escalón de p píxeles (dos rectángulos en cruz). */
    private fun pixelRect(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, p: Float, color: Int) {
        px.color = color
        canvas.drawRect(x + p, y, x + w - p, y + h, px)
        canvas.drawRect(x, y + p, x + w, y + h - p, px)
    }

    private fun bitmap(canvas: Canvas, bmp: Array<String>, x: Float, y: Float, size: Float, color: Int) {
        px.color = color
        for (f in bmp.indices) {
            val fila = bmp[f]
            for (c in fila.indices) {
                if (fila[c] != 'X') continue
                val l = x + c * size
                val t = y + f * size
                canvas.drawRect(l, t, l + size, t + size, px)
            }
        }
    }

    private fun fitText(str: String, maxW: Float, size: Float) {
        text.textSize = size
        val tw = text.measureText(str)
        if (tw > maxW) text.textSize = size * maxW / tw
    }

    /** En silencio el controlador lleva el nivel a 0; el mockup sigue mostrando el volumen en gris. */
    private fun level(s: OverlayState) = if (s.muted) clamp01(s.index / 100f) else clamp01(s.level)

    private fun clamp01(x: Float) = if (x < 0f) 0f else if (x > 1f) 1f else x

    private fun easeOut(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }

    private fun easeBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }
}
