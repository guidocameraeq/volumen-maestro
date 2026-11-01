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
    val rebootEnabled: Boolean = false,
    val rebootDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
    val rebootHour: Int = 5,
    val rebootMinute: Int = 0,
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
        put("rebootEnabled", rebootEnabled)
        put("rebootDays", JSONArray(rebootDays.sorted().map { it.name }))
        put("rebootHour", rebootHour)
        put("rebootMinute", rebootMinute)
    }.toString()

    companion object {
        const val MIN_DB_PER_POINT = 0.2f
        const val MAX_DB_PER_POINT = 1.0f
        /** 2: shortcutKey pasó de KEYCODE de Android a código crudo del kernel. */
        const val VERSION = 2
        private const val ANDROID_KEYCODE_INFO = 165
        private const val RAW_INFO_TCL = 0xC3

        /** En la versión 1 el atajo era un KEYCODE de Android; solo Info tiene equivalente conocido. */
        private fun migrateShortcut(version: Int, key: Int): Int = when {
            version >= VERSION -> key
            key == ANDROID_KEYCODE_INFO -> RAW_INFO_TCL
            else -> 0
        }

        /** Lee un JSON tolerando campos faltantes o valores viejos (copias de versiones anteriores). */
        fun fromJson(text: String): Config {
            val o = JSONObject(text)
            val d = Config()
            val days = o.optJSONArray("rebootDays")?.let { arr ->
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
                shortcutKey = migrateShortcut(o.optInt("version", 1), o.optInt("shortcutKey", d.shortcutKey)),
                shortcutMode = enumOrNull<ShortcutMode>(o.optString("shortcutMode")) ?: d.shortcutMode,
                rebootEnabled = o.optBoolean("rebootEnabled", d.rebootEnabled),
                rebootDays = days,
                rebootHour = o.optInt("rebootHour", d.rebootHour).coerceIn(0, 23),
                rebootMinute = o.optInt("rebootMinute", d.rebootMinute).coerceIn(0, 59),
            )
        }

        private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
            enumValues<T>().firstOrNull { it.name == name }
    }
}
