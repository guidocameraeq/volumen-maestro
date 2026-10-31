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
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Espacial (mockup 6): nebulosa con estrellas que titilan y entran en "warp", planeta con anillos
 * y un cohete que recorre la trayectoria hasta el nivel. En el lateral el cohete despega de la Tierra.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object EspacialTheme : Theme {
    override val id = "espacial"
    override val name = "Espacial"
    override val description = "Nebulosa con estrellas en modo warp, un planeta con anillos y un cohete que viaja hasta tu nivel."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.45f
    private const val SIDE_R = 16f
    private const val N_CHISPAS = 32
    private const val VIDA_CHISPA = 0.6f

    private val BASE = 0xFF070B22.toInt()
    private val BLANCO = 0xFFFFFFFF.toInt()

    // Trayectoria del panel inferior
    private const val X0 = 150f
    private const val X1 = 735f
    private const val TY = 85f

    // Estrellas precalculadas con semilla fija (iguales en la TV y en la vista previa).
    private val estX = FloatArray(110)
    private val estY = FloatArray(110)
    private val estR = FloatArray(110)
    private val estF = FloatArray(110)
    private val estV = FloatArray(110)
    private val latX = FloatArray(45)
    private val latY = FloatArray(45)
    private val latR = FloatArray(45)
    private val latZ = FloatArray(45)
    private val chispaSel = FloatArray(N_CHISPAS) { (it * 0.618034f) % 1f }

    init {
        val rnd = Random(42)
        for (i in estX.indices) {
            estX[i] = rnd.nextFloat()
            estY[i] = rnd.nextFloat()
            estR[i] = 0.8f + rnd.nextFloat() * 1.8f
            estF[i] = rnd.nextFloat() * 6.28f
            estV[i] = 0.3f + rnd.nextFloat()
        }
        for (i in latX.indices) {
            latX[i] = rnd.nextFloat()
            latY[i] = rnd.nextFloat()
            latR[i] = 0.8f + rnd.nextFloat() * 1.6f
            latZ[i] = 0.3f + rnd.nextFloat() * 0.7f
        }
    }

    private val rect = RectF()
    private val rect2 = RectF()
    private val path = Path()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, BW, 0f,
            intArrayOf(0xFF0A0D2A.toInt(), 0xFF120A33.toInt(), 0xFF070B22.toInt()), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val nebula1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(288f, 30f, 260f, Color.argb(71, 147, 51, 234), Color.argb(0, 147, 51, 234), Shader.TileMode.CLAMP)
    }
    private val nebula2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(768f, 150f, 240f, Color.argb(51, 56, 189, 248), Color.argb(0, 56, 189, 248), Shader.TileMode.CLAMP)
    }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(97, 165, 180, 252)
    }
    private val starLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val starDot = Paint().apply { isAntiAlias = false }

    // Planeta con anillos
    private const val PX = 68f
    private const val PY = 75f
    private const val PR = 34f
    private val ringRect = RectF(PX - PR * 1.7f, PY - PR * 0.42f, PX + PR * 1.7f, PY + PR * 0.42f)
    private val ringBack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(191, 252, 211, 77)
    }
    private val ringFront = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(230, 252, 211, 77)
    }
    private val planetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            PX - PR * 0.4f, PY - PR * 0.4f, PR * 1.4f,
            intArrayOf(0xFFFDBA74.toInt(), 0xFFEA580C.toInt(), 0xFF7C2D12.toInt()), floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    // Trayectoria, estela y chispas
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(56, 255, 255, 255)
    }
    private val dotOn = 0xFFC4B5FD.toInt()
    private val dotOff = Color.argb(64, 255, 255, 255)
    // Gradientes de 0 a 1 en x: se estiran con canvas.scale hasta la punta del cohete.
    private val trailShader = LinearGradient(0f, 0f, 1f, 0f, Color.argb(0, 125, 211, 252), 0xFF7DD3FC.toInt(), Shader.TileMode.CLAMP)
    private val trailMuteShader = LinearGradient(0f, 0f, 1f, 0f, Color.argb(0, 125, 211, 252), 0xFF64748B.toInt(), Shader.TileMode.CLAMP)
    private val trail = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trailGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = trailShader }
    private val spark = Paint().apply { isAntiAlias = false; color = 0xFFFDE047.toInt() }

    // Textos
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 17f
        letterSpacing = 3f / 17f
        textAlign = Paint.Align.LEFT
    }
    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, 800, false)
        textSize = 64f
        textAlign = Paint.Align.RIGHT
        setShadowLayer(12f, 0f, 0f, 0xFF818CF8.toInt())
    }
    private val msgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, 700, false)
        textAlign = Paint.Align.CENTER
        color = BLANCO
        letterSpacing = 0.04f
        setShadowLayer(12f, 0f, 0f, 0xFF818CF8.toInt())
    }
    private val NUM = Array(101) { it.toString() }

    // Cohete (coordenadas propias: nariz hacia +x)
    private val flamePath = Path().apply {
        // Normalizada: x 0 = tobera, x -1 = punta; se estira con canvas.scale(L, 1).
        moveTo(0.03f, -7f)
        quadTo(-0.6f, -5f, -1f, 0f)
        quadTo(-0.6f, 5f, 0.03f, 7f)
        close()
    }
    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, -1f, 0f,
            intArrayOf(0xFFFFF7C2.toInt(), 0xFFFDBA3A.toInt(), Color.argb(0, 249, 115, 22)), floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val finPath = Path().apply {
        moveTo(-12f, -8f); lineTo(-26f, -19f); lineTo(-24f, -8f); close()
        moveTo(-12f, 8f); lineTo(-26f, 19f); lineTo(-24f, 8f); close()
    }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEF4444.toInt() }
    private val nozzle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF475569.toInt() }
    private val bodyPath = Path().apply {
        moveTo(-22f, -9f); lineTo(10f, -9f)
        quadTo(30f, -8f, 34f, 0f)
        quadTo(30f, 8f, 10f, 9f)
        lineTo(-22f, 9f); close()
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, -9f, 0f, 9f,
            intArrayOf(0xFFFFFFFF.toInt(), 0xFFCBD5E1.toInt(), 0xFF64748B.toInt()), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val windowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF38BDF8.toInt() }
    private val windowStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFF1E293B.toInt()
    }

    // Lateral
    private val sidePath = Path().apply { addRoundRect(RectF(0f, 0f, SW, SH), SIDE_R, SIDE_R, Path.Direction.CW) }
    private val sideBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, SH, 0xFF070A22.toInt(), 0xFF0C0F35.toInt(), Shader.TileMode.CLAMP)
    }
    private val moon = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCBD5E1.toInt() }
    private val crater = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF94A3B8.toInt() }
    private const val EX = 54f
    private const val EY = 486f
    private const val ER = 96f
    private val earthPath = Path().apply { addCircle(EX, EY, ER, Path.Direction.CW) }
    // Resplandor de la Tierra con un anillo de gradiente (en vez de shadowBlur).
    private val earthGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            EX, EY, ER + 16f,
            intArrayOf(Color.argb(110, 125, 211, 252), Color.argb(110, 125, 211, 252), Color.argb(0, 125, 211, 252)),
            floatArrayOf(0f, ER / (ER + 16f), 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val earthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            EX - 30f, EY - 60f, 150f,
            intArrayOf(0xFF60A5FA.toInt(), 0xFF1D4ED8.toInt(), 0xFF0B1F4D.toInt()), floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val continent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(191, 34, 197, 94) }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(89, 255, 255, 255)
    }
    private val exhaust = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, 1f, Color.argb(128, 253, 224, 71), Color.argb(0, 253, 224, 71), Shader.TileMode.CLAMP)
    }

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val sc = canvas.save()
        if (w != BW.toInt() || h != BH.toInt()) canvas.scale(w / BW, h / BH)
        val t = s.timeSec
        val vis = min(s.appear, clamp01(t / ENTRADA))
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))

        // Placa de seguridad opaca (siempre).
        fill.shader = null
        fill.color = BASE
        canvas.drawRect(36f, 11f, 924f, 142f, fill)

        val rx = 36f * (1f - p)
        val ry = 11f * (1f - p)
        val rr = rx + 888f + 72f * p
        val rb = ry + 131f + 19f * p
        val r = lerp(8f, 28f, p)
        canvas.clipRect(rx, ry, rr, rb)
        rect.set(rx, ry, rr, rb)
        canvas.drawRoundRect(rect, r, r, bgBottom)

        val c1 = canvas.save()
        path.reset()
        path.addRoundRect(rect, r, r, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawRect(rect, nebula1)
        canvas.drawRect(rect, nebula2)
        // Warp: estelas largas al entrar (primeros 0,6 s) y al irse.
        val warp = 80f * max(1f - easeOut(clamp01(t / 0.6f)), 1f - ap)
        val rw = rr - rx
        val rh = rb - ry
        for (i in estX.indices) {
            val x = rx + estX[i] * rw
            val y = ry + estY[i] * rh
            val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 2f * estV[i] + estF[i]))
            if (warp > 2f) {
                starLine.color = white(a)
                starLine.strokeWidth = estR[i]
                canvas.drawLine(x - warp * estV[i], y, x, y, starLine)
            } else {
                starDot.color = white(a)
                canvas.drawRect(x, y, x + estR[i], y + estR[i], starDot)
            }
        }
        canvas.restoreToCount(c1)
        rect2.set(rx + 1f, ry + 1f, rr - 1f, rb - 1f)
        canvas.drawRoundRect(rect2, r, r, border)

        if (ap > 0f) {
            val lay = layer(canvas, ap, rx, ry, rr, rb)
            drawPlanet(canvas)
            val msg = s.message
            if (msg != null) {
                fitText(msgPaint, msg, 740f, 40f)
                canvas.drawText(msg, 535f, 90f, msgPaint)
            } else {
                drawVolume(canvas, s)
            }
            canvas.restoreToCount(lay)
        }
        canvas.restoreToCount(sc)
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState) {
        val t = s.timeSec
        val mute = s.muted
        val v = level(s)
        labelPaint.color = if (mute) 0xFFFCA5A5.toInt() else 0xFFA5B4FC.toInt()
        canvas.drawText(if (mute) "SILENCIO · SIN SEÑAL" else "VOLUMEN", X0, 44f, labelPaint)

        // Línea punteada que avanza (a mano: DashPathEffect obligaría a crear uno por cuadro).
        val tip = X0 + (X1 - X0) * v
        var xs = X0 - 16f + (t * 20f) % 16f
        while (xs < X1) {
            val a = max(xs, X0)
            val b = min(xs + 6f, X1)
            if (b > a) canvas.drawLine(a, TY, b, TY, dash)
            xs += 16f
        }
        for (i in 0..10) {
            val x = X0 + (X1 - X0) * i / 10f
            fill.color = if (x <= tip) dotOn else dotOff
            canvas.drawCircle(x, TY, 3f, fill)
        }

        // Estela hasta el cohete, con resplandor de trazos anchos y transparentes.
        if (tip - X0 > 2f) {
            val c = canvas.save()
            canvas.translate(X0, 0f)
            canvas.scale(tip - X0, 1f)
            if (!mute) {
                trailGlow.alpha = 40
                canvas.drawRect(0f, TY - 7f, 1f, TY + 7f, trailGlow)
                trailGlow.alpha = 80
                canvas.drawRect(0f, TY - 4f, 1f, TY + 4f, trailGlow)
            }
            trail.shader = if (mute) trailMuteShader else trailShader
            canvas.drawRect(0f, TY - 2f, 1f, TY + 2f, trail)
            canvas.restoreToCount(c)
        }

        // Chispas del escape: ranuras fijas que se reciclan cada 0,6 s (sin estado).
        if (!mute) {
            val frac = 40f * (0.3f + v) * VIDA_CHISPA / N_CHISPAS
            for (i in 0 until N_CHISPAS) {
                if (chispaSel[i] >= frac) continue
                val tt = t + i * (VIDA_CHISPA / N_CHISPAS)
                val ciclo = floor(tt / VIDA_CHISPA).toInt()
                val age = tt - ciclo * VIDA_CHISPA
                val seed = i * 7919 + ciclo * 104729
                val x = tip - 30f - (40f + 60f * hash01(seed)) * age
                val y = TY + (hash01(seed + 1) - 0.5f) * 6f + (hash01(seed + 2) - 0.5f) * 20f * age
                spark.alpha = ((1f - age / VIDA_CHISPA) * 255f).toInt().coerceIn(0, 255)
                canvas.drawRect(x, y, x + 3f, y + 3f, spark)
            }
        }

        val boost = if (!mute) exp(-s.sinceChangeSec * 4f) * 0.5f else 0f
        val fuego = if (mute) 0f else (0.6f + 0.4f * v) * (1f + 0.25f * ruido(t * 20f)) + boost
        drawRocket(canvas, tip, TY, 0f, 1f, fuego)

        numPaint.color = if (mute) 0xFF94A3B8.toInt() else BLANCO
        canvas.drawText(NUM[s.index.coerceIn(0, 100)], 928f, 108f, numPaint)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val sc = canvas.save()
        if (w != SW.toInt() || h != SH.toInt()) canvas.scale(w / SW, h / SH)
        val t = s.timeSec
        val vis = min(s.appear, clamp01(t / ENTRADA))
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val mute = s.muted
        val v = level(s)

        rect.set(0f, 0f, SW, SH)
        canvas.drawRoundRect(rect, SIDE_R, SIDE_R, sideBg)
        val c1 = canvas.save()
        canvas.clipPath(sidePath)

        // Estrellas que caen: más rápido cuanto más volumen (en silencio quedan casi quietas).
        val velVol = 30f + v * 320f
        val vel = if (mute) 8f else velVol
        val scroll = if (mute) {
            val desde = min(s.sinceChangeSec, t)
            (t - desde) * velVol + desde * 8f
        } else {
            t * vel
        }
        for (i in latX.indices) {
            val z = latZ[i]
            val y = (latY[i] * SH + scroll * z) % SH
            val x = latX[i] * SW
            val len = 1f + vel * z * 0.05f
            starLine.color = white(0.35f + 0.5f * z)
            starLine.strokeWidth = latR[i]
            canvas.drawLine(x, y - len, x, y, starLine)
        }

        // Luna
        canvas.drawCircle(82f, 26f, 11f, moon)
        canvas.drawCircle(78f, 23f, 3f, crater)
        canvas.drawCircle(85f, 30f, 2f, crater)

        // Tierra con resplandor y continentes que se desplazan.
        canvas.drawCircle(EX, EY, ER + 16f, earthGlow)
        canvas.drawCircle(EX, EY, ER, earthPaint)
        val c2 = canvas.save()
        canvas.clipPath(earthPath)
        for (k in 0 until 3) {
            val x = EX - 130f + ((k * 90f + t * 8f) % 260f)
            val y = EY - 74f + k * 14f
            rect2.set(x - 26f, y - 9f, x + 26f, y + 9f)
            canvas.drawOval(rect2, continent)
        }
        canvas.restoreToCount(c2)

        for (i in 0..10) {
            val y = 364f - i * 30f
            canvas.drawLine(8f, y, if (i % 5 != 0) 14f else 20f, y, tick)
        }

        val alt = clamp(v * ap, -0.05f, 1.05f)
        val cy = 364f - alt * 300f + (if (mute) sin(t * 1.5f) * 4f else 0f)
        if (!mute && ap > 0f) {
            val len = 394f - (cy + 20f)
            if (len > 2f) {
                val c3 = canvas.save()
                canvas.translate(0f, cy + 20f)
                canvas.scale(1f, len)
                canvas.drawRect(EX - 1.5f, 2f / len, EX + 1.5f, 1f, exhaust)
                canvas.restoreToCount(c3)
            }
        }
        val lay = layer(canvas, max(0.2f, ap), 0f, 0f, SW, SH)
        val fuego = if (mute) 0f else (0.5f + 0.5f * v) * (1f + 0.3f * ruido(t * 18f))
        drawRocket(canvas, EX, cy, -90f, 0.85f, fuego)
        canvas.restoreToCount(lay)

        canvas.restoreToCount(c1)
        rect2.set(1f, 1f, SW - 1f, SH - 1f)
        canvas.drawRoundRect(rect2, SIDE_R - 1f, SIDE_R - 1f, border)
        canvas.restoreToCount(sc)
    }

    private fun drawPlanet(canvas: Canvas) {
        // Mitad trasera del anillo, el planeta y la mitad delantera encima.
        var c = canvas.save()
        canvas.rotate(-20.05f, PX, PY)
        canvas.drawArc(ringRect, 180f, 180f, false, ringBack)
        canvas.restoreToCount(c)
        canvas.drawCircle(PX, PY, PR, planetPaint)
        c = canvas.save()
        canvas.rotate(-20.05f, PX, PY)
        canvas.drawArc(ringRect, 0f, 180f, false, ringFront)
        canvas.restoreToCount(c)
    }

    private fun drawRocket(canvas: Canvas, x: Float, y: Float, deg: Float, esc: Float, fuego: Float) {
        val c = canvas.save()
        canvas.translate(x, y)
        if (deg != 0f) canvas.rotate(deg)
        if (esc != 1f) canvas.scale(esc, esc)
        if (fuego > 0.02f) {
            val c2 = canvas.save()
            canvas.translate(-26f, 0f)
            canvas.scale(12f + 30f * fuego, 1f)
            canvas.drawPath(flamePath, flamePaint)
            canvas.restoreToCount(c2)
        }
        canvas.drawPath(finPath, red)
        canvas.drawRect(-27f, -6f, -22f, 6f, nozzle)
        canvas.drawPath(bodyPath, bodyPaint)
        canvas.drawRect(-8f, -9f, -4f, 9f, red)
        canvas.drawCircle(8f, 0f, 5f, windowFill)
        canvas.drawCircle(8f, 0f, 5f, windowStroke)
        canvas.restoreToCount(c)
    }

    /** Capa con alfa solo durante la entrada/salida; el resto del tiempo es un save() común. */
    private fun layer(canvas: Canvas, alpha: Float, l: Float, t: Float, r: Float, b: Float): Int =
        if (alpha >= 0.995f) canvas.save()
        else canvas.saveLayerAlpha(l, t, r, b, (alpha * 255f).toInt().coerceIn(0, 255))

    private fun fitText(paint: Paint, str: String, maxW: Float, size: Float) {
        paint.textSize = size
        val tw = paint.measureText(str)
        if (tw > maxW) paint.textSize = size * maxW / tw
    }

    private fun white(a: Float) = Color.argb((clamp01(a) * 255f).toInt(), 255, 255, 255)

    /** En silencio el controlador lleva el nivel a 0; el mockup sigue mostrando el volumen. */
    private fun level(s: OverlayState) = if (s.muted) clamp01(s.index / 100f) else clamp01(s.level)

    private fun ruido(x: Float) = sin(x * 1.7f) * 0.5f + sin(x * 3.13f + 1.3f) * 0.3f + sin(x * 7.7f + 4f) * 0.2f

    private fun hash01(n: Int): Float {
        var x = n * -0x61c88647
        x = x xor (x ushr 16)
        x *= 0x7feb352d
        x = x xor (x ushr 15)
        x *= -0x7b935975
        x = x xor (x ushr 16)
        return (x ushr 8) / 16777216f
    }

    private fun clamp(x: Float, a: Float, b: Float) = if (x < a) a else if (x > b) b else x
    private fun clamp01(x: Float) = clamp(x, 0f, 1f)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun easeOut(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
}
