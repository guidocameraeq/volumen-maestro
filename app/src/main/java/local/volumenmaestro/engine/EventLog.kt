package local.volumenmaestro.engine

import android.util.Log
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/** Últimos eventos del motor, para la pantalla Registro. */
class EventLog(private val capacity: Int = 100) {
    private val items = ArrayDeque<String>()
    private val format = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US)

    @Synchronized
    fun add(message: String) {
        Log.i(Shell.TAG, message)
        if (items.size == capacity) items.removeFirst()
        items.addLast(format.format(Date()) + "  " + message)
    }

    @Synchronized
    fun snapshot(): ArrayList<String> = ArrayList(items.reversed())
}
