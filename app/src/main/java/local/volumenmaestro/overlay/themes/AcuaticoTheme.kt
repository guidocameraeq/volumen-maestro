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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Fondo submarino con cáusticas y burbujas; el agua avanza por un tubo con el frente ondulado y
 * el número flota en una burbuja. En silencio el agua se congela. En el lateral, un tubo que se
 * llena con burbujas y un pez.
 */
object AcuaticoTheme : Theme {
    override val id = "acuatico"
    override val name = "Acuático"
    override val description = "Agua que avanza por un tubo entre burbujas, con el número flotando; en silencio se congela."

    private const val ENTRADA = 0.45f
    private const val N_TUBO = 14
    private const val N_FONDO = 18
    private const val N_LAT = 16
    // Tubo del panel inferior.
    private const val TX0 = 160f
    private const val TX1 = 850f
    private const val TY = 60f
    private const val TH = 48f
    // Tubo del lateral.
    private const val LX0 = 22f
    private const val LX1 = 86f
    private const val LY = 20f
    private const val LH = 392f

    private const val TXT_VOLUMEN = "Volumen"
    private const val TXT_SILENCIO = "Silencio · agua congelada"

    private val CELESTE = 0xFFBAE6FD.toInt()
    private val numeros = Array(101) { it.toString() }

    /** Números al azar fijos (semilla constante) para las burbujas. */
    private val azar = FloatArray(256).also { a ->
        var x = 0x5BD1E995
        for (i in a.indices) {
            x = x xor (x shl 13)
            x = x xor (x ushr 17)
            x = x xor (x shl 5)
            a[i] = (x ushr 8) / 16777216f
        }
    }
    // Índices en `azar`: burbujas del tubo (4 valores c/u), del fondo (4) y del lateral (5).
    private const val B_TUBO = 0
    private const val B_FONDO = 64
    private const val B_LAT = 144

