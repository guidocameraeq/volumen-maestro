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
import kotlin.math.pow
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Hilo de seda (art déco): telaraña en abanico con gotas de rocío que brillan de a una y un
 * hilo que la arañita recorre hasta el nivel; lo recorrido queda plateado. Niebla muy suave.
 * En el lateral, la arañita cuelga de su hilo a la altura del volumen (arriba = fuerte).
 * Coordenadas locales: panel inferior 960x150 y lateral 108x430.
 */
object TelaranaTheme : Theme {
    override val id = "telarana"
    override val name = "Hilo de seda"
    override val description =
        "Telaraña art déco en plata y una arañita que viaja por su hilo hasta tu nivel; en silencio encoge las patas."

    private const val BW = 960f
    private const val BH = 150f
    private const val SW = 108f
    private const val SH = 430f
    private const val ENTRADA = 0.5f
    private const val PI = 3.1415927f

    private const val BASE = 0xFF07090D.toInt()
    private const val FONDO_A = 0xFF0F141C.toInt()
    private const val FONDO_B = 0xFF05070A.toInt()
    private const val PLATA = 0xFFC9D1DB.toInt()
    private const val PLATA_CLARA = 0xFFEEF2F6.toInt()
    private const val PLATA_BAJA = 0xFF9AA5B3.toInt()
    private const val CARMIN = 0xFFB3202F.toInt()
    private const val APAGADO = 0xFF4B5462.toInt()
    private const val CUERPO = 0xFF0A0C10.toInt()

    private val PLATA_12 = conAlfa(PLATA, 0.12f)
    private val PLATA_20 = conAlfa(PLATA, 0.2f)
    private val PLATA_22 = conAlfa(PLATA, 0.22f)
    private val PLATA_25 = conAlfa(PLATA, 0.25f)
    private val PLATA_30 = conAlfa(PLATA, 0.3f)
    private val PLATA_50 = conAlfa(PLATA, 0.5f)
    private val PLATA_60 = conAlfa(PLATA, 0.6f)
    private val PLATA_75 = conAlfa(PLATA, 0.75f)
    private val CLARA_75 = conAlfa(PLATA_CLARA, 0.75f)
    private val CLARA_80 = conAlfa(PLATA_CLARA, 0.8f)

    private val NUM = Array(101) { it.toString() }

    // Gotas de rocío fijas: anillo, rayo y fase del brillo.
    private val ROCIO_ANILLO = intArrayOf(2, 4, 5, 3, 1, 5)
    private val ROCIO_RAYO = intArrayOf(3, 1, 5, 6, 2, 2)
    private val ROCIO_FASE = floatArrayOf(0.3f, 2.1f, 4.0f, 5.3f, 1.4f, 3.2f)
    private val ANILLOS_INF = floatArrayOf(16f, 30f, 46f, 64f, 84f, 106f)
    private val ANILLOS_LAT = floatArrayOf(10f, 20f, 31f, 42f)
    private val PATA_ANG = floatArrayOf(-1.05f, -0.35f, 0.35f, 1.05f)

    // El hilo sale del anillo de afuera de la tela de la esquina (origen 40,12, a 30°).
    private val HILO_X = 40f + 106f * cos(PI / 6f)
    private val HILO_Y = 12f + 106f * sin(PI / 6f)

    private val oval = RectF()
    private val path = Path()
    private val mayus = CharArray(80)
    private val marcoLat = Path().also { chaflan(it, 6.5f, 6.5f, 95f, 417f, 10f) }

