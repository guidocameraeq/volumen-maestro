package local.volumenmaestro

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import local.volumenmaestro.engine.Engine

/**
 * Servicio de accesibilidad: Android lo arranca con la TV y lo mantiene vivo.
 * Le da al motor lo que solo un servicio así puede tener: ventanas encima de todo y
 * acceso a las teclas del control (para el atajo). Conserva el nombre de la versión 1.
 */
class VolumeService : AccessibilityService() {
    override fun onServiceConnected() = Engine.start(this)

    override fun onKeyEvent(event: KeyEvent): Boolean = Engine.onKey(event)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        Engine.stop()
        super.onDestroy()
    }
}