    private val base = Paint().apply { color = 0xFF053052.toInt() }
    private val placaInf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, 150f,
            intArrayOf(0xFF0A4A78.toInt(), 0xFF053052.toInt(), 0xFF021B2E.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val placaLat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, 430f, 0xFF0A4A78.toInt(), 0xFF021B2E.toInt(), Shader.TileMode.CLAMP)
    }
    private val agua = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, TY, 0f, TY + TH, 0xFF38BDF8.toInt(), 0xFF0369A1.toInt(), Shader.TileMode.CLAMP)
    }
    private val aguaHelada = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, TY, 0f, TY + TH, 0xFFE0F7FF.toInt(), 0xFF7FB8D6.toInt(), Shader.TileMode.CLAMP)
    }
    // En el lateral el degradé arranca en la superficie: se define en 0..1 y se ubica con una matriz.
    private val aguaLatShader = LinearGradient(0f, 0f, 0f, 1f, 0xFF38BDF8.toInt(), 0xFF075985.toInt(), Shader.TileMode.CLAMP)
    private val heladaLatShader = LinearGradient(0f, 0f, 0f, 1f, 0xFFE0F7FF.toInt(), 0xFF7FB8D6.toInt(), Shader.TileMode.CLAMP)
    private val aguaLat = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = aguaLatShader }
    private val heladaLat = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = heladaLatShader }
    private val matriz = Matrix()
    /** Relleno de la burbuja del número, en coordenadas con el centro vertical en y = 0. */
    private val burbujaRelleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(68f, -14f, 64f, 0x8CBAE6FD.toInt(), 0xBF0C4A6E.toInt(), Shader.TileMode.CLAMP)
    }
    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        textSize = 44f
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }
    private val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 21f
        color = CELESTE
    }
    private val mensaje = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        setShadowLayer(4f, 0f, 1f, 0xCC01121F.toInt())
    }

    private val panel = RectF()
    private val caja = RectF()
    private val ovalo = RectF()
    private val camino = Path()
    private val superficie = Path()
    private val recorte = Path()

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val vis = visibilidad(s)
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))

        // Placa de seguridad opaca desde el primer cuadro.
        canvas.drawRect(36f, 11f, 924f, 142f, base)
        val r = lerp(8f, 30f, p)
        panel.set(lerp(36f, 0f, p), lerp(11f, 0f, p), lerp(924f, w.toFloat(), p), lerp(142f, h.toFloat(), p))
        canvas.drawRoundRect(panel, r, r, placaInf)

        val msg = s.message
        val m = if (msg != null) (if (esApagado(msg)) 1f else 0f) else hielo(s)
        val ta = if (msg != null) s.timeSec else tiempoAgua(s)

        // Fondo: cáusticas y burbujas que suben.
        canvas.save()
        recorte.reset()
        recorte.addRoundRect(panel, r, r, Path.Direction.CW)
        canvas.clipPath(recorte)
        trazo.strokeWidth = 6f
        trazo.color = 0x0FB4E6FF
        for (k in 0 until 5) {
            val yBase = panel.top + 18f + k * 28f
            var x = panel.left
            camino.reset()
            camino.moveTo(x, yBase + sin((x + 480f) * 0.02f + ta * 1.3f + k) * 8f)
            while (x < panel.right + 30f) {
                x += 30f
                camino.lineTo(x, yBase + sin((x + 480f) * 0.02f + ta * 1.3f + k) * 8f)
            }
            canvas.drawPath(camino, trazo)
        }
        trazo.strokeWidth = 1.5f
        trazo.color = 0x40C8F0FF
        for (i in 0 until N_FONDO) {
            val j = B_FONDO + i * 4
            val bx = azar[j]
            val y = panel.top + frac(azar[j + 1] - ta * (0.2f + azar[j + 3] * 0.5f) * 0.25f) * panel.height()
            val x = panel.left + bx * panel.width() + sin(ta * 2f + bx * 20f) * 4f
            canvas.drawCircle(x, y, 1.5f + azar[j + 2] * 3f, trazo)
        }
        canvas.restore()

        trazo.strokeWidth = 2f
        trazo.color = 0x737DD3FC
        caja.set(panel.left + 1f, panel.top + 1f, panel.right - 1f, panel.bottom - 1f)
        canvas.drawRoundRect(caja, r, r, trazo)
        if (ap <= 0f) return

        val capa = if (ap < 1f) canvas.saveLayerAlpha(0f, 0f, w.toFloat(), h.toFloat(), (ap * 255f).toInt()) else canvas.save()
        canvas.translate(0f, (1f - ap) * 18f)
        val cy = 75f + sin(ta * 2f) * 3f
        burbuja(canvas, cy)
        if (msg == null) {
            val v = nivelVisible(s)
            canvas.drawText(numeros[s.index.coerceIn(0, 100)], 76f, cy + 15f, numero)
            canvas.drawText(if (s.muted) TXT_SILENCIO else TXT_VOLUMEN, TX0, 46f, etiqueta)
            tubo(canvas, v, m, ta)
            parlante(canvas, 905f, 83f, 44f, CELESTE, s.muted, v, 4f)
        } else {
            parlante(canvas, 82f, cy, 50f, Color.WHITE, m > 0.5f, 1f, 4.5f)
            val dosPuntos = msg.indexOf(':')
            var ini = if (dosPuntos >= 0) dosPuntos + 1 else 0
            while (ini < msg.length && msg[ini] == ' ') ini++
            if (dosPuntos > 0) canvas.drawText(msg, 0, dosPuntos, TX0, 46f, etiqueta)
            // El agua entra al tubo cuando aparece el mensaje.
            tubo(canvas, easeOut(clamp01(s.sinceChangeSec / 0.7f)), m, ta)
            mensaje.textSize = 26f
            val ancho = mensaje.measureText(msg, ini, msg.length)
            if (ancho > 640f) mensaje.textSize = 26f * 640f / ancho
            canvas.drawText(msg, ini, msg.length, (TX0 + TX1) / 2f, 93f, mensaje)
        }
        canvas.restoreToCount(capa)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val vis = visibilidad(s)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        caja.set(0f, 0f, w.toFloat(), h.toFloat())
        canvas.drawRoundRect(caja, 16f, 16f, placaLat)

        val ta = tiempoAgua(s)
        val m = hielo(s)
        val v = nivelVisible(s)
        val fondo = LY + LH

        caja.set(LX0, LY, LX1, fondo)
        relleno.color = 0xFF01121F.toInt()
        canvas.drawRoundRect(caja, 30f, 30f, relleno)
        canvas.save()
        recorte.reset()
        recorte.addRoundRect(caja, 30f, 30f, Path.Direction.CW)
        canvas.clipPath(recorte)

        val lvl = fondo - LH * v * ap
        val amp = (2.5f + v * 4f) * (1f - m)
        camino.reset()
        superficie.reset()
        camino.moveTo(LX0, fondo)
        var x = LX0
        var primero = true
        while (x <= LX1) {
            val y = lvl + sin((x + 1740f) * 0.15f + ta * 3f) * amp + sin((x + 1740f) * 0.31f - ta * 4.2f) * amp * 0.5f
            camino.lineTo(x, y)
            if (primero) superficie.moveTo(x, y) else superficie.lineTo(x, y)
            primero = false
            x += 4f
        }
        camino.lineTo(LX1, fondo)
        camino.close()
        matriz.setScale(1f, max(1f, fondo - lvl))
        matriz.postTranslate(0f, lvl)
        aguaLatShader.setLocalMatrix(matriz)
        canvas.drawPath(camino, aguaLat)
        if (m > 0.01f) {
            heladaLatShader.setLocalMatrix(matriz)
            heladaLat.alpha = (m * 255f).toInt()
            canvas.drawPath(camino, heladaLat)
        }
        trazo.strokeWidth = 2f
        trazo.color = 0x8CFFFFFF.toInt()
        canvas.drawPath(superficie, trazo)

        // Burbujas que suben desde el fondo (más cuanto más volumen; quietas en silencio).
        trazo.strokeWidth = 1.5f
        trazo.color = 0xA6FFFFFF.toInt()
        val cuantas = min(N_LAT, (4f + v * 12f).toInt())
        for (i in 0 until cuantas) {
            val j = B_LAT + i * 5
            val br = 2f + azar[j + 2] * 4f
            val vel = 40f + azar[j + 3] * 60f
            val by = fondo - 6f - frac(azar[j + 1] + ta * vel / LH) * LH
            val bx = LX0 + 8f + azar[j] * (LX1 - LX0 - 16f) + sin(ta * 3f + azar[j + 4] * 6f) * 3f
            if (by > lvl + br) canvas.drawCircle(bx, by, br, trazo)
        }

        if (v > 0.25f && ap > 0.3f) {
            val px = (LX0 + LX1) / 2f + sin(ta * 0.9f) * 16f
            val dir = if (cos(ta * 0.9f) > 0f) 1f else -1f
            val py = lerp(lvl, fondo, 0.5f) + sin(ta * 2.1f) * 4f
            relleno.color = if (s.muted) 0xFF9FB8C8.toInt() else 0xFFFB923C.toInt()
            ovalo.set(px - 9f, py - 5f, px + 9f, py + 5f)
            canvas.drawOval(ovalo, relleno)
            camino.reset()
            camino.moveTo(px - dir * 7f, py)
            camino.lineTo(px - dir * 15f, py - 5f)
            camino.lineTo(px - dir * 15f, py + 5f)
            camino.close()
            canvas.drawPath(camino, relleno)
            relleno.color = 0xFF0B1F33.toInt()
            canvas.drawCircle(px + dir * 4f, py - 1f, 1.4f, relleno)
        }

        trazo.strokeWidth = 2f
        trazo.color = 0x4DFFFFFF
        for (i in 1 until 10) {
            val y = fondo - LH * i / 10f
            canvas.drawLine(LX1 - 12f, y, LX1 - 4f, y, trazo)
        }
        relleno.color = 0x1FFFFFFF
        canvas.drawRect(LX0 + 8f, LY + 24f, LX0 + 14f, fondo - 24f, relleno)
        canvas.restore()

        trazo.color = 0x997DD3FC.toInt()
        canvas.drawRoundRect(caja, 30f, 30f, trazo)
    }

    /** Burbuja del número, centrada en (76, cy). */
    private fun burbuja(canvas: Canvas, cy: Float) {
        canvas.save()
        canvas.translate(0f, cy)
        canvas.drawCircle(76f, 0f, 52f, burbujaRelleno)
        trazo.strokeWidth = 3f
        trazo.color = CELESTE
        canvas.drawCircle(76f, 0f, 52f, trazo)
        trazo.strokeWidth = 4f
        trazo.strokeCap = Paint.Cap.ROUND
        trazo.color = 0xA6FFFFFF.toInt()
        ovalo.set(36f, -40f, 116f, 40f)
        canvas.drawArc(ovalo, -143.24f, 45.84f, false, trazo)
        trazo.strokeCap = Paint.Cap.BUTT
        canvas.restore()
    }

    /** Tubo horizontal con el agua hasta la fracción [v]; [m] = cuánto está congelada. */
    private fun tubo(canvas: Canvas, v: Float, m: Float, ta: Float) {
        caja.set(TX0, TY, TX1, TY + TH)
        relleno.color = 0xFF01121F.toInt()
        canvas.drawRoundRect(caja, 24f, 24f, relleno)
        canvas.save()
        recorte.reset()
        recorte.addRoundRect(caja, 24f, 24f, Path.Direction.CW)
        canvas.clipPath(recorte)
        val fw = (TX1 - TX0) * v
        if (fw > 0.5f) {
            // Frente del agua ondulado (quieto si está congelada).
            camino.reset()
            camino.moveTo(TX0, TY + TH)
            camino.lineTo(TX0, TY)
            var y = TY
            while (y <= TY + TH) {
                camino.lineTo(TX0 + fw + sin((y + 882f) * 0.18f + ta * 6f) * 5f * (1f - m), y)
                y += 4f
            }
            camino.close()
            canvas.drawPath(camino, agua)
            if (m > 0.01f) {
                aguaHelada.alpha = (m * 255f).toInt()
                canvas.drawPath(camino, aguaHelada)
            }
            trazo.strokeWidth = 1.5f
            trazo.color = 0x99FFFFFF.toInt()
            for (i in 0 until N_TUBO) {
                val j = B_TUBO + i * 4
                val bx = TX0 + azar[j] * fw
                val by = TY + TH - frac(azar[j + 1] + ta * (0.3f + azar[j + 3] * 0.7f) * 0.8f) * TH
                if (bx < TX0 + fw - 8f) canvas.drawCircle(bx, by, 2f + azar[j + 2] * 4f, trazo)
            }
        }
        relleno.color = 0x2EFFFFFF
        canvas.drawRect(TX0 + 10f, TY + 6f, TX1 - 10f, TY + 11f, relleno)
        canvas.restore()
        trazo.strokeWidth = 2f
        trazo.color = 0x997DD3FC.toInt()
        canvas.drawRoundRect(caja, 24f, 24f, trazo)
    }

    /** Ícono de parlante centrado en (cx, cy), ocupa ±s/2. */
    private fun parlante(canvas: Canvas, cx: Float, cy: Float, s: Float, color: Int, mute: Boolean, nivel: Float, grosor: Float) {
        relleno.color = color
        trazo.color = color
        trazo.strokeWidth = grosor
        trazo.strokeCap = Paint.Cap.ROUND
        val x = cx - s * 0.12f
        camino.reset()
        camino.moveTo(x - s * 0.38f, cy - s * 0.16f)
        camino.lineTo(x - s * 0.18f, cy - s * 0.16f)
        camino.lineTo(x + s * 0.06f, cy - s * 0.4f)
        camino.lineTo(x + s * 0.06f, cy + s * 0.4f)
        camino.lineTo(x - s * 0.18f, cy + s * 0.16f)
        camino.lineTo(x - s * 0.38f, cy + s * 0.16f)
        camino.close()
        canvas.drawPath(camino, relleno)
        if (mute) {
            val mx = x + s * 0.32f
            val d = s * 0.13f
            canvas.drawLine(mx - d, cy - d, mx + d, cy + d, trazo)
            canvas.drawLine(mx + d, cy - d, mx - d, cy + d, trazo)
        } else {
            val n = if (nivel <= 0f) 0 else if (nivel < 0.34f) 1 else if (nivel < 0.67f) 2 else 3
            val ax = x + s * 0.08f
            for (i in 1..n) {
                val rad = s * (0.08f + 0.14f * i)
                ovalo.set(ax - rad, cy - rad, ax + rad, cy + rad)
                canvas.drawArc(ovalo, -42.97f, 85.94f, false, trazo)
            }
        }
        trazo.strokeCap = Paint.Cap.BUTT
    }

    private fun visibilidad(s: OverlayState) = min(clamp01(s.timeSec / ENTRADA), clamp01(s.appear))

    /**
     * Nivel a mostrar: en silencio el agua queda congelada en el volumen. Con sonido sigue a level,
     * pero sin quedarse más de 3 puntos atrás (al quitar el silencio el tubo no se vacía).
     */
    private fun nivelVisible(s: OverlayState): Float {
        val objetivo = s.index / 100f
        return clamp01(if (s.muted) objetivo else max(s.level, objetivo - 0.03f))
    }

    /**
     * 0..1: cuánto hielo hay. Se congela al silenciar; al quitar el silencio el nivel animado
     * arranca de 0, así que level/objetivo chico indica que se está derritiendo.
     */
    private fun hielo(s: OverlayState): Float {
        if (s.muted) return 1f - exp(-9f * s.sinceChangeSec)
        val objetivo = s.index / 100f
        if (objetivo <= 0.005f || s.sinceChangeSec > 0.6f) return 0f
        return clamp01((0.9f - s.level / objetivo) / 0.9f)
    }

    /** Reloj del agua: casi se detiene en silencio. */
    private fun tiempoAgua(s: OverlayState): Float =
        if (s.muted) s.timeSec - 0.97f * min(s.sinceChangeSec, s.timeSec) else s.timeSec

    private fun esApagado(msg: String): Boolean {
        val clave = "APAGADO"
        var i = 0
        while (i + clave.length <= msg.length) {
            if (msg.regionMatches(i, clave, 0, clave.length, ignoreCase = true)) return true
            i++
        }
        return false
    }

    private fun frac(x: Float) = x - floor(x)
    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeOut(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
}
