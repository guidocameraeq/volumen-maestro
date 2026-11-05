package local.volumenmaestro.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import local.volumenmaestro.AdbClient
import local.volumenmaestro.AdbKeys
import local.volumenmaestro.VolumeService
import local.volumenmaestro.audio.AudioSetup
import local.volumenmaestro.audio.MasterVolume
import local.volumenmaestro.audio.MixVolume
import local.volumenmaestro.audio.VolumeCurve
import local.volumenmaestro.overlay.OverlayController
import local.volumenmaestro.overlay.ThemeRegistry
import local.volumenmaestro.recientes.AppPanel
import local.volumenmaestro.settings.Config
import local.volumenmaestro.settings.ConfigStore
import local.volumenmaestro.settings.ShortcutMode
import local.volumenmaestro.system.Optimizer
import local.volumenmaestro.system.RebootScheduler
import java.io.File
import java.io.IOException
import java.security.KeyPair
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * El motor: vive en el proceso :motor mientras el servicio de accesibilidad está activo.
 * El hilo principal maneja los carteles y las teclas; [commands] corre los comandos lentos.
 */
object Engine {
    private const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    private const val MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
    private const val WATCHDOG_MS = 10_000L
    private const val LONG_PRESS_MS = 2_000L
    private const val LEARN_TIMEOUT_MS = 20_000L
    private const val KEY_LAST_REBOOT_NIGHT = "reboot_last_night"

    val log = EventLog()
    val main = Handler(Looper.getMainLooper())
    val commands = Executors.newSingleThreadExecutor()

    @Volatile var service: VolumeService? = null
        private set
    val running: Boolean get() = service != null

    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var store: ConfigStore
    private lateinit var audio: AudioManager
    lateinit var shell: Shell
        private set
    private lateinit var master: MasterVolume
    private val mix = MixVolume { log.add(it) }
    lateinit var appPanel: AppPanel
        private set
    private var overlay: OverlayController? = null
    @Volatile private var keys: KeyPair? = null
    private var receiverRegistered = false

    @Volatile var config = Config()
        private set
    @Volatile var enabled = true
        private set
    @Volatile var status = "Iniciando"
        private set
    @Volatile var incompatible = false
        private set
    @Volatile var health: List<HealthCheck> = emptyList()
        private set
    @Volatile var backupFound: Config? = null
        private set

    @Volatile var learning = false
        private set
    @Volatile var learnedKey = 0
        private set
    @Volatile var rejectedKey = 0
        private set

    private var shortcutDown = false

    // ---------------------------------------------------------------- ciclo de vida

