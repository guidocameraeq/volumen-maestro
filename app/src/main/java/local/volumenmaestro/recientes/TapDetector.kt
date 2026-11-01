package local.volumenmaestro.recientes

/**
 * Decide si un botón se tocó una o dos veces: el segundo toque tiene que llegar antes de
 * [windowMs]. El toque simple recién se sabe cuando pasa la ventana sin un segundo toque.
 */
class TapDetector(private val windowMs: Long) {
    private var firstAt = NONE

    /** Un toque. Devuelve true si completa un doble toque. */
    fun press(now: Long): Boolean {
        if (firstAt != NONE && now - firstAt <= windowMs) {
            firstAt = NONE
            return true
        }
        firstAt = now
        return false
    }

    /** Llamar al terminar la ventana del primer toque. Devuelve true si quedó en toque simple. */
    fun expire(now: Long): Boolean {
        if (firstAt == NONE || now - firstAt < windowMs) return false
        firstAt = NONE
        return true
    }

    private companion object {
        const val NONE = Long.MIN_VALUE
    }
}
