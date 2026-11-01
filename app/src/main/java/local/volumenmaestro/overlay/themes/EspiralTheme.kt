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
import android.os.Build
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Colina espiral: luna enorme con murciélagos, rayado finito de traje y la colina que se
 * enrosca adelante (el rizo se desenrolla al aparecer). Barra de rayas que se pinta de marfil
 * con un moño de murciélago en la punta. En el lateral, la misma barra parada.
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object EspiralTheme : Theme {
    override val id = "espiral"
    override val name = "Colina espiral"
    override val description = "Luna enorme, rayas finitas y la colina que se enrosca; un moño de murciélago marca tu nivel."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.45f
    private const val TAU = 6.2831855f
    private const val RAD_A_GRADOS = 57.29578f

    private const val BASE = 0xFF0B0B0E.toInt()
    private const val ALTO = 0xFF15151A.toInt()
    private const val BAJO = 0xFF08080A.toInt()
    private const val MARFIL = 0xFFEFE9DA.toInt()
    private const val BORDE = 0xFF9A9488.toInt()
    private const val GRIS = 0xFF67646D.toInt()
    private const val APAGADO = 0xFF2C2B32.toInt()
    private const val SOMBRA = 0xFF0E0E12.toInt()

    private val RAYA = conAlfa(MARFIL, 0.035f)
    private val MARCO = conAlfa(MARFIL, 0.22f)
    private val MARFIL_25 = conAlfa(MARFIL, 0.25f)
    private val MARFIL_40 = conAlfa(MARFIL, 0.4f)
    private val MARFIL_60 = conAlfa(MARFIL, 0.6f)
    private val MARFIL_70 = conAlfa(MARFIL, 0.7f)
    private val MARFIL_72 = conAlfa(MARFIL, 0.72f)
    private val MARFIL_75 = conAlfa(MARFIL, 0.75f)
    private val CRATER = Color.argb(26, 120, 112, 98)

    private val NUM = Array(101) { it.toString() }

    // Cráteres de la luna: desplazamiento y radio relativos al radio.
    private val CRATER_X = floatArrayOf(0.28f, -0.3f, 0.12f)
    private val CRATER_Y = floatArrayOf(-0.2f, 0.25f, 0.42f)
    private val CRATER_R = floatArrayOf(0.22f, 0.16f, 0.12f)
    // Festón del borde de afuera de cada ala del moño.
    private val MONIO_X = floatArrayOf(0.86f, 0.8f, 0.55f)
    private val MONIO_Y = floatArrayOf(-0.12f, 0.24f, 0.5f)

    // El rizo de la colina: espiral logarítmica fija y el grosor de cada tramo (se afina hacia el centro).
    private val rizoInf = espiral(150f, 56f, 178f, 98f, 2.1f, 72, 2.5f)
    private val rizoInfAncho = FloatArray(72) { lerp(8f, 1.2f, it / 72f) }
    private val rizoLat = espiral(50f, 378f, 62f, 394f, 1.9f, 54, 1.4f)
    private val rizoLatAncho = FloatArray(54) { lerp(4.5f, 1f, it / 54f) }

    private val colinaInf = Path().apply {
        moveTo(30f, 150f)
        lineTo(30f, 132f)
        quadTo(100f, 104f, 178f, 98f)
        quadTo(236f, 100f, 282f, 150f)
        close()
    }
    private val colinaInfBorde = Path().apply {
        moveTo(30f, 132f)
        quadTo(100f, 104f, 178f, 98f)
        quadTo(236f, 100f, 282f, 150f)
    }
    private val colinaLat = Path().apply {
        moveTo(0f, 430f)
        lineTo(0f, 412f)
        quadTo(28f, 396f, 62f, 394f)
        quadTo(90f, 394f, 108f, 412f)
        lineTo(108f, 430f)
        close()
    }
    private val colinaLatBorde = Path().apply {
        moveTo(0f, 412f)
        quadTo(28f, 396f, 62f, 394f)
        quadTo(90f, 394f, 108f, 412f)
    }

    private val rect = RectF()
    private val path = Path()
    private val clip = Path()
    private val matriz = Matrix()
    private val mayus = CharArray(80)

    private val placa = Paint().apply { color = BASE }
    private val fondoInf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, BH, ALTO, BAJO, Shader.TileMode.CLAMP)
    }
    private val fondoLat = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, SH, ALTO, BAJO, Shader.TileMode.CLAMP)
    }
    /** Halo de la luna: radio 1 en el origen, se traslada y escala. */
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f, MARFIL, MARFIL and 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    /** Disco de la luna con el brillo corrido arriba a la izquierda (radio 1, se ubica con [matriz]). */
    private val discoShader: Shader = if (Build.VERSION.SDK_INT >= 31) {
        RadialGradient(
            -0.3f, -0.35f, 0.1f, 0f, 0f, 1f,
            longArrayOf(Color.pack(0xFFF8F4EA.toInt()), Color.pack(0xFFD4CDBB.toInt())),
            null, Shader.TileMode.CLAMP,
        )
    } else {
        RadialGradient(
            -0.3f, -0.35f, 1.3f, intArrayOf(0xFFF8F4EA.toInt(), 0xFFD4CDBB.toInt()),
            floatArrayOf(0.08f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val disco = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = discoShader }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val redondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val monioTrazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
        strokeJoin = Paint.Join.ROUND
    }
    private val serifItalica = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    private val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textSize = 15f
        letterSpacing = 6f / 15f
    }
    private val cabecera = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.CENTER
        textSize = 15f
        letterSpacing = 5f / 15f
    }
    private val silencioLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.SERIF
        textAlign = Paint.Align.CENTER
        textSize = 13f
        letterSpacing = 4f / 13f
    }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifItalica
        textAlign = Paint.Align.RIGHT
    }
    private val numeroLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifItalica
        textAlign = Paint.Align.CENTER
    }
    private val cuerpo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifItalica
        textAlign = Paint.Align.CENTER
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
        rr(canvas, rx, ry, rw, rh, lerp(4f, 16f, p), fondoInf)
        // Rayado de traje.
        fill.color = RAYA
        for (i in 0 until 69) {
            val x = 7f + i * 14f
            canvas.drawRect(x, 0f, x + 1f, BH, fill)
        }
        trazo.strokeWidth = 1f
        trazo.color = MARCO
        rr(canvas, rx + 7.5f, ry + 7.5f, rw - 15f, rh - 15f, lerp(2f, 10f, p), trazo)

        if (ap > 0f) {
            val t = s.timeSec
            val m = silencio(s)
            luna(canvas, 150f, 66f + (1f - ap) * 18f, 52f, m, ap)
            for (i in 0..1) {
                val fase = (t * 0.05f + i * 0.47f) % 1f
                murcielago(
                    canvas, lerp(70f, 245f, fase), 34f + i * 26f + sin(t * 0.8f + i * 2f) * 5f,
                    6f - i, sin(t * 9f + i * 1.7f), ap,
                )
            }
            fill.tint(BASE, ap)
            canvas.drawPath(colinaInf, fill)
            trazo.strokeWidth = 1.4f
            trazo.tint(BORDE, ap)
            canvas.drawPath(colinaInfBorde, trazo)
            rizo(canvas, rizoInf, rizoInfAncho, ap * 1.15f, ap)

            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, t, ap) else drawVolume(canvas, s, m, ap)
        }
        canvas.restore()
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, m: Float, ap: Float) {
        val t = s.timeSec
        val vf = clamp01(s.level) * ap
        etiqueta.tint(MARFIL_72, ap)
        espaciado(canvas, if (s.muted) "SILENCIO" else "VOLUMEN", 306f, 44f, etiqueta)

        // Barra de rayas finas: apagadas y, recortadas hasta el nivel, en marfil.
        val x0 = 306f
        val x1 = 772f
        val y = 62f
        val bh = 30f
        val fw = (x1 - x0) * vf
        fill.tint(APAGADO, ap)
        rayas(canvas, x0, y, bh)
        if (fw > 0.5f) {
            canvas.save()
            canvas.clipRect(x0, y, x0 + fw, y + bh)
            fill.tint(MARFIL, ap)
            rayas(canvas, x0, y, bh)
            canvas.restore()
        }
        trazo.strokeWidth = 1f
        trazo.tint(MARFIL_40, ap)
        canvas.drawLine(x0 - 10f, y - 7.5f, x1 + 10f, y - 7.5f, trazo)
        canvas.drawLine(x0 - 10f, y + bh + 7.5f, x1 + 10f, y + bh + 7.5f, trazo)
        fill.tint(MARFIL_60, ap)
        rombo(canvas, x0 - 12f, y - 7.5f, 2.6f)
        rombo(canvas, x1 + 12f, y - 7.5f, 2.6f)
        rombo(canvas, x0 - 12f, y + bh + 7.5f, 2.6f)
        rombo(canvas, x1 + 12f, y + bh + 7.5f, 2.6f)
        for (k in 0..10) {
            val xx = x0 + (x1 - x0) * k / 10f + 0.5f
            trazo.tint(if (k / 10f <= vf + 1e-3f) MARFIL_75 else MARFIL_25, ap)
            canvas.drawLine(xx, y + bh + 8f, xx, y + bh + (if (k % 5 == 0) 15f else 12f), trazo)
        }

        // El moño cierra las alas en silencio y se sacude un toque al cambiar.
        val abre = lerp(1f, 0.3f, m) * (1f + 0.04f * sin(t * 2.4f))
        val since = s.sinceChangeSec
        val rot = if (s.muted) 0f else 0.2f * exp(-since * 5f) * sin(since * 24f)
        monio(canvas, x0 + fw, y + bh / 2f, 22f, abre, rot, ap)

        val n = num(s.index)
        numero.tint(if (s.muted) GRIS else MARFIL, ap)
        fitText(numero, n, 0, n.length, 80f, 124f)
        canvas.drawText(n, 914f, 114f, numero)
    }

    /** "Volumen Maestro: ENCENDIDO" → moño, rótulo chico y espaciado, y el valor en itálica grande. */
    private fun drawMessage(canvas: Canvas, msg: String, t: Float, ap: Float) {
        val cx = 600f
        monio(canvas, cx, 30f, 15f, 1f + 0.04f * sin(t * 2.4f), 0f, ap)
        val sep = msg.indexOf(": ")
        val bodyStart = if (sep > 0) sep + 2 else 0
        if (sep > 0) {
            val n = mayusculas(msg, sep)
            cabecera.tint(MARFIL_72, ap)
            canvas.drawText(mayus, 0, n, cx, 66f, cabecera)
        }
        cuerpo.tint(MARFIL, ap)
        fitText(cuerpo, msg, bodyStart, msg.length, 46f, 560f)
        canvas.drawText(msg, bodyStart, msg.length, cx, if (sep > 0) 118f else 104f, cuerpo)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / SW, h / SH)
        canvas.clipRect(0f, 0f, SW, SH)
        canvas.drawRect(0f, 0f, SW, SH, placa)
        canvas.drawRect(0f, 0f, SW, SH, fondoLat)
        fill.color = RAYA
        for (i in 0 until 9) {
            val x = 6f + i * 12f
            canvas.drawRect(x, 0f, x + 1f, SH, fill)
        }
        trazo.strokeWidth = 1f
        trazo.color = MARCO
        rr(canvas, 6.5f, 6.5f, 95f, 417f, 8f, trazo)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        if (ap > 0f) {
            val t = s.timeSec
            val m = silencio(s)
            val vf = clamp01(s.level) * ap
            luna(canvas, 54f, 56f + (1f - ap) * 10f, 28f, m, ap)
            murcielago(canvas, lerp(16f, 92f, (t * 0.06f) % 1f), 46f + sin(t * 0.9f) * 4f, 4.5f, sin(t * 9f), ap)
            val n = num(s.index)
            numeroLat.tint(if (s.muted) GRIS else MARFIL, ap)
            fitText(numeroLat, n, 0, n.length, 30f, 92f)
            canvas.drawText(n, 54f, 124f, numeroLat)

            // Barra rayada parada: crece desde abajo.
            val x0 = 41f
            val bw = 26f
            val top = 146f
            val bot = 340f
            val fh = (bot - top) * vf
            fill.tint(APAGADO, ap)
            rayasLat(canvas, x0, bw, top, bot)
            if (fh > 0.5f) {
                canvas.save()
                canvas.clipRect(x0, bot - fh, x0 + bw, bot)
                fill.tint(MARFIL, ap)
                rayasLat(canvas, x0, bw, top, bot)
                canvas.restore()
            }
            trazo.strokeWidth = 1f
            trazo.tint(MARFIL_40, ap)
            canvas.drawLine(33.5f, top - 8f, 33.5f, bot + 8f, trazo)
            canvas.drawLine(74.5f, top - 8f, 74.5f, bot + 8f, trazo)
            for (k in 0..10) {
                val yy = bot - (bot - top) * k / 10f + 0.5f
                trazo.tint(if (k / 10f <= vf + 1e-3f) MARFIL_75 else MARFIL_25, ap)
                canvas.drawLine(26f, yy, 33f, yy, trazo)
            }
            if (m > 0f) {
                canvas.save()
                canvas.translate(54f, (top + bot) / 2f)
                canvas.rotate(-90f)
                fill.tint(BASE, ap * m)
                canvas.drawRect(-46f, -9f, 46f, 9f, fill)
                silencioLat.tint(MARFIL_70, ap * m)
                canvas.drawText("SILENCIO", 0f, 5f, silencioLat)
                canvas.restore()
            }
            val since = s.sinceChangeSec
            val rot = if (s.muted) 0f else 0.2f * exp(-since * 5f) * sin(since * 24f)
            monio(canvas, 54f, bot - fh, 17f, lerp(1f, 0.3f, m) * (1f + 0.04f * sin(t * 2.4f)), rot, ap)

            fill.tint(BASE, ap)
            canvas.drawPath(colinaLat, fill)
            trazo.strokeWidth = 1.3f
            trazo.tint(BORDE, ap)
            canvas.drawPath(colinaLatBorde, trazo)
            rizo(canvas, rizoLat, rizoLatAncho, ap * 1.15f, ap)
        }
        canvas.restore()
    }

    /** Luna con halo y cráteres; en silencio una sombra la tapa hasta dejar una medialuna. */
    private fun luna(canvas: Canvas, cx: Float, cy: Float, r: Float, m: Float, a: Float) {
        val hr = r * 2.4f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(hr, hr)
        halo.alpha = (255f * 0.17f * (1f - 0.75f * m) * a).toInt()
        canvas.drawRect(-1f, -1f, 1f, 1f, halo)
        canvas.restore()

        matriz.setScale(r, r)
        matriz.postTranslate(cx, cy)
        discoShader.setLocalMatrix(matriz)
        disco.alpha = (255f * a).toInt()
        canvas.drawCircle(cx, cy, r, disco)
        fill.tint(CRATER, a)
        for (i in 0..2) canvas.drawCircle(cx + CRATER_X[i] * r, cy + CRATER_Y[i] * r, CRATER_R[i] * r, fill)

        if (m > 0f) {
            canvas.save()
            clip.reset()
            clip.addCircle(cx, cy, r + 0.5f, Path.Direction.CW)
            canvas.clipPath(clip)
            fill.tint(SOMBRA, a)
            canvas.drawCircle(cx + lerp(2f * r + 3f, r * 0.28f, m), cy - r * 0.05f * m, r * 1.03f, fill)
            canvas.restore()
        }
    }

    /** Murciélago diminuto (oscuro: solo se ve cuando cruza la luna). */
    private fun murcielago(canvas: Canvas, x: Float, y: Float, sz: Float, aleteo: Float, a: Float) {
        val al = aleteo * sz * 0.55f
        path.reset()
        for (lado in 0..1) {
            val k = if (lado == 0) -1f else 1f
            path.moveTo(0f, -sz * 0.12f)
            path.quadTo(k * sz * 0.5f, -al - sz * 0.25f, k * sz, -al)
            path.quadTo(k * sz * 0.72f, -al * 0.3f + sz * 0.05f, k * sz * 0.62f, sz * 0.2f)
            path.quadTo(k * sz * 0.42f, sz * 0.02f, k * sz * 0.3f, sz * 0.28f)
            path.quadTo(k * sz * 0.15f, sz * 0.1f, 0f, sz * 0.3f)
            path.close()
        }
        canvas.save()
        canvas.translate(x, y)
        fill.tint(BASE, a)
        canvas.drawPath(path, fill)
        canvas.restore()
    }

    /** Moño de murciélago: dos alas festoneadas y un nudo; [abre] achica las alas a lo ancho. */
    private fun monio(canvas: Canvas, x: Float, y: Float, sz: Float, abre: Float, rot: Float, a: Float) {
        path.reset()
        for (lado in 0..1) {
            val k = (if (lado == 0) -1f else 1f) * abre
            path.moveTo(0f, -sz * 0.16f)
            path.quadTo(k * sz * 0.5f, -sz * 0.66f, k * sz, -sz * 0.52f)
            var px = k * sz
            var py = -sz * 0.52f
            for (j in 0..2) {
                val qx = k * sz * MONIO_X[j]
                val qy = sz * MONIO_Y[j]
                path.quadTo((px + qx) / 2f - k * sz * 0.17f, (py + qy) / 2f, qx, qy)
                px = qx
                py = qy
            }
            path.quadTo(k * sz * 0.25f, sz * 0.3f, 0f, sz * 0.16f)
            path.close()
        }
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rot * RAD_A_GRADOS)
        fill.tint(BASE, a)
        monioTrazo.tint(MARFIL, a)
        canvas.drawPath(path, fill)
        canvas.drawPath(path, monioTrazo)
        rect.set(-sz * 0.13f, -sz * 0.21f, sz * 0.13f, sz * 0.21f)
        canvas.drawRoundRect(rect, sz * 0.08f, sz * 0.08f, fill)
        canvas.drawRoundRect(rect, sz * 0.08f, sz * 0.08f, monioTrazo)
        canvas.restore()
    }

    /**
     * El rizo de la colina hasta [prog]: tramos rectos con grosor propio, dibujados dos veces
     * (primero el borde claro, más ancho, y encima el relleno oscuro).
     */
    private fun rizo(canvas: Canvas, pts: FloatArray, anchos: FloatArray, prog: Float, a: Float) {
        val n = floor(anchos.size * clamp01(prog)).toInt()
        if (n < 1) return
        for (pasada in 0..1) {
            redondo.tint(if (pasada == 0) BORDE else BASE, a)
            val extra = if (pasada == 0) 2.6f else 0f
            for (i in 0 until n) {
                redondo.strokeWidth = anchos[i] + extra
                canvas.drawLine(pts[i * 2], pts[i * 2 + 1], pts[i * 2 + 2], pts[i * 2 + 3], redondo)
            }
        }
    }

    /** Rayas verticales de la barra de abajo (cada 6 px, de 2,4 de ancho), con el color de [fill]. */
    private fun rayas(canvas: Canvas, x0: Float, y: Float, h: Float) {
        for (i in 0 until 78) {
            val x = x0 + i * 6f
            canvas.drawRect(x, y, x + 2.4f, y + h, fill)
        }
    }

    /** Rayas horizontales de la barra del lateral, de abajo hacia arriba. */
    private fun rayasLat(canvas: Canvas, x0: Float, w: Float, top: Float, bot: Float) {
        var k = 0
        while (true) {
            val y = bot - 2.4f - k * 6f
            if (y <= top) break
            canvas.drawRect(x0, y, x0 + w, y + 2.4f, fill)
            k++
        }
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

    private fun espiral(cx: Float, cy: Float, sx: Float, sy: Float, vueltas: Float, n: Int, rFin: Float): FloatArray {
        val r0 = hypot(sx - cx, sy - cy)
        val a0 = atan2(sy - cy, sx - cx)
        val pts = FloatArray((n + 1) * 2)
        for (i in 0..n) {
            val u = i / n.toFloat()
            val a = a0 - u * vueltas * TAU
            val r = r0 * (rFin / r0).pow(u)
            pts[i * 2] = cx + cos(a) * r
            pts[i * 2 + 1] = cy + sin(a) * r
        }
        return pts
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
}
