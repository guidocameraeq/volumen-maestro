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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Sable de energía rojo. La hoja mide el volumen: se dispara con un destello al aparecer,
 * el halo titila, salta chispas con gravedad en cada cambio y en silencio se guarda con
 * un último chisporroteo. En el lateral, el mismo sable en vertical con un cristal que late.
 */
object SableTheme : Theme {
    override val id = "sable"
    override val name = "Sable láser rojo"
    override val description = "Una hoja de energía roja que se prende con un destello y crece con el volumen; en silencio se guarda."

    private const val ENTRADA = 0.40f
    // La hoja se dispara cuando el panel ya apareció y tarda DUR_ENCENDIDO en estirarse.
    private const val T_ENCENDIDO = 0.25f
    private const val DUR_ENCENDIDO = 0.22f
    // Al silenciar, el nivel baja a 0 y la hoja se retrae; a este tiempo salta el último chisporroteo.
    private const val T_GUARDADO = 0.28f
    private const val GRAVEDAD = 520f
    private const val LARGO_INF = 600f
    private const val LARGO_LAT = 295f
    // Emisor de la hoja (coordenadas locales de cada cartel).
    private const val EX = 165f
    private const val EY = 75f
    private const val LEX = 54f
    private const val LEY = 329f
    private const val TEXTO_X = 928f

    private const val PUNTA = 0
    private const val GUARDA = 1
    private const val PUNTA_LAT = 2
    private const val GUARDA_LAT = 3

    private const val TXT_VOLUMEN = "VOLUMEN"
    private const val TXT_SILENCIO = "SILENCIO"

    private val ROJO = 0xFFFF2A2A.toInt()
    private val ROJO_HALO = 0xFFFF1A1A.toInt()
    private val ROSA = 0xFFFF8A80.toInt()
    private val NUCLEO = 0xFFFFF3F0.toInt()
    private val CHISPA_INI = 0xFFFFF4E0.toInt()
    private val CHISPA_FIN = 0xFFFF3020.toInt()

    // Resplandor de la hoja: trazos superpuestos de ancho creciente y alfa decreciente (en vez de blur).
    private val haloAncho = floatArrayOf(8f, 6f, 4.2f, 2.6f, 1.8f)
    private val haloAlfa = floatArrayOf(0.045f, 0.07f, 0.12f, 0.26f, 0.42f)

    private val numeros = Array(101) { it.toString() }

    /** Números al azar fijos (semilla constante) para las chispas: mismo resultado en la vista previa y en la TV. */
    private val azar = FloatArray(512).also { a ->
        var x = 0x2545F491
        for (i in a.indices) {
            x = x xor (x shl 13)
            x = x xor (x ushr 17)
            x = x xor (x shl 5)
            a[i] = (x ushr 8) / 16777216f
        }
    }