    private val placa = Paint().apply { color = BASE }
    private val fondoInf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, BH, FONDO_A, FONDO_B, Shader.TileMode.CLAMP)
    }
    private val fondoLat = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, SH, FONDO_A, FONDO_B, Shader.TileMode.CLAMP)
    }
    /** Banco de niebla: radio 1 en el origen, se estira a una elipse. */
    private val bruma = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f, Color.argb(15, 180, 195, 215), Color.argb(0, 180, 195, 215), Shader.TileMode.CLAMP,
        )
    }
    private val numInfShader = LinearGradient(0f, 54f, 0f, 120f, PLATA_CLARA, PLATA_BAJA, Shader.TileMode.CLAMP)
    private val numLatShader = LinearGradient(0f, 392f, 0f, 416f, PLATA_CLARA, PLATA_BAJA, Shader.TileMode.CLAMP)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val redondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val pata = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.1f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    // Condensada fina; si la TV no tiene el peso 300 usa la condensada normal (nunca otra familia).
    private val fina = Typeface.create(Typeface.create("sans-serif-condensed", Typeface.NORMAL), 300, false)
    private val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fina
        textSize = 15f
        letterSpacing = 7f / 15f
    }
    private val cabecera = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fina
        textAlign = Paint.Align.CENTER
        textSize = 16f
        letterSpacing = 8f / 16f
    }
    private val silencioLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fina
        textAlign = Paint.Align.CENTER
        textSize = 12f
        letterSpacing = 3f / 12f
    }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fina
        textAlign = Paint.Align.RIGHT
    }
    private val numeroLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fina
        textAlign = Paint.Align.CENTER
    }
    private val cuerpo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fina
        textAlign = Paint.Align.CENTER
        shader = LinearGradient(0f, 80f, 0f, 118f, PLATA_CLARA, PLATA_BAJA, Shader.TileMode.CLAMP)
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
        chaflan(path, rx, ry, rw, rh, lerp(4f, 14f, p))
        canvas.drawPath(path, fondoInf)
        trazo.strokeWidth = 1f
        trazo.color = PLATA_30
        chaflan(path, rx + 7.5f, ry + 7.5f, rw - 15f, rh - 15f, lerp(3f, 10f, p))
        canvas.drawPath(path, trazo)
        trazo.color = PLATA_12
        chaflan(path, rx + 11.5f, ry + 11.5f, rw - 23f, rh - 23f, lerp(2f, 8f, p))
        canvas.drawPath(path, trazo)
        niebla(canvas, s.timeSec, BW, 132f, 230f, 30f, 3)

        if (ap > 0f) {
            abanico(canvas, 40f, 12f, 0f, PI / 2f, 7, ANILLOS_INF, 0.34f, s.timeSec, ap * 1.2f, ap)
            val msg = s.message
            if (msg != null) drawMessage(canvas, msg, s.timeSec, ap) else drawVolume(canvas, s, ap)
        }
        canvas.restore()
    }

    private fun drawVolume(canvas: Canvas, s: OverlayState, ap: Float) {
        val t = s.timeSec
        val m = silencio(s)
        val vf = clamp01(s.level) * ap
        val since = s.sinceChangeSec
        etiqueta.tint(if (s.muted) CARMIN else PLATA_75, ap)
        espaciado(canvas, if (s.muted) "SILENCIO" else "VOLUMEN", 178f, 42f, etiqueta)

        // El hilo: lo recorrido brilla, lo que falta queda tenue; rebota un toque al cambiar.
        val xa = HILO_X
        val ya = HILO_Y
        val xb = 760f
        val xs = lerp(xa + 16f, xb - 14f, vf)
        val ys = ya + 7f + (if (s.muted) 0f else 5f * exp(-since * 3.5f) * sin(since * 12f))
        trazo.strokeWidth = 1f
        trazo.tint(PLATA_20, ap)
        canvas.drawLine(xs, ys, xb, ya, trazo)
        brillo(canvas, xa, ya, xs, ys, PLATA_CLARA, 1.7f, (1f - 0.75f * m) * ap)
        // Rocío sobre el tramo recorrido.
        var x = xa + 34f
        while (x < xs - 8f) {
            val yy = lerp(ya, ys, (x - xa) / max(1f, xs - xa))
            val br = max(0f, sin(t * 1.3f + x * 0.07f)).pow(8)
            fill.tint(PLATA_CLARA, (0.35f + 0.65f * br) * (1f - 0.7f * m) * ap)
            canvas.drawCircle(x, yy + 1.4f, 1.4f + 0.6f * br, fill)
            x += 46f
        }
        trazo.tint(PLATA_50, ap)
        canvas.drawLine(xb, ya, 786f, ya, trazo) // amarre al divisor
        val sw = 2f * sin(t * 1.4f) + (if (s.muted) 0f else 4f * exp(-since * 3f) * sin(since * 9f))
        trazo.tint(PLATA_CLARA, (0.8f - 0.5f * m) * ap)
        canvas.drawLine(xs, ys, xs + sw, ys + 12f, trazo)
        arania(canvas, xs + sw, ys + 12f, 1.05f, t, m, exp(-since * 4f), ap)

        // Reglita.
        val r0 = xa + 16f
        val r1 = xb - 14f
        trazo.tint(PLATA_22, ap)
        canvas.drawLine(r0, 130.5f, r1, 130.5f, trazo)
        for (k in 0..10) {
            val xx = lerp(r0, r1, k / 10f) + 0.5f
            trazo.tint(if (k / 10f <= vf + 1e-3f) CLARA_80 else PLATA_25, ap)
            canvas.drawLine(xx, 130f, xx, if (k % 5 == 0) 122f else 125f, trazo)
        }

        // Divisor y número.
        trazo.tint(PLATA_30, ap)
        canvas.drawLine(786.5f, 26f, 786.5f, 124f, trazo)
        fill.tint(PLATA_60, ap)
        rombo(canvas, 786.5f, 26f, 2.5f)
        rombo(canvas, 786.5f, 124f, 2.5f)
        val n = num(s.index)
        numero.shader = if (s.muted) null else numInfShader
        numero.tint(if (s.muted) APAGADO else PLATA_CLARA, ap)
        fitText(numero, n, 0, n.length, 90f, 118f)
        canvas.drawText(n, 914f, 120f, numero)
    }

    /** "Volumen Maestro: ENCENDIDO" → rótulo espaciado, el valor en letra fina y una arañita colgando. */
    private fun drawMessage(canvas: Canvas, msg: String, t: Float, ap: Float) {
        val cx = 540f
        val sep = msg.indexOf(": ")
        val bodyStart = if (sep > 0) sep + 2 else 0
        if (sep > 0) {
            val n = mayusculas(msg, sep)
            cabecera.tint(PLATA_75, ap)
            canvas.drawText(mayus, 0, n, cx, 58f, cabecera)
        }
        trazo.strokeWidth = 1f
        trazo.tint(PLATA_30, ap)
        canvas.drawLine(cx - 120f, 71.5f, cx + 120f, 71.5f, trazo)
        fill.tint(CLARA_80, ap)
        rombo(canvas, cx, 71.5f, 2.5f)
        cuerpo.alpha = (255f * ap).toInt()
        fitText(cuerpo, msg, bodyStart, msg.length, 50f, 560f)
        canvas.drawText(msg, bodyStart, msg.length, cx, if (sep > 0) 118f else 104f, cuerpo)

        val sw = 2f * sin(t * 1.4f)
        val ya = 50f + 4f * sin(t * 0.7f)
        trazo.tint(CLARA_75, ap)
        canvas.drawLine(868f, 8f, 868f + sw, ya, trazo)
        arania(canvas, 868f + sw, ya, 1f, t, 0f, 0f, ap)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        canvas.save()
        canvas.scale(w / SW, h / SH)
        canvas.clipRect(0f, 0f, SW, SH)
        canvas.drawRect(0f, 0f, SW, SH, placa)
        canvas.drawRect(0f, 0f, SW, SH, fondoLat)
        trazo.strokeWidth = 1f
        trazo.color = PLATA_30
        canvas.drawPath(marcoLat, trazo)
        niebla(canvas, s.timeSec, SW, 400f, 70f, 22f, 2)

        val vis = min(clamp01(s.timeSec / ENTRADA), s.appear)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        if (ap > 0f) {
            val t = s.timeSec
            val m = silencio(s)
            val v = clamp01(s.level)
            val since = s.sinceChangeSec
            abanico(canvas, 8f, 8f, 0f, PI / 2f, 5, ANILLOS_LAT, 0.32f, t, ap * 1.2f, ap)
            abanico(canvas, 100f, 8f, PI / 2f, PI, 5, ANILLOS_LAT, 0.32f, t + 2f, ap * 1.2f, ap)

            // Al aparecer baja desde arriba hasta la altura del volumen.
            val yMin = 74f
            val yMax = 318f
            val ys = lerp(16f, lerp(yMax, yMin, v), ap)
            val sw = 2.2f * sin(t * 1.2f) + (if (s.muted) 0f else 6f * exp(-since * 2.6f) * sin(since * 8f))
            trazo.strokeWidth = 1f
            for (k in 0..10) {
                val yy = lerp(yMax, yMin, k / 10f) + 12.5f
                trazo.tint(if (k / 10f <= v + 1e-3f) CLARA_75 else PLATA_22, ap)
                canvas.drawLine(90f, yy, if (k % 5 == 0) 99f else 96f, yy, trazo)
            }
            fill.tint(PLATA, ap)
            canvas.drawCircle(54f, 13f, 2f, fill)
            brillo(canvas, 54f, 14f, 54f + sw, ys, PLATA_CLARA, 1.2f, (1f - 0.7f * m) * ap)
            arania(canvas, 54f + sw, ys, 1.15f, t, m, exp(-since * 3f), ap)
            if (m > 0f) {
                silencioLat.tint(CARMIN, m * ap)
                canvas.drawText("SILENCIO", 54f, 384f, silencioLat)
            }
            val n = num(s.index)
            numeroLat.shader = if (s.muted) null else numLatShader
            numeroLat.tint(if (s.muted) APAGADO else PLATA_CLARA, ap)
            fitText(numeroLat, n, 0, n.length, 34f, 92f)
            canvas.drawText(n, 54f, 416f, numeroLat)
        }
        canvas.restore()
    }

    /** Telaraña en abanico desde (ox, oy): rayos, anillos curvados hacia adentro y gotas de rocío. */
    private fun abanico(
        canvas: Canvas, ox: Float, oy: Float, a0: Float, a1: Float, rayos: Int, anillos: FloatArray,
        alfa: Float, t: Float, prog: Float, ap: Float,
    ) {
        val lim = anillos[anillos.size - 1] * clamp01(prog)
        val paso = (a1 - a0) / (rayos - 1)
        trazo.strokeWidth = 1f
        trazo.tint(PLATA, alfa * ap)
        path.reset()
        for (k in 0 until rayos) {
            val a = a0 + paso * k
            path.moveTo(ox, oy)
            path.lineTo(ox + cos(a) * lim, oy + sin(a) * lim)
        }
        canvas.drawPath(path, trazo)
        for (r in anillos) {
            if (r > lim) break
            path.reset()
            for (k in 0 until rayos) {
                val a = a0 + paso * k
                val px = ox + cos(a) * r
                val py = oy + sin(a) * r
                if (k == 0) {
                    path.moveTo(px, py)
                } else {
                    val am = a - paso * 0.5f
                    path.quadTo(ox + cos(am) * r * 0.9f, oy + sin(am) * r * 0.9f, px, py)
                }
            }
            canvas.drawPath(path, trazo)
        }
        for (d in 0 until ROCIO_ANILLO.size) {
            val j = ROCIO_ANILLO[d]
            val k = ROCIO_RAYO[d]
            if (j >= anillos.size || k >= rayos || anillos[j] > lim) continue
            val r = anillos[j]
            val br = max(0f, sin(t * 1.1f + ROCIO_FASE[d])).pow(10)
            val a = a0 + paso * k
            fill.tint(PLATA_CLARA, (0.25f + 0.75f * br) * min(1f, alfa * 2.5f) * ap)
            canvas.drawCircle(ox + cos(a) * r, oy + sin(a) * r, 1.2f + br * 0.9f, fill)
        }
    }

    /** Arañita colgada de su hilo en (x, y); [encoge] repliega las patas y [mov] las agita. */
    private fun arania(canvas: Canvas, x: Float, y: Float, esc: Float, t: Float, encoge: Float, mov: Float, ap: Float) {
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(esc, esc)
        pata.tint(PLATA, ap)
        val l1 = 8.5f * lerp(1f, 0.55f, encoge)
        val l2 = 9f * lerp(1f, 0.35f, encoge)
        for (lado in 0..1) {
            val sg = if (lado == 0) -1f else 1f
            for (k in 0..3) {
                val a = PATA_ANG[k] + sin(t * 2.2f + k * 1.3f + sg) * (0.05f + 0.22f * mov)
                val rx = sg * 2.6f
                val ry = 16.5f + (k - 1.5f) * 1.6f
                val kx = rx + sg * cos(a) * l1
                val ky = ry + sin(a) * l1 - 3.2f
                val fx = kx + sg * cos(a) * l2 * 0.55f
                val fy = ky + (sin(a) * 0.55f + 0.8f) * l2
                path.reset()
                path.moveTo(rx, ry)
                path.lineTo(kx, ky)
                path.lineTo(fx, fy)
                canvas.drawPath(path, pata)
            }
        }
        fill.tint(CUERPO, ap)
        oval.set(-6f, 1f, 6f, 16f)
        canvas.drawOval(oval, fill)
        canvas.drawOval(oval, pata)
        canvas.drawCircle(0f, 19.5f, 3.8f, fill)
        canvas.drawCircle(0f, 19.5f, 3.8f, pata)
        // Reloj de arena de viuda negra.
        fill.tint(CARMIN, ap)
        path.reset()
        path.moveTo(-2f, 5.5f)
        path.lineTo(2f, 5.5f)
        path.lineTo(0f, 8.5f)
        path.close()
        path.moveTo(-2f, 11.5f)
        path.lineTo(2f, 11.5f)
        path.lineTo(0f, 8.5f)
        path.close()
        canvas.drawPath(path, fill)
        canvas.restore()
    }

    /** Bancos de niebla que pasan despacio (fórmula cerrada en t, sin estado). */
    private fun niebla(canvas: Canvas, t: Float, ancho: Float, y: Float, rx: Float, ry: Float, n: Int) {
        val ciclo = ancho + 2f * rx
        for (k in 0 until n) {
            val cx = ((t * 9f + k * ciclo / n) % ciclo) - rx
            canvas.save()
            canvas.translate(cx, y + sin(t * 0.4f + k) * 4f)
            canvas.scale(rx, ry)
            canvas.drawRect(-1f, -1f, 1f, 1f, bruma)
            canvas.restore()
        }
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

    /** Octógono de esquinas cortadas (el marco art déco). */
    private fun chaflan(p: Path, x: Float, y: Float, w: Float, h: Float, c: Float) {
        p.reset()
        p.moveTo(x + c, y)
        p.lineTo(x + w - c, y)
        p.lineTo(x + w, y + c)
        p.lineTo(x + w, y + h - c)
        p.lineTo(x + w - c, y + h)
        p.lineTo(x + c, y + h)
        p.lineTo(x, y + h - c)
        p.lineTo(x, y + c)
        p.close()
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

    private fun fitText(p: Paint, text: String, start: Int, end: Int, size: Float, maxW: Float) {
        p.textSize = size
        val tw = p.measureText(text, start, end)
        if (tw > maxW) p.textSize = size * maxW / tw
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
