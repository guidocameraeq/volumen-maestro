package local.volumenmaestro.recientes

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import local.volumenmaestro.engine.Shell
import java.io.IOException
import java.util.concurrent.Executor

/**
 * El botón TCL Home como "Alt-Tab", del lado del motor: un toque abre el panel de apps abiertas
 * ([RecentsActivity]), dos toques vuelven a la app anterior. También saca las vistas previas.
 * Todo lo que toca la TV pasa por la shell; los comandos corren en [commands], nunca en [main].
 */
class AppPanel(
    private val context: Context,
    private val shell: () -> Shell.Channel,
    private val commands: Executor,
    private val main: Handler,
    private val log: (String) -> Unit,
) {
    private val taps = TapDetector(DOUBLE_TAP_MS)
    private var lastSnapshotAt = 0L

    private val singleTap = Runnable {
        if (taps.expire(SystemClock.uptimeMillis())) commands.execute { openPanel() }
    }

    /** El botón TCL Home (solo cuenta el "apretado"). Corre en [main]. */
    fun onKey(down: Boolean) {
        if (!down) return
        main.removeCallbacks(singleTap)
        if (taps.press(SystemClock.uptimeMillis())) {
            commands.execute { switchToPrevious() }
        } else {
            main.postDelayed(singleTap, DOUBLE_TAP_MS)
        }
    }

    /** Cada tanto, desde el watchdog del motor: una vista previa de la app que está en pantalla. */
    fun tick() {
        val now = SystemClock.uptimeMillis()
        if (now - lastSnapshotAt < SNAPSHOT_EVERY_MS) return
        if (!context.getSystemService(PowerManager::class.java).isInteractive) return
        lastSnapshotAt = now
        try {
            RecentApps.current(list())?.let { snapshot(it.packageName) }
        } catch (e: IOException) {
            // sin conexión: se reintenta en la próxima vuelta
        }
    }

    fun close(pkg: String) {
        if (pkg in RecentApps.PROTECTED) return
        try {
            closeTasks(listOf(pkg))
            log("Panel de apps: cerrada $pkg")
        } catch (e: IOException) {
            log("Panel de apps: no se pudo cerrar $pkg: ${e.message}")
        }
    }

    fun closeAll(packages: List<String>) {
        val closable = packages.filter { it !in RecentApps.PROTECTED }
        try {
            closeTasks(closable)
            log("Panel de apps: limpiadas ${closable.size} apps")
        } catch (e: IOException) {
            log("Panel de apps: no se pudo limpiar todo: ${e.message}")
        }
    }

    private fun list(): List<RecentApp> = RecentApps.parse(shell().run(RecentApps.COMMAND))

    /** Cierra las apps y las saca de la lista del sistema (`am stack remove` con el número de tarea). */
    private fun closeTasks(packages: List<String>) {
        val tasks = RecentApps.parse(shell().run(RecentApps.COMMAND), distinct = false)
        for (pkg in packages) {
            shell().run("am force-stop $pkg")
            tasks.filter { it.packageName == pkg }.forEach { shell().run("am stack remove ${it.taskId}") }
            Snapshots.delete(context, pkg)
        }
    }

    private fun openPanel() {
        try {
            val apps = list()
            RecentApps.current(apps)?.let { snapshot(it.packageName) }
            val intent = Intent(context, RecentsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putStringArrayListExtra(RecentsActivity.EXTRA_APPS, ArrayList(apps.map { it.packageName }))
                .putExtra(RecentsActivity.EXTRA_CURRENT, RecentApps.current(apps)?.packageName)
            main.post { context.startActivity(intent) }
        } catch (e: IOException) {
            log("Panel de apps: no se pudo leer la lista: ${e.message}")
        }
    }

    private fun switchToPrevious() {
        try {
            val target = RecentApps.previous(list()) ?: return
            val component = launchComponent(target.packageName) ?: return
            // Igual que el inicio: si la app sigue abierta, vuelve donde estaba.
            shell().run("am start -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER -n $component -f 0x10200000")
        } catch (e: IOException) {
            log("Panel de apps: no se pudo volver a la app anterior: ${e.message}")
        }
    }

    private fun launchComponent(pkg: String): String? {
        for (category in listOf("android.intent.category.LEANBACK_LAUNCHER", "android.intent.category.LAUNCHER")) {
            val out = shell().run("cmd package resolve-activity --brief -a android.intent.action.MAIN -c $category $pkg")
            out.lineSequence().lastOrNull { it.startsWith("$pkg/") }?.let { return it.trim() }
        }
        return null
    }

    private fun snapshot(pkg: String) {
        val command = Snapshots.captureCommand(context) ?: return
        // Solo si esa app es la que tiene el foco: no otra cosa encima (el panel, un menú, Ambient).
        if (!shell().run("dumpsys window | grep -m1 mCurrentFocus").contains(" $pkg/")) return
        shell().run(command)
        Snapshots.store(context, pkg)
    }

    companion object {
        /** TCL Home en el control TCL RC813 (`KEY_DASHBOARD` del kernel). */
        const val KEY_TCL_HOME = 0xCC
        const val DOUBLE_TAP_MS = 400L
        private const val SNAPSHOT_EVERY_MS = 60_000L
    }
}
