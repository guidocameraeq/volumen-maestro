package local.volumenmaestro.settings

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

enum class CurveType { EVEN_DB, SMOOTH, AMPLIFIER }
enum class Transition(val millis: Int) { OFF(0), FAST(150), SMOOTH(400) }
enum class OverlayDuration(val millis: Long) { NORMAL(3_800), LONG(5_000), VERY_LONG(7_000) }
enum class ShortcutMode { SINGLE_PRESS, LONG_PRESS }

/** Todo lo que el usuario puede configurar. Se guarda como JSON en el proceso motor. */
data class Config(
    val curve: CurveType = CurveType.EVEN_DB,
    val dbPerPoint: Float = 0.5f,
    val zeroIsSilence: Boolean = true,
    val transition: Transition = Transition.OFF,
    val themeId: String = "clasico",
    val overlayDuration: OverlayDuration = OverlayDuration.NORMAL,
    val showSide: Boolean = true,
    /** Código crudo del kernel del botón del atajo (getevent; Info del TCL RC813 = 0xC3). 0 = sin atajo. */
    val shortcutKey: Int = 0,
    val shortcutMode: ShortcutMode = ShortcutMode.LONG_PRESS,
    /** TCL Home: un toque abre el panel de apps abiertas, dos vuelven a la app anterior. */
    val appPanel: Boolean = true,
    /** Reinicio al apagar: en [rebootDays], la primera vez que se apaga la TV desde [rebootHour]:[rebootMinute]. */
    val rebootEnabled: Boolean = false,
    val rebootDays: Set<DayOfWeek> = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    val rebootHour: Int = 22,
    val rebootMinute: Int = 0,
    /** true: una sola vez por semana (la primera noche elegida); false: una por cada noche elegida. */
    val rebootOncePerWeek: Boolean = false,
) {
    fun toJson(): String = JSONObject().apply {
        put("version", VERSION)
        put("curve", curve.name)
        put("dbPerPoint", dbPerPoint.toDouble())
        put("zeroIsSilence", zeroIsSilence)
        put("transition", transition.name)
        put("themeId", themeId)
        put("overlayDuration", overlayDuration.name)
        put("showSide", showSide)
        put("shortcutKey", shortcutKey)
        put("shortcutMode", shortcutMode.name)
        put("appPanel", appPanel)
        put("rebootEnabled", rebootEnabled)
        put("rebootDays", JSONArray(rebootDays.sorted().map { it.name }))
        put("rebootHour", rebootHour)
        put("rebootMinute", rebootMinute)
        put("rebootOncePerWeek", rebootOncePerWeek)
    }.toString()

    companion object {
        const val MIN_DB_PER_POINT = 0.2f
        const val MAX_DB_PER_POINT = 1.0f
        /**
         * 2: shortcutKey pasó de KEYCODE de Android a código crudo del kernel.
         * 3: el reinicio pasó de alarma a hora fija a "al apagar de noche"; días y hora vuelven al valor por defecto.
         */
        const val VERSION = 3
        private const val VERSION_RAW_SHORTCUT = 2
        private const val VERSION_REBOOT_AT_POWER_OFF = 3
        private const val ANDROID_KEYCODE_INFO = 165
        private const val RAW_INFO_TCL = 0xC3

        /** En la versión 1 el atajo era un KEYCODE de Android; solo Info tiene equivalente conocido. */
        private fun migrateShortcut(version: Int, key: Int): Int = when {
            version >= VERSION_RAW_SHORTCUT -> key
            key == ANDROID_KEYCODE_INFO -> RAW_INFO_TCL
            else -> 0
        }

        /** Lee un JSON tolerando campos faltantes o valores viejos (copias de versiones anteriores). */
        fun fromJson(text: String): Config {
            val o = JSONObject(text)
            val d = Config()
            val version = o.optInt("version", 1)
            // Los días y la hora de antes eran de la alarma (por ejemplo lunes a las 05:00): no sirven como "desde las".
            val keepRebootTime = version >= VERSION_REBOOT_AT_POWER_OFF
            val days = o.optJSONArray("rebootDays")?.takeIf { keepRebootTime }?.let { arr ->
                (0 until arr.length()).mapNotNull { enumOrNull<DayOfWeek>(arr.optString(it)) }.toSet()
            } ?: d.rebootDays
            return Config(
                curve = enumOrNull<CurveType>(o.optString("curve")) ?: d.curve,
                dbPerPoint = o.optDouble("dbPerPoint", d.dbPerPoint.toDouble()).toFloat()
                    .coerceIn(MIN_DB_PER_POINT, MAX_DB_PER_POINT),
                zeroIsSilence = o.optBoolean("zeroIsSilence", d.zeroIsSilence),
                transition = enumOrNull<Transition>(o.optString("transition")) ?: d.transition,
                themeId = o.optString("themeId", d.themeId).ifEmpty { d.themeId },
                overlayDuration = enumOrNull<OverlayDuration>(o.optString("overlayDuration")) ?: d.overlayDuration,
                showSide = o.optBoolean("showSide", d.showSide),
                shortcutKey = migrateShortcut(version, o.optInt("shortcutKey", d.shortcutKey)),
                shortcutMode = enumOrNull<ShortcutMode>(o.optString("shortcutMode")) ?: d.shortcutMode,
                appPanel = o.optBoolean("appPanel", d.appPanel),
                rebootEnabled = o.optBoolean("rebootEnabled", d.rebootEnabled),
                rebootDays = days,
                rebootHour = if (keepRebootTime) o.optInt("rebootHour", d.rebootHour).coerceIn(0, 23) else d.rebootHour,
                rebootMinute = if (keepRebootTime) o.optInt("rebootMinute", d.rebootMinute).coerceIn(0, 59) else d.rebootMinute,
                rebootOncePerWeek = o.optBoolean("rebootOncePerWeek", d.rebootOncePerWeek),
            )
        }

        private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
            enumValues<T>().firstOrNull { it.name == name }
    }
}
