package local.volumenmaestro

import local.volumenmaestro.audio.MasterVolume
import local.volumenmaestro.settings.Config
import local.volumenmaestro.settings.CurveType
import local.volumenmaestro.settings.OverlayDuration
import local.volumenmaestro.settings.ShortcutMode
import local.volumenmaestro.settings.Transition
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class ConfigTest {
    @Test fun roundTrip() {
        val c = Config(
            curve = CurveType.AMPLIFIER, dbPerPoint = 0.7f, zeroIsSilence = false, transition = Transition.SMOOTH,
            themeId = "sable", overlayDuration = OverlayDuration.LONG, showSide = false, shortcutKey = 165,
            shortcutMode = ShortcutMode.SINGLE_PRESS, rebootEnabled = true,
            rebootDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), rebootHour = 4, rebootMinute = 30,
        )
        assertEquals(c, Config.fromJson(c.toJson()))
    }

    @Test fun oldOrPartialBackupUsesDefaults() {
        val c = Config.fromJson("""{"themeId":"neon","curve":"NO_EXISTE","dbPerPoint":5}""")
        assertEquals("neon", c.themeId)
        assertEquals(CurveType.EVEN_DB, c.curve)
        assertEquals(Config.MAX_DB_PER_POINT, c.dbPerPoint)
        assertEquals(Config().rebootDays, c.rebootDays)
    }

    @Test fun migratesInfoShortcutFromVersion1() {
        // En la v1 el atajo era KEYCODE_INFO (165); ahora es el código crudo del control (0xC3).
        assertEquals(0xC3, Config.fromJson("""{"version":1,"shortcutKey":165}""").shortcutKey)
        assertEquals(0, Config.fromJson("""{"version":1,"shortcutKey":166}""").shortcutKey)
        assertEquals(0xC3, Config.fromJson("""{"version":2,"shortcutKey":195}""").shortcutKey)
    }

    @Test fun parsesAudioFlingerFloat() {
        assertEquals(1f, MasterVolume.parseFloat("Result: Parcel(00000000 3f800000   '.......?')")!!, 0f)
        assertEquals(0.0961f, MasterVolume.parseFloat("Result: Parcel(00000000 3dc4d014   '.......=')")!!, 0.0001f)
    }
}
