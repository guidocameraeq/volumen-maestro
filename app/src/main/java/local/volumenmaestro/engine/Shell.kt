package local.volumenmaestro.engine

import android.util.Log
import local.volumenmaestro.AdbClient
import java.io.IOException
import java.security.KeyPair

/**
 * Comandos con permisos de shell a través del adbd de la propia TV (127.0.0.1:5555).
 * Hay dos canales para que una tarea larga (optimizar) no frene los cambios de volumen.
 */
class Shell(private val keys: () -> KeyPair?, private val onWaitingForUser: () -> Unit) {

    inner class Channel(private val name: String) {
        private var client: AdbClient? = null

        @Synchronized
        fun run(command: String): String {
            var attempt = 0
            while (true) {
                try {
                    val c = client ?: connect().also { client = it }
                    val out = c.shell(command)
                    lastError = null
                    return out
                } catch (e: IOException) {
                    client?.close()
                    client = null
                    if (++attempt >= 2) {
                        lastError = e.message ?: e.toString()
                        Log.w(TAG, "[$name] falló: $command", e)
                        throw e
                    }
                }
            }
        }

        @Synchronized
        fun reboot() {
            val c = client ?: connect().also { client = it }
            c.reboot()
        }

        @Synchronized
        fun close() {
            client?.close()
            client = null
        }
    }

    @Volatile var lastError: String? = null
        private set
    @Volatile var waitingForUser = false
        private set

    val volume = Channel("volumen")
    val commands = Channel("comandos")

    /** Conexión aparte para un comando que no termina (getevent). La cierra quien la usa. */
    fun openStream(): AdbClient = connect()

    val connected: Boolean get() = lastError == null && !waitingForUser

    private fun connect(): AdbClient {
        val k = keys() ?: throw IOException("claves no listas")
        val c = AdbClient.connect("127.0.0.1", 5555, k) {
            waitingForUser = true
            onWaitingForUser()
        }
        waitingForUser = false
        return c
    }

    fun close() {
        volume.close()
        commands.close()
    }

    companion object {
        const val TAG = "VolumenMaestro"
    }
}
