package local.volumenmaestro.overlay.themes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Neón cyberpunk: placa violeta oscura con líneas de CRT, marco de neón degradado,
 * 32 segmentos con un destello que los recorre y vúmetro estéreo en el lateral.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object NeonTheme : Theme {
    override val id = "neon"
    override val name = "Neón cyberpunk"
    override val description = "Violeta, magenta y cian brillando como un cartel de neón."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.40f
    private const val ARC_DEG = 42.97f // 0,75 rad

    private const val VIOLET = 0xFFA855F7.toInt()
    private const val PINK = 0xFFFF2BD6.toInt()
    private const val CYAN = 0xFF22D3EE.toInt()
    private const val MUTE_PINK = 0xFFFF4D8D.toInt()
    private const val LILAC = 0xFFC4B5FD.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val TEXT = 0xFFFBE8FF.toInt()

    private val NUM = Array(101) { it.toString() }
    private val segBottom = IntArray(32) { escala(it / 31f) }
    private val segSide = IntArray(20) { escala(it / 19f) }

    private val rect = RectF()
    private val oval = RectF()
    private val path = Path()
    private val sideClip = Path().apply {
        addRoundRect(RectF(0f, 0f, SW, SH), 16f, 16f, Path.Direction.CW)
    }

    private val base = Paint().apply { color = 0xFF0A0416.toInt() }
    private val bgBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, BH, 0xFF170A33.toInt(), 0xFF07020F.toInt(), Shader.TileMode.CLAMP)
    }
    private val bgSide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, SH, 0xFF170A33.toInt(), 0xFF06020D.toInt(), Shader.TileMode.CLAMP)
    }
    private val crt = Paint().apply { color = Color.argb(8, 255, 255, 255) }
    private val borderBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        shader = LinearGradient(
            0f, 0f, BW, 0f, intArrayOf(VIOLET, PINK, CYAN), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val borderSide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        shader = LinearGradient(
            0f, 0f, 0f, SH, intArrayOf(CYAN, PINK, VIOLET), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val glowStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spkFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textSize = 18f
        letterSpacing = 0.28f
    }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        textAlign = Paint.Align.RIGHT
        textSize = 68f
    }
    private val sideText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val msgHead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.22f
    }
    private val msgBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
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
        val r = lerp(8f, 26f, p)
        canvas.clipRect(rx, ry, rx + rw, ry + rh)
        rr(canvas, rx, ry, rw, rh, r, bgBottom)
        crtLines(canvas, rx, ry, rw, rh)
        neonFrame(canvas, rx + 3f, ry + 3f, rw - 6f, rh - 6f, max(4f, r - 3f), borderBottom, 1f)

        if (ap > 0f) {
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, ap) else drawVolume(canvas, s, ap)
        }
        canvas.restore()
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float) {
        val v = clamp01(s.level)
        val mute = s.muted
        val m = muteAmount(s)
        val t = s.timeSec

        // Parlante con resplandor: dos pasadas anchas y tenues debajo de la nítida.
        val ci = if (mute) MUTE_PINK else CYAN
        speaker(canvas, 72f, 75f, 62f, ci, ap * 0.14f, 17f, 12f, mute, v)
        speaker(canvas, 72f, 75f, 62f, ci, ap * 0.28f, 10f, 5f, mute, v)
        speaker(canvas, 72f, 75f, 62f, ci, ap, 5f, 0f, mute, v)

        val blink = mute && hash(floor(t * 30f).toInt()) < 0.08f
        val lc = if (!mute) LILAC else if (blink) 0xFF5A1030.toInt() else MUTE_PINK
        label.tint(lc, ap)
        canvas.drawText(if (mute) "SILENCIO" else "VOLUMEN", 142f, 44f, label)

        // 32 segmentos que se encienden en secuencia al entrar, con un destello que los recorre.
        val n = 32
        val x0 = 142f
        val x1 = 732f
        val sw = (x1 - x0) / n
        val sy = 56f
        val sh = 44f
        val lit = (v * n * clamp01(ap * 1.25f - 0.1f)).roundToInt()
        val sweep = x0 - 140f + ((t * 0.5f) % 1f) * (x1 - x0 + 280f)
        val litAlpha = ap * (1f - 0.72f * m)
        for (i in 0 until n) {
            val x = x0 + i * sw + 2f
            val segW = sw - 5f
            if (i < lit) {
                val c = segBottom[i]
                fill.tint(c, litAlpha * 0.3f)
                rr(canvas, x - 3f, sy - 3f, segW + 6f, sh + 6f, 7f, fill)
                fill.tint(c, litAlpha)
                rr(canvas, x, sy, segW, sh, 4f, fill)
                val d = 1f - abs(x + segW / 2f - sweep) / 70f
                if (d > 0f && m < 0.5f) {
                    fill.tint(WHITE, ap * d * 0.8f)
                    rr(canvas, x, sy, segW, sh, 4f, fill)
                }
            } else {
                fill.tint(0xFF24123F.toInt(), ap)
                rr(canvas, x, sy, segW, sh, 4f, fill)
            }
        }

        // Silencio: una raya rosa que tacha la barra.
        if (m > 0.01f) {
            val ex = x0 + (x1 - x0) * m
            glowStroke.strokeWidth = 12f
            glowStroke.tint(MUTE_PINK, ap * m * 0.25f)
            canvas.drawLine(x0, sy + sh / 2f, ex, sy + sh / 2f, glowStroke)
            glowStroke.strokeWidth = 4f
            glowStroke.tint(MUTE_PINK, ap * m)
            canvas.drawLine(x0, sy + sh / 2f, ex, sy + sh / 2f, glowStroke)
        }

        number.tint(if (mute) 0xFF8A6A9A.toInt() else TEXT, ap)
        number.setShadowLayer(14f, 0f, 0f, withAlpha(PINK, ap))
        canvas.drawText(num(s.index), 928f, 103f, number)
    }

    /** "Volumen Maestro: ENCENDIDO" → rótulo chico arriba y valor grande con resplandor. */
    private fun drawMessage(canvas: Canvas, msg: String, ap: Float) {
        val sep = msg.indexOf(": ")
        val bodyStart = if (sep > 0) sep + 2 else 0
        var bodyY = 92f
        if (sep > 0) {
            msgHead.tint(LILAC, ap)
            fitText(msgHead, msg, 0, sep, 18f, 800f)
            canvas.drawText(msg, 0, sep, BW / 2f, 50f, msgHead)
            bodyY = 110f
        }
        msgBody.tint(TEXT, ap)
        msgBody.setShadowLayer(14f, 0f, 0f, withAlpha(PINK, ap))
        fitText(msgBody, msg, bodyStart, msg.length, 44f, 820f)
        canvas.drawText(msg, bodyStart, msg.length, BW / 2f, bodyY, msgBody)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / SW, h / SH)
        canvas.clipPath(sideClip)
        canvas.drawRect(0f, 0f, SW, SH, base)
        canvas.drawRect(0f, 0f, SW, SH, bgSide)
        crtLines(canvas, 0f, 0f, SW, SH)
        neonFrame(canvas, 3f, 3f, SW - 6f, SH - 6f, 13f, borderSide, 0.9f)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        if (ap <= 0f) {
            canvas.restore()
            return
        }
        val v = clamp01(s.level)
        val m = muteAmount(s)
        val t = s.timeSec

        sideText.textSize = 24f
        sideText.tint(if (s.muted) MUTE_PINK else TEXT, ap)
        sideText.setShadowLayer(8f, 0f, 0f, withAlpha(PINK, ap))
        canvas.drawText(num(s.index), SW / 2f, 38f, sideText)
        sideText.clearShadowLayer()

        // Vúmetro estéreo: cada canal "respira" con ruido y deja marcado su pico.
        val obj = v * (1f - m) * ap
        meterColumn(canvas, 20f, meter(t, obj, 5.3f, 0f), peak(t, obj, 5.3f, 0f), "L", ap)
        meterColumn(canvas, 60f, meter(t, obj, 4.7f, 10f), peak(t, obj, 4.7f, 10f), "R", ap)

        if (m > 0.01f) {
            canvas.save()
            canvas.translate(SW / 2f, SH / 2f)
            canvas.rotate(-90f)
            fill.tint(Color.argb(230, 10, 4, 22), ap * m)
            canvas.drawRect(-110f, -20f, 110f, 20f, fill)
            sideText.typeface = number.typeface
            sideText.textSize = 24f
            sideText.tint(MUTE_PINK, ap * m)
            sideText.setShadowLayer(8f, 0f, 0f, withAlpha(MUTE_PINK, ap * m))
            canvas.drawText("SILENCIO", 0f, 9f, sideText)
            sideText.clearShadowLayer()
            sideText.typeface = label.typeface
            canvas.restore()
        }
        canvas.restore()
    }

    private fun meterColumn(canvas: Canvas, x: Float, niv: Float, pico: Float, et: String, ap: Float) {
        val n = 20
        val bot = SH - 40f
        val sh = (bot - 54f) / n
        for (i in 0 until n) {
            val y = bot - (i + 1) * sh + 2f
            if ((i + 0.5f) / n <= niv) {
                val c = segSide[i]
                fill.tint(c, ap * 0.3f)
                rr(canvas, x - 2f, y - 2f, 32f, sh - 1f, 5f, fill)
                fill.tint(c, ap)
            } else {
                fill.tint(0xFF22113D.toInt(), ap)
            }
            rr(canvas, x, y, 28f, sh - 5f, 3f, fill)
        }
        if (pico > 0.02f) {
            val pk = min(n - 1, floor(pico * n).toInt())
            val py = bot - (pk + 1) * sh + 2f
            fill.tint(WHITE, ap)
            canvas.drawRect(x, py, x + 28f, py + 3f, fill)
        }
        sideText.textSize = 15f
        sideText.tint(LILAC, ap)
        canvas.drawText(et, x + 14f, bot + 26f, sideText)
    }

    private fun meter(t: Float, obj: Float, freq: Float, off: Float) =
        obj * (0.82f + 0.18f * (0.5f + 0.5f * ruido(t * freq + off)))

    /** Pico sin estado: el máximo de los últimos 0,6 s con la caída lenta del mockup. */
    private fun peak(t: Float, obj: Float, freq: Float, off: Float): Float {
        var best = 0f
        for (k in 0..15) {
            val back = k * 0.04f
            val value = meter(t - back, obj, freq, off) - 0.22f * back
            if (value > best) best = value
        }
        return best
    }

    private fun crtLines(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        var ly = y + 2f
        while (ly < y + h) {
            canvas.drawRect(x, ly, x + w, ly + 2f, crt)
            ly += 6f
        }
    }

    /** Marco de neón: dos trazos anchos y tenues hacen de resplandor, encima el degradado. */
    private fun neonFrame(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float, border: Paint, k: Float) {
        glowStroke.strokeWidth = 13f
        glowStroke.tint(PINK, 0.10f * k)
        rr(canvas, x, y, w, h, r, glowStroke)
        glowStroke.strokeWidth = 7f
        glowStroke.tint(PINK, 0.22f * k)
        rr(canvas, x, y, w, h, r, glowStroke)
        rr(canvas, x, y, w, h, r, border)
    }

    /** Ícono de parlante centrado en (cx,cy), ocupa ±sz/2. bodyStroke > 0 engorda el cuerpo (para el resplandor). */
    private fun speaker(
        c: Canvas, cx: Float, cy: Float, sz: Float, color: Int, alpha: Float,
        grosor: Float, bodyStroke: Float, mute: Boolean, nivel: Float,
    ) {
        spkFill.tint(color, alpha)
        spkFill.style = if (bodyStroke > 0f) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
        spkFill.strokeWidth = bodyStroke
        spkFill.strokeJoin = Paint.Join.ROUND
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
        if (mute) {
            val mx = x + sz * 0.32f
            val d = sz * 0.13f
            c.drawLine(mx - d, cy - d, mx + d, cy + d, spkStroke)
            c.drawLine(mx + d, cy - d, mx - d, cy + d, spkStroke)
        } else {
            val waves = if (nivel <= 0f) 0 else if (nivel < 0.34f) 1 else if (nivel < 0.67f) 2 else 3
            val ax = x + sz * 0.08f
            for (i in 1..waves) {
                val rad = sz * (0.08f + 0.14f * i)
                oval.set(ax - rad, cy - rad, ax + rad, cy + rad)
                c.drawArc(oval, -ARC_DEG, ARC_DEG * 2f, false, spkStroke)
            }
        }
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

    private fun escala(t: Float): Int {
        val i = min(1, floor(t * 2f).toInt())
        return mix(if (i == 0) VIOLET else PINK, if (i == 0) PINK else CYAN, t * 2f - i)
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val k = clamp01(t)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * k).roundToInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * k).roundToInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).roundToInt(),
        )
    }

    private fun num(i: Int) = if (i in 0..100) NUM[i] else i.toString()
    private fun muteAmount(s: OverlayState) = if (s.muted) 1f - exp(-s.sinceChangeSec * 9f) else 0f
    private fun withAlpha(c: Int, a: Float) = (c and 0x00FFFFFF) or ((Color.alpha(c) * clamp01(a)).toInt() shl 24)
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
    private fun ruido(x: Float) = sin(x * 1.7f) * 0.5f + sin(x * 3.13f + 1.3f) * 0.3f + sin(x * 7.7f + 4f) * 0.2f
    private fun hash(n: Int): Float {
        var x = n * 0x9E3779B1.toInt()
        x = x xor (x ushr 15)
        x *= 0x2C1B3C6D
        x = x xor (x ushr 12)
        x *= 0x297A2D39
        x = x xor (x ushr 15)
        return (x ushr 8) / 16777216f
    }
}
