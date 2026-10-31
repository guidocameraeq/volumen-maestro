package local.volumenmaestro.overlay.themes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import local.volumenmaestro.overlay.OverlayState
import local.volumenmaestro.overlay.Theme

/**
 * Terminal de fósforo verde: se enciende como un CRT, tipea el comando, barra ASCII [####----]
 * y glitch al cambiar; en silencio pasa a ámbar. En el lateral, lluvia de caracteres más
 * brillante debajo del nivel.
 */
object TerminalTheme : Theme {
    override val id = "terminal"
    override val name = "Terminal hacker"
    override val description = "Fósforo verde que se prende como un monitor viejo y tipea el volumen; en silencio pasa a ámbar."

    private const val ENTRADA = 0.35f
    private const val X0 = 25f
    private const val CY = 75f
    private const val CELDAS = 36
    private const val FILA = 18f
    private const val COLUMNAS = 6

    private const val PROMPT = "usuario@tv:~\$ "
    private const val CMD_SILENCIO = "silencio --activar"
    private const val CMD_MENSAJE = "volumen-maestro --estado"
    private const val SALIDA = "> "
    private const val ESTADO_SILENCIO = "> ESTADO: SILENCIO ACTIVADO"
    private const val ESTADO_OK = "> estado: audio activo"
    private const val SIL = "SIL"

    private val FONDO = 0xFF010A03.toInt()
    private val VERDE = 0xFF00FF66.toInt()
    private val VERDE_TENUE = 0xFF0B5A2A.toInt()
    private val VERDE_MEDIO = 0xFF35C46A.toInt()
    private val AMBAR = 0xFFFFB000.toInt()
    private val AMBAR_TENUE = 0xFF4A3300.toInt()
    private val AMBAR_MEDIO = 0xFFC98A00.toInt()

    private val cmdVolumen = Array(101) { "volumen --nivel $it" }
    private val tres = Array(101) { it.toString().padStart(3, '0') }
    private val celda = charArrayOf('[', ']', '#', '-')
    // Solo caracteres que existen en cualquier fuente monoespaciada (el katakana puede faltar en algunas TV).
    private val glifos = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ<>=+*#\$%&@".toCharArray()

    // Parámetros fijos de cada columna de lluvia (semilla constante).
    private val colFase = FloatArray(COLUMNAS) { azar01(mezcla(it, 1, 99)) }
    private val colVel = FloatArray(COLUMNAS) { 70f + azar01(mezcla(it, 2, 99)) * 110f }
    private val colLargo = IntArray(COLUMNAS) { 8 + (azar01(mezcla(it, 3, 99)) * 10f).toInt() }

