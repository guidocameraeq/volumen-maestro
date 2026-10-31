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
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Ondas líquidas (mockup 7): tres ondas senoidales superpuestas cuya amplitud depende del volumen;
 * la parte encendida llega hasta el nivel y en la punta hay una gota con ondas expansivas.
 * En el lateral, tres cuerdas que vibran. Coordenadas locales: inferior 960x150 y lateral 108x430.
 */
object OndasTheme : Theme {
    override val id = "ondas"
    override val name = "Ondas líquidas"
    override val description = "Tres ondas de colores que se agitan con el volumen y una gota que marca hasta dónde llegás."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.40f
    private const val SIDE_R = 16f
    private const val TAU = 6.2831855f

    private val BASE = 0xFF0A0F24.toInt()
    private val VIOLETA = 0xFFA78BFA.toInt()

    // Ondas del panel inferior: de x 180 a 820, centradas en y 75.
    private const val X0 = 180f
    private const val X1 = 820f
    private const val WD = X1 - X0
    private const val CY = 75f
    private const val PASO = 6f
    private val NPTS = (WD / PASO).toInt() + 1
    // Envolvente fija: las ondas nacen y mueren en los extremos.
    private val ENV = FloatArray(NPTS) { j -> envelope(j * PASO / WD) }

    private val CAPA_A = floatArrayOf(1f, 0.65f, 0.45f)
    private val CAPA_FR = floatArrayOf(2.3f, 3.4f, 1.6f)
    private val CAPA_VEL = floatArrayOf(2.2f, -1.6f, 3.1f)
    private val CAPA_LW = floatArrayOf(5f, 3f, 2.5f)
    private val CAPA_AL = floatArrayOf(1f, 0.55f, 0.4f)
    private val ondas = arrayOf(Path(), Path(), Path())

    // Cuerdas del lateral: de y 60 a 410.
    private const val Y0 = 60f
    private const val Y1 = 410f
    private val CUERDAS_X = floatArrayOf(26f, 54f, 82f)
    private val cuerda = Path()

