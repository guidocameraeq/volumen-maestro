package local.volumenmaestro.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import local.volumenmaestro.engine.Engine

/** Recibe las alarmas del reinicio programado y se las pasa al motor. */
class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Engine.onRebootAlarm(context.applicationContext, intent.action == RebootScheduler.ACTION_RETRY)
    }
}
