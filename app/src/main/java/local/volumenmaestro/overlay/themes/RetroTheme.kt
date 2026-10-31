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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Retro analógico: consola de madera y bronce con VU meter de aguja, contador mecánico
 * que rueda, lámpara de silencio, rejilla de parlante y perilla que gira con el volumen.
 * En el lateral, una escala vertical con puntero y una lámpara piloto.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object RetroTheme : Theme {
    override val id = "retro"
    override val name = "Retro analógico"
    override val description = "Madera, bronce y una aguja de VU como la del equipo de música del abuelo."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.40f

    private const val DARK = 0xFF3A2A1A.toInt()
    private const val RED = 0xFFC0392B.toInt()
    private const val LAMP_RED = 0xFFFF3B30.toInt()
    private const val BRASS = 0xFFC9A14A.toInt()

    // VU: pivote fuera del dial, arco de -π/2-0,8 a -π/2+0,8.
    private const val VU_PX = 150f
    private const val VU_PY = 174f
    private const val VU_R = 100f
    private const val A0 = -2.3707964f
    private const val A_SPAN = 1.6f
    private const val A0_DEG = -135.8366f
    private const val SPAN_DEG = 91.6732f

    // Perilla.
    private const val KX = 862f
    private const val KY = 75f
    private const val KR = 50f

    private val NUM = Array(10) { it.toString() }
    private val VU_LABELS = arrayOf("0", "50", "100")
    private val SCALE_LABELS = arrayOf("0", "20", "40", "60", "80", "100")
    private val BRONZE = intArrayOf(0xFFF3D58A.toInt(), 0xFFB8862E.toInt(), 0xFF7A5418.toInt(), 0xFFE2B85C.toInt())
    private val BRONZE_POS = floatArrayOf(0f, 0.35f, 0.7f, 1f)
    private val WOOD = intArrayOf(0xFF6B4226.toInt(), 0xFF4E2E18.toInt(), 0xFF3A2110.toInt())
    private val WOOD_POS = floatArrayOf(0f, 0.5f, 1f)

    private val rect = RectF()
    private val oval = RectF()
    private val path = Path()
    private val clip = Path()
    private val sideClip = Path().apply {
        addRoundRect(RectF(0f, 0f, SW, SH), 14f, 14f, Path.Direction.CW)
    }
    private val sideFace = Path().apply {
        addRoundRect(RectF(18f, 24f, 90f, 374f), 7f, 7f, Path.Direction.CW)
    }

    /** Vetas verticales del lateral (tamaño fijo, se arman una sola vez). */
    private val sideGrain = Path().apply {
        for (k in 0 until 6) {
            val x0 = (k + 0.5f) * SW / 6f
            var yy = 0f
            while (yy <= SH + 30f) {
                val xx = x0 + sin((yy + 326f) * 0.017f + k * 1.9f) * 3f
                if (yy == 0f) moveTo(xx, yy) else lineTo(xx, yy)
                yy += 30f
            }
        }
    }

    /** Contorno dentado de la perilla (fijo; solo gira la marca). */
    private val knobOutline = Path().apply {
        for (k in 0..48) {
            val a = k / 48f * 2f * PI.toFloat()
            val rad = KR * if (k % 2 == 1) 0.95f else 1f
            val x = KX + cos(a) * rad
            val y = KY + sin(a) * rad
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private val base = Paint().apply { color = 0xFF4E2E18.toInt() }
    private val woodBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, BH, WOOD, WOOD_POS, Shader.TileMode.CLAMP)
    }
    private val woodSide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, SW, 0f, WOOD, WOOD_POS, Shader.TileMode.CLAMP)
    }
    private val grain = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(71, 25, 12, 4)
    }
    private val brassFrameBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        shader = LinearGradient(0f, 0f, BW, BH, BRONZE, BRONZE_POS, Shader.TileMode.CLAMP)
    }
    private val brassFrameSide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        shader = LinearGradient(0f, 0f, SW, SH, BRONZE, BRONZE_POS, Shader.TileMode.CLAMP)
    }
    private val vuBezel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 14f, 0f, 136f, BRONZE, BRONZE_POS, Shader.TileMode.CLAMP)
    }
    private val vuFace = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            VU_PX, 136f, 230f,
            intArrayOf(0xFFFFF6D8.toInt(), 0xFFFFF6D8.toInt(), 0xFFE2C98E.toInt()),
            floatArrayOf(0f, 0.043f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val vuGloss = Paint().apply {
        shader = LinearGradient(0f, 14f, 0f, 14f + 122f * 0.45f, Color.argb(89, 255, 255, 255), 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    private val drumFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 52f, 0f, 124f,
            intArrayOf(0xFF050505.toInt(), 0xFF2A2A2A.toInt(), 0xFF050505.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val drumShade = Paint().apply {
        shader = LinearGradient(
            0f, 52f, 0f, 124f,
            intArrayOf(Color.argb(153, 0, 0, 0), 0, 0, Color.argb(153, 0, 0, 0)),
            floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val lampHaloBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(550f, 43f, 28f, Color.argb(150, 255, 59, 48), Color.argb(0, 255, 59, 48), Shader.TileMode.CLAMP)
    }
    private val knobBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(KX - KR, KY - KR, KX + KR, KY + KR, BRONZE, BRONZE_POS, Shader.TileMode.CLAMP)
    }
    private val knobCap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            KX - KR * 0.3f, KY - KR * 0.35f, KR * 1.2f, 0xFFF3D58A.toInt(), 0xFF8A5C1A.toInt(), Shader.TileMode.CLAMP,
        )
    }
    private val plateBronze = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 18f, 0f, 132f, BRONZE, BRONZE_POS, Shader.TileMode.CLAMP)
    }
    private val plateFace = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 24f, 0f, 126f, 0xFFFFF6D8.toInt(), 0xFFE2C98E.toInt(), Shader.TileMode.CLAMP)
    }
    private val scaleBezel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(18f, 0f, 90f, 0f, BRONZE, BRONZE_POS, Shader.TileMode.CLAMP)
    }
    private val scaleFace = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            18f, 0f, 90f, 0f,
            intArrayOf(0xFFE8D29C.toInt(), 0xFFFFF3D1.toInt(), 0xFFE8D29C.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val scaleGloss = Paint().apply {
        shader = LinearGradient(
            18f, 0f, 90f, 0f,
            intArrayOf(0x00FFFFFF, Color.argb(71, 255, 255, 255), 0x00FFFFFF),
            floatArrayOf(0f, 0.3f, 0.45f), Shader.TileMode.CLAMP,
        )
    }
    private val lampRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(39f, 387f, 69f, 417f, BRONZE, BRONZE_POS, Shader.TileMode.CLAMP)
    }
    private val lampHaloRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(54f, 402f, 26f, Color.argb(160, 255, 59, 48), Color.argb(0, 255, 59, 48), Shader.TileMode.CLAMP)
    }
    private val lampHaloAmber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(54f, 402f, 26f, Color.argb(160, 255, 179, 71), Color.argb(0, 255, 179, 71), Shader.TileMode.CLAMP)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono }
    private val digit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = mono
        textAlign = Paint.Align.CENTER
        textSize = 52f
        color = 0xFFF4F1EA.toInt()
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
        val r = lerp(6f, 18f, p)
        canvas.clipRect(rx, ry, rx + rw, ry + rh)
        woodPanel(canvas, rx, ry, rw, rh, r)
        rr(canvas, rx + 4f, ry + 4f, rw - 8f, rh - 8f, max(3f, r - 4f), brassFrameBottom)

        if (ap > 0f) {
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, ap) else drawVolume(canvas, s, ap)
        }
        canvas.restore()
    }

    /** Madera con vetas horizontales que siguen la forma de la placa. */
    private fun woodPanel(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float) {
        rr(canvas, x, y, w, h, r, woodBottom)
        canvas.save()
        clip.reset()
        rect.set(x, y, x + w, y + h)
        clip.addRoundRect(rect, r, r, Path.Direction.CW)
        canvas.clipPath(clip)
        path.reset()
        for (k in 0 until 12) {
            val y0 = y + (k + 0.5f) * h / 12f
            var xx = x
            while (xx <= x + w + 40f) {
                val ax = xx + 480f // las vetas usan la coordenada de pantalla, como el mockup
                val yy = y0 + sin(ax * 0.013f + k * 1.7f) * 4f + sin(ax * 0.051f + k) * 1.5f
                if (xx == x) path.moveTo(xx, yy) else path.lineTo(xx, yy)
                xx += 40f
            }
        }
        canvas.drawPath(path, grain)
        canvas.restore()
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float) {
        val v = clamp01(s.level)
        val mute = s.muted
        val m = muteAmount(s)
        val t = s.timeSec
        val a255 = (255 * ap).toInt()

        // ── VU meter
        val vx = 25f
        val vy = 14f
        val vw = 250f
        val vh = 122f
        vuBezel.alpha = a255
        rr(canvas, vx - 5f, vy - 5f, vw + 10f, vh + 10f, 12f, vuBezel)
        vuFace.alpha = a255
        rr(canvas, vx, vy, vw, vh, 9f, vuFace)
        canvas.save()
        clip.reset()
        rect.set(vx, vy, vx + vw, vy + vh)
        clip.addRoundRect(rect, 9f, 9f, Path.Direction.CW)
        canvas.clipPath(clip)

        stroke.strokeWidth = 2f
        stroke.tint(DARK, ap)
        oval.set(VU_PX - VU_R, VU_PY - VU_R, VU_PX + VU_R, VU_PY + VU_R)
        canvas.drawArc(oval, A0_DEG, SPAN_DEG, false, stroke)
        stroke.strokeWidth = 7f
        stroke.tint(RED, ap)
        val rRed = VU_R + 4f
        oval.set(VU_PX - rRed, VU_PY - rRed, VU_PX + rRed, VU_PY + rRed)
        canvas.drawArc(oval, A0_DEG + SPAN_DEG * 0.8f, SPAN_DEG * 0.2f, false, stroke)
        stroke.tint(DARK, ap)
        for (i in 0..10) {
            val a = A0 + A_SPAN * i / 10f
            val l = if (i % 5 == 0) 16f else 9f
            stroke.strokeWidth = if (i % 5 == 0) 2.5f else 1.5f
            val c = cos(a)
            val sn = sin(a)
            canvas.drawLine(VU_PX + c * VU_R, VU_PY + sn * VU_R, VU_PX + c * (VU_R + l), VU_PY + sn * (VU_R + l), stroke)
        }
        text.textAlign = Paint.Align.CENTER
        text.letterSpacing = 0f
        text.textSize = 15f
        text.tint(DARK, ap)
        for (j in 0..2) {
            val a = A0 + A_SPAN * j / 2f
            canvas.drawText(VU_LABELS[j], VU_PX + cos(a) * (VU_R + 30f), VU_PY + sin(a) * (VU_R + 30f) + 5f, text)
        }
        text.textSize = 24f
        canvas.drawText("VU", VU_PX, 121f, text)

        // Aguja: sigue al nivel con un poco de temblor; el rebote de resorte sale del reloj.
        val jitter = 0.93f + 0.07f * (0.5f + 0.5f * ruido(t * 6f))
        val needle = (v * jitter * springStep(t, 4.5f, 10.94f) * ap + kick(s.sinceChangeSec)).coerceIn(-0.03f, 1.05f)
        val an = A0 + A_SPAN * needle
        stroke.strokeWidth = 2.5f
        stroke.tint(0xFF161616.toInt(), ap)
        canvas.drawLine(VU_PX, VU_PY, VU_PX + cos(an) * (VU_R + 26f), VU_PY + sin(an) * (VU_R + 26f), stroke)
        if (m > 0.01f) {
            fill.color = Color.argb((0.25f * m * ap * 255f).toInt(), 40, 20, 0)
            canvas.drawRect(vx, vy, vx + vw, vy + vh, fill)
        }
        vuGloss.alpha = a255
        canvas.drawRect(vx, vy, vx + vw, vy + vh * 0.45f, vuGloss)
        canvas.restore()

        // ── Contador mecánico: el último dígito rueda hasta su lugar después de cada cambio.
        text.textAlign = Paint.Align.LEFT
        text.textSize = 17f
        text.letterSpacing = 3f / 17f
        text.tint(0xFFE9C46A.toInt(), ap)
        canvas.drawText("VOLUMEN", 310f, 42f, text)
        val roll = if (mute) 0f else exp(-s.sinceChangeSec * 10f)
        odometer(canvas, 310f, 52f, (s.index - roll).coerceIn(0f, 999f), ap)

        // ── Lámpara de silencio
        if (mute) {
            lampHaloBottom.alpha = a255
            canvas.drawCircle(550f, 43f, 28f, lampHaloBottom)
        }
        fill.tint(if (mute) LAMP_RED else 0xFF4A1512.toInt(), ap)
        canvas.drawCircle(550f, 43f, 10f, fill)
        stroke.strokeWidth = 3f
        stroke.tint(BRASS, ap)
        canvas.drawCircle(550f, 43f, 12f, stroke)
        text.textSize = 16f
        text.letterSpacing = 2f / 16f
        text.tint(if (mute) 0xFFFFB4A8.toInt() else 0xFFB08A5A.toInt(), ap)
        canvas.drawText("SILENCIO", 570f, 49f, text)

        // ── Rejilla del parlante
        fill.tint(0xFF2A170B.toInt(), ap)
        rr(canvas, 525f, 63f, 240f, 66f, 8f, fill)
        stroke.strokeWidth = 2f
        stroke.tint(0xFF7A5418.toInt(), ap)
        rr(canvas, 525f, 63f, 240f, 66f, 8f, stroke)
        fill.tint(0xFF100803.toInt(), ap)
        var gx = 539f
        while (gx < 760f) {
            var gy = 75f
            while (gy < 124f) {
                canvas.drawCircle(gx, gy, 3.6f, fill)
                gy += 14f
            }
            gx += 14f
        }

        drawKnob(canvas, v, ap)
    }

    private fun odometer(canvas: Canvas, x: Float, y: Float, valor: Float, ap: Float) {
        val w = 54f
        val h = 72f
        val gap = 6f
        val n = 3
        val mid = -(digit.ascent() + digit.descent()) / 2f
        val a255 = (255 * ap).toInt()
        var pw = 1f
        for (i in 0 until n) {
            val whole = floor(valor / pw + 1e-4f)
            val resto = valor - whole * pw
            val pos = if (i == 0) valor % 10f else (whole % 10f) + max(0f, resto - (pw - 1f))
            val bx = x + (n - 1 - i) * (w + gap)
            fill.tint(BRASS, ap)
            rr(canvas, bx - 3f, y - 3f, w + 6f, h + 6f, 6f, fill)
            drumFill.alpha = a255
            rr(canvas, bx, y, w, h, 4f, drumFill)
            canvas.save()
            canvas.clipRect(bx, y, bx + w, y + h)
            val d0 = floor(pos).toInt().mod(10)
            val fr = pos - floor(pos)
            digit.alpha = a255
            canvas.drawText(NUM[d0], bx + w / 2f, y + h / 2f - fr * h + mid, digit)
            canvas.drawText(NUM[(d0 + 1) % 10], bx + w / 2f, y + h / 2f + (1f - fr) * h + mid, digit)
            drumShade.alpha = a255
            canvas.drawRect(bx, y, bx + w, y + h, drumShade)
            canvas.restore()
            pw *= 10f
        }
    }

    /** Perilla de bronce con puntos de escala; la marca gira con el volumen. */
    private fun drawKnob(canvas: Canvas, v: Float, ap: Float) {
        for (i in 0..10) {
            val a = (-PI * 1.25 + i / 10.0 * PI * 1.5).toFloat()
            fill.tint(if (i / 10f <= v + 0.001f) 0xFFFFD27A.toInt() else 0xFF6B4A2A.toInt(), ap)
            canvas.drawCircle(KX + cos(a) * (KR + 9f), KY + sin(a) * (KR + 9f), 2.4f, fill)
        }
        knobBody.alpha = (255 * ap).toInt()
        canvas.drawPath(knobOutline, knobBody)
        knobCap.alpha = (255 * ap).toInt()
        canvas.drawCircle(KX, KY, KR * 0.78f, knobCap)
        val a = (-PI * 1.25).toFloat() + v * (PI * 1.5).toFloat()
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeWidth = 6f
        stroke.tint(0xFF3A2110.toInt(), ap)
        canvas.drawLine(KX + cos(a) * KR * 0.25f, KY + sin(a) * KR * 0.25f, KX + cos(a) * KR * 0.7f, KY + sin(a) * KR * 0.7f, stroke)
        stroke.strokeCap = Paint.Cap.BUTT
    }

    /** Chapa de bronce con rótulo crema: "Volumen Maestro" arriba y el estado grabado abajo. */
    private fun drawMessage(canvas: Canvas, msg: String, ap: Float) {
        plateBronze.alpha = (255 * ap).toInt()
        rr(canvas, 96f, 18f, 768f, 114f, 12f, plateBronze)
        plateFace.alpha = (255 * ap).toInt()
        rr(canvas, 102f, 24f, 756f, 102f, 8f, plateFace)
        val sep = msg.indexOf(": ")
        val bodyStart = if (sep > 0) sep + 2 else 0
        var bodyY = 88f
        text.textAlign = Paint.Align.CENTER
        if (sep > 0) {
            text.letterSpacing = 3f / 17f
            text.tint(0xFF7A5418.toInt(), ap)
            fitText(text, msg, 0, sep, 17f, 680f)
            canvas.drawText(msg, 0, sep, BW / 2f, 56f, text)
            bodyY = 104f
        }
        text.letterSpacing = 0.04f
        text.tint(DARK, ap)
        fitText(text, msg, bodyStart, msg.length, 36f, 700f)
        canvas.drawText(msg, bodyStart, msg.length, BW / 2f, bodyY, text)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / SW, h / SH)
        canvas.clipPath(sideClip)
        canvas.drawRect(0f, 0f, SW, SH, base)
        canvas.drawRect(0f, 0f, SW, SH, woodSide)
        canvas.drawPath(sideGrain, grain)
        rr(canvas, 3f, 3f, SW - 6f, SH - 6f, 11f, brassFrameSide)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val v = clamp01(s.level)
        val mute = s.muted

        // Escala vertical con puntero.
        rr(canvas, 14f, 20f, 80f, 358f, 10f, scaleBezel)
        canvas.drawPath(sideFace, scaleFace)
        canvas.save()
        canvas.clipPath(sideFace)
        val a = max(0.35f, ap)
        val y0 = 356f
        val hh = 314f
        fill.tint(RED, a)
        canvas.drawRect(82f, y0 - hh, 87f, y0 - hh + hh * 0.2f, fill)
        text.textAlign = Paint.Align.LEFT
        text.letterSpacing = 0f
        text.textSize = 13f
        text.tint(DARK, a)
        stroke.tint(DARK, a)
        for (i in 0..20) {
            val y = y0 - hh * i / 20f
            val l = if (i % 2 == 0) 14f else 7f
            stroke.strokeWidth = if (i % 2 == 0) 2f else 1.2f
            canvas.drawLine(80f - l, y, 80f, y, stroke)
            if (i % 4 == 0) canvas.drawText(SCALE_LABELS[i / 4], 23f, y + 5f, text)
        }
        val pos = (v * springStep(s.timeSec, 3.5f, 8.82f) * ap + kick(s.sinceChangeSec) * 0.8f).coerceIn(-0.02f, 1.03f)
        val py = y0 - hh * pos
        stroke.strokeWidth = 2.5f
        stroke.tint(RED, a)
        canvas.drawLine(20f, py, 86f, py, stroke)
        path.reset()
        path.moveTo(88f, py)
        path.lineTo(78f, py - 7f)
        path.lineTo(78f, py + 7f)
        path.close()
        canvas.drawPath(path, fill)
        scaleGloss.alpha = (255 * a).toInt()
        canvas.drawRect(18f, 24f, 90f, 374f, scaleGloss)
        canvas.restore()

        // Lámpara piloto: ámbar según el volumen, roja en silencio.
        canvas.drawCircle(54f, 402f, 15f, lampRing)
        val glow = if (mute) 1f else if (v > 0.2f) v * ap else 0f
        if (glow > 0f) {
            val halo = if (mute) lampHaloRed else lampHaloAmber
            halo.alpha = (255 * glow).toInt()
            canvas.drawCircle(54f, 402f, 26f, halo)
        }
        fill.color = if (mute) LAMP_RED else mix(0xFF5A3A10.toInt(), 0xFFFFC65A.toInt(), v * ap)
        canvas.drawCircle(54f, 402f, 11f, fill)
        canvas.restore()
    }

    /** Respuesta de un resorte subamortiguado a un escalón (0 → 1, con sobrepaso). */
    private fun springStep(t: Float, decay: Float, wd: Float): Float {
        if (t <= 0f) return 0f
        return 1f - exp(-decay * t) * (cos(wd * t) + decay / wd * sin(wd * t))
    }

    /** Pequeño cimbronazo mecánico después de cada cambio de volumen. */
    private fun kick(sc: Float) = 0.02f * exp(-6f * sc) * sin(14f * sc)

    private fun fitText(p: Paint, str: String, start: Int, end: Int, size: Float, maxW: Float) {
        p.textSize = size
        val tw = p.measureText(str, start, end)
        if (tw > maxW) p.textSize = size * maxW / tw
    }

    private fun rr(c: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) {
        val rad = max(0f, min(r, min(w, h) / 2f))
        rect.set(x, y, x + w, y + h)
        c.drawRoundRect(rect, rad, rad, p)
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val k = clamp01(t)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * k).roundToInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * k).roundToInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).roundToInt(),
        )
    }

    private fun muteAmount(s: OverlayState) = if (s.muted) 1f - exp(-s.sinceChangeSec * 9f) else 0f
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
}
