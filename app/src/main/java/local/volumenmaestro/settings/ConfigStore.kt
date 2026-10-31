package local.volumenmaestro.settings

import android.content.Context
import android.util.Base64
import local.volumenmaestro.engine.Shell
import java.io.File
import java.io.IOException

/**
 * Guarda la configuración en la carpeta privada del motor y una copia en
 * /sdcard/VolumenMaestro/ajustes.json. La copia se escribe y se lee por la shell,
 * así sobrevive a una reinstalación (una app nueva no puede leer archivos de la anterior).
 */
class ConfigStore(context: Context) {
    private val file = File(context.filesDir, "config.json")

    val exists: Boolean get() = file.exists()

    fun load(): Config = try {
        if (file.exists()) Config.fromJson(file.readText()) else Config()
    } catch (e: Exception) {
        Config()
    }

    fun save(config: Config) {
        val tmp = File(file.parentFile, "config.json.tmp")
        tmp.writeText(config.toJson())
        tmp.renameTo(file)
    }

    fun writeBackup(sh: Shell.Channel, config: Config) {
        val b64 = Base64.encodeToString(config.toJson().toByteArray(), Base64.NO_WRAP)
        sh.run("mkdir -p $BACKUP_DIR && echo $b64 | base64 -d > $BACKUP_FILE")
    }

    /** La copia de seguridad, o null si no hay o no se puede leer. */
    fun readBackup(sh: Shell.Channel): Config? = try {
        val text = sh.run("cat $BACKUP_FILE 2>/dev/null").trim()
        if (text.startsWith("{")) Config.fromJson(text) else null
    } catch (e: IOException) {
        null
    } catch (e: Exception) {
        null
    }

    companion object {
        const val BACKUP_DIR = "/sdcard/VolumenMaestro"
        const val BACKUP_FILE = "$BACKUP_DIR/ajustes.json"
    }
}