    private val base = Paint().apply { color = 0xFF08080B.toInt() }
    private val placaInf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, 150f, 0xFF131319.toInt(), 0xFF060608.toInt(), Shader.TileMode.CLAMP)
    }
    private val placaLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, 430f, 0xFF131319.toInt(), 0xFF060608.toInt(), Shader.TileMode.CLAMP)
    }
    private val metal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, -18f, 0f, 18f,
            intArrayOf(0xFF8D949E.toInt(), 0xFFEEF1F4.toInt(), 0xFF9AA1AB.toInt(), 0xFF4B515A.toInt(), 0xFF22262C.toInt()),
            floatArrayOf(0f, 0.28f, 0.5f, 0.78f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    /** Brillo rojo circular de radio 100 centrado en (0,0); se ubica y escala con el canvas. */
    private val resplandor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 100f,
            intArrayOf(0xFFFF1E1E.toInt(), 0x59FF1E1E, 0x00FF1E1E),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    /** Destello blanco y rojo del encendido, radio 100 en (0,0). */
    private val destello = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 100f,
            intArrayOf(0xE6FFFFFF.toInt(), 0x80FF3C28.toInt(), 0x00FF0000),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x59FF3C3C
    }
    private val condensada: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    private val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = condensada
        textSize = 18f
        textAlign = Paint.Align.RIGHT
        letterSpacing = 0.12f
    }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = condensada
        textSize = 76f
        textAlign = Paint.Align.RIGHT
    }
    private val valor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = condensada
        textAlign = Paint.Align.RIGHT
    }

    private val emisor = Path().apply {
        moveTo(-18f, -18f)
        lineTo(0f, -23f)
        lineTo(0f, 23f)
        lineTo(-18f, 18f)
        close()
    }
    private val rombo = Path()
    private val panel = RectF()
    private val caja = RectF()

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val vis = visibilidad(s)
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val t = s.timeSec

        // Placa de seguridad: tapa el cartel del sistema siempre, también con appear = 0.
        canvas.drawRect(36f, 11f, 924f, 142f, base)
        val r = lerp(8f, 22f, p)
        panel.set(lerp(36f, 0f, p), lerp(11f, 0f, p), lerp(924f, w.toFloat(), p), lerp(142f, h.toFloat(), p))
        canvas.drawRoundRect(panel, r, r, placaInf)

        val msg = s.message
        val encendida = if (msg != null) !esApagado(msg) else !s.muted
        val enc = encendido(s)

        // Con mensaje, la hoja se estira hasta donde empieza el texto.
        var largoMax = LARGO_INF
        var inicioValor = 0
        var dosPuntos = -1
        if (msg != null) {
            dosPuntos = msg.indexOf(':')
            inicioValor = if (dosPuntos >= 0) dosPuntos + 1 else 0
            while (inicioValor < msg.length && msg[inicioValor] == ' ') inicioValor++
            valor.textSize = 50f
            var ancho = valor.measureText(msg, inicioValor, msg.length)
            if (ancho > 560f) {
                valor.textSize = 50f * 560f / ancho
                ancho = 560f
            }
            val anchoEtiqueta = if (dosPuntos > 0) etiqueta.measureText(msg, 0, dosPuntos) else 0f
            largoMax = (TEXTO_X - max(ancho, anchoEtiqueta) - 50f - EX).coerceIn(60f, LARGO_INF)
        }
        val largo = when {
            msg != null -> if (encendida) enc * largoMax else 0f
            else -> enc * clamp01(s.level) * LARGO_INF
        }

        // Reflejo rojo de la hoja sobre la placa.
        if (largo > 1f) {
            reflejo(canvas, EX + largo / 2f, EY, largo / 2f + 70f, 62f, 0.26f * (0.9f + 0.1f * ruido(t * 20f)))
        }
        caja.set(panel.left + 1f, panel.top + 1f, panel.right - 1f, panel.bottom - 1f)
        canvas.drawRoundRect(caja, r, r, borde)
        if (ap <= 0f) return

        val capa = if (ap < 1f) canvas.saveLayerAlpha(0f, 0f, w.toFloat(), h.toFloat(), (ap * 255f).toInt()) else -1

        val previa = s.timeSec - s.sinceChangeSec > T_ENCENDIDO + DUR_ENCENDIDO
        val pulso = if (msg == null && !s.muted && previa) 1f + 0.35f * clamp01(1f - s.sinceChangeSec / 0.2f) else 1f
        canvas.save()
        canvas.translate(EX, EY)
        hoja(canvas, largo, 12f, t, 0f, pulso)
        empunadura(canvas, encendida && enc > 0f)
        canvas.restore()

        val fl = intensidadDestello(s, encendida, msg != null)
        if (fl > 0f) dibujarDestello(canvas, EX, EY, 70f * fl + 10f, fl)

        if (msg == null) {
            // Chispas en la punta en cada cambio (solo si la hoja ya estaba encendida antes del cambio).
            if (!s.muted && largo > 8f && previa && s.sinceChangeSec < 0.7f) {
                chispas(canvas, PUNTA, EX + largo, EY, s.sinceChangeSec, 7, s.index)
            }
            val tg = s.sinceChangeSec - T_GUARDADO
            if (s.muted && previa && tg >= 0f && tg < 0.8f) chispas(canvas, GUARDA, EX, EY, tg, 14, s.index)

            etiqueta.color = if (s.muted) 0xFFFF6B6B.toInt() else 0xFFFF9A9A.toInt()
            canvas.drawText(if (s.muted) TXT_SILENCIO else TXT_VOLUMEN, TEXTO_X, 40f, etiqueta)
            numero.color = if (s.muted) 0xFF7A5A5A.toInt() else 0xFFFFE8E4.toInt()
            numero.setShadowLayer(if (s.muted) 7f else 22f, 0f, 0f, ROJO)
            canvas.drawText(numeros[s.index.coerceIn(0, 100)], TEXTO_X, 114f, numero)
        } else {
            if (dosPuntos > 0) {
                etiqueta.color = 0xFFFF9A9A.toInt()
                canvas.drawText(msg, 0, dosPuntos, TEXTO_X, 40f, etiqueta)
            }
            valor.color = if (encendida) 0xFFFFE8E4.toInt() else 0xFF9A6A6A.toInt()
            valor.setShadowLayer(if (encendida) 18f else 6f, 0f, 0f, ROJO)
            canvas.drawText(msg, inicioValor, msg.length, TEXTO_X, if (dosPuntos > 0) 108f else 92f, valor)
        }

        if (capa >= 0) canvas.restoreToCount(capa)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val vis = visibilidad(s)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val t = s.timeSec

        caja.set(0f, 0f, w.toFloat(), h.toFloat())
        canvas.drawRoundRect(caja, 16f, 16f, placaLat)

        val enc = encendido(s)
        val encendida = !s.muted
        val largo = enc * clamp01(s.level) * LARGO_LAT
        if (largo > 1f) {
            reflejo(canvas, LEX, LEY - largo / 2f, 50f, largo / 2f + 50f, 0.28f * (0.9f + 0.1f * ruido(t * 20f)))
        }
        caja.set(1f, 1f, w - 1f, h - 1f)
        canvas.drawRoundRect(caja, 15f, 15f, borde)

        val alfa = max(0.3f, ap)
        val capa = if (alfa < 1f) canvas.saveLayerAlpha(0f, 0f, w.toFloat(), h.toFloat(), (alfa * 255f).toInt()) else -1

        val previa = s.timeSec - s.sinceChangeSec > T_ENCENDIDO + DUR_ENCENDIDO
        val pulso = if (!s.muted && previa) 1f + 0.35f * clamp01(1f - s.sinceChangeSec / 0.2f) else 1f
        canvas.save()
        canvas.translate(LEX, LEY)
        canvas.rotate(-90f)
        hoja(canvas, largo, 9f, t, 7f, pulso)
        canvas.scale(0.62f, 0.62f)
        empunadura(canvas, encendida && enc > 0f)
        canvas.restore()

        // Cristal que late (más rápido con más volumen); en silencio queda tenue.
        caja.set(LEX - 8f, LEY + 33f, LEX + 8f, LEY + 63f)
        relleno.color = 0xFF050506.toInt()
        canvas.drawRoundRect(caja, 3f, 3f, relleno)
        val lat = 0.5f + 0.5f * sin(t * (3f + clamp01(s.level) * 9f))
        val brillo = if (s.muted) 0.25f + 0.15f * lat else 0.5f + 0.5f * lat
        reflejo(canvas, LEX, LEY + 48f, 8f + 16f * brillo, 8f + 16f * brillo, brillo)
        val tam = 5f + lat * 2.5f
        val cy = LEY + 48f
        rombo.reset()
        rombo.moveTo(LEX, cy - tam)
        rombo.lineTo(LEX + tam, cy)
        rombo.lineTo(LEX, cy + tam)
        rombo.lineTo(LEX - tam, cy)
        rombo.close()
        relleno.color = Color.argb((brillo * 255f).toInt(), 255, (40f + lat * 70f).toInt(), (40f + lat * 40f).toInt())
        canvas.drawPath(rombo, relleno)

        val fl = intensidadDestello(s, encendida, false)
        if (fl > 0f) dibujarDestello(canvas, LEX, LEY, 45f * fl + 8f, fl)

        if (!s.muted && largo > 8f && previa && s.sinceChangeSec < 0.7f) {
            chispas(canvas, PUNTA_LAT, LEX, LEY - largo, s.sinceChangeSec, 6, s.index)
        }
        val tg = s.sinceChangeSec - T_GUARDADO
        if (s.muted && previa && tg >= 0f && tg < 0.8f) chispas(canvas, GUARDA_LAT, LEX, LEY - 3f, tg, 8, s.index)

        if (capa >= 0) canvas.restoreToCount(capa)
    }

    /** Hoja en coordenadas locales: emisor en (0,0) y la hoja hacia +x. */
    private fun hoja(canvas: Canvas, largo: Float, grosor: Float, t: Float, semilla: Float, pulso: Float) {
        if (largo < 1f) return
        val fl = 1f + 0.07f * ruido(t * 23f + semilla) + 0.03f * sin(t * 61f + semilla)
        for (i in haloAncho.indices) {
            trazo.strokeWidth = grosor * haloAncho[i] * fl
            trazo.color = conAlfa(ROJO_HALO, haloAlfa[i] * fl * pulso)
            canvas.drawLine(0f, 0f, largo, 0f, trazo)
        }
        trazo.strokeWidth = grosor * 1.3f * fl
        trazo.color = ROJO
        canvas.drawLine(0f, 0f, largo, 0f, trazo)
        trazo.strokeWidth = grosor * 0.85f
        trazo.color = conAlfa(ROSA, 0.9f)
        canvas.drawLine(0f, 0f, largo, 0f, trazo)
        trazo.strokeWidth = grosor * 0.55f
        trazo.color = NUCLEO
        canvas.drawLine(0f, 0f, largo, 0f, trazo)
        // Punta un poco más encendida.
        reflejo(canvas, largo, 0f, grosor * 2.2f, grosor * 2.2f, 0.45f * fl)
    }

    /** Empuñadura en coordenadas locales: el emisor termina en x = 0 y el pomo en x = -140. */
    private fun empunadura(canvas: Canvas, encendida: Boolean) {
        caja.set(-140f, -14f, -126f, 14f)
        relleno.color = 0xFF1C1F24.toInt()
        canvas.drawRoundRect(caja, 5f, 5f, relleno)
        caja.set(-128f, -16f, -46f, 16f)
        canvas.drawRoundRect(caja, 4f, 4f, metal)
        relleno.color = 0xFF14161A.toInt()
        var x = -122f
        while (x < -52f) {
            canvas.drawRect(x, -16f, x + 4f, 16f, relleno)
            x += 10f
        }
        caja.set(-48f, -18f, -18f, 18f)
        canvas.drawRoundRect(caja, 3f, 3f, metal)
        if (encendida) reflejo(canvas, -34f, -20.5f, 16f, 12f, 0.8f)
        caja.set(-40f, -25f, -28f, -16f)
        relleno.color = if (encendida) 0xFFFF3B3B.toInt() else 0xFF4A0F0F.toInt()
        canvas.drawRoundRect(caja, 2f, 2f, relleno)
        canvas.drawPath(emisor, metal)
        relleno.color = 0xFF0D0E11.toInt()
        canvas.drawRect(-3f, -21f, 0f, 21f, relleno)
    }

    /** Resplandor elíptico suave centrado en (cx,cy) con radios rx, ry. */
    private fun reflejo(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, alfa: Float) {
        if (alfa <= 0f || rx <= 0f || ry <= 0f) return
        resplandor.alpha = (clamp01(alfa) * 255f).toInt()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(rx / 100f, ry / 100f)
        canvas.drawCircle(0f, 0f, 100f, resplandor)
        canvas.restore()
    }

    private fun dibujarDestello(canvas: Canvas, cx: Float, cy: Float, radio: Float, intensidad: Float) {
        destello.alpha = (clamp01(intensidad) * 255f).toInt()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radio / 100f, radio / 100f)
        canvas.drawCircle(0f, 0f, 100f, destello)
        canvas.restore()
    }

    /**
     * Destello en el emisor: al encenderse por primera vez, al volver del silencio (el nivel arranca
     * de 0 y sube, así que level/objetivo es chico) y al terminar de guardarse.
     */
    private fun intensidadDestello(s: OverlayState, encendida: Boolean, conMensaje: Boolean): Float {
        var f = 0f
        val te = s.timeSec - T_ENCENDIDO
        if (encendida && te >= 0f) f = 1f - te * 3f
        if (!conMensaje) {
            val previa = s.timeSec - s.sinceChangeSec > T_ENCENDIDO + DUR_ENCENDIDO
            if (s.muted) {
                val tg = s.sinceChangeSec - T_GUARDADO
                if (previa && tg >= 0f) f = max(f, 0.7f - tg * 3f)
            } else if (s.sinceChangeSec < 0.4f) {
                val objetivo = s.index / 100f
                if (objetivo > 0.005f) f = max(f, clamp01((0.9f - s.level / objetivo) / 0.9f))
            }
        }
        return clamp01(f)
    }

    /** Chispas con gravedad en forma cerrada: posición = origen + v·t + ½·g·t². */
    private fun chispas(canvas: Canvas, tipo: Int, x0: Float, y0: Float, t: Float, n: Int, semilla: Int) {
        for (i in 0 until n) {
            val j = semilla * 29 + i * 4 + tipo * 97
            val r1 = azar[j and 511]
            val r2 = azar[(j + 1) and 511]
            val r3 = azar[(j + 2) and 511]
            val r4 = azar[(j + 3) and 511]
            var x = x0
            var y = y0
            val vx: Float
            val vy: Float
            val vida: Float
            when (tipo) {
                PUNTA -> {
                    y += (r3 - 0.5f) * 8f
                    vx = (r1 - 0.3f) * 320f
                    vy = -(80f + r2 * 220f)
                    vida = 0.3f + r4 * 0.35f
                }
                GUARDA -> {
                    x += 4f
                    vx = r1 * 260f - 40f
                    vy = 60f - r2 * 260f
                    vida = 0.4f + r4 * 0.4f
                }
                PUNTA_LAT -> {
                    x += (r3 - 0.5f) * 6f
                    vx = (r1 - 0.5f) * 200f
                    vy = -(60f + r2 * 160f)
                    vida = 0.3f + r4 * 0.3f
                }
                else -> {
                    vx = (r1 - 0.5f) * 160f
                    vy = -(r2 * 200f)
                    vida = 0.4f + r4 * 0.3f
                }
            }
            chispa(canvas, x, y, vx, vy, t, vida)
        }
    }

    private fun chispa(canvas: Canvas, x0: Float, y0: Float, vx: Float, vy0: Float, t: Float, vida: Float) {
        if (t < 0f || t >= vida) return
        val k = t / vida
        val x = x0 + vx * t
        val y = y0 + vy0 * t + 0.5f * GRAVEDAD * t * t
        val vy = vy0 + GRAVEDAD * t
        val colaX = x - vx * 0.025f
        val colaY = y - vy * 0.025f
        val a = 1f - k
        trazo.strokeWidth = 6f
        trazo.color = conAlfa(CHISPA_FIN, 0.28f * a)
        canvas.drawLine(x, y, colaX, colaY, trazo)
        trazo.strokeWidth = 2.5f
        trazo.color = conAlfa(mezclar(CHISPA_INI, CHISPA_FIN, k), a)
        canvas.drawLine(x, y, colaX, colaY, trazo)
    }

    /** Entrada propia (0,4 s) medida con timeSec; la salida sigue a appear. */
    private fun visibilidad(s: OverlayState) = min(clamp01(s.timeSec / ENTRADA), clamp01(s.appear))

    /** 0..1: cuánto está estirada la hoja (enciende tras la entrada y se guarda apenas empieza la salida). */
    private fun encendido(s: OverlayState): Float {
        val sube = clamp01((s.timeSec - T_ENCENDIDO) / DUR_ENCENDIDO)
        val guarda = clamp01((s.appear - 0.5f) / 0.5f)
        return easeOut(min(sube, guarda))
    }

    private fun esApagado(msg: String): Boolean {
        val clave = "APAGADO"
        var i = 0
        while (i + clave.length <= msg.length) {
            if (msg.regionMatches(i, clave, 0, clave.length, ignoreCase = true)) return true
            i++
        }
        return false
    }

    private fun ruido(x: Float) = sin(x * 1.7f) * 0.5f + sin(x * 3.13f + 1.3f) * 0.3f + sin(x * 7.7f + 4f) * 0.2f
    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeOut(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
    private fun conAlfa(color: Int, alfa: Float) = (color and 0x00FFFFFF) or ((clamp01(alfa) * 255f).toInt() shl 24)
    private fun mezclar(a: Int, b: Int, t: Float): Int {
        val k = clamp01(t)
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        return Color.rgb(
            ar + ((((b shr 16) and 0xFF) - ar) * k).toInt(),
            ag + ((((b shr 8) and 0xFF) - ag) * k).toInt(),
            ab + (((b and 0xFF) - ab) * k).toInt(),
        )
    }
}
