package local.volumenmaestro.overlay.themes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Minimal vidrio: vidrio esmerilado simulado con manchas de color difusas que se mueven,
 * barra finita con perilla que late al cambiar y número grande de trazo fino.
 * En el lateral, una píldora vertical que se llena estilo iOS.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object VidrioTheme : Theme {
    override val id = "vidrio"
    override val name = "Minimal vidrio"
    override val description = "Vidrio esmerilado, líneas finas y cero ruido: elegante y tranqui."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.35f
    private const val ARC_DEG = 42.97f // 0,75 rad

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val INK = 0xFF1B2130.toInt()

    private val NUM = Array(101) { it.toString() }

    // Manchas de color: r, g, b, centro x/y relativo, radio relativo y velocidad (como el mockup).
    private val BLOB_RGB = intArrayOf(Color.rgb(79, 124, 255), Color.rgb(255, 122, 182), Color.rgb(61, 214, 196))
    private val BLOB_X = floatArrayOf(0.2f, 0.75f, 0.5f)
    private val BLOB_Y = floatArrayOf(0.3f, 0.6f, 1.0f)
    private val BLOB_R = floatArrayOf(0.55f, 0.5f, 0.6f)
    private val BLOB_S = floatArrayOf(0.31f, 0.23f, 0.17f)

    private val rect = RectF()
    private val oval = RectF()
    private val path = Path()
    private val clip = Path()
    private val sideClip = Path().apply {
        addRoundRect(RectF(0f, 0f, SW, SH), 16f, 16f, Path.Direction.CW)
    }
    /** Píldora del lateral: x 24..84, y 26..404, radio 24. */
    private val pill = Path().apply {
        addRoundRect(RectF(24f, 26f, 84f, 404f), 24f, 24f, Path.Direction.CW)
    }

    private val base = Paint().apply { color = INK }
    private val glassBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, BH, 0xFF283043.toInt(), 0xFF161B26.toInt(), Shader.TileMode.CLAMP)
    }
    private val glassSide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, SH, 0xFF283043.toInt(), 0xFF161B26.toInt(), Shader.TileMode.CLAMP)
    }
    private val highlightBottom = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, BH * 0.5f, Color.argb(26, 255, 255, 255), 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    private val highlightSide = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, SH * 0.5f, Color.argb(26, 255, 255, 255), 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    /** Una mancha por color, de radio 1 en el origen: se traslada y escala al dibujarla. */
    private val blobs = Array(3) { i ->
        val c = BLOB_RGB[i]
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                0f, 0f, 1f,
                Color.argb(77, Color.red(c), Color.green(c), Color.blue(c)),
                Color.argb(0, Color.red(c), Color.green(c), Color.blue(c)),
                Shader.TileMode.CLAMP,
            )
        }
    }
    private val knobHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f, Color.argb(115, 255, 255, 255), 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(56, 255, 255, 255)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spkFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val light = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = light
        textSize = 22f
    }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        textAlign = Paint.Align.RIGHT
        textSize = 78f
    }
    private val sideNumber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        textSize = 22f
    }
    private val msgText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = light
        textAlign = Paint.Align.CENTER
    }

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / BW, h / BH)
        canvas.clipRect(0f, 0f, BW, BH)
        // Placa de seguridad: tapa el cartel del sistema desde el primer cuadro.
        canvas.drawRect(36f, 11f, 924f, 142f, base)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val rx = lerp(36f, 0f, p)
        val ry = lerp(11f, 0f, p)
        val rw = lerp(888f, BW, p)
        val rh = lerp(131f, BH, p)
        val r = lerp(8f, 40f, p)
        canvas.clipRect(rx, ry, rx + rw, ry + rh)
        glass(canvas, rx, ry, rw, rh, r, s.timeSec, glassBottom, highlightBottom)

        if (ap > 0f) {
            canvas.save()
            canvas.translate(0f, (1f - ap) * 14f)
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, ap) else drawVolume(canvas, s, ap)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float) {
        val v = clamp01(s.level)
        val mute = s.muted

        speaker(canvas, 72f, 75f, 50f, if (mute) Color.argb(140, 255, 255, 255) else Color.argb(235, 255, 255, 255), ap, 3.5f, mute, v)
        label.tint(Color.argb(158, 255, 255, 255), ap)
        canvas.drawText(if (mute) "Silencio" else "Volumen", 144f, 50f, label)

        val x0 = 144f
        val x1 = 745f
        val y = 78f
        fill.tint(Color.argb(36, 255, 255, 255), ap)
        rr(canvas, x0, y - 4f, x1 - x0, 8f, 4f, fill)
        val fw = (x1 - x0) * v
        if (fw > 1f) {
            fill.tint(if (mute) Color.argb(89, 255, 255, 255) else WHITE, ap)
            rr(canvas, x0, y - 4f, fw, 8f, 4f, fill)
        }

        // Perilla que "late" justo después de cada cambio.
        val kb = 1f + 0.3f * exp(-s.sinceChangeSec * 7f)
        val kx = x0 + fw
        if (!mute) {
            val hr = 11f * kb + 14f
            canvas.save()
            canvas.translate(kx, y)
            canvas.scale(hr, hr)
            knobHalo.alpha = (255 * ap).toInt()
            canvas.drawRect(-1f, -1f, 1f, 1f, knobHalo)
            canvas.restore()
        }
        fill.tint(if (mute) Color.argb(128, 255, 255, 255) else WHITE, ap)
        canvas.drawCircle(kx, y, 11f * kb, fill)

        number.tint(if (mute) Color.argb(89, 255, 255, 255) else WHITE, ap)
        canvas.drawText(num(s.index), 925f, 108f, number)
    }

    /** "Volumen Maestro: ENCENDIDO" → rótulo tenue arriba y el valor en trazo fino abajo. */
    private fun drawMessage(canvas: Canvas, msg: String, ap: Float) {
        val sep = msg.indexOf(": ")
        val bodyStart = if (sep > 0) sep + 2 else 0
        var bodyY = 90f
        if (sep > 0) {
            msgText.tint(Color.argb(158, 255, 255, 255), ap)
            fitText(msgText, msg, 0, sep, 22f, 760f)
            canvas.drawText(msg, 0, sep, BW / 2f, 54f, msgText)
            bodyY = 106f
        }
        msgText.tint(WHITE, ap)
        fitText(msgText, msg, bodyStart, msg.length, 44f, 800f)
        canvas.drawText(msg, bodyStart, msg.length, BW / 2f, bodyY, msgText)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / SW, h / SH)
        canvas.clipPath(sideClip)
        canvas.drawRect(0f, 0f, SW, SH, base)
        glass(canvas, 0f, 0f, SW, SH, 16f, s.timeSec + 3f, glassSide, highlightSide)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val v = clamp01(s.level)
        val mute = s.muted
        val tx = 24f
        val ty = 26f
        val tw = 60f
        val th = 378f
        fill.color = Color.argb(26, 255, 255, 255)
        canvas.drawPath(pill, fill)

        // El contenido se dibuja dos veces: blanco sobre el vidrio y oscuro sobre el relleno.
        val fh = th * v * ap
        val fy = ty + th - fh
        val a = max(0.3f, ap)
        canvas.save()
        canvas.clipRect(tx, ty, tx + tw, fy)
        sideContent(canvas, s, Color.argb(204, 255, 255, 255), a, v, mute)
        canvas.restore()
        if (fh > 0.5f) {
            canvas.save()
            canvas.clipPath(pill)
            canvas.clipRect(tx, fy, tx + tw, fy + fh)
            fill.tint(if (mute) Color.argb(97, 255, 255, 255) else Color.argb(240, 255, 255, 255), a)
            canvas.drawRect(tx, fy, tx + tw, fy + fh, fill)
            sideContent(canvas, s, INK, a, v, mute)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun sideContent(canvas: Canvas, s: OverlayState, color: Int, a: Float, v: Float, mute: Boolean) {
        speaker(canvas, 54f, 376f, 30f, color, a, 2.6f, mute, v)
        sideNumber.tint(color, a)
        canvas.drawText(num(s.index), 54f, 64f, sideNumber)
    }

    /** Vidrio esmerilado: degradado, tres manchas difusas que flotan, brillo arriba y filo claro. */
    private fun glass(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float, t: Float,
        bg: Paint, highlight: Paint,
    ) {
        rr(canvas, x, y, w, h, r, bg)
        canvas.save()
        clip.reset()
        rect.set(x, y, x + w, y + h)
        clip.addRoundRect(rect, r, r, Path.Direction.CW)
        canvas.clipPath(clip)
        val big = max(w, h)
        for (i in 0 until 3) {
            val cx = x + w * (BLOB_X[i] + 0.08f * sin(t * BLOB_S[i] * 2f + i))
            val cy = y + h * (BLOB_Y[i] + 0.15f * cos(t * BLOB_S[i] * 2.3f + i))
            val rad = big * BLOB_R[i] * 0.6f
            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(rad, rad)
            canvas.drawRect(-1f, -1f, 1f, 1f, blobs[i])
            canvas.restore()
        }
        canvas.drawRect(x, y, x + w, y + h * 0.5f, highlight)
        canvas.restore()
        rr(canvas, x + 0.75f, y + 0.75f, w - 1.5f, h - 1.5f, r, edge)
    }

    /** Parlante tachado en silencio (las ondas siguen el nivel). */
    private fun speaker(c: Canvas, cx: Float, cy: Float, sz: Float, color: Int, alpha: Float, grosor: Float, mute: Boolean, nivel: Float) {
        spkFill.tint(color, alpha)
        spkStroke.tint(color, alpha)
        spkStroke.strokeWidth = grosor
        val x = cx - sz * 0.12f
        path.reset()
        path.moveTo(x - sz * 0.38f, cy - sz * 0.16f)
        path.lineTo(x - sz * 0.18f, cy - sz * 0.16f)
        path.lineTo(x + sz * 0.06f, cy - sz * 0.4f)
        path.lineTo(x + sz * 0.06f, cy + sz * 0.4f)
        path.lineTo(x - sz * 0.18f, cy + sz * 0.16f)
        path.lineTo(x - sz * 0.38f, cy + sz * 0.16f)
        path.close()
        c.drawPath(path, spkFill)
        val waves = if (nivel <= 0f) 0 else if (nivel < 0.34f) 1 else if (nivel < 0.67f) 2 else 3
        val ax = x + sz * 0.08f
        for (i in 1..waves) {
            val rad = sz * (0.08f + 0.14f * i)
            oval.set(ax - rad, cy - rad, ax + rad, cy + rad)
            c.drawArc(oval, -ARC_DEG, ARC_DEG * 2f, false, spkStroke)
        }
        if (mute) c.drawLine(cx - sz * 0.45f, cy - sz * 0.42f, cx + sz * 0.45f, cy + sz * 0.42f, spkStroke)
    }

    private fun fitText(p: Paint, text: String, start: Int, end: Int, size: Float, maxW: Float) {
        p.textSize = size
        val tw = p.measureText(text, start, end)
        if (tw > maxW) p.textSize = size * maxW / tw
    }

    private fun rr(c: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) {
        val rad = max(0f, min(r, min(w, h) / 2f))
        rect.set(x, y, x + w, y + h)
        c.drawRoundRect(rect, rad, rad, p)
    }

    private fun num(i: Int) = if (i in 0..100) NUM[i] else i.toString()
    private fun Paint.tint(c: Int, a: Float) {
        color = c
        alpha = (Color.alpha(c) * clamp01(a)).toInt()
    }
    private fun clamp01(x: Float) = if (x < 0f) 0f else if (x > 1f) 1f else x
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeOut(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
}