    private val rect = RectF()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, BW, 0f,
            intArrayOf(0xFF0B1030.toInt(), 0xFF111743.toInt(), 0xFF0B1030.toInt()), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    // Brillo superior: gradiente que se apaga a media altura (equivale al recorte del mockup).
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, BH * 0.5f, Color.argb(18, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
    }
    private val borderBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        shader = LinearGradient(0f, 0f, BW, 0f, Color.argb(140, 34, 211, 238), Color.argb(140, 244, 114, 182), Shader.TileMode.CLAMP)
    }
    private val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val waveShader = LinearGradient(
        X0, 0f, X1, 0f,
        intArrayOf(0xFF22D3EE.toInt(), VIOLETA, 0xFFF472B6.toInt()), floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    private val wave = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        shader = waveShader
    }
    // Resplandor: el mismo trazo más ancho, violeta y casi transparente.
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = VIOLETA
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val dropGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 24f,
            intArrayOf(Color.argb(150, 167, 139, 250), Color.argb(150, 167, 139, 250), Color.argb(0, 167, 139, 250)),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val drop = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(191, 255, 255, 255)
    }

    // Parlante en (892, 75), tamaño 48
    private const val SPK_S = 48f
    private const val SPK_X = 892f - SPK_S * 0.12f
    private val speakerBody = Path().apply {
        moveTo(SPK_X - SPK_S * 0.38f, CY - SPK_S * 0.16f)
        lineTo(SPK_X - SPK_S * 0.18f, CY - SPK_S * 0.16f)
        lineTo(SPK_X + SPK_S * 0.06f, CY - SPK_S * 0.4f)
        lineTo(SPK_X + SPK_S * 0.06f, CY + SPK_S * 0.4f)
        lineTo(SPK_X - SPK_S * 0.18f, CY + SPK_S * 0.16f)
        lineTo(SPK_X - SPK_S * 0.38f, CY + SPK_S * 0.16f)
        close()
    }
    private val speakerArcs = arrayOf(RectF(), RectF(), RectF())
    private val speakerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(217, 255, 255, 255) }
    private val speakerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(217, 255, 255, 255)
    }

    init {
        for (i in 0 until 3) {
            val rad = SPK_S * (0.08f + 0.14f * (i + 1))
            val cx = SPK_X + SPK_S * 0.08f
            speakerArcs[i].set(cx - rad, CY - rad, cx + rad, CY + rad)
        }
    }

    // Textos (Quicksand → sans del sistema)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textSize = 20f
        color = Color.argb(140, 255, 255, 255)
        textAlign = Paint.Align.LEFT
    }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 62f
        textAlign = Paint.Align.LEFT
    }
    private val sideNumber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val msgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
        setShadowLayer(10f, 0f, 0f, BASE)
    }
    private val NUM = Array(101) { it.toString() }

    // Lateral
    private val sideBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, SH, 0xFF111743.toInt(), BASE, Shader.TileMode.CLAMP)
    }
    private val sideBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        shader = LinearGradient(0f, 0f, 0f, SH, Color.argb(140, 244, 114, 182), Color.argb(140, 34, 211, 238), Shader.TileMode.CLAMP)
    }
    private val stringShader = LinearGradient(
        0f, Y1, 0f, Y0,
        intArrayOf(0xFF22D3EE.toInt(), VIOLETA, 0xFFF472B6.toInt()), floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    private val stringFaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(36, 255, 255, 255)
    }
    private val stringOn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val sc = canvas.save()
        if (w != BW.toInt() || h != BH.toInt()) canvas.scale(w / BW, h / BH)
        val t = s.timeSec
        val vis = min(s.appear, clamp01(t / ENTRADA))
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))

        // Placa de seguridad opaca (siempre).
        fill.color = BASE
        canvas.drawRect(36f, 11f, 924f, 142f, fill)

        val rx = 36f * (1f - p)
        val ry = 11f * (1f - p)
        val rr = rx + 888f + 72f * p
        val rb = ry + 131f + 19f * p
        val r = lerp(8f, 34f, p)
        canvas.clipRect(rx, ry, rr, rb)
        rect.set(rx, ry, rr, rb)
        canvas.drawRoundRect(rect, r, r, bgBottom)
        canvas.drawRoundRect(rect, r, r, highlight)
        rect.set(rx + 1f, ry + 1f, rr - 1f, rb - 1f)
        canvas.drawRoundRect(rect, r, r, borderBottom)

        if (ap > 0f) {
            val lay = layer(canvas, ap, rx, ry, rr, rb)
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, t, ap) else drawVolume(canvas, s, ap, ry, rb)
            canvas.restoreToCount(lay)
        }
        canvas.restoreToCount(sc)
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float, top: Float, bottom: Float) {
        val t = s.timeSec
        val mute = s.muted
        val v = level(s)
        val m = muteAmount(s)

        canvas.drawText(if (mute) "silencio" else "volumen", 40f, 44f, label)
        number.color = if (mute) Color.argb(102, 255, 255, 255) else 0xFFFFFFFF.toInt()
        canvas.drawText(NUM[s.index.coerceIn(0, 100)], 40f, 108f, number)

        val lx = X0 + WD * v
        val amp = (4f + v * 36f) * (1f - m) * ap
        for (i in 0 until 3) buildWave(i, amp, t)
        for (i in 0 until 3) {
            faint.color = Color.argb(31, 255, 255, 255)
            faint.strokeWidth = CAPA_LW[i]
            canvas.drawPath(ondas[i], faint)
        }

        // Parte encendida: hasta el nivel.
        val c = canvas.save()
        canvas.clipRect(X0 - 10f, top, lx, bottom)
        for (i in 2 downTo 0) {
            val a = CAPA_AL[i] * (1f - 0.7f * m)
            if (i == 0) {
                glow.alpha = (a * 30f).toInt()
                glow.strokeWidth = CAPA_LW[i] + 10f
                canvas.drawPath(ondas[i], glow)
                glow.alpha = (a * 55f).toInt()
                glow.strokeWidth = CAPA_LW[i] + 5f
                canvas.drawPath(ondas[i], glow)
            }
            wave.alpha = (a * 255f).toInt().coerceIn(0, 255)
            wave.strokeWidth = CAPA_LW[i]
            canvas.drawPath(ondas[i], wave)
        }
        canvas.restoreToCount(c)

        // Gota en la punta con onda expansiva.
        val dy = yOnda(0, lx, amp, t)
        val ciclo = (t * 1.2f) % 1f
        ring.color = Color.argb((0.6f * (1f - ciclo) * (1f - m) * 255f).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawCircle(lx, dy, 8f + ciclo * 24f, ring)
        val c2 = canvas.save()
        canvas.translate(lx, dy)
        canvas.drawCircle(0f, 0f, 24f, dropGlow)
        drop.color = if (mute) 0xFF94A3B8.toInt() else 0xFFFFFFFF.toInt()
        canvas.drawCircle(0f, 0f, 8f, drop)
        canvas.restoreToCount(c2)

        drawSpeaker(canvas, mute, v)
    }

    private fun drawMessage(canvas: Canvas, msg: String, t: Float, ap: Float) {
        // Las ondas quedan de fondo, tranquilas, y el mensaje va encima.
        val amp = 12f * ap
        for (i in 0 until 3) buildWave(i, amp, t)
        for (i in 2 downTo 0) {
            faint.color = Color.argb(31, 255, 255, 255)
            faint.strokeWidth = CAPA_LW[i]
            canvas.drawPath(ondas[i], faint)
            wave.alpha = (CAPA_AL[i] * 90f).toInt()
            wave.strokeWidth = CAPA_LW[i]
            canvas.drawPath(ondas[i], wave)
        }
        fitText(msgPaint, msg, 820f, 42f)
        canvas.drawText(msg, BW / 2f, 90f, msgPaint)
    }

    private fun drawSpeaker(canvas: Canvas, mute: Boolean, nivel: Float) {
        canvas.drawPath(speakerBody, speakerFill)
        if (mute) {
            val mx = SPK_X + SPK_S * 0.32f
            val d = SPK_S * 0.13f
            canvas.drawLine(mx - d, CY - d, mx + d, CY + d, speakerStroke)
            canvas.drawLine(mx + d, CY - d, mx - d, CY + d, speakerStroke)
        } else {
            val n = if (nivel <= 0f) 0 else if (nivel < 0.34f) 1 else if (nivel < 0.67f) 2 else 3
            // ±0,75 rad = ±42,97°
            for (i in 0 until n) canvas.drawArc(speakerArcs[i], -42.97f, 85.94f, false, speakerStroke)
        }
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val sc = canvas.save()
        if (w != SW.toInt() || h != SH.toInt()) canvas.scale(w / SW, h / SH)
        val t = s.timeSec
        val vis = min(s.appear, clamp01(t / ENTRADA))
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val mute = s.muted
        val v = level(s)
        val m = muteAmount(s)

        rect.set(0f, 0f, SW, SH)
        canvas.drawRoundRect(rect, SIDE_R, SIDE_R, sideBg)
        rect.set(1f, 1f, SW - 1f, SH - 1f)
        canvas.drawRoundRect(rect, SIDE_R - 1f, SIDE_R - 1f, sideBorder)

        val lay = layer(canvas, max(0.25f, ap), 0f, 0f, SW, SH)
        sideNumber.color = if (mute) Color.argb(102, 255, 255, 255) else 0xFFFFFFFF.toInt()
        canvas.drawText(NUM[s.index.coerceIn(0, 100)], SW / 2f, 42f, sideNumber)

        val ly = Y1 - (Y1 - Y0) * v * ap
        val amp = (1.5f + v * 13f) * (1f - m) * ap
        if (mute) {
            stringOn.shader = null
            stringOn.color = 0xFF64748B.toInt()
        } else {
            stringOn.shader = stringShader
            stringOn.color = 0xFFFFFFFF.toInt()
        }
        for (k in CUERDAS_X.indices) {
            val bx = CUERDAS_X[k]
            cuerda.reset()
            var y = Y0
            var first = true
            while (y <= Y1) {
                val u = (y - Y0) / (Y1 - Y0)
                val dx = sin(u * PI.toFloat() * (3 + k) - t * (4f + k * 1.3f)) * amp * sin(PI.toFloat() * u)
                if (first) cuerda.moveTo(bx + dx, y) else cuerda.lineTo(bx + dx, y)
                first = false
                y += PASO
            }
            canvas.drawPath(cuerda, stringFaint)
            val c = canvas.save()
            canvas.clipRect(0f, ly, SW, Y1 + 6f)
            glow.alpha = 45
            glow.strokeWidth = 9f
            canvas.drawPath(cuerda, glow)
            canvas.drawPath(cuerda, stringOn)
            canvas.restoreToCount(c)
        }

        canvas.drawLine(14f, ly, 94f, ly, line)
        val ciclo = (t * 1.1f) % 1f
        ring.color = Color.argb((0.5f * (1f - ciclo) * (1f - m) * 255f).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawCircle(SW / 2f, ly, 6f + ciclo * 26f, ring)
        val c2 = canvas.save()
        canvas.translate(SW / 2f, ly)
        canvas.drawCircle(0f, 0f, 20f, dropGlow)
        drop.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(0f, 0f, 6f, drop)
        canvas.restoreToCount(c2)

        canvas.restoreToCount(lay)
        canvas.restoreToCount(sc)
    }

    private fun buildWave(i: Int, amp: Float, t: Float) {
        val p = ondas[i]
        p.reset()
        val k = amp * CAPA_A[i]
        for (j in 0 until NPTS) {
            val u = j * PASO / WD
            val y = CY + sin(u * TAU * CAPA_FR[i] + t * CAPA_VEL[i]) * k * ENV[j]
            val x = X0 + j * PASO
            if (j == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
    }

    private fun yOnda(i: Int, x: Float, amp: Float, t: Float): Float {
        val u = (x - X0) / WD
        return CY + sin(u * TAU * CAPA_FR[i] + t * CAPA_VEL[i]) * amp * CAPA_A[i] * envelope(u)
    }

    private fun envelope(u: Float): Float = max(0f, sin(PI.toFloat() * u)).pow(0.7f)

    /** Silencio suavizado (0..1) a partir del tiempo desde el último cambio. */
    private fun muteAmount(s: OverlayState) = if (s.muted) 1f - exp(-s.sinceChangeSec * 9f) else 0f

    /** En silencio el controlador lleva el nivel a 0; el mockup sigue mostrando el volumen. */
    private fun level(s: OverlayState) = if (s.muted) clamp01(s.index / 100f) else clamp01(s.level)

    private fun layer(canvas: Canvas, alpha: Float, l: Float, t: Float, r: Float, b: Float): Int =
        if (alpha >= 0.995f) canvas.save()
        else canvas.saveLayerAlpha(l, t, r, b, (alpha * 255f).toInt().coerceIn(0, 255))

    private fun fitText(paint: Paint, str: String, maxW: Float, size: Float) {
        paint.textSize = size
        val tw = paint.measureText(str)
        if (tw > maxW) paint.textSize = size * maxW / tw
    }

    private fun clamp01(x: Float) = if (x < 0f) 0f else if (x > 1f) 1f else x
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun easeOut(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
}