    private val mono: Typeface = Typeface.MONOSPACE
    private val monoNegrita: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val guiones = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }
    /** Texto con resplandor (sombra del color del fósforo). */
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono }
    /** Texto tenue, sin resplandor. */
    private val plano = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = mono
        textSize = 54f
        textAlign = Paint.Align.RIGHT
    }
    private val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = mono
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }
    private val lluvia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = monoNegrita
        textSize = 17f
        textAlign = Paint.Align.CENTER
    }

    private val panel = RectF()
    private val caja = RectF()
    private val recorte = Path()

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val vis = visibilidad(s)
        val p = easeOut(vis)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))

        // Placa de seguridad opaca desde el primer cuadro.
        relleno.color = FONDO
        canvas.drawRect(36f, 11f, 924f, 142f, relleno)
        val r = lerp(4f, 10f, p)
        panel.set(lerp(36f, 0f, p), lerp(11f, 0f, p), lerp(924f, w.toFloat(), p), lerp(142f, h.toFloat(), p))
        canvas.drawRoundRect(panel, r, r, relleno)

        val msg = s.message
        val ambar = if (msg != null) esApagado(msg) else s.muted
        val col = if (ambar) AMBAR else VERDE
        val tenue = if (ambar) AMBAR_TENUE else VERDE_TENUE
        val medio = if (ambar) AMBAR_MEDIO else VERDE_MEDIO

        // Encendido tipo CRT: la imagen se abre desde una línea horizontal en el centro.
        val abre = easeOut(ap)
        val hh = (panel.height() - 16f) * abre / 2f
        if (hh > 0.5f) {
            canvas.save()
            canvas.clipRect(panel.left + 8f, CY - hh, panel.right - 8f, CY + hh)
            if (s.sinceChangeSec < 0.12f) {
                canvas.translate((azar01(mezcla((s.timeSec * 60f).toInt(), 7, 3)) - 0.5f) * 10f, 0f)
            }
            texto.setShadowLayer(8f, 0f, 0f, col)
            if (msg != null) contenidoMensaje(canvas, s, msg, col, medio)
            else contenidoVolumen(canvas, s, ap, col, tenue, medio)
            canvas.restore()
        }

        lineasCrt(canvas, panel, r, 0x59000000)
        bordeBrillante(canvas, panel, r, col)
        if (abre < 1f) {
            relleno.color = Color.argb(((1f - abre) * 255f).toInt(), 190, 255, 210)
            canvas.drawRect(panel.left + 8f, CY - hh - 2f, panel.right - 8f, CY - hh + 2f, relleno)
            canvas.drawRect(panel.left + 8f, CY + hh - 2f, panel.right - 8f, CY + hh + 2f, relleno)
        }
    }

    private fun contenidoVolumen(canvas: Canvas, s: OverlayState, ap: Float, col: Int, tenue: Int, medio: Int) {
        val idx = s.index.coerceIn(0, 100)
        // Se tipea al abrir y otra vez al silenciar.
        val tipeo = if (s.muted) min(s.timeSec, s.sinceChangeSec) else s.timeSec

        texto.textSize = 26f
        texto.color = medio
        canvas.drawText(PROMPT, X0, 42f, texto)
        val pw = texto.measureText(PROMPT)
        val cmd = if (s.muted) CMD_SILENCIO else cmdVolumen[idx]
        val n = min(cmd.length, (tipeo * 55f).toInt()).coerceAtLeast(0)
        texto.color = col
        canvas.drawText(cmd, 0, n, X0 + pw, 42f, texto)
        val cw = texto.measureText(cmd, 0, n)
        cursor(canvas, s, X0 + pw + cw + 3f, 20f, 44f, col)

        // Barra [####------]
        texto.textSize = 30f
        plano.textSize = 30f
        plano.color = tenue
        val paso = (810f - X0) / (CELDAS + 2)
        val llenas = (nivelVisible(s) * CELDAS * clamp01(ap * 1.2f)).roundToInt()
        texto.color = col
        canvas.drawText(celda, 0, 1, X0, 86f, texto)
        for (i in 0 until CELDAS) {
            val x = X0 + paso * (i + 1)
            if (i < llenas) canvas.drawText(celda, 2, 1, x, 86f, texto)
            else canvas.drawText(celda, 3, 1, x, 86f, plano)
        }
        canvas.drawText(celda, 1, 1, X0 + paso * (CELDAS + 1), 86f, texto)

        numero.color = col
        numero.setShadowLayer(10f, 0f, 0f, col)
        canvas.drawText(tres[idx], 925f, 98f, numero)

        texto.textSize = 24f
        if (s.muted) {
            if (((s.timeSec * 3f).toInt() and 1) == 0) {
                texto.color = col
                canvas.drawText(ESTADO_SILENCIO, X0, 128f, texto)
            }
        } else {
            texto.color = medio
            canvas.drawText(ESTADO_OK, X0, 128f, texto)
        }
    }

    /** Con mensaje: tipea un comando y después la salida con el texto. */
    private fun contenidoMensaje(canvas: Canvas, s: OverlayState, msg: String, col: Int, medio: Int) {
        val tipeo = min(s.timeSec, s.sinceChangeSec)
        texto.textSize = 26f
        texto.color = medio
        canvas.drawText(PROMPT, X0, 42f, texto)
        val pw = texto.measureText(PROMPT)
        val n1 = min(CMD_MENSAJE.length, (tipeo * 55f).toInt()).coerceAtLeast(0)
        texto.color = col
        canvas.drawText(CMD_MENSAJE, 0, n1, X0 + pw, 42f, texto)
        var cx = X0 + pw + texto.measureText(CMD_MENSAJE, 0, n1) + 3f
        var arriba = 20f
        var abajo = 44f

        val t2 = tipeo - CMD_MENSAJE.length / 55f - 0.12f
        if (t2 > 0f) {
            texto.textSize = 34f
            val total = texto.measureText(SALIDA) + texto.measureText(msg)
            if (total > 870f) texto.textSize = 34f * 870f / total
            canvas.drawText(SALIDA, X0, 96f, texto)
            val x2 = X0 + texto.measureText(SALIDA)
            val n2 = min(msg.length, (t2 * 70f).toInt())
            canvas.drawText(msg, 0, n2, x2, 96f, texto)
            cx = x2 + texto.measureText(msg, 0, n2) + 3f
            arriba = 96f - texto.textSize * 0.75f
            abajo = 99f
        }
        cursor(canvas, s, cx, arriba, abajo, col)
    }

    private fun cursor(canvas: Canvas, s: OverlayState, x: Float, arriba: Float, abajo: Float, col: Int) {
        if (((s.timeSec * 2.2f).toInt() and 1) != 0) return
        val ancho = (abajo - arriba) * 0.55f
        relleno.color = conAlfa(col, 0.25f)
        canvas.drawRect(x - 2f, arriba - 2f, x + ancho + 2f, abajo + 2f, relleno)
        relleno.color = col
        canvas.drawRect(x, arriba, x + ancho, abajo, relleno)
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        val vis = visibilidad(s)
        val ap = easeOut(clamp01((vis - 0.3f) / 0.7f))
        val wf = w.toFloat()
        val hf = h.toFloat()

        caja.set(0f, 0f, wf, hf)
        relleno.color = FONDO
        canvas.drawRoundRect(caja, 6f, 6f, relleno)

        val arriba = 10f
        val abajo = hf - 10f
        val lvl = abajo - (abajo - arriba) * nivelVisible(s) * ap
        val ta = tiempoCongelado(s)
        val alfa = max(0.15f, ap)
        val asc = -lluvia.ascent()

        // Lluvia de caracteres: cada columna cae a velocidad fija y se repite en ciclos.
        for (i in 0 until COLUMNAS) {
            val x = 13f + i * 16.4f
            val largo = colLargo[i]
            val ciclo = hf + 20f + largo * FILA + 120f
            val pos = colFase[i] * ciclo + colVel[i] * 0.9f * ta
            val vuelta = floor(pos / ciclo)
            val cabY = pos - vuelta * ciclo - 120f
            val cab = floor(cabY / FILA).toInt()
            for (k in 0 until largo) {
                val fila = cab - k
                val y = fila * FILA
                if (y < -FILA || y > hf) continue
                val a = 1f - k.toFloat() / largo
                val bajo = y + FILA / 2f > lvl
                lluvia.color = colorLluvia(s.muted, k == 0, bajo, a, alfa)
                canvas.drawText(glifos, glifo(i, fila, vuelta.toInt(), ta), 1, x, y + asc, lluvia)
            }
        }

        val col = if (s.muted) AMBAR else VERDE
        if (ap > 0f) {
            trazo.strokeWidth = 6f
            trazo.color = conAlfa(col, 0.18f)
            canvas.drawLine(6f, lvl, wf - 6f, lvl, trazo)
            guiones.color = col
            canvas.drawLine(6f, lvl, wf - 6f, lvl, guiones)

            val by = (lvl - 32f).coerceIn(arriba + 2f, abajo - 28f)
            caja.set(24f, by, 84f, by + 26f)
            relleno.color = FONDO
            canvas.drawRoundRect(caja, 3f, 3f, relleno)
            trazo.strokeWidth = 1.5f
            trazo.color = col
            canvas.drawRoundRect(caja, 3f, 3f, trazo)
            etiqueta.color = col
            etiqueta.setShadowLayer(8f, 0f, 0f, col)
            canvas.drawText(if (s.muted) SIL else tres[s.index.coerceIn(0, 100)], 54f, by + 20f, etiqueta)
        }

        caja.set(0f, 0f, wf, hf)
        lineasCrt(canvas, caja, 6f, 0x4D000000)
        bordeBrillante(canvas, caja, 7f, col)
    }

    private fun colorLluvia(muted: Boolean, cabeza: Boolean, bajo: Boolean, a: Float, alfa: Float): Int = when {
        muted && cabeza -> Color.argb((0.9f * alfa * 255f).toInt(), 255, 220, 150)
        muted -> Color.argb((a * (if (bajo) 0.8f else 0.3f) * alfa * 255f).toInt(), 255, 176, 0)
        cabeza -> conAlfa(if (bajo) 0xFFEAFFEF.toInt() else 0xFF5F9C6D.toInt(), alfa)
        bajo -> Color.argb((a * alfa * 255f).toInt(), 80, 255, 140)
        else -> Color.argb((a * 0.55f * alfa * 255f).toInt(), 20, 130, 60)
    }

    /** Cada celda cambia de carácter cada ~3 s, en momentos distintos. */
    private fun glifo(col: Int, fila: Int, vuelta: Int, ta: Float): Int {
        val fase = azar01(mezcla(col, fila, 0))
        val cambio = floor(ta * 0.35f + fase).toInt()
        return (mezcla(col, fila, vuelta * 131 + cambio) and 0x7FFFFFFF) % glifos.size
    }

    private fun lineasCrt(canvas: Canvas, r: RectF, radio: Float, color: Int) {
        canvas.save()
        recorte.reset()
        recorte.addRoundRect(r, radio, radio, Path.Direction.CW)
        canvas.clipPath(recorte)
        relleno.color = color
        var y = r.top + 2f
        while (y < r.bottom) {
            canvas.drawRect(r.left, y, r.right, min(y + 2f, r.bottom), relleno)
            y += 4f
        }
        canvas.restore()
    }

    /** Borde con resplandor: tres trazos de ancho creciente y alfa decreciente. */
    private fun bordeBrillante(canvas: Canvas, r: RectF, radio: Float, col: Int) {
        val rb = max(2f, radio - 2f)
        val l = r.left + 2f
        val t = r.top + 2f
        val rr = r.right - 2f
        val b = r.bottom - 2f
        trazo.strokeWidth = 8f
        trazo.color = conAlfa(col, 0.07f)
        canvas.drawRoundRect(l, t, rr, b, rb, rb, trazo)
        trazo.strokeWidth = 4.5f
        trazo.color = conAlfa(col, 0.15f)
        canvas.drawRoundRect(l, t, rr, b, rb, rb, trazo)
        trazo.strokeWidth = 2f
        trazo.color = conAlfa(col, 0.55f)
        canvas.drawRoundRect(l, t, rr, b, rb, rb, trazo)
    }

    private fun visibilidad(s: OverlayState) = min(clamp01(s.timeSec / ENTRADA), clamp01(s.appear))

    /**
     * Nivel a mostrar: en silencio la barra queda en el volumen (en ámbar). Con sonido sigue a level,
     * pero sin quedarse más de 3 puntos atrás (así al quitar el silencio no se vacía y vuelve a llenar).
     */
    private fun nivelVisible(s: OverlayState): Float {
        val objetivo = s.index / 100f
        return clamp01(if (s.muted) objetivo else max(s.level, objetivo - 0.03f))
    }

    /** Reloj de la lluvia: casi se detiene en silencio. */
    private fun tiempoCongelado(s: OverlayState): Float =
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

    private fun mezcla(a: Int, b: Int, c: Int): Int {
        var x = a * 0x27D4EB2D + b * 0x165667B1 + c * 0x61C88647
        x = x xor (x ushr 15)
        x *= 0x2C1B3C6D
        x = x xor (x ushr 12)
        x *= 0x297A2D39
        return x xor (x ushr 15)
    }

    private fun azar01(x: Int) = ((x ushr 8) and 0xFFFF) / 65536f
    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeOut(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
    private fun conAlfa(color: Int, alfa: Float) = (color and 0x00FFFFFF) or ((clamp01(alfa) * 255f).toInt() shl 24)
}
