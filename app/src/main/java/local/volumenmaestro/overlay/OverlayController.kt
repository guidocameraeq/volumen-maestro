package local.volumenmaestro.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Las dos ventanas que tapan los carteles de TCL (coordenadas para 1920x1080):
 * panel inferior x 480-1440 / y 882-1032 y lateral x 1740-1848 / y 326-756.
 * Solo anima mientras están visibles. Todos los métodos se llaman en el hilo principal.
 */
class OverlayController(context: Context) {
    var theme: Theme = ClassicTheme
    var showSide = true
    var durationMs = 3_800L

    private val wm = context.getSystemService(WindowManager::class.java)
    private val bottom = ThemeView(context) { c, w, h -> theme.drawBottom(c, w, h, state()) }
    private val side = ThemeView(context) { c, w, h -> theme.drawSide(c, w, h, state()) }

    private var target = 0f
    private var shown = 0f
    private var index = 0
    private var muted = false
    private var message: String? = null
    private var appear = 0f
    private var shownAt = 0L
    private var changedAt = 0L
    private var hideAt = 0L
    private var lastFrame = 0L
    private var animating = false

    private val frame = Choreographer.FrameCallback { tick() }

    init {
        wm.addView(bottom, params(BOTTOM_W, BOTTOM_H, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, BOTTOM_MARGIN))
        wm.addView(side, params(SIDE_W, SIDE_H, Gravity.TOP or Gravity.START, SIDE_X, SIDE_Y))
        bottom.visibility = View.GONE
        side.visibility = View.GONE
    }

    fun showVolume(index: Int, max: Int, muted: Boolean) {
        this.index = index
        this.muted = muted
        this.message = null
        // En silencio el nivel animado baja a 0 (el sable guarda la hoja, las barras se vacían);
        // los temas que quieren congelar la barra usan index.
        target = if (muted) 0f else index.toFloat() / max.coerceAtLeast(1)
        changedAt = SystemClock.uptimeMillis()
        show()
    }

    fun showMessage(text: String) {
        message = text
        changedAt = SystemClock.uptimeMillis()
        show()
    }

    fun release() {
        Choreographer.getInstance().removeFrameCallback(frame)
        wm.removeView(bottom)
        wm.removeView(side)
    }

    private fun show() {
        val now = SystemClock.uptimeMillis()
        if (bottom.visibility != View.VISIBLE) {
            shownAt = now
            shown = target
            appear = 0f
            bottom.visibility = View.VISIBLE
        }
        side.visibility = if (showSide && message == null) View.VISIBLE else View.GONE
        hideAt = now + durationMs
        if (!animating) {
            animating = true
            lastFrame = now
            Choreographer.getInstance().postFrameCallback(frame)
        }
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrame).coerceIn(0, 100)) / 1000f
        lastFrame = now
        shown += (target - shown) * (dt * 12f).coerceAtMost(1f)
        appear = if (now < hideAt) (appear + dt / APPEAR_SEC).coerceAtMost(1f)
        else (appear - dt / DISAPPEAR_SEC).coerceAtLeast(0f)

        if (now >= hideAt && appear <= 0f) {
            bottom.visibility = View.GONE
            side.visibility = View.GONE
            animating = false
            return
        }
        bottom.invalidate()
        if (side.visibility == View.VISIBLE) side.invalidate()
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private fun state() = OverlayState(
        level = shown,
        index = index,
        muted = muted,
        timeSec = (SystemClock.uptimeMillis() - shownAt) / 1000f,
        appear = appear,
        sinceChangeSec = (SystemClock.uptimeMillis() - changedAt) / 1000f,
        message = message,
    )

    private fun params(w: Int, h: Int, gravity: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        this.gravity = gravity
        this.x = x
        this.y = y
    }

    private class ThemeView(context: Context, val drawer: (Canvas, Int, Int) -> Unit) : View(context) {
        override fun onDraw(canvas: Canvas) = drawer(canvas, width, height)
    }

    companion object {
        const val BOTTOM_W = 960
        const val BOTTOM_H = 150
        const val BOTTOM_MARGIN = 48
        const val SIDE_X = 1740
        const val SIDE_Y = 326
        const val SIDE_W = 108
        const val SIDE_H = 430
        private const val APPEAR_SEC = 0.18f
        private const val DISAPPEAR_SEC = 0.3f
    }
}
