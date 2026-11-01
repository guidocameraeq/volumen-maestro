package local.volumenmaestro.overlay.themes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Candelabro gótico: diez velas sobre empapelado de rombos y filigrana dorada que se prenden
 * de izquierda a derecha (cada una vale 10) y una varilla que brilla hasta el nivel.
 * En silencio se apagan con un hilo de humo. En el lateral, una vela alta: la cera es la barra.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object CandelabroTheme : Theme {
    override val id = "candelabro"
    override val name = "Candelabro gótico"
    override val description =
        "Diez velas sobre filigrana dorada que se van prendiendo a medida que subís; en silencio se apagan con un hilo de humo."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.45f

    private const val BASE = 0xFF130A0B.toInt()
    private const val FONDO_A = 0xFF24110F.toInt()
    private const val FONDO_B = 0xFF0B0607.toInt()
    private const val ORO = 0xFFC9A35A.toInt()
    private const val ORO_CLARO = 0xFFF0DCA6.toInt()
    private const val ORO_OSC = 0xFF4E3A1F.toInt()
    private const val CERA = 0xFFEFE3C8.toInt()
    private const val CERA_SOMBRA = 0xFFB8A585.toInt()
    private const val APAGADO = 0xFF6E5A40.toInt()
    private const val GOTA = 0xFFF3E9D2.toInt()
    private const val TAPA = 0xFFF7EEDB.toInt()
    private const val MECHA = 0xFF2A1D14.toInt()

    private val ENREJADO = conAlfa(ORO_CLARO, 0.028f)
    private val ORO_18 = conAlfa(ORO, 0.18f)
    private val ORO_25 = conAlfa(ORO, 0.25f)
    private val ORO_45 = conAlfa(ORO, 0.45f)
    private val ORO_50 = conAlfa(ORO, 0.5f)
    private val ORO_60 = conAlfa(ORO, 0.6f)
    private val ORO_85 = conAlfa(ORO, 0.85f)
    private val ORO_CLARO_60 = conAlfa(ORO_CLARO, 0.6f)
    private val ORO_CLARO_70 = conAlfa(ORO_CLARO, 0.7f)
    private val LLAMA_CENTRO = Color.argb(242, 255, 247, 220)
    private val LLAMA_AZUL = Color.argb(89, 122, 162, 255)

    private val NUM = Array(101) { it.toString() }

    // Alturas y gotas de cera fijas de las diez velas (desplazamiento x y largo).
    private val VELA_ALTO = floatArrayOf(54f, 62f, 50f, 66f, 57f, 52f, 64f, 56f, 61f, 51f)
    private val GOTA_DX = floatArrayOf(-4f, 3f, -2f, 4f, -5f, 2f, -3f, 4f, -1f, 3f)
    private val GOTA_LEN = floatArrayOf(9f, 14f, 6f, 11f, 13f, 8f, 15f, 7f, 12f, 10f)
    private val GOTA_LAT_DX = floatArrayOf(-9f, -2f, 5f, 8f)
    private val GOTA_LAT_LEN = floatArrayOf(22f, 9f, 30f, 14f)

    private val rect = RectF()
    private val oval = RectF()
    private val path = Path()
    private val matriz = Matrix()
    private val mayus = CharArray(80)

    /** Pie del candelero del lateral (fijo). */
    private val pie = Path().apply {
        moveTo(48f, 388f)
        lineTo(60f, 388f)
        lineTo(57f, 404f)
        lineTo(51f, 404f)
        close()
    }

    private val placa = Paint().apply { color = BASE }
    private val fondoInf = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, BH, FONDO_A, FONDO_B, Shader.TileMode.CLAMP)
    }
    private val fondoLat = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, SH, FONDO_A, FONDO_B, Shader.TileMode.CLAMP)
    }
    /** Cera de la vela: degradé horizontal en 0..1 que se ubica con [matriz] en cada vela. */
    private val ceraShader = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(CERA_SOMBRA, CERA, 0xFF9C8A6C.toInt()),
        floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP,
    )
    private val cera = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = ceraShader }
    /** Llama: degradé de la base (y = 0) a la punta (y = 1), se estira con [matriz]. */
    private val llamaShader = LinearGradient(
        0f, 0f, 0f, 1f, Color.argb(242, 255, 170, 70), Color.argb(0, 255, 120, 40), Shader.TileMode.CLAMP,
    )
    private val llamaExt = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = llamaShader }
    // Halos: radio 1 en el origen, se trasladan y escalan.
    private val haloCalido = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f, Color.rgb(255, 170, 80), Color.argb(0, 255, 170, 80), Shader.TileMode.CLAMP)
    }
    private val haloVela = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f, Color.rgb(255, 180, 74), Color.argb(0, 255, 180, 74), Shader.TileMode.CLAMP)
    }
    // Hilo de humo, con el origen en la mecha (sube hacia y negativo).
    private val humoInf = humoPaint(42f)
    private val humoLat = humoPaint(70f)
    private val candelero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            28f, 0f, 80f, 0f,
            intArrayOf(0xFF7A5A2C.toInt(), 0xFFE2C27A.toInt(), 0xFF5E4524.toInt()),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val numInfShader = LinearGradient(0f, 66f, 0f, 116f, ORO_CLARO, ORO, Shader.TileMode.CLAMP)
    private val numLatShader = LinearGradient(0f, 26f, 0f, 50f, ORO_CLARO, ORO, Shader.TileMode.CLAMP)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val redondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.RIGHT
        textSize = 14f
        letterSpacing = 5f / 14f
    }
    private val cabecera = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.CENTER
        textSize = 15f
        letterSpacing = 5f / 15f
        color = ORO
    }
    private val silencioLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.CENTER
        textSize = 10f
        letterSpacing = 2.5f / 10f
    }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.RIGHT
    }
    private val numeroLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.CENTER
    }
    private val cuerpo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.CENTER
        shader = LinearGradient(0f, 80f, 0f, 118f, ORO_CLARO, ORO, Shader.TileMode.CLAMP)
    }

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / BW, h / BH)
        canvas.clipRect(0f, 0f, BW, BH)
        // Placa de seguridad: tapa el cartel del sistema desde el primer cuadro.
        canvas.drawRect(36f, 11f, 924f, 142f, placa)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val rx = lerp(36f, 0f, p)
        val ry = lerp(11f, 0f, p)
        val rw = lerp(888f, BW, p)
        val rh = lerp(131f, BH, p)
        canvas.clipRect(rx, ry, rx + rw, ry + rh)
        canvas.drawRect(rx, ry, rx + rw, ry + rh, fondoInf)
        enrejado(canvas, rx, ry, rw, rh)
        filigrana(canvas, rx + 7f, ry + 7f, rw - 14f, rh - 14f)

        if (ap > 0f) {
            canvas.save()
            canvas.translate(0f, (1f - ap) * 8f)
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, s.timeSec, ap) else drawVolume(canvas, s, ap)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float) {
        val t = s.timeSec
        val m = silencio(s)
        // En silencio la cera queda donde estaba (usa index); solo se apagan las llamas.
        val vf = (if (s.muted) s.index / 100f else clamp01(s.level)) * ap

        // Luz cálida sobre la pared.
        if (vf > 0.01f) {
            halo(canvas, haloCalido, 70f + 636f * vf / 2f, 70f, 140f + 300f * vf, 0.10f * min(1f, vf * 3f) * (1f - m) * ap)
        }
        // Varilla dorada que se ilumina hasta el nivel.
        redondo.strokeWidth = 2f
        redondo.tint(ORO_OSC, ap)
        canvas.drawLine(70f, 120f, 708f, 120f, redondo)
        if (vf > 0.003f) brillo(canvas, 70f, 120f, 70f + 638f * vf, 120f, ORO_CLARO, 1.8f, (1f - 0.6f * m) * ap)

        for (i in 0 until 10) {
            val cx = 102f + i * 64f
            val top = 110f - VELA_ALTO[i]
            oval.set(cx - 12f, 112f - 3.4f, cx + 12f, 112f + 3.4f)
            fill.tint(ORO_OSC, ap)
            canvas.drawOval(oval, fill)
            trazo.strokeWidth = 1f
            trazo.tint(ORO_85, ap)
            canvas.drawOval(oval, trazo)
            vela(canvas, cx, top, 111f, 12f, ap)
            fill.tint(GOTA, ap)
            rr(canvas, cx + GOTA_DX[i] - 1.5f, top, 3f, GOTA_LEN[i], 1.5f, fill)
            redondo.strokeWidth = 1.2f
            redondo.tint(MECHA, ap)
            canvas.drawLine(cx, top - 0.5f, cx + 0.6f, top - 5f, redondo)

            // Cada vela vale 10: la última prendida crece de a poco hasta el nivel exacto.
            val fi = clamp01(vf * 10f - i)
            if (fi > 0f) {
                val fl = 1f + 0.07f * ruido(t * 6.3f + i * 2.1f)
                val lad = 1.6f * ruido(t * 1.9f + i * 1.3f) * fi
                halo(canvas, haloVela, cx, top - 10f, 30f * (0.4f + 0.6f * fi), 0.30f * fi * (1f - m) * ap)
                llama(canvas, cx, top - 3f, lerp(8f, 22f, fi) * fl, lad, (1f - m) * ap)
                if (m > 0f) {
                    humo(canvas, humoInf, cx + 0.6f, top - 5f, t, i, m * (0.5f * exp(-s.sinceChangeSec * 0.6f) + 0.18f), 42f, ap)
                }
            }
        }

        etiqueta.tint(if (s.muted) ORO_60 else ORO, ap)
        espaciado(canvas, if (s.muted) "SILENCIO" else "VOLUMEN", 906f, 44f, etiqueta)
        redondo.strokeWidth = 1f
        redondo.tint(ORO_50, ap)
        canvas.drawLine(780f, 55.5f, 906f, 55.5f, redondo)
        fill.tint(ORO_CLARO, ap)
        rombo(canvas, 843f, 55.5f, 3f)

        val n = num(s.index)
        numero.shader = if (s.muted) null else numInfShader
        numero.tint(if (s.muted) APAGADO else ORO, ap)
        fitText(numero, n, 0, n.length, 64f, 126f)
        canvas.drawText(n, 906f, 116f, numero)
    }

    /** "Volumen Maestro: ENCENDIDO" → dos velas a los costados y el texto entre filigranas. */
    private fun drawMessage(canvas: Canvas, msg: String, t: Float, ap: Float) {
        halo(canvas, haloCalido, 480f, 80f, 380f, 0.07f * ap)
        for (j in 0..1) {
            val cx = if (j == 0) 118f else 842f
            val i = if (j == 0) 2 else 5
            oval.set(cx - 13f, 120f - 3.6f, cx + 13f, 120f + 3.6f)
            fill.tint(ORO_OSC, ap)
            canvas.drawOval(oval, fill)
            trazo.strokeWidth = 1f
            trazo.tint(ORO_85, ap)
            canvas.drawOval(oval, trazo)
            vela(canvas, cx, 64f, 119f, 13f, ap)
            trazo.strokeWidth = 1.2f
            trazo.tint(MECHA, ap)
            canvas.drawLine(cx, 63.5f, cx + 0.6f, 59f, trazo)
            halo(canvas, haloVela, cx, 50f, 34f, 0.3f * ap)
            llama(canvas, cx, 61f, 22f * (1f + 0.07f * ruido(t * 6.3f + i)), 1.6f * ruido(t * 1.9f + i), ap)
        }
        val sep = msg.indexOf(": ")
        val bodyStart = if (sep > 0) sep + 2 else 0
        if (sep > 0) {
            val n = mayusculas(msg, sep)
            cabecera.tint(ORO, ap)
            canvas.drawText(mayus, 0, n, 480f, 54f, cabecera)
        }
        trazo.strokeWidth = 1f
        trazo.tint(ORO_50, ap)
        canvas.drawLine(380f, 68.5f, 580f, 68.5f, trazo)
        fill.tint(ORO_CLARO, ap)
        rombo(canvas, 480f, 68.5f, 3f)
        cuerpo.alpha = (255f * ap).toInt()
        fitText(cuerpo, msg, bodyStart, msg.length, 44f, 560f)
        canvas.drawText(msg, bodyStart, msg.length, 480f, if (sep > 0) 116f else 104f, cuerpo)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / SW, h / SH)
        canvas.clipRect(0f, 0f, SW, SH)
        canvas.drawRect(0f, 0f, SW, SH, placa)
        canvas.drawRect(0f, 0f, SW, SH, fondoLat)
        enrejado(canvas, 0f, 0f, SW, SH)
        filigrana(canvas, 6f, 6f, 96f, 418f)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        if (ap > 0f) {
            val t = s.timeSec
            val m = silencio(s)
            val vf = (if (s.muted) s.index / 100f else clamp01(s.level)) * ap
            // Una sola vela alta: la cera es la barra y la llama marca el nivel.
            val bot = 384f
            val top = bot - lerp(12f, 270f, vf)
            halo(canvas, haloCalido, 54f, top - 16f, 110f, 0.16f * (1f - m) * ap)
            trazo.strokeWidth = 1f
            for (k in 0..10) {
                val yy = bot - 258f * k / 10f - 12f * (1f - k / 10f) + 0.5f
                trazo.tint(if (k / 10f <= vf + 1e-3f) ORO_CLARO_70 else ORO_25, ap)
                canvas.drawLine(86f, yy, if (k % 5 == 0) 95f else 92f, yy, trazo)
            }

            // Candelero.
            candelero.alpha = (255f * ap).toInt()
            trazo.tint(ORO_CLARO_60, ap)
            canvas.drawPath(pie, candelero)
            canvas.drawCircle(54f, 406f, 4f, candelero)
            oval.set(54f - 19f, 415f - 4.5f, 54f + 19f, 415f + 4.5f)
            canvas.drawOval(oval, candelero)
            canvas.drawOval(oval, trazo)
            oval.set(54f - 26f, 386f - 5f, 54f + 26f, 386f + 5f)
            canvas.drawOval(oval, candelero)
            canvas.drawOval(oval, trazo)

            vela(canvas, 54f, top, 385f, 22f, ap)
            fill.tint(GOTA, ap)
            for (j in 0..3) rr(canvas, 54f + GOTA_LAT_DX[j] - 2f, top, 4f, min(GOTA_LAT_LEN[j], bot - top - 2f), 2f, fill)
            trazo.strokeWidth = 1.4f
            trazo.tint(MECHA, ap)
            canvas.drawLine(54f, top - 1f, 54.8f, top - 7f, trazo)
            val fl = 1f + 0.07f * ruido(t * 6.3f)
            val lad = 2.2f * ruido(t * 1.9f)
            halo(canvas, haloVela, 54f, top - 18f, 38f, 0.32f * (1f - m) * ap)
            llama(canvas, 54f, top - 4f, 32f * fl, lad, (1f - m) * ap)
            if (s.muted) {
                humo(canvas, humoLat, 54.8f, top - 7f, t, 0, m * (0.55f * exp(-s.sinceChangeSec * 0.6f) + 0.2f), 70f, ap)
            }

            val n = num(s.index)
            numeroLat.shader = if (s.muted) null else numLatShader
            numeroLat.tint(if (s.muted) APAGADO else ORO, ap)
            fitText(numeroLat, n, 0, n.length, 30f, 92f)
            canvas.drawText(n, 54f, 50f, numeroLat)
            trazo.strokeWidth = 1f
            trazo.tint(ORO_50, ap)
            canvas.drawLine(28f, 62.5f, 80f, 62.5f, trazo)
            fill.tint(ORO_CLARO, ap)
            rombo(canvas, 54f, 62.5f, 2.6f)
            if (m > 0f) {
                silencioLat.tint(ORO, 0.8f * m * ap)
                canvas.drawText("SILENCIO", 54f, 80f, silencioLat)
            }
        }
        canvas.restore()
    }

    /** Empapelado de rombos casi invisible (un solo trazo: los cruces no se marcan más). */
    private fun enrejado(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        path.reset()
        var k = -h
        while (k < w + h) {
            path.moveTo(x + k, y)
            path.lineTo(x + k + h, y + h)
            path.moveTo(x + k, y + h)
            path.lineTo(x + k + h, y)
            k += 22f
        }
        trazo.strokeWidth = 1f
        trazo.color = ENREJADO
        canvas.drawPath(path, trazo)
    }

    /** Marco doble dorado con rombos y cuartos de círculo en las esquinas. */
    private fun filigrana(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        trazo.strokeWidth = 1f
        trazo.color = ORO_45
        canvas.drawRect(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, trazo)
        trazo.color = ORO_18
        canvas.drawRect(x + 4.5f, y + 4.5f, x + w - 4.5f, y + h - 4.5f, trazo)
        esquina(canvas, x + 4.5f, y + 4.5f, 0f)
        esquina(canvas, x + w - 4.5f, y + 4.5f, 90f)
        esquina(canvas, x + 4.5f, y + h - 4.5f, -90f)
        esquina(canvas, x + w - 4.5f, y + h - 4.5f, 180f)
    }

    private fun esquina(canvas: Canvas, cx: Float, cy: Float, desde: Float) {
        fill.color = ORO_CLARO_70
        rombo(canvas, cx, cy, 3f)
        trazo.color = ORO_50
        oval.set(cx - 11f, cy - 11f, cx + 11f, cy + 11f)
        canvas.drawArc(oval, desde, 90f, false, trazo)
    }

    /** Cuerpo de cera con su tapa clara. */
    private fun vela(canvas: Canvas, cx: Float, top: Float, base: Float, ancho: Float, a: Float) {
        matriz.setScale(ancho, 1f)
        matriz.postTranslate(cx - ancho / 2f, 0f)
        ceraShader.setLocalMatrix(matriz)
        cera.alpha = (255f * a).toInt()
        canvas.drawRect(cx - ancho / 2f, top, cx + ancho / 2f, base, cera)
        val ry = max(1.4f, ancho * 0.14f)
        oval.set(cx - ancho / 2f, top - ry, cx + ancho / 2f, top + ry)
        fill.tint(TAPA, a)
        canvas.drawOval(oval, fill)
    }

    /** Llama de vela: cuerpo con degradé, centro claro y un toque azul en la base. */
    private fun llama(canvas: Canvas, x: Float, yb: Float, h: Float, ladeo: Float, a: Float) {
        if (h < 1f || a <= 0f) return
        val w = h * 0.3f
        matriz.setScale(1f, -h)
        matriz.postTranslate(0f, yb)
        llamaShader.setLocalMatrix(matriz)
        llamaExt.alpha = (255f * clamp01(a)).toInt()
        path.reset()
        path.moveTo(x, yb)
        path.cubicTo(x - w * 1.1f, yb, x - w * 0.7f + ladeo * 0.4f, yb - h * 0.55f, x + ladeo, yb - h)
        path.cubicTo(x + w * 0.7f + ladeo * 0.4f, yb - h * 0.55f, x + w * 1.1f, yb, x, yb)
        canvas.drawPath(path, llamaExt)

        val hi = h * 0.55f
        val wi = w * 0.55f
        val yi = yb - 0.5f
        path.reset()
        path.moveTo(x, yi)
        path.cubicTo(x - wi * 1.1f, yi, x - wi * 0.7f + ladeo * 0.25f, yb - hi * 0.55f, x + ladeo * 0.5f, yb - hi)
        path.cubicTo(x + wi * 0.7f + ladeo * 0.25f, yb - hi * 0.55f, x + wi * 1.1f, yi, x, yi)
        fill.tint(LLAMA_CENTRO, a)
        canvas.drawPath(path, fill)

        oval.set(x - w * 0.45f, yb - 1f - h * 0.08f, x + w * 0.45f, yb - 1f + h * 0.08f)
        fill.tint(LLAMA_AZUL, a)
        canvas.drawOval(oval, fill)
    }

    /** Hilo de humo que ondula al subir desde la mecha apagada. */
    private fun humo(canvas: Canvas, p: Paint, x: Float, y: Float, t: Float, i: Int, a: Float, alto: Float, ap: Float) {
        if (a <= 0.01f) return
        path.reset()
        path.moveTo(0f, 0f)
        for (k in 1..14) path.lineTo(sin(k * 0.55f - t * 2.2f + i * 1.7f) * k * 0.38f, -k * alto / 14f)
        p.alpha = (255f * min(1f, 0.9f * a) * clamp01(ap)).toInt()
        canvas.save()
        canvas.translate(x, y)
        canvas.drawPath(path, p)
        canvas.restore()
    }

    /** Halo radial de radio [r] con alfa [a] (ya multiplicado por la aparición). */
    private fun halo(canvas: Canvas, p: Paint, cx: Float, cy: Float, r: Float, a: Float) {
        if (a <= 0f) return
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(r, r)
        p.alpha = (255f * clamp01(a)).toInt()
        canvas.drawRect(-1f, -1f, 1f, 1f, p)
        canvas.restore()
    }

    /** Brillo sin desenfoque: tres trazos superpuestos, del más ancho y tenue al más fino. */
    private fun brillo(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, color: Int, ancho: Float, a: Float) {
        redondo.color = color
        redondo.strokeWidth = ancho * 4.5f
        redondo.alpha = (255f * 0.06f * clamp01(a)).toInt()
        canvas.drawLine(x0, y0, x1, y1, redondo)
        redondo.strokeWidth = ancho * 2.4f
        redondo.alpha = (255f * 0.14f * clamp01(a)).toInt()
        canvas.drawLine(x0, y0, x1, y1, redondo)
        redondo.strokeWidth = ancho
        redondo.alpha = (255f * clamp01(a)).toInt()
        canvas.drawLine(x0, y0, x1, y1, redondo)
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

    /** Texto con letterSpacing alineado como el mockup (Android suma medio espacio antes de la primera letra). */
    private fun espaciado(canvas: Canvas, text: String, x: Float, y: Float, p: Paint) {
        val esp = p.letterSpacing * p.textSize
        val dx = when (p.textAlign) {
            Paint.Align.LEFT -> -esp / 2f
            Paint.Align.RIGHT -> esp / 2f
            else -> 0f
        }
        canvas.drawText(text, x + dx, y, p)
    }

    /** Copia msg[0, end) en mayúsculas al buffer fijo [mayus]; devuelve cuántas letras copió. */
    private fun mayusculas(msg: String, end: Int): Int {
        val n = min(end, mayus.size)
        for (i in 0 until n) mayus[i] = msg[i].uppercaseChar()
        return n
    }

    private fun humoPaint(alto: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        strokeCap = Paint.Cap.ROUND
        shader = LinearGradient(
            0f, 0f, 0f, -alto, Color.rgb(225, 215, 205), Color.argb(0, 215, 205, 195), Shader.TileMode.CLAMP,
        )
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

    /** Silencio suave: 0 → 1 en 0,45 s desde el último cambio. */
    private fun silencio(s: OverlayState) = if (s.muted) easeOut(clamp01(s.sinceChangeSec / 0.45f)) else 0f
    private fun num(i: Int) = if (i in 0..100) NUM[i] else i.toString()
    private fun conAlfa(rgb: Int, a: Float) = ((a * 255f + 0.5f).toInt() shl 24) or (rgb and 0x00FFFFFF)
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
