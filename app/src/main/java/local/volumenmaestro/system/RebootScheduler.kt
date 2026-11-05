package local.volumenmaestro.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import local.volumenmaestro.settings.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reinicio al apagar: en los días elegidos, la primera vez que se apaga la TV desde la hora elegida
 * hasta las 06:00 siguientes ("la noche" de ese día), se reinicia. No usa alarmas: en reposo sin red
 * (Doze, ADR-015) nada despierta a la TV para que suenen.
 */
object RebootScheduler {
    /** La noche de un día termina a esta hora. */
    const val NIGHT_END_HOUR = 6

    /** Día elegido cuya noche contiene a [now], o null si [now] no cae en ninguna. */
    fun nightOf(now: LocalDateTime, days: Set<DayOfWeek>, hour: Int, minute: Int): LocalDate? {
        for (back in 0L..1L) {
            val day = now.toLocalDate().minusDays(back)
            if (day.dayOfWeek !in days) continue
            val start = day.atTime(hour, minute)
            val end = if (hour < NIGHT_END_HOUR) day.atTime(NIGHT_END_HOUR, 0) else day.plusDays(1).atTime(NIGHT_END_HOUR, 0)
            if (!now.isBefore(start) && now.isBefore(end)) return day
        }
        return null
    }

    /** ¿Hay que reiniciar si se apaga la TV en [now]? [lastNight] es la última noche en que se reinició. */
    fun shouldReboot(now: LocalDateTime, config: Config, lastNight: LocalDate?): Boolean {
        if (!config.rebootEnabled) return false
        val night = nightOf(now, config.rebootDays, config.rebootHour, config.rebootMinute) ?: return false
        if (lastNight == null) return true
        val daysSince = night.toEpochDay() - lastNight.toEpochDay()
        return if (config.rebootOncePerWeek) daysSince >= 7 else daysSince >= 1
    }

    /**
     * Comienzo de la próxima noche en que apagar la TV la reinicia (la actual si ya empezó y todavía
     * no se reinició), o null si no hay días elegidos o el reinicio está apagado.
     */
    fun nextNight(now: LocalDateTime, config: Config, lastNight: LocalDate?): LocalDateTime? {
        if (!config.rebootEnabled || config.rebootDays.isEmpty()) return null
        for (offset in -1L..14L) {
            val day = now.toLocalDate().plusDays(offset)
            if (day.dayOfWeek !in config.rebootDays) continue
            val start = day.atTime(config.rebootHour, config.rebootMinute)
            val end = if (config.rebootHour < NIGHT_END_HOUR) day.atTime(NIGHT_END_HOUR, 0)
                else day.plusDays(1).atTime(NIGHT_END_HOUR, 0)
            if (!now.isBefore(end)) continue
            if (shouldReboot(maxOf(now, start), config, lastNight)) return start
        }
        return null
    }

    fun nextNightMillis(config: Config, lastNight: LocalDate?, zone: ZoneId = ZoneId.systemDefault()): Long? =
        nextNight(LocalDateTime.now(zone), config, lastNight)?.atZone(zone)?.toInstant()?.toEpochMilli()

    /** Cancela las alarmas del reinicio a hora fija que dejó la versión 2.3 (su receptor ya no existe). */
    fun cancelLegacyAlarms(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (action in listOf("local.volumenmaestro.REBOOT", "local.volumenmaestro.REBOOT_RETRY")) {
            val intent = Intent(action).setComponent(ComponentName(context.packageName, "local.volumenmaestro.system.RebootReceiver"))
            PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                ?.let { am.cancel(it); it.cancel() }
        }
    }
}