    fun start(s: VolumeService) {
        service = s
        app = s.applicationContext
        prefs = app.getSharedPreferences("volumen", Context.MODE_PRIVATE)
        store = ConfigStore(app)
        config = store.load()
        audio = app.getSystemService(AudioManager::class.java)
        shell = Shell({ keys }) {
            status = "Aceptá \"Permitir depuración\" en la TV"
            main.post { overlay?.showMessage("Volumen Maestro: aceptá el permiso de depuración") }
        }
        master = MasterVolume(shell.volume)
        appPanel = AppPanel(app, { shell.commands }, commands, main) { log.add(it) }
        overlay = OverlayController(s)
        applyOverlayConfig()
        enabled = true

        app.registerReceiver(receiver, IntentFilter().apply {
            addAction(VOLUME_CHANGED)
            addAction(MUTE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        receiverRegistered = true

        commands.execute {
            try {
                keys = AdbKeys.load(app)
            } catch (e: Exception) {
                status = "No se pudieron crear las claves: $e"
                log.add(status)
                return@execute
            }
            log.add("Motor iniciado")
            try {
                if (!store.exists) backupFound = store.readBackup(shell.commands)
                incompatible = master.read() == null
                if (incompatible) log.add("El volumen maestro no responde: TV incompatible")
                handleRebootMarker()
                AudioSetup.enable(shell.commands, prefs, restartNetflix = false)
                mix.ensure()
                status = "Conectado"
            } catch (e: IOException) {
                status = "Sin conexión: ${e.message}"
                log.add(status)
            }
            applyVolume(0)
            RebootScheduler.cancelLegacyAlarms(app)
            refreshHealth()
        }
        main.postDelayed(watchdog, WATCHDOG_MS)
        startEventReader()
    }

    fun stop() {
        eventsRunning = false
        readerGeneration++
        eventStream?.close() // destraba el lector, que está esperando una señal del control
        main.removeCallbacks(watchdog)
        if (receiverRegistered) app.unregisterReceiver(receiver)
        receiverRegistered = false
        overlay?.release()
        overlay = null
        master.stop()
        commands.execute {
            mix.release()
            shell.close()
        }
        service = null
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> onScreenOn()
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                else -> {
                    val stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (stream == AudioManager.STREAM_MUSIC) onTvVolumeChanged()
                }
            }
        }
    }

    private var lastIndex = -1
    private var lastMuted = false

    private fun onTvVolumeChanged() {
        if (!enabled) return
        val i = index()
        val m = muted()
        if (i == lastIndex && m == lastMuted) return
        lastIndex = i
        lastMuted = m
        overlay?.showVolume(i, maxIndex(), m)
        applyVolume(config.transition.millis)
    }

    /** Al prender la TV siempre queda ENCENDIDO, y se reafirman los ajustes por si TCL los cambió. */
    private fun onScreenOn() {
        if (!enabled) {
            setEnabled(true, "al prender la TV")
            return
        }
        commands.execute {
            try {
                AudioSetup.enable(shell.commands, prefs, restartNetflix = false)
            } catch (e: IOException) {
                log.add("No se pudieron revisar los ajustes al prender: ${e.message}")
            }
            mix.ensure()
            master.reapply()
        }
    }

    private val watchdog = object : Runnable {
        override fun run() {
            commands.execute {
                if (!enabled || incompatible) return@execute
                mix.ensure()
                if (config.appPanel) appPanel.tick()
                val real = master.read() ?: return@execute
                if (abs(real - expectedGain()) > 0.0005f) {
                    log.add("El volumen maestro cambió a $real, lo vuelvo a aplicar")
                    master.reapply()
                }
            }
            main.postDelayed(this, WATCHDOG_MS)
        }
    }

    // ---------------------------------------------------------------- volumen

    fun index() = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    fun maxIndex() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    fun muted() = audio.isStreamMute(AudioManager.STREAM_MUSIC)

    private fun expectedGain() = VolumeCurve.gain(index(), maxIndex(), muted(), config)

    private fun applyVolume(rampMillis: Int) {
        if (!enabled || incompatible) return
        master.setTarget(expectedGain(), rampMillis)
    }

    // ---------------------------------------------------------------- modos

    fun setEnabled(on: Boolean, reason: String) {
        enabled = on
        main.post {
            overlay?.showMessage(if (on) "Volumen Maestro: ENCENDIDO" else "Volumen Maestro: APAGADO · Dolby activado")
        }
        status = if (on) "Activando…" else "Desactivando…"
        commands.execute {
            try {
                if (on) {
                    AudioSetup.enable(shell.commands, prefs, restartNetflix = true)
                    mix.ensure()
                    applyVolume(0)
                } else {
                    AudioSetup.disable(shell.commands, prefs)
                    master.setTarget(1f, 0)
                    mix.release()
                }
                status = if (on) "Conectado" else "Apagado: la TV está como venía"
                log.add("Modo ${if (on) "ENCENDIDO" else "APAGADO"} ($reason)")
            } catch (e: IOException) {
                status = "No se pudo cambiar el modo: ${e.message}"
                log.add(status)
            }
            refreshHealth()
        }
    }

    // ---------------------------------------------------------------- configuración

    fun updateConfig(newConfig: Config) {
        val old = config
        config = newConfig
        store.save(newConfig)
        main.post { applyOverlayConfig() }
        if (old.curve != newConfig.curve || old.dbPerPoint != newConfig.dbPerPoint ||
            old.zeroIsSilence != newConfig.zeroIsSilence
        ) applyVolume(newConfig.transition.millis)
        if (old.rebootEnabled != newConfig.rebootEnabled || old.rebootDays != newConfig.rebootDays ||
            old.rebootHour != newConfig.rebootHour || old.rebootMinute != newConfig.rebootMinute ||
            old.rebootOncePerWeek != newConfig.rebootOncePerWeek
        ) {
            log.add(if (newConfig.rebootEnabled) "Reinicio al apagar actualizado" else "Reinicio al apagar apagado")
        }
        commands.execute {
            try {
                store.writeBackup(shell.commands, newConfig)
            } catch (e: IOException) {
                log.add("No se pudo guardar la copia de seguridad: ${e.message}")
            }
        }
    }

    private fun applyOverlayConfig() {
        overlay?.apply {
            theme = ThemeRegistry.byId(config.themeId)
            showSide = config.showSide
            durationMs = config.overlayDuration.millis
        }
    }

    fun restoreBackup() {
        backupFound?.let {
            backupFound = null
            updateConfig(it)
            log.add("Ajustes restaurados de la copia de seguridad")
        }
    }

    fun dismissBackup() {
        backupFound = null
        store.save(config)
    }

    fun exportBackup(): Boolean = try {
        store.writeBackup(shell.commands, config)
        log.add("Copia de seguridad exportada")
        true
    } catch (e: IOException) {
        false
    }

    fun importBackup(): Boolean {
        val c = store.readBackup(shell.commands) ?: return false
        updateConfig(c)
        log.add("Copia de seguridad importada")
        return true
    }

    fun previewTheme(id: String) {
        main.post {
            val o = overlay ?: return@post
            val saved = o.theme
            o.theme = ThemeRegistry.byId(id)
            o.showVolume(index(), maxIndex(), muted())
            main.postDelayed({ if (o.theme.id == id) o.theme = ThemeRegistry.byId(config.themeId) }, config.overlayDuration.millis + 400)
            if (saved.id == id) return@post
        }
    }

    // ---------------------------------------------------------------- chequeos

    fun refreshHealth() {
        val list = mutableListOf<HealthCheck>()
        val sh = shell.commands
        fun read(cmd: String) = try { sh.run(cmd).trim() } catch (e: IOException) { null }

        val connected = read("echo ok") == "ok"
        list += if (connected) HealthCheck("adb", "Conexión interna", Level.OK, "La app puede cambiar ajustes de la TV.")
        else HealthCheck("adb", "Conexión interna", Level.ERROR,
            if (shell.waitingForUser) "Aceptá \"Permitir depuración\" en la TV." else "No se pudo conectar: ${shell.lastError ?: "?"}",
            "Reintentar")
        if (!connected) {
            health = list
            return
        }
        list += if (read("settings get global adb_enabled") == "1")
            HealthCheck("debug", "Depuración USB", Level.OK, "Activada.")
        else HealthCheck("debug", "Depuración USB", Level.ERROR,
            "Está apagada: la app no puede funcionar. Activala en Settings → System → Developer options.")
        list += if (read("settings get global tcl_device_out_type") == AudioSetup.OUT_TYPE_OPTICAL.toString())
            HealthCheck("optical", "Salida de audio", Level.OK, "Optical: el sonido sale solo por el equipo.")
        else HealthCheck("optical", "Salida de audio", Level.WARN,
            "No está en \"Optical\". Cambiala en Display & Sound → Audio Output Settings → Audio Output.",
            "Abrir ajustes de audio")
        val spdif = read("settings get system tcl_sys_audio_spdif_outmode")
        val wantSpdif = if (enabled) AudioSetup.SPDIF_PCM else AudioSetup.SPDIF_AUTO
        list += if (spdif == wantSpdif.toString())
            HealthCheck("spdif", "Salida digital", Level.OK, if (enabled) "PCM: todo pasa por el volumen." else "Auto: el Dolby pasa al equipo.")
        else HealthCheck("spdif", "Salida digital", Level.WARN,
            "No está en ${if (enabled) "PCM" else "Auto"}.", "Arreglar")
        if (enabled) {
            val ok = try { AudioSetup.netflixDolbyOff(sh) } catch (e: IOException) { false }
            list += if (ok) HealthCheck("netflix", "Netflix sin Dolby", Level.OK, "Netflix manda audio común.")
            else HealthCheck("netflix", "Netflix sin Dolby", Level.WARN, "Netflix volvió a mandar Dolby.", "Arreglar")
        }
        // Solo informa: el modo lo elige el usuario en el menú de la TV (ADR-015).
        list += if (read("settings get system tcl_sys_network_standby_enabled") == "1")
            HealthCheck("standby", "Reposo", Level.OK, "Con red: Alexa y el Chromecast pueden prender la TV; la red la despierta de a ratos.")
        else HealthCheck("standby", "Reposo", Level.OK, "Sin red: la TV duerme del todo; Alexa no la puede prender.")
        list += if (!incompatible) HealthCheck("master", "Volumen maestro", Level.OK, "Responde.")
        else HealthCheck("master", "Volumen maestro", Level.ERROR,
            "No responde: puede que una actualización de la TV lo haya cambiado.", "Volver a probar")
        health = list
    }

    fun fix(id: String) {
        try {
            when (id) {
                "adb" -> shell.commands.run("echo ok")
                "optical" -> shell.commands.run("am start -n com.android.tv.settings/.MainSettings")
                "spdif", "netflix" ->
                    if (enabled) AudioSetup.enable(shell.commands, prefs, restartNetflix = id == "netflix")
                    else AudioSetup.disable(shell.commands, prefs)
                "master" -> {
                    incompatible = master.read() == null
                    if (!incompatible) master.reapply()
                }
            }
            log.add("Arreglo aplicado: $id")
        } catch (e: IOException) {
            log.add("No se pudo arreglar $id: ${e.message}")
        }
        refreshHealth()
    }

    fun repairAll() {
        for (c in health.filter { it.level != Level.OK && it.fixLabel != null && it.id != "optical" }) fix(c.id)
        master.reapply()
        refreshHealth()
    }

    // ---------------------------------------------------------------- optimizar y reinicio

    fun stats(): Optimizer.Stats? = try { Optimizer.stats(shell.commands) } catch (e: IOException) { null }

    fun optimize(): Optimizer.Result? = try {
        Optimizer.run(shell.commands).also {
            log.add("Optimizado: ${it.freedStorageMb} MB de espacio y ${it.freedMemoryMb} MB de memoria")
        }
    } catch (e: IOException) {
        log.add("No se pudo optimizar: ${e.message}")
        null
    }

    fun rebootNow() {
        log.add("Reinicio pedido desde la app")
        try { shell.commands.reboot() } catch (e: IOException) { log.add("No se pudo reiniciar: ${e.message}") }
    }

    /** Última noche en que se reinició al apagar (ver [RebootScheduler]), o null si nunca. */
    val lastRebootNight: LocalDate?
        get() = if (::prefs.isInitialized && prefs.contains(KEY_LAST_REBOOT_NIGHT))
            LocalDate.ofEpochDay(prefs.getLong(KEY_LAST_REBOOT_NIGHT, 0)) else null

    /**
     * Al apagar la TV: si es una noche elegida y todavía no se reinició, reinicia en el momento.
     * No se puede esperar: TCL suelta los wakelocks de las apps y duerme la TV a ~1,3 s de apretar
     * el botón (ADR-016). Si el hilo quedara dormido antes de reiniciar, al despertar la pantalla ya
     * está prendida y no reinicia: la noche queda sin marcar y se reintenta la próxima vez que se apague.
     */
    private fun onScreenOff() {
        val now = LocalDateTime.now()
        if (!RebootScheduler.shouldReboot(now, config, lastRebootNight)) return
        val night = RebootScheduler.nightOf(now, config.rebootDays, config.rebootHour, config.rebootMinute) ?: return
        prefs.edit().putLong(KEY_LAST_REBOOT_NIGHT, night.toEpochDay()).commit()
        val marker = File(app.filesDir, "reboot_marker")
        marker.writeText(System.currentTimeMillis().toString())
        log.add("TV apagada de noche: reiniciando")
        commands.execute {
            val failed = when {
                app.getSystemService(PowerManager::class.java).isInteractive -> "la TV se prendió antes"
                else -> try { shell.commands.reboot(); null } catch (e: IOException) { e.message ?: e.toString() }
            }
            if (failed != null) {
                log.add("Reinicio al apagar no hecho: $failed")
                prefs.edit().remove(KEY_LAST_REBOOT_NIGHT).apply()
                marker.delete()
            }
        }
    }

    /** Después de un reinicio al apagar, si la pantalla se prendió sola, la vuelve a apagar. */
    private fun handleRebootMarker() {
        val marker = File(app.filesDir, "reboot_marker")
        if (!marker.exists()) return
        val at = marker.readText().toLongOrNull() ?: 0L
        marker.delete()
        if (System.currentTimeMillis() - at > 20 * 60 * 1000) return
        log.add("Reinicio al apagar hecho")
        Thread.sleep(15_000)
        if (app.getSystemService(PowerManager::class.java).isInteractive) {
            shell.commands.run("input keyevent KEYCODE_SLEEP")
            log.add("Pantalla apagada después del reinicio")
        }
    }

    // ---------------------------------------------------------------- atajo del control

    /*
     * El atajo se escucha leyendo las señales crudas del control con `getevent` por ADB, en vez de
     * con el filtro de teclas de accesibilidad: ese filtro hace que Android deje de repetir las
     * teclas mantenidas (el volumen bajaba de a un paso). Las señales no se pueden "consumir":
     * el botón sigue haciendo lo suyo, por eso conviene uno que no se use.
     * Los códigos son los del kernel (Info en el TCL RC813 = 0xC3), no los KEYCODE de Android.
     */

    private val learnTimeout = Runnable { stopLearning() }
    @Volatile private var eventsRunning = false
    /*
     * Android a veces desconecta y reconecta el servicio de accesibilidad (por ejemplo al instalar o
     * quitar una app): stop() + start() en el mismo proceso. Cada lector lleva su número y se va
     * cuando ya no es el actual; si no, quedaban dos `getevent` y cada toque llegaba repetido.
     */
    @Volatile private var readerGeneration = 0
    @Volatile private var eventStream: AdbClient? = null

    fun startLearning() {
        learning = true
        learnedKey = 0
        rejectedKey = 0
        main.removeCallbacks(learnTimeout)
        main.postDelayed(learnTimeout, LEARN_TIMEOUT_MS)
    }

    fun stopLearning() {
        learning = false
        main.removeCallbacks(learnTimeout)
    }

    private fun startEventReader() {
        eventsRunning = true
        val generation = ++readerGeneration
        Thread({
            while (eventsRunning && generation == readerGeneration) {
                try {
                    val client = shell.openStream()
                    eventStream = client
                    try {
                        val pending = StringBuilder()
                        client.stream("getevent") { text ->
                            if (generation != readerGeneration) throw IOException("lector viejo")
                            pending.append(text)
                            while (true) {
                                val nl = pending.indexOf("\n")
                                if (nl < 0) break
                                parseEvent(pending.substring(0, nl))
                                pending.delete(0, nl + 1)
                            }
                        }
                    } finally {
                        client.close()
                    }
                } catch (e: Exception) {
                    if (eventsRunning && generation == readerGeneration) Thread.sleep(5_000)
                }
            }
        }, "atajo-control").apply { isDaemon = true; start() }
    }

    /** Una línea de getevent: "/dev/input/event9: 0001 00c3 00000001" (tipo, código, valor). */
    private fun parseEvent(line: String) {
        val parts = line.substringAfter(": ", "").trim().split(Regex("\\s+"))
        if (parts.size != 3 || parts[0] != "0001") return // solo EV_KEY
        val code = parts[1].toIntOrNull(16) ?: return
        val value = parts[2].toLongOrNull(16) ?: return
        if (value == 2L) return // repetición del kernel
        main.post { onRawKey(code, value == 1L) }
    }

    private fun onRawKey(code: Int, down: Boolean) {
        if (learning) {
            if (!down) return
            if (code in RAW_NOT_ALLOWED) {
                rejectedKey = code
                return
            }
            learnedKey = code
            stopLearning()
            log.add("Botón detectado: 0x%02X".format(code))
            return
        }
        if (code == AppPanel.KEY_TCL_HOME) {
            if (config.appPanel) appPanel.onKey(down)
            return
        }
        if (config.shortcutKey == 0 || code != config.shortcutKey) return
        when (config.shortcutMode) {
            ShortcutMode.SINGLE_PRESS -> if (down) toggleFromShortcut()
            ShortcutMode.LONG_PRESS -> if (down) {
                shortcutDown = true
                main.removeCallbacks(longPress)
                main.postDelayed(longPress, LONG_PRESS_MS)
            } else {
                shortcutDown = false
                main.removeCallbacks(longPress)
            }
        }
    }

    /** Teclas que no pueden ser atajo: volumen, encendido, flechas, OK, atrás e inicio. */
    private val RAW_NOT_ALLOWED = setOf(
        113, 114, 115, 116, // KEY_MUTE, VOLUMEDOWN, VOLUMEUP, POWER
        103, 108, 105, 106, // flechas
        28, 352, 353,       // ENTER, OK, SELECT
        1, 158, 102, 172,   // ESC, BACK, HOME, HOMEPAGE
    )

    private val longPress = Runnable {
        if (shortcutDown) toggleFromShortcut()
    }

    private fun toggleFromShortcut() = setEnabled(!enabled, "atajo del control")

    // ---------------------------------------------------------------- desinstalar

    /** Deja la TV como venía antes de que la interfaz abra el desinstalador. */
    fun prepareUninstall() {
        try {
            AudioSetup.disable(shell.commands, prefs)
            master.setTarget(1f, 0)
            mix.release()
            val component = "${app.packageName}/${VolumeService::class.java.name}"
            val shortComponent = "${app.packageName}/.VolumeService"
            val current = shell.commands.run("settings get secure enabled_accessibility_services").trim()
            val rest = current.split(':').filter { it.isNotBlank() && it != component && it != shortComponent && it != "null" }
            log.add("Desinstalación: TV restaurada")
            shell.commands.run(
                if (rest.isEmpty()) "settings delete secure enabled_accessibility_services"
                else "settings put secure enabled_accessibility_services ${rest.joinToString(":")}",
            )
        } catch (e: IOException) {
            log.add("Desinstalación incompleta: ${e.message}")
        }
    }
}
