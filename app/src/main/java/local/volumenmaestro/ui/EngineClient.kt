package local.volumenmaestro.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import local.volumenmaestro.engine.EngineService
import local.volumenmaestro.engine.HealthCheck
import local.volumenmaestro.engine.Protocol
import local.volumenmaestro.settings.Config

/** Lo que la interfaz sabe del motor. */
data class EngineState(
    val bound: Boolean = false,
    val running: Boolean = false,
    val enabled: Boolean = true,
    val status: String = "",
    val connected: Boolean = false,
    val waitingAuth: Boolean = false,
    val incompatible: Boolean = false,
    val index: Int = 0,
    val max: Int = 100,
    val muted: Boolean = false,
    val config: Config = Config(),
    val health: List<HealthCheck> = emptyList(),
    val learning: Boolean = false,
    val learnedKey: Int = 0,
    val rejectedKey: Int = 0,
    val backupFound: Boolean = false,
    val nextReboot: Long = 0,
)

/** Se conecta al EngineService (proceso :motor) y le manda comandos por Messenger. */
class EngineClient(private val context: Context) {
    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state

    private var service: Messenger? = null
    private val callbacks = HashMap<Int, ArrayDeque<(Bundle) -> Unit>>()
    private val main = Handler(Looper.getMainLooper())

    private val incoming = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val data = msg.data ?: Bundle()
            if (data.containsKey(Protocol.K_RUNNING)) _state.value = parse(data)
            callbacks[msg.what]?.removeFirstOrNull()?.invoke(data)
        }
    })

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = Messenger(binder)
            _state.value = _state.value.copy(bound = true)
            refresh()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            _state.value = _state.value.copy(bound = false, running = false)
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            refresh()
            main.postDelayed(this, 1_000)
        }
    }

    fun start() {
        context.bindService(Intent(context, EngineService::class.java), connection, Context.BIND_AUTO_CREATE)
        main.post(poll)
    }

    fun stop() {
        main.removeCallbacks(poll)
        try {
            context.unbindService(connection)
        } catch (_: IllegalArgumentException) {
        }
        service = null
    }

    fun refresh() = send(Protocol.GET_STATE)

    fun send(what: Int, data: Bundle = Bundle(), onReply: ((Bundle) -> Unit)? = null) {
        val s = service ?: return
        if (onReply != null) callbacks.getOrPut(what) { ArrayDeque() }.addLast(onReply)
        try {
            s.send(Message.obtain(null, what).apply {
                this.data = data
                replyTo = incoming
            })
        } catch (_: RemoteException) {
        }
    }

    fun setEnabled(on: Boolean) = send(Protocol.SET_ENABLED, Bundle().apply { putBoolean(Protocol.K_VALUE, on) })

    fun setConfig(config: Config) {
        _state.value = _state.value.copy(config = config) // se ve al instante, sin esperar al motor
        send(Protocol.SET_CONFIG, Bundle().apply { putString(Protocol.K_CONFIG, config.toJson()) })
    }

    fun withId(what: Int, id: String, onReply: ((Bundle) -> Unit)? = null) =
        send(what, Bundle().apply { putString(Protocol.K_ID, id) }, onReply)

    private fun parse(b: Bundle): EngineState {
        if (!b.getBoolean(Protocol.K_RUNNING)) return EngineState(bound = true, running = false)
        return EngineState(
            bound = true,
            running = true,
            enabled = b.getBoolean(Protocol.K_ENABLED),
            status = b.getString(Protocol.K_STATUS) ?: "",
            connected = b.getBoolean(Protocol.K_CONNECTED),
            waitingAuth = b.getBoolean(Protocol.K_WAITING_AUTH),
            incompatible = b.getBoolean(Protocol.K_INCOMPATIBLE),
            index = b.getInt(Protocol.K_INDEX),
            max = b.getInt(Protocol.K_MAX, 100),
            muted = b.getBoolean(Protocol.K_MUTED),
            config = b.getString(Protocol.K_CONFIG)?.let { Config.fromJson(it) } ?: Config(),
            health = HealthCheck.listFromJson(b.getString(Protocol.K_HEALTH)),
            learning = b.getBoolean(Protocol.K_LEARNING),
            learnedKey = b.getInt(Protocol.K_LEARNED),
            rejectedKey = b.getInt(Protocol.K_REJECTED),
            backupFound = b.getBoolean(Protocol.K_BACKUP_FOUND),
            nextReboot = b.getLong(Protocol.K_NEXT_REBOOT),
        )
    }
}
