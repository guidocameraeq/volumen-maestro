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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Medieval: pergamino que se desenrolla desde el centro, barra dorada con gema, número
 * con su versión en romanos y sello de lacre al silenciar. En el lateral, una antorcha
 * sobre pared de piedra cuya llama crece con el volumen (y humea en silencio).
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object MedievalTheme : Theme {
    override val id = "medieval"
    override val name = "Medieval"
    override val description = "Pergamino, sello de lacre y una antorcha que arde más fuerte cuanto más subís."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.50f
    private const val ARC_DEG = 42.97f // 0,75 rad
    private const val TAU = 6.2831855f

    private const val INK = 0xFF6B3F1D.toInt()
    private const val WINE = 0xFF8B1A1A.toInt()
    private const val GOLD_EDGE = 0xFFC99A2E.toInt()

    // Torch: centro y base de la llama en coordenadas del lateral.
    private const val TCX = 54f
    private const val TBY = 264f
    private const val SPARK_SLOTS = 36
    private const val SMOKE_SLOTS = 18

    private val NUM = Array(101) { it.toString() }
    private val ROMAN = Array(101) { romano(it) }

    // Manchas del pergamino y tonos de los ladrillos: misma semilla (7) y mismo orden que el mockup.
    private val spots = FloatArray(70 * 3)       // x, y (0..1) y radio
    private val spotColor = IntArray(70)
    private val bricks = FloatArray(40 * 4)      // left, top, right, bottom
    private val brickColor = IntArray(40)
    private val brickCount: Int

    init {
        var st = 7
        fun rnd(): Float {
            st = st xor (st shl 13)
            st = st xor (st ushr 17)
            st = st xor (st shl 5)
            return ((st.toLong() and 0xFFFFFFFFL) % 100000L).toFloat() / 100000f
        }
        for (i in 0 until 70) {
            spots[i * 3] = rnd()
            spots[i * 3 + 1] = rnd()
            spots[i * 3 + 2] = 1f + rnd() * 4f
            spotColor[i] = Color.argb(((0.05f + rnd() * 0.09f) * 255f).roundToInt(), 120, 80, 30)
        }
        val tones = FloatArray(61) { rnd() }
        var idx = 0
        var fila = 0
        var y = 0f
        while (y < SH) {
            val off = (fila % 2) * 27f
            var x = -off
            while (x < SW) {
                val tn = tones[idx % tones.size]
                bricks[idx * 4] = x + 2f
                bricks[idx * 4 + 1] = y + 2f
                bricks[idx * 4 + 2] = x + 52f
                bricks[idx * 4 + 3] = y + 25f
                brickColor[idx] = Color.rgb(
                    (64 + tn * 20).roundToInt(), (58 + tn * 15).roundToInt(), (52 + tn * 12).roundToInt(),
                )
                idx++
                x += 54f
            }
            fila++
            y += 27f
        }
        brickCount = idx
    }

    private val rect = RectF()
    private val oval = RectF()
    private val path = Path()
    private val clip = Path()
    private val sideClip = Path().apply {
        addRoundRect(RectF(0f, 0f, SW, SH), 10f, 10f, Path.Direction.CW)
    }

    /** Escudo centrado en (70,75) del panel inferior. */
    private val shield = Path().apply {
        val cx = 70f
        val cy = 75f
        moveTo(cx - 34f, cy - 40f)
        lineTo(cx + 34f, cy - 40f)
        lineTo(cx + 34f, cy - 4f)
        quadTo(cx + 34f, cy + 28f, cx, cy + 44f)
        quadTo(cx - 34f, cy + 28f, cx - 34f, cy - 4f)
        close()
    }

    /** Sello de lacre de radio 38 centrado en el origen, con borde ondulado. */
    private val seal = Path().apply {
        for (i in 0..60) {
            val a = i / 60f * TAU
            val rad = 38f * (1f + 0.06f * sin(a * 10f))
            val x = cos(a) * rad
            val y = sin(a) * rad
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** Cabo de la antorcha (fijo). */
    private val handle = Path().apply {
        moveTo(TCX - 11f, 274f)
        lineTo(TCX + 11f, 274f)
        lineTo(TCX + 6f, 412f)
        lineTo(TCX - 6f, 412f)
        close()
    }

    private val base = Paint().apply { color = 0xFFDCC088.toInt() }
    private val parchment = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, BW, BH,
            intArrayOf(0xFFF1DFB0.toInt(), 0xFFDCC088.toInt(), 0xFFEAD3A0.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plain = Paint()
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shieldFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 35f, 0f, 119f, 0xFFA5221C.toInt(), 0xFF6D0F0B.toInt(), Shader.TileMode.CLAMP)
    }
    private val sealFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(-11.4f, -11.4f, 54f, 0xFFD0453A.toInt(), 0xFF6D0F0B.toInt(), Shader.TileMode.CLAMP)
    }
    private val goldBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 62f, 0f, 90f,
            intArrayOf(0xFFFFE08A.toInt(), 0xFFE2A93A.toInt(), 0xFFA8701C.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    /** Gema dibujada en el origen (se traslada a la punta de la barra). */
    private val gem = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            -3f, -3f, 13f,
            intArrayOf(0xFFFF8A7A.toInt(), 0xFFC0392B.toInt(), 0xFF5A0B08.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            TCX - 11f, 0f, TCX + 11f, 0f,
            intArrayOf(0xFF3B2412.toInt(), 0xFF7A4F2A.toInt(), 0xFF3B2412.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    /** Resplandor de la antorcha: radio 1 en el origen, se escala al tamaño del fuego. */
    private val torchGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f, Color.argb(128, 255, 160, 60), Color.argb(0, 255, 120, 30), Shader.TileMode.CLAMP,
        )
    }
    private val spkFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("serif", Typeface.BOLD)
        textSize = 24f
        letterSpacing = 0.06f
    }
    private val motto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("serif", Typeface.ITALIC)
        textSize = 19f
    }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("serif", Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        textSize = 64f
    }
    private val roman = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("serif", Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        textSize = 16f
        letterSpacing = 0.1f
    }
    private val sealText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("serif", Typeface.BOLD_ITALIC)
        textAlign = Paint.Align.CENTER
        textSize = 36f
    }
    private val msgHead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }
    private val msgBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("serif", Typeface.BOLD)
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
        val r = lerp(6f, 14f, p)
        canvas.clipRect(rx, ry, rx + rw, ry + rh)
        rr(canvas, rx, ry, rw, rh, r, parchment)

        // Manchas y bordes envejecidos, recortados al pergamino.
        canvas.save()
        clip.reset()
        rect.set(rx, ry, rx + rw, ry + rh)
        clip.addRoundRect(rect, r, r, Path.Direction.CW)
        canvas.clipPath(clip)
        for (i in 0 until 70) {
            fill.color = spotColor[i]
            canvas.drawCircle(rx + spots[i * 3] * rw, ry + spots[i * 3 + 1] * rh, spots[i * 3 + 2], fill)
        }
        stroke.strokeWidth = 28f
        stroke.color = Color.argb(51, 110, 70, 25)
        canvas.drawPath(clip, stroke)
        stroke.strokeWidth = 10f
        stroke.color = Color.argb(71, 90, 55, 20)
        canvas.drawPath(clip, stroke)
        canvas.restore()

        stroke.color = INK
        stroke.strokeWidth = 3f
        rr(canvas, rx + 12f, ry + 12f, rw - 24f, rh - 24f, 6f, stroke)
        stroke.strokeWidth = 1.2f
        rr(canvas, rx + 18f, ry + 18f, rw - 36f, rh - 36f, 4f, stroke)
        fill.color = WINE
        rombo(canvas, rx + 12f, ry + 12f, 8f)
        rombo(canvas, rx + rw - 12f, ry + 12f, 8f)
        rombo(canvas, rx + 12f, ry + rh - 12f, 8f)
        rombo(canvas, rx + rw - 12f, ry + rh - 12f, 8f)

        if (ap > 0f) {
            // El contenido se desenrolla desde el centro.
            val half = rw / 2f * ap
            canvas.save()
            canvas.clipRect(BW / 2f - half, ry, BW / 2f + half, ry + rh)
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, ap) else drawVolume(canvas, s, ap)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float) {
        val v = clamp01(s.level)
        val mute = s.muted
        val m = muteAmount(s)

        // Escudo con parlante; al silenciar lo reemplaza un sello de lacre que "cae" con rebote.
        if (m < 0.99f) {
            val a = ap * (1f - m)
            shieldFill.alpha = (255 * a).toInt()
            canvas.drawPath(shield, shieldFill)
            stroke.strokeWidth = 4f
            stroke.tint(GOLD_EDGE, a)
            canvas.drawPath(shield, stroke)
            speaker(canvas, 70f, 71f, 34f, 0xFFF1DFB0.toInt(), a, 3.5f, v)
        }
        if (m > 0.01f) {
            val k = easeBack(clamp01(m))
            canvas.save()
            canvas.translate(70f, 75f)
            canvas.scale(k, k)
            sealFill.alpha = (255 * ap).toInt()
            canvas.drawPath(seal, sealFill)
            stroke.strokeWidth = 3f
            stroke.tint(Color.argb(71, 0, 0, 0), ap)
            canvas.drawCircle(0f, 0f, 38f * 0.68f, stroke)
            sealText.tint(Color.argb(230, 246, 199, 182), ap)
            canvas.drawText("S", 0f, 2f - (sealText.ascent() + sealText.descent()) / 2f, sealText)
            canvas.restore()
        }

        val label = if (mute) "Silencio" else "Volumen"
        title.tint(0xFF5A3415.toInt(), ap)
        canvas.drawText(label, 144f, 48f, title)
        if (m > 0.01f) {
            motto.tint(WINE, ap * m)
            canvas.drawText("· la corte guarda silencio", 144f + title.measureText(label) + 14f, 48f, motto)
        }

        // Riel de cuero, barra dorada, marcas y gema en la punta.
        val x0 = 144f
        val x1 = 725f
        val ty = 62f
        val th = 28f
        fill.tint(0xFF6E4726.toInt(), ap)
        rr(canvas, x0, ty, x1 - x0, th, 14f, fill)
        stroke.strokeWidth = 2f
        stroke.tint(0xFF3E2410.toInt(), ap)
        rr(canvas, x0, ty, x1 - x0, th, 14f, stroke)
        val fw = (x1 - x0 - 8f) * v
        if (fw > 1f) {
            goldBar.alpha = (255 * ap * (1f - 0.55f * m)).toInt()
            rr(canvas, x0 + 4f, ty + 4f, fw, th - 8f, 10f, goldBar)
        }
        stroke.strokeWidth = 2f
        stroke.tint(INK, ap)
        for (i in 1 until 10) {
            val x = x0 + 4f + (x1 - x0 - 8f) * i / 10f
            val len = if (i == 5) 14f else 10f
            canvas.drawLine(x, ty + th + 4f, x, ty + th + len, stroke)
        }
        canvas.save()
        canvas.translate(x0 + 4f + fw, ty + th / 2f)
        gem.alpha = (255 * ap).toInt()
        canvas.drawCircle(0f, 0f, 11f, gem)
        stroke.strokeWidth = 2f
        stroke.tint(GOLD_EDGE, ap)
        canvas.drawCircle(0f, 0f, 11f, stroke)
        canvas.restore()

        number.tint(0xFF4A2410.toInt(), ap)
        canvas.drawText(num(s.index), 925f, 98f, number)
        roman.tint(WINE, ap)
        canvas.drawText(if (s.index in 0..100) ROMAN[s.index] else "", 925f, 128f, roman)
    }

    /** "Volumen Maestro: ENCENDIDO" → título chico arriba y el valor grande, como un edicto. */
    private fun drawMessage(canvas: Canvas, msg: String, ap: Float) {
        val sep = msg.indexOf(": ")
        val bodyStart = if (sep > 0) sep + 2 else 0
        var bodyY = 92f
        if (sep > 0) {
            msgHead.tint(0xFF5A3415.toInt(), ap)
            fitText(msgHead, msg, 0, sep, 24f, 760f)
            canvas.drawText(msg, 0, sep, BW / 2f, 56f, msgHead)
            bodyY = 106f
        }
        msgBody.tint(0xFF4A2410.toInt(), ap)
        fitText(msgBody, msg, bodyStart, msg.length, 42f, 780f)
        canvas.drawText(msg, bodyStart, msg.length, BW / 2f, bodyY, msgBody)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / SW, h / SH)
        canvas.clipPath(sideClip)
        // Pared de piedra: el color de fondo hace de argamasa.
        plain.color = 0xFF2A2622.toInt()
        canvas.drawRect(0f, 0f, SW, SH, plain)
        for (i in 0 until brickCount) {
            plain.color = brickColor[i]
            canvas.drawRect(bricks[i * 4], bricks[i * 4 + 1], bricks[i * 4 + 2], bricks[i * 4 + 3], plain)
        }

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val v = clamp01(s.level)
        val m = muteAmount(s)
        val t = s.timeSec
        val fuego = ap * (1f - m)
        val fl = 1f + 0.08f * ruido(t * 9f)
        val fh = (30f + v * 190f) * fuego * fl

        if (fuego > 0.01f) {
            val rad = 70f + v * 170f
            canvas.save()
            canvas.translate(TCX, TBY - fh * 0.5f)
            canvas.scale(rad, rad)
            torchGlow.alpha = (255 * fuego).toInt()
            canvas.drawRect(-1f, -1f, 1f, 1f, torchGlow)
            canvas.restore()
        }

        // Antorcha: soporte de hierro, cabo de madera y cabeza envuelta.
        plain.color = 0xFF1D1D1F.toInt()
        canvas.drawRect(TCX - 6f, 314f, TCX + 6f, 348f, plain)
        canvas.drawRect(TCX - 22f, 324f, TCX + 22f, 333f, plain)
        canvas.drawPath(handle, handleFill)
        plain.color = 0xFF2A2A2E.toInt()
        canvas.drawRect(TCX - 13f, 326f, TCX + 13f, 332f, plain)
        fill.color = 0xFF4A3520.toInt()
        rr(canvas, TCX - 15f, 258f, 30f, 24f, 5f, fill)
        stroke.strokeWidth = 2f
        stroke.color = 0xFF2B1D10.toInt()
        canvas.drawLine(TCX - 15f, 266f, TCX + 15f, 272f, stroke)
        canvas.drawLine(TCX - 15f, 274f, TCX + 15f, 280f, stroke)

        if (fh > 2f) {
            val sway = ruido(t * 2.7f + 3f) * fh * 0.12f
            val fw = 30f + v * 26f
            // Resplandor de la llama: dos copias más grandes y tenues.
            fill.tint(0xFFFF7A00.toInt(), 0.16f)
            flame(canvas, TBY + 2f, fw * 1.6f, fh * 1.14f, sway)
            fill.tint(0xFFFF7A00.toInt(), 0.3f)
            flame(canvas, TBY + 1f, fw * 1.25f, fh * 1.06f, sway)
            fill.color = 0xFFFF5A00.toInt()
            flame(canvas, TBY, fw, fh, sway)
            fill.color = 0xFFFFAE00.toInt()
            flame(canvas, TBY + 2f, fw * 0.66f, fh * 0.72f, sway * 0.8f)
            fill.color = 0xFFFFF4C2.toInt()
            flame(canvas, TBY + 3f, fw * 0.36f, fh * 0.42f, sway * 0.5f)
        }

        drawSparks(canvas, t, v * fuego, TBY - fh * 0.45f)
        if (m > 0.01f) drawSmoke(canvas, s, m * ap)

        // Brasa que late cuando la antorcha está apagada.
        if (m > 0.01f) {
            val a = (0.55f + 0.35f * sin(t * 5f)) * m * ap
            fill.tint(0xFFFF6A1A.toInt(), a * 0.3f)
            canvas.drawCircle(TCX, TBY - 2f, 14f, fill)
            fill.tint(0xFFFF6E1E.toInt(), a)
            canvas.drawCircle(TCX, TBY - 2f, 7f, fill)
        }
        canvas.restore()
    }

    /**
     * Chispas sin estado: cada "ranura" tiene su propia vida y se recicla en ciclos;
     * la semilla del ciclo decide posición, velocidad y si se enciende (según la intensidad).
     */
    private fun drawSparks(canvas: Canvas, t: Float, intensity: Float, y0: Float) {
        if (intensity <= 0.01f) return
        val density = intensity * 0.92f
        for (i in 0 until SPARK_SLOTS) {
            val life = 0.7f + hash(i * 7 + 1) * 0.8f
            val tt = t + hash(i * 7 + 2) * life
            val cycle = floor(tt / life).toInt()
            val age = tt - cycle * life
            if (t - age < 0f) continue
            val seed = i * 7919 + cycle * 104729
            if (hash(seed) > density) continue
            val x = TCX + (hash(seed + 1) - 0.5f) * 20f + (hash(seed + 2) - 0.5f) * 30f * age + sin(age * 9f) * 3f
            val y = y0 - (50f + hash(seed + 3) * 90f) * age
            fill.tint(0xFFFFD27A.toInt(), 1f - age / life)
            canvas.drawRect(x, y, x + 3f, y + 3f, fill)
        }
    }

    /** Humo que sube desde la antorcha apagada; solo nacen bocanadas posteriores al silencio. */
    private fun drawSmoke(canvas: Canvas, s: OverlayState, amount: Float) {
        val t = s.timeSec
        val muteStart = t - s.sinceChangeSec
        for (i in 0 until SMOKE_SLOTS) {
            val life = 2f + hash(i * 13 + 5)
            val tt = t + hash(i * 13 + 6) * life
            val cycle = floor(tt / life).toInt()
            val age = tt - cycle * life
            val born = t - age
            if (born < 0f || born < muteStart) continue
            val seed = i * 6151 + cycle * 92821
            if (hash(seed) > amount * 0.97f) continue
            val k = age / life
            val x = TCX + (hash(seed + 1) - 0.5f) * 12f * age + sin(age * 2.5f) * 8f
            val y = TBY - 6f - (28f + hash(seed + 2) * 18f) * age
            fill.color = Color.argb((0.35f * (1f - k) * 255f).toInt(), 175, 175, 175)
            canvas.drawCircle(x, y, 5f + age * 14f, fill)
        }
    }

    /** Llama con base redondeada; usa el color de [fill]. */
    private fun flame(canvas: Canvas, by: Float, w: Float, h: Float, sway: Float) {
        val cx = TCX
        path.reset()
        path.moveTo(cx - w / 2f, by)
        path.cubicTo(cx - w / 2f, by - h * 0.38f, cx - w * 0.12f + sway * 0.6f, by - h * 0.62f, cx + sway, by - h)
        path.cubicTo(cx + w * 0.12f + sway * 0.6f, by - h * 0.62f, cx + w / 2f, by - h * 0.38f, cx + w / 2f, by)
        path.quadTo(cx + w / 2f, by + w * 0.42f, cx, by + w * 0.42f)
        path.quadTo(cx - w / 2f, by + w * 0.42f, cx - w / 2f, by)
        path.close()
        canvas.drawPath(path, fill)
    }

    /** Rombo con el color de [fill]. */
    private fun rombo(canvas: Canvas, cx: Float, cy: Float, sz: Float) {
        path.reset()
        path.moveTo(cx, cy - sz)
        path.lineTo(cx + sz, cy)
        path.lineTo(cx, cy + sz)
        path.lineTo(cx - sz, cy)
        path.close()
        canvas.drawPath(path, fill)
    }

    private fun speaker(c: Canvas, cx: Float, cy: Float, sz: Float, color: Int, alpha: Float, grosor: Float, nivel: Float) {
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

    private fun romano(n: Int): String {
        if (n <= 0) return "N"
        val values = intArrayOf(100, 90, 50, 40, 10, 9, 5, 4, 1)
        val letters = arrayOf("C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        val sb = StringBuilder()
        var rest = n
        for (i in values.indices) {
            while (rest >= values[i]) {
                sb.append(letters[i])
                rest -= values[i]
            }
        }
        return sb.toString()
    }

    private fun num(i: Int) = if (i in 0..100) NUM[i] else i.toString()
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
    private fun easeBack(t: Float): Float {
        val u = t - 1f
        return 1f + 2.70158f * u * u * u + 1.70158f * u * u
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
