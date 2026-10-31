package local.volumenmaestro.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** El diseño de la versión 1: panel oscuro con número y barra. Es el tema por defecto. */
object ClassicTheme : Theme {
    override val id = "clasico"
    override val name = "Clásico"
    override val description = "Panel oscuro y simple, el de la primera versión."

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF202124.toInt() }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3C4043.toInt() }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E88E5.toInt() }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        canvas.drawRoundRect(rect, h * 0.27f, h * 0.27f, bg)
        text.textSize = h * 0.29f
        val label = s.message ?: if (s.muted) "Volumen: silencio" else "Volumen: ${s.index}"
        canvas.drawText(label, w / 2f, h * 0.48f, text)
        if (s.message == null) {
            val left = w * 0.1f
            val right = w * 0.9f
            val top = h * 0.66f
            rect.set(left, top, right, top + h * 0.12f)
            canvas.drawRoundRect(rect, 8f, 8f, track)
            val level = if (s.muted) 0f else s.level
            rect.set(left, top, left + (right - left) * level, top + h * 0.12f)
            canvas.drawRoundRect(rect, 8f, 8f, fill)
        }
    }

    override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        canvas.drawRoundRect(rect, w * 0.3f, w * 0.3f, bg)
        val pad = w * 0.3f
        val barW = (w - 2 * pad)
        rect.set(pad, h * 0.08f, pad + barW, h * 0.92f)
        canvas.drawRoundRect(rect, barW / 2, barW / 2, track)
        val level = if (s.muted) 0f else s.level
        val top = h * 0.92f - (h * 0.84f) * level
        rect.set(pad, top, pad + barW, h * 0.92f)
        canvas.drawRoundRect(rect, barW / 2, barW / 2, fill)
    }
}
