package local.volumenmaestro.engine

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import local.volumenmaestro.settings.Config
import local.volumenmaestro.system.RebootScheduler

/**
 * Puerta de entrada de la interfaz al motor. Corre en el proceso :motor y responde por Messenger.
 * Los comandos lentos se ejecutan en Engine.commands y responden cuando terminan.
 */
class EngineService : Service() {

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val replyTo = msg.replyTo
            val data = msg.data ?: Bundle()
            val what = msg.what
            if (!Engine.running && what != Protocol.GET_STATE) {
                reply(replyTo, what, Bundle().apply { putBoolean(Protocol.K_OK, false) })
                return
            }
            when (what) {
                Protocol.GET_STATE -> reply(replyTo, what, state())
                Protocol.SET_ENABLED -> {
                    Engine.setEnabled(data.getBoolean(Protocol.K_VALUE), "desde la app")
                    reply(replyTo, what, state())
                }
                Protocol.SET_CONFIG -> {
                    data.getString(Protocol.K_CONFIG)?.let { Engine.updateConfig(Config.fromJson(it)) }
                    reply(replyTo, what, state())
                }
                Protocol.PREVIEW_THEME -> {
                    Engine.previewTheme(data.getString(Protocol.K_ID) ?: "")
                    reply(replyTo, what, Bundle())
                }
                Protocol.START_LEARN -> { Engine.startLearning(); reply(replyTo, what, state()) }
                Protocol.STOP_LEARN -> { Engine.stopLearning(); reply(replyTo, what, state()) }
                Protocol.RESTORE_BACKUP -> { Engine.restoreBackup(); reply(replyTo, what, state()) }
                Protocol.DISMISS_BACKUP -> { Engine.dismissBackup(); reply(replyTo, what, state()) }
                Protocol.GET_LOG -> reply(replyTo, what, Bundle().apply {
                    putStringArrayList(Protocol.K_LOG, Engine.log.snapshot())
                })
                else -> slow(replyTo, what, data)
            }
        }
    }

    private fun slow(replyTo: Messenger?, what: Int, data: Bundle): Unit = Engine.commands.execute {
        val out = Bundle()
        when (what) {
            Protocol.REPAIR_ALL -> Engine.repairAll()
            Protocol.FIX -> Engine.fix(data.getString(Protocol.K_ID) ?: "")
            Protocol.OPTIMIZE -> Engine.optimize()?.let {
                out.putBoolean(Protocol.K_OK, true)
                out.putLong(Protocol.K_FREED_STORAGE, it.freedStorageMb)
                out.putLong(Protocol.K_FREED_MEMORY, it.freedMemoryMb)
            }
            Protocol.GET_STATS -> Engine.stats()?.let {
                out.putLong(Protocol.K_MEM_AVAIL, it.memAvailableKb)
                out.putLong(Protocol.K_MEM_TOTAL, it.memTotalKb)
                out.putLong(Protocol.K_DATA_FREE, it.dataFreeKb)
                out.putLong(Protocol.K_UPTIME, it.uptimeSec)
            }
            Protocol.REBOOT_NOW -> Engine.rebootNow()
            Protocol.EXPORT_BACKUP -> out.putBoolean(Protocol.K_OK, Engine.exportBackup())
            Protocol.IMPORT_BACKUP -> out.putBoolean(Protocol.K_OK, Engine.importBackup())
            Protocol.PREPARE_UNINSTALL -> { Engine.prepareUninstall(); out.putBoolean(Protocol.K_OK, true) }
            Protocol.CLOSE_APP -> Engine.appPanel.close(data.getString(Protocol.K_ID) ?: "")
            Protocol.CLOSE_ALL -> Engine.appPanel.closeAll(data.getStringArrayList(Protocol.K_APPS).orEmpty())
        }
        if (what == Protocol.REPAIR_ALL || what == Protocol.FIX) out.putAll(state())
        handler.post { reply(replyTo, what, out) }
    }

    private fun state() = Bundle().apply {
        putBoolean(Protocol.K_RUNNING, Engine.running)
        if (!Engine.running) return@apply
        putBoolean(Protocol.K_ENABLED, Engine.enabled)
        putString(Protocol.K_STATUS, Engine.status)
        putBoolean(Protocol.K_CONNECTED, Engine.shell.connected)
        putBoolean(Protocol.K_WAITING_AUTH, Engine.shell.waitingForUser)
        putBoolean(Protocol.K_INCOMPATIBLE, Engine.incompatible)
        putInt(Protocol.K_INDEX, Engine.index())
        putInt(Protocol.K_MAX, Engine.maxIndex())
        putBoolean(Protocol.K_MUTED, Engine.muted())
        putString(Protocol.K_CONFIG, Engine.config.toJson())
        putString(Protocol.K_HEALTH, HealthCheck.listToJson(Engine.health))
        putBoolean(Protocol.K_LEARNING, Engine.learning)
        putInt(Protocol.K_LEARNED, Engine.learnedKey)
        putInt(Protocol.K_REJECTED, Engine.rejectedKey)
        putBoolean(Protocol.K_BACKUP_FOUND, Engine.backupFound != null)
        putLong(Protocol.K_NEXT_REBOOT, RebootScheduler.nextMillis(Engine.config) ?: 0L)
    }

    private fun reply(to: Messenger?, what: Int, data: Bundle) {
        to ?: return
        try {
            to.send(Message.obtain(null, what).apply { this.data = data })
        } catch (_: RemoteException) {
        }
    }

    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent): IBinder = messenger.binder
}
