package local.volumenmaestro.audio

import android.content.SharedPreferences
import local.volumenmaestro.engine.Shell

/**
 * Los ajustes de la TV que cambian entre los dos modos.
 *
 * Encendido: todo el audio sale como PCM por la fibra (sin Dolby), así el volumen maestro
 * le llega a todas las apps, Netflix incluido.
 * Apagado: como venía la TV: las apps mandan Dolby y la fibra lo pasa tal cual al equipo.
 */
object AudioSetup {
    /** Lo que trae Netflix de fábrica en esta TV, por si no llegamos a leer el original. */
    private const val NETFLIX_ORIGINAL =
        "{\"audiocaps\":{\"continuousAudio\":true,\"aaudio\":true,\"aacLatencyCompensation\":0," +
            "\"uiAudioBufferDelayOffset\":70,\"pcm\":{\"mixing\":true,\"transition\":\"ease\",\"bufSize\":8192}," +
            "\"ddplus\":{\"enabled\":true,\"mixing\":true,\"transition\":\"ease\",\"bufSize\":13824}," +
            "\"atmos\":{\"enabled\":true,\"mixing\":true,\"transition\":\"ease\",\"bufSize\":21504,\"virtualization\":true}}}"

    private const val DOLBY_ON = "\"ddplus\":{\"enabled\":true"
    private const val DOLBY_OFF = "\"ddplus\":{\"enabled\":false"
    private const val PREF_ORIGINAL = "netflix_original"

    // Valores de "Digital Audio Out" (tcl_sys_audio_spdif_outmode).
    const val SPDIF_PCM = 1
    const val SPDIF_AUTO = 3
    const val OUT_TYPE_OPTICAL = 32

    fun enable(sh: Shell.Channel, prefs: SharedPreferences, restartNetflix: Boolean) {
        val current = sh.run("settings get global nrdp_audio_platform_capabilities").trim()
        if (current.contains(DOLBY_ON)) prefs.edit().putString(PREF_ORIGINAL, current).apply()
        val original = prefs.getString(PREF_ORIGINAL, null) ?: NETFLIX_ORIGINAL
        val noDolby = original.replace(DOLBY_ON, DOLBY_OFF)
            .replace("\"atmos\":{\"enabled\":true", "\"atmos\":{\"enabled\":false")

        sh.run("settings put global encoded_surround_output 1")
        sh.run("settings put global nrdp_external_surround_sound_enabled 0")
        sh.run("settings put global nrdp_audio_platform_capabilities '$noDolby'")
        sh.run("settings put system tcl_sys_audio_spdif_outmode $SPDIF_PCM")
        if (restartNetflix) sh.run("am force-stop com.netflix.ninja")
    }

    fun disable(sh: Shell.Channel, prefs: SharedPreferences) {
        val original = prefs.getString(PREF_ORIGINAL, null) ?: NETFLIX_ORIGINAL
        sh.run("settings delete global encoded_surround_output")
        sh.run("settings put global nrdp_external_surround_sound_enabled 1")
        sh.run("settings put global nrdp_audio_platform_capabilities '$original'")
        sh.run("settings put system tcl_sys_audio_spdif_outmode $SPDIF_AUTO")
        sh.run("am force-stop com.netflix.ninja")
    }

    fun netflixDolbyOff(sh: Shell.Channel): Boolean =
        sh.run("settings get global nrdp_audio_platform_capabilities").contains(DOLBY_OFF)
}
