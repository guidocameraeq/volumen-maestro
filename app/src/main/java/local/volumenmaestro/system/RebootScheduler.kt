package local.volumenmaestro.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import local.volumenmaestro.settings.Config
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

/** Calcula y programa el reinicio semanal. */
object RebootScheduler {
    const val ACTION_REBOOT = "local.volumenmaestro.REBOOT"
    const val ACTION_RETRY = "local.volumenmaestro.REBOOT_RETRY"
    /** Si la TV está en uso a la hora programada, se espera hasta esto a que se apague. */
    const val MAX_WAIT_MS = 3 * 60 * 60 * 1000L
    const val RETRY_MS = 10 * 60 * 1000L

    /** Próximo momento programado estrictamente después de [now], o null si no hay días elegidos. */
    fun next(now: LocalDateTime, days: Set<DayOfWeek>, hour: Int, minute: Int): LocalDateTime? {
        if (days.isEmpty()) return null
        for (offset in 0..7) {
            val candidate = now.toLocalDate().plusDays(offset.toLong()).atTime(hour, minute)
            if (candidate.dayOfWeek in days && candidate.isAfter(now)) return candidate
        }
        return null
    }

    fun nextMillis(config: Config, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!config.rebootEnabled) return null
        val n = next(LocalDateTime.now(zone), config.rebootDays, config.rebootHour, config.rebootMinute) ?: return null
        return n.atZone(zone).toInstant().toEpochMilli()
    }

    fun schedule(context: Context, config: Config) {
        cancel(context, ACTION_REBOOT)
        val at = nextMillis(config) ?: return
        set(context, ACTION_REBOOT, at)
    }

    fun scheduleRetry(context: Context) = set(context, ACTION_RETRY, System.currentTimeMillis() + RETRY_MS)

    fun cancelAll(context: Context) {
        cancel(context, ACTION_REBOOT)
        cancel(context, ACTION_RETRY)
    }

    private fun set(context: Context, action: String, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = intent(context, action)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exactAllowed) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun cancel(context: Context, action: String) =
        context.getSystemService(AlarmManager::class.java).cancel(intent(context, action))

    private fun intent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(),
        Intent(context, RebootReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
