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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Steampunk (mockup 8): placa de cobre remachada con manómetro de aguja temblorosa, engranajes,
 * tubo de presión ámbar con burbujas y vapor al cambiar el volumen. En el lateral, cremallera y piñón.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object SteampunkTheme : Theme {
    override val id = "steampunk"
    override val name = "Steampunk"
    override val description = "Placa de cobre con manómetro, engranajes que giran y vapor cada vez que tocás el volumen."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.45f
    private const val TAU = 6.2831855f

    private val BASE = 0xFF2A1D10.toInt()
    private val COBRE = 0xFFB87333.toInt()
    private val COBRE_CLARO = 0xFFE0A96D.toInt()
    private val TINTA = 0xFF2B1A0A.toInt()
    private val MARRON = 0xFF5A3A12.toInt()
    private val ROJO_OSCURO = 0xFF8B1A1A.toInt()
    private val BRONCE = intArrayOf(0xFFF3D58A.toInt(), 0xFFB8862E.toInt(), 0xFF7A5418.toInt(), 0xFFE2B85C.toInt())
    private val BRONCE_POS = floatArrayOf(0f, 0.35f, 0.7f, 1f)

    private val maquina: Typeface = Typeface.create("serif-monospace", Typeface.NORMAL)

    // Tubo de presión
    private const val TX0 = 300f
    private const val TX1 = 735f
    private const val TY = 58f
    private const val TH = 34f

    private val rect = RectF()
    private val path = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plateBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, BH, 0xFF4D3521.toInt(), 0xFF24180C.toInt(), Shader.TileMode.CLAMP)
    }
    private val plateSide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, SH, 0xFF4D3521.toInt(), 0xFF24180C.toInt(), Shader.TileMode.CLAMP)
    }
    private val edgeThick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = COBRE
    }
    private val edgeThin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = COBRE_CLARO
    }
    // Remache: gradiente en coordenadas propias, se dibuja con translate.
    private val rivetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            -1.2f, -1.2f, 5.2f,
            intArrayOf(0xFFF6D9A0.toInt(), 0xFFA8692A.toInt(), 0xFF4A2A10.toInt()), floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = COBRE_CLARO
    }
    private val tubePath = Path().apply { addRoundRect(RectF(TX0, TY, TX1, TY + TH), 17f, 17f, Path.Direction.CW) }
    private val tubeDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A0F06.toInt() }
    private val tubeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = COBRE_CLARO
    }
    private val liquid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, TY, 0f, TY + TH,
            intArrayOf(0xFFFBBF24.toInt(), 0xFFD97706.toInt(), 0xFF92400E.toInt()), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val liquidMute = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B5A3A.toInt() }
    private val bubble = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 255, 240, 200) }
    private val glassShine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(46, 255, 255, 255) }
    private val steam = Paint(Paint.ANTI_ALIAS_FLAG)

    // Burbujas precalculadas con semilla fija.
    private val burbX = FloatArray(10)
    private val burbY = FloatArray(10)
    private val burbV = FloatArray(10)

    init {
        val rnd = Random(3)
        for (i in burbX.indices) {
            burbX[i] = rnd.nextFloat()
            burbY[i] = rnd.nextFloat()
            burbV[i] = 0.3f + rnd.nextFloat() * 0.7f
        }
    }

    // Placa del número
    private val numPlate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 30f, 0f, 120f, 0xFFF0CF8A.toInt(), 0xFFA87332.toInt(), Shader.TileMode.CLAMP)
    }
    private val numPlateStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = MARRON
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = maquina
        textSize = 16f
        letterSpacing = 2f / 16f
        textAlign = Paint.Align.LEFT
    }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(maquina, Typeface.BOLD)
        textSize = 50f
        textAlign = Paint.Align.CENTER
        color = TINTA
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = maquina
        textSize = 13f
        textAlign = Paint.Align.CENTER
    }
    private val msgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(maquina, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = TINTA
    }
    private val NUM = Array(101) { it.toString() }

    private val gearA = Gear(12, 36f, 28f, 0xFFE6C27A.toInt(), 0xFF7A5A20.toInt())
    private val gearB = Gear(9, 28f, 21f, 0xFFE0A070.toInt(), 0xFF7A3F1A.toInt())
    private val gearS1 = Gear(10, 22f, 16f, 0xFFE6C27A.toInt(), 0xFF7A5A20.toInt())
    private val gearS2 = Gear(7, 16f, 11f, 0xFFE0A070.toInt(), 0xFF7A3F1A.toInt())
    private val gauge = Gauge(95f, 75f, 58f, true, BRONCE, BRONCE_POS, maquina)
    private val gaugeSide = Gauge(54f, 56f, 28f, false, BRONCE, BRONCE_POS, maquina)

    // Lateral: cremallera
    private val rackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(30f, 0f, 48f, 0f, BRONCE, BRONCE_POS, Shader.TileMode.CLAMP)
    }
    private val toothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC9A14A.toInt() }
    private val pointerPath = Path().apply { moveTo(20f, 0f); lineTo(30f, -7f); lineTo(30f, 7f); close() }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB91C1C.toInt() }

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
        canvas.clipRect(rx, ry, rr, rb)
        plate(canvas, rx, ry, rr, rb, lerp(6f, 16f, p), plateBottom)
        var x = rx + 30f
        while (x < rr - 20f) {
            rivet(canvas, x, ry + 14f)
            rivet(canvas, x, rb - 14f)
            x += 70f
        }

        if (ap > 0f) {
            val lay = layer(canvas, ap, rx, ry, rr, rb)
            val v = level(s)
            val m = muteAmount(s)

            // Engranajes: giran más rápido con más volumen y además acompañan el cambio de nivel.
            // En silencio se frenan donde estaban.
            val vel = 0.4f + 3.2f * clamp01(s.index / 100f)
            val ang = runTime(s) * vel + v * TAU
            gearA.draw(canvas, 210f, 45f, ang)
            gearB.draw(canvas, 248f, 86f, -ang * 12f / 9f + 0.35f)

            val aguja = v * (1f - m) * (0.97f + 0.03f * ruido(t * 8f)) * ap
            gauge.draw(canvas, aguja)

            val msg = s.message
            if (msg != null) drawMessage(canvas, msg) else drawVolume(canvas, s, v)
            canvas.restoreToCount(lay)
        }
        canvas.restoreToCount(sc)
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, v: Float) {
        val t = s.timeSec
        val mute = s.muted

        // Tubo de presión con marcas, tapas de cobre y líquido ámbar.
        for (i in 0..10) {
            val x = TX0 + 17f + (TX1 - TX0 - 34f) * i / 10f
            canvas.drawLine(x, 44f, x, if (i % 5 != 0) 50f else 54f, tick)
        }
        fill.color = COBRE
        rect.set(TX0 - 10f, TY - 6f, TX0 + 6f, TY + TH + 6f)
        canvas.drawRoundRect(rect, 4f, 4f, fill)
        rect.set(TX1 - 6f, TY - 6f, TX1 + 10f, TY + TH + 6f)
        canvas.drawRoundRect(rect, 4f, 4f, fill)
        canvas.drawPath(tubePath, tubeDark)
        val c = canvas.save()
        canvas.clipPath(tubePath)
        val lw = (TX1 - TX0) * v
        if (lw > 1f) {
            canvas.drawRect(TX0, TY, TX0 + lw, TY + TH, if (mute) liquidMute else liquid)
            val fase = runTime(s) * 0.4f
            for (i in burbX.indices) {
                val bx = TX0 + ((burbX[i] + fase * burbV[i]) % 1f) * lw
                val by = TY + 7f + burbY[i] * (TH - 14f) + sin(t * 3f + burbX[i] * 9f) * 2f
                canvas.drawCircle(bx, by, 2.5f, bubble)
            }
        }
        canvas.drawRect(TX0, TY + 5f, TX1, TY + 10f, glassShine)
        canvas.restoreToCount(c)
        canvas.drawPath(tubePath, tubeStroke)

        label.color = if (mute) 0xFFF87171.toInt() else 0xFFD6B27A.toInt()
        canvas.drawText(if (mute) "VÁLVULA CERRADA" else "PRESIÓN DE VOLUMEN", TX0, 122f, label)

        // Placa del número
        rect.set(765f, 30f, 925f, 120f)
        canvas.drawRoundRect(rect, 8f, 8f, numPlate)
        canvas.drawRoundRect(rect, 8f, 8f, numPlateStroke)
        rivet(canvas, 775f, 40f)
        rivet(canvas, 915f, 40f)
        rivet(canvas, 775f, 110f)
        rivet(canvas, 915f, 110f)
        canvas.drawText(NUM[s.index.coerceIn(0, 100)], 845f, 88f, number)
        small.color = if (mute) ROJO_OSCURO else MARRON
        canvas.drawText(if (mute) "SILENCIO" else "NIVEL", 845f, 112f, small)

        // Vapor: ráfaga al cambiar y soplido continuo por encima del 85 %.
        // Sin estado: cada bocanada sale de un instante de emisión fijo y se calcula por su edad.
        if (!mute) {
            val since = s.sinceChangeSec
            if (since < 0.35f + 1.2f) {
                for (k in 0 until 14) {
                    val age = since - k / 40f
                    if (age < 0f || age >= 1.2f) continue
                    puffBottom(canvas, k * 31 + s.index * 977, age)
                }
            }
            if (v > 0.85f) {
                for (i in 0 until 10) {
                    val tt = t + i * 0.12f
                    val ciclo = floor(tt / 1.2f).toInt()
                    val age = tt - ciclo * 1.2f
                    if (age > since) continue
                    puffBottom(canvas, 5000 + i * 131 + ciclo * 7, age)
                }
            }
        }
    }

    private fun drawMessage(canvas: Canvas, msg: String) {
        // Plaqueta de bronce con el mensaje, a la derecha de los engranajes.
        fitText(msgPaint, msg, 560f, 34f)
        val pw = min(630f, msgPaint.measureText(msg) + 70f)
        val cx = 615f
        val l = cx - pw / 2f
        val r = cx + pw / 2f
        rect.set(l, 30f, r, 120f)
        canvas.drawRoundRect(rect, 8f, 8f, numPlate)
        canvas.drawRoundRect(rect, 8f, 8f, numPlateStroke)
        rivet(canvas, l + 10f, 40f)
        rivet(canvas, r - 10f, 40f)
        rivet(canvas, l + 10f, 110f)
        rivet(canvas, r - 10f, 110f)
        canvas.drawText(msg, cx, 88f, msgPaint)
    }

    private fun puffBottom(canvas: Canvas, seed: Int, age: Float) {
        val x = 742f + (10f + 25f * hash01(seed)) * age
        val y = 54f - (30f + 40f * hash01(seed + 1)) * age
        steam.color = Color.argb((0.38f * (1f - age / 1.2f) * 255f).toInt().coerceIn(0, 255), 232, 226, 214)
        canvas.drawCircle(x, y, 4f + age * 18f, steam)
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

        plate(canvas, 0f, 0f, SW, SH, 12f, plateSide)
        rivet(canvas, 14f, 14f)
        rivet(canvas, SW - 14f, 14f)
        rivet(canvas, 14f, SH - 14f)
        rivet(canvas, SW - 14f, SH - 14f)

        val c = canvas.save()
        canvas.clipRect(9f, 9f, SW - 9f, SH - 9f)
        val lay = layer(canvas, max(0.3f, ap), 0f, 0f, SW, SH)
        val obj = v * (1f - m) * ap
        gaugeSide.draw(canvas, obj)

        for (i in 0..10) {
            val y = 404f - i * 30f
            canvas.drawLine(12f, y, if (i % 5 != 0) 18f else 23f, y, tick)
        }

        // Cremallera que sube con el nivel y piñón que la acompaña.
        val rackTop = 404f - clamp(obj, -0.02f, 1.03f) * 300f
        canvas.drawRect(30f, rackTop, 48f, 434f, rackPaint)
        path.reset()
        var ty = rackTop + 6f
        while (ty < 434f) {
            path.moveTo(48f, ty)
            path.lineTo(55f, ty + 2f)
            path.lineTo(55f, ty + 7f)
            path.lineTo(48f, ty + 9f)
            path.close()
            ty += 12f
        }
        canvas.drawPath(path, toothPaint)
        val c2 = canvas.save()
        canvas.translate(0f, rackTop)
        canvas.drawPath(pointerPath, pointerPaint)
        canvas.restoreToCount(c2)

        val angP = obj * 300f / 19f
        gearS1.draw(canvas, 72f, 314f, angP)
        gearS2.draw(canvas, 74f, 348f, -angP * 10f / 7f + 0.2f)

        // Vapor por la válvula de arriba cuando la presión pasa el 85 %.
        if (!mute && obj > 0.85f) {
            val since = s.sinceChangeSec
            for (i in 0 until 14) {
                val tt = t + i * 0.1f
                val ciclo = floor(tt / 1.4f).toInt()
                val age = tt - ciclo * 1.4f
                if (age > since) continue
                val seed = 9000 + i * 131 + ciclo * 7
                val x = 90f + (hash01(seed) - 0.5f) * 14f * age
                val y = 94f - (25f + 30f * hash01(seed + 1)) * age
                steam.color = Color.argb((0.35f * (1f - age / 1.4f) * 255f).toInt().coerceIn(0, 255), 232, 226, 214)
                canvas.drawCircle(x, y, 3f + age * 10f, steam)
            }
        }
        canvas.restoreToCount(lay)
        canvas.restoreToCount(c)
        canvas.restoreToCount(sc)
    }

    private fun plate(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, paint: Paint) {
        rect.set(l, t, r, b)
        canvas.drawRoundRect(rect, rad, rad, paint)
        val r2 = max(2f, rad - 3f)
        rect.set(l + 3f, t + 3f, r - 3f, b - 3f)
        canvas.drawRoundRect(rect, r2, r2, edgeThick)
        val r3 = max(2f, rad - 6f)
        rect.set(l + 8f, t + 8f, r - 8f, b - 8f)
        canvas.drawRoundRect(rect, r3, r3, edgeThin)
    }

    private fun rivet(canvas: Canvas, x: Float, y: Float) {
        val c = canvas.save()
        canvas.translate(x, y)
        canvas.drawCircle(0f, 0f, 4f, rivetPaint)
        canvas.restoreToCount(c)
    }

    /** Reloj de la maquinaria: en silencio se queda en el instante en que se silenció. */
    private fun runTime(s: OverlayState) = if (s.muted) max(0f, s.timeSec - s.sinceChangeSec) else s.timeSec

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

    /** Engranaje con forma, gradiente y agujeros precalculados; se dibuja rotando el canvas. */
    private class Gear(n: Int, re: Float, ri: Float, c1: Int, c2: Int) {
        private val teeth = Path()
        private val holes = FloatArray(8)
        private val holeR = ri * 0.17f
        private val hubR = ri * 0.25f
        private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(-re * 0.3f, -re * 0.3f, re * 1.35f, c1, c2, Shader.TileMode.CLAMP)
        }
        private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(102, 0, 0, 0)
        }
        private val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E140A.toInt() }
        private val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A1C0F.toInt() }
        private val hubRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = c1
        }

        init {
            val st = 2.0 * PI / n
            for (i in 0 until n) {
                val a = i * st
                point(ri, a, i == 0)
                point(re, a + st * 0.18, false)
                point(re, a + st * 0.46, false)
                point(ri, a + st * 0.64, false)
            }
            teeth.close()
            for (k in 0 until 4) {
                val a = k * PI / 2 + PI / 4
                holes[k * 2] = (cos(a) * ri * 0.55).toFloat()
                holes[k * 2 + 1] = (sin(a) * ri * 0.55).toFloat()
            }
        }

        private fun point(r: Float, a: Double, first: Boolean) {
            val x = (cos(a) * r).toFloat()
            val y = (sin(a) * r).toFloat()
            if (first) teeth.moveTo(x, y) else teeth.lineTo(x, y)
        }

        fun draw(canvas: Canvas, cx: Float, cy: Float, angRad: Float) {
            val c = canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(angRad * 57.29578f)
            canvas.drawPath(teeth, body)
            canvas.drawPath(teeth, outline)
            for (k in 0 until 4) canvas.drawCircle(holes[k * 2], holes[k * 2 + 1], holeR, hole)
            canvas.drawCircle(0f, 0f, hubR, hub)
            canvas.drawCircle(0f, 0f, hubR, hubRing)
            canvas.restoreToCount(c)
        }
    }

    /** Manómetro: arco de 270° desde 135°, zona roja en el último 20 %, aguja y reflejo. */
    private class Gauge(
        private val cx: Float,
        private val cy: Float,
        private val r: Float,
        private val labels: Boolean,
        bronze: IntArray,
        bronzePos: FloatArray,
        font: Typeface,
    ) {
        private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, bronze, bronzePos, Shader.TileMode.CLAMP)
        }
        private val face = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy - r * 0.3f, r * 1.3f, 0xFFFBF3DC.toInt(), 0xFFE0CFA2.toInt(), Shader.TileMode.CLAMP)
        }
        private val redZone = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = 0xFFB91C1C.toInt()
        }
        private val redRect = RectF(cx - (r - 8f), cy - (r - 8f), cx + (r - 8f), cy + (r - 8f))
        private val tickMajor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0xFF3A2A1A.toInt()
        }
        private val tickMinor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = 0xFF3A2A1A.toInt()
        }
        private val major = FloatArray(3 * 4)
        private val minor = FloatArray(8 * 4)
        private val labelText = arrayOf("0", "50", "100")
        private val labelX = FloatArray(3)
        private val labelY = FloatArray(3)
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 14f
            textAlign = Paint.Align.CENTER
            color = 0xFF3A2A1A.toInt()
        }
        private val needle = Path().apply {
            moveTo(-r * 0.15f, -3f)
            lineTo(r - 9f, 0f)
            lineTo(-r * 0.15f, 3f)
            close()
        }
        private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B1A1A.toInt() }
        private val hubR = max(4f, r * 0.12f)
        private val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC9A14A.toInt() }
        private val shineRect = RectF(cx - (r - 3f), cy - (r - 3f), cx + (r - 3f), cy + (r - 3f))
        private val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(115, 255, 255, 255)
        }

        init {
            var mj = 0
            var mn = 0
            for (i in 0..10) {
                val a = Math.toRadians(135.0 + 27.0 * i)
                val isMajor = i % 5 == 0
                val ri = r - (if (isMajor) 17f else 12f)
                val ro = r - 4f
                val arr = if (isMajor) major else minor
                val k = if (isMajor) mj++ * 4 else mn++ * 4
                arr[k] = cx + (cos(a) * ri).toFloat()
                arr[k + 1] = cy + (sin(a) * ri).toFloat()
                arr[k + 2] = cx + (cos(a) * ro).toFloat()
                arr[k + 3] = cy + (sin(a) * ro).toFloat()
            }
            for (j in 0 until 3) {
                val a = Math.toRadians(135.0 + 27.0 * j * 5)
                labelX[j] = cx + (cos(a) * (r - 30f)).toFloat()
                labelY[j] = cy + (sin(a) * (r - 30f)).toFloat() + 5f
            }
        }

        fun draw(canvas: Canvas, value: Float) {
            canvas.drawCircle(cx, cy, r + 6f, rim)
            canvas.drawCircle(cx, cy, r, face)
            canvas.drawArc(redRect, 351f, 54f, false, redZone)
            canvas.drawLines(minor, tickMinor)
            canvas.drawLines(major, tickMajor)
            if (labels) for (j in 0 until 3) canvas.drawText(labelText[j], labelX[j], labelY[j], labelPaint)
            val v = if (value < -0.02f) -0.02f else if (value > 1.04f) 1.04f else value
            val c = canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(135f + 270f * v)
            canvas.drawPath(needle, needlePaint)
            canvas.restoreToCount(c)
            canvas.drawCircle(cx, cy, hubR, hub)
            canvas.drawArc(shineRect, 198f, 63f, false, shine)
        }
    }
}
