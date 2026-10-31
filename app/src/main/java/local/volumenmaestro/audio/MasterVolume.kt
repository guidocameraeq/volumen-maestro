package local.volumenmaestro.audio

import local.volumenmaestro.engine.Shell
import java.io.IOException
import java.util.Locale

/**
 * Volumen maestro de AudioFlinger, con transiciones suaves. Todo corre en un hilo propio:
 * si llega un valor nuevo a mitad de una rampa, sigue desde donde estaba.
 */
class MasterVolume(private val channel: Shell.Channel) {
    private val lock = Object()
    private var target = 1f
    private var rampMillis = 0
    private var dirty = false
    @Volatile private var running = true

    /** Último valor que mandamos; -1 = desconocido. */
    @Volatile var current = -1f
        private set

    private val thread = Thread(::loop, "volumen-maestro").apply { isDaemon = true; start() }

    fun setTarget(gain: Float, rampMillis: Int) {
        synchronized(lock) {
            target = gain.coerceIn(0f, 1f)
            this.rampMillis = rampMillis
            dirty = true
            lock.notifyAll()
        }
    }

    /** Vuelve a mandar el valor objetivo (por ejemplo si el sistema lo reseteó). */
    fun reapply() {
        current = -1f
        synchronized(lock) {
            dirty = true
            rampMillis = 0
            lock.notifyAll()
        }
    }

    /** Lee el volumen maestro real de AudioFlinger, o null si no responde. */
    fun read(): Float? = try {
        parseFloat(channel.run("service call media.audio_flinger $TX_GET"))
    } catch (e: IOException) {
        null
    }

    fun stop() {
        running = false
        synchronized(lock) { lock.notifyAll() }
    }

    private fun loop() {
        while (running) {
            val goal: Float
            val ramp: Int
            synchronized(lock) {
                while (!dirty && running) lock.wait()
                if (!running) return
                dirty = false
                goal = target
                ramp = rampMillis
            }
            try {
                val from = current
                if (from < 0f || ramp <= 0 || from == goal) {
                    send(goal)
                    continue
                }
                val steps = (ramp / STEP_MS).coerceAtLeast(1)
                val fromDb = VolumeCurve.gainToDb(from)
                val toDb = VolumeCurve.gainToDb(goal)
                var interrupted = false
                for (i in 1 until steps) {
                    if (dirtyNow()) { interrupted = true; break }
                    send(VolumeCurve.dbToGain(fromDb + (toDb - fromDb) * i / steps))
                    Thread.sleep(STEP_MS.toLong())
                }
                if (!interrupted) send(goal)
            } catch (e: IOException) {
                current = -1f // se reintenta en el próximo cambio o en el watchdog
            } catch (e: InterruptedException) {
                return
            }
        }
    }

    private fun dirtyNow(): Boolean = synchronized(lock) { dirty }

    private fun send(gain: Float) {
        channel.run(String.format(Locale.US, "service call media.audio_flinger %d f %.6f", TX_SET, gain))
        current = gain
    }

    companion object {
        // Códigos de IAudioFlingerService en Android 12.
        const val TX_SET = 7
        const val TX_GET = 9
        private const val STEP_MS = 25
        private val PARCEL_FLOAT = Regex("Parcel\\(00000000 ([0-9a-f]{8})")

        fun parseFloat(out: String): Float? =
            PARCEL_FLOAT.find(out)?.groupValues?.get(1)?.toLong(16)?.toInt()?.let(java.lang.Float::intBitsToFloat)
    }
}
