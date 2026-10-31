package local.volumenmaestro

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import local.volumenmaestro.engine.Engine

/**
 * Servicio de accesibilidad: Android lo arranca con la TV y lo mantiene vivo, y le da al motor
 * ventanas encima de todo. No filtra teclas: con el filtro activo, Android deja de repetir las
 * teclas mantenidas (el atajo del control se escucha con getevent, ver Engine).
 * Conserva el nombre de la versión 1 para seguir habilitado al actualizar.
 */
class VolumeService : AccessibilityService() {
    override fun onServiceConnected() = Engine.start(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        Engine.stop()
        super.onDestroy()
    }
}
