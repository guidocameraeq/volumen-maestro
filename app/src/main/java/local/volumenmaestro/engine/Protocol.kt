package local.volumenmaestro.engine

import org.json.JSONArray
import org.json.JSONObject

/** Mensajes entre la interfaz (proceso principal) y el motor (proceso :motor). */
object Protocol {
    const val GET_STATE = 1
    const val SET_ENABLED = 2
    const val SET_CONFIG = 3
    const val REPAIR_ALL = 4
    const val FIX = 5
    const val OPTIMIZE = 6
    const val GET_STATS = 7
    const val REBOOT_NOW = 8
    const val PREVIEW_THEME = 9
    const val START_LEARN = 10
    const val STOP_LEARN = 11
    const val RESTORE_BACKUP = 12
    const val DISMISS_BACKUP = 13
    const val EXPORT_BACKUP = 14
    const val IMPORT_BACKUP = 15
    const val PREPARE_UNINSTALL = 16
    const val GET_LOG = 17

    const val K_RUNNING = "running"
    const val K_ENABLED = "enabled"
    const val K_STATUS = "status"
    const val K_CONNECTED = "connected"
    const val K_WAITING_AUTH = "waitingAuth"
    const val K_INCOMPATIBLE = "incompatible"
    const val K_INDEX = "index"
    const val K_MAX = "max"
    const val K_MUTED = "muted"
    const val K_CONFIG = "config"
    const val K_HEALTH = "health"
    const val K_LEARNING = "learning"
    const val K_LEARNED = "learned"
    const val K_REJECTED = "rejected"
    const val K_BACKUP_FOUND = "backupFound"
    const val K_NEXT_REBOOT = "nextReboot"
    const val K_VALUE = "value"
    const val K_ID = "id"
    const val K_OK = "ok"
    const val K_MESSAGE = "message"
    const val K_LOG = "log"
    const val K_MEM_AVAIL = "memAvail"
    const val K_MEM_TOTAL = "memTotal"
    const val K_DATA_FREE = "dataFree"
    const val K_UPTIME = "uptime"
    const val K_FREED_STORAGE = "freedStorage"
    const val K_FREED_MEMORY = "freedMemory"
}

enum class Level { OK, WARN, ERROR }

/** Un renglón del semáforo de Inicio. */
data class HealthCheck(
    val id: String,
    val title: String,
    val level: Level,
    val detail: String,
    /** Texto del botón para arreglarlo, o null si solo es un aviso. */
    val fixLabel: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("level", level.name)
        .put("detail", detail).put("fix", fixLabel ?: "")

    companion object {
        fun listToJson(list: List<HealthCheck>): String = JSONArray(list.map { it.toJson() }).toString()

        fun listFromJson(text: String?): List<HealthCheck> {
            if (text.isNullOrEmpty()) return emptyList()
            val arr = JSONArray(text)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HealthCheck(
                    o.getString("id"), o.getString("title"), Level.valueOf(o.getString("level")),
                    o.getString("detail"), o.optString("fix").ifEmpty { null },
                )
            }
        }
    }
}
