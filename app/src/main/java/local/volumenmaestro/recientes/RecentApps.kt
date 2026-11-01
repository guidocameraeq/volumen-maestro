package local.volumenmaestro.recientes

/** Una app abierta, según la lista de tareas del sistema. */
data class RecentApp(val taskId: Int, val packageName: String, val visible: Boolean)

/**
 * Lee la lista de apps abiertas de `dumpsys activity recents` (Google TV no tiene pantalla de
 * recientes, pero el sistema la sigue llevando, ordenada de la más nueva a la más vieja).
 */
object RecentApps {
    const val COMMAND = "dumpsys activity recents"

    /** Nunca aparecen en el panel: el inicio, ajustes, pantallas del sistema y la propia app. */
    val HIDDEN = setOf(
        "local.volumenmaestro",
        "com.google.android.apps.tv.launcherx",
        "com.google.android.tvlauncher",
        "com.android.tv.settings",
        "com.tcl.dashboard",
        "com.tcl.gamebar",
        "com.google.android.apps.tv.dreamx",
        "com.google.android.katniss",
        "com.android.systemui",
    )

    /** Nunca se cierran con "Limpiar todo": Alexa, Ambient, Game Bar, el inicio y la propia app. */
    val PROTECTED = setOf(
        "com.tcl.smartalexa",
        "com.tcl.ttvs",
        "com.google.android.apps.tv.dreamx",
        "com.tcl.gamebar",
        "com.google.android.apps.tv.launcherx",
        "local.volumenmaestro",
    )

    // "  * Recent #3: Task{e1e8ae2 #5155 type=standard A=10096:com.disney.disneyplus U=0 visible=false ... sz=1}"
    private val HEADER = Regex("""^\s*\* Recent #\d+: Task\{\S+ #(\d+) type=(\w+) .*?visible=(true|false)""")
    // "    mActivityComponent=com.disney.disneyplus/com.bamtechmedia.dominguez.main.MainActivity"
    private val COMPONENT = Regex("""^\s*mActivityComponent=([^/\s]+)/""")

    /**
     * Las apps recientes (tareas "standard"), sin las ocultas ni repetidas, en el orden del sistema.
     * Incluye las que la TV cerró por memoria (siguen en la lista; al abrirlas arrancan de cero).
     * Las "undefined" son tareas viejas guardadas de antes de reiniciar: no cuentan.
     * Con [distinct] en false trae todas las tareas de cada app (para cerrarlas todas).
     */
    fun parse(dump: String, distinct: Boolean = true): List<RecentApp> {
        val out = ArrayList<RecentApp>()
        val seen = HashSet<String>()
        var taskId = -1
        var live = false
        var visible = false
        for (line in dump.lineSequence()) {
            val header = HEADER.find(line)
            if (header != null) {
                taskId = header.groupValues[1].toInt()
                live = header.groupValues[2] == "standard"
                visible = header.groupValues[3] == "true"
                continue
            }
            if (!live) continue
            val pkg = COMPONENT.find(line)?.groupValues?.get(1) ?: continue
            live = false
            if (pkg in HIDDEN || (!seen.add(pkg) && distinct)) continue
            out += RecentApp(taskId, pkg, visible)
        }
        return out
    }

    /** La app que está en pantalla, si es una de la lista. */
    fun current(apps: List<RecentApp>): RecentApp? = apps.firstOrNull { it.visible }

    /** La app anterior: la más nueva que no está en pantalla (desde el inicio, la última usada). */
    fun previous(apps: List<RecentApp>): RecentApp? = apps.firstOrNull { !it.visible }
}
