package local.volumenmaestro

import local.volumenmaestro.recientes.RecentApps
import local.volumenmaestro.recientes.TapDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAppsTest {
    // Recorte real de `dumpsys activity recents` en la TV (01/11/2026).
    private val dump = """
        ACTIVITY MANAGER RECENT TASKS (dumpsys activity recents)
          * Recent #0: Task{5439a29 #5179 type=dream A=10055:android:com.google.android.apps.tv.dreamx/dream U=0 visible=true mode=fullscreen translucent=false sz=1}
            mActivityComponent=com.google.android.apps.tv.dreamx/android.service.dreams.DreamActivity
          * Recent #1: Task{6616c6f #5153 type=home A=10052:com.google.android.apps.tv.launcherx U=0 rootTaskId=1 visible=false mode=fullscreen translucent=true sz=1}
            mActivityComponent=com.google.android.apps.tv.launcherx/.home.HomeActivity
          * Recent #2: Task{e1e8ae2 #5155 type=standard A=10096:com.disney.disneyplus U=0 visible=true mode=fullscreen translucent=false sz=1}
            mActivityComponent=com.disney.disneyplus/com.bamtechmedia.dominguez.main.MainActivity
          * Recent #3: Task{962277e #5174 type=standard A=10115:local.volumenmaestro U=0 visible=false mode=fullscreen translucent=true sz=0}
            mActivityComponent=local.volumenmaestro/.ui.MainActivity
          * Recent #4: Task{230e02 #5165 type=standard A=10079:com.google.android.youtube.tv.main U=0 visible=false mode=fullscreen translucent=true sz=1}
            mActivityComponent=com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity
          * Recent #5: Task{8910394 #5158 type=standard A=10105:com.plexapp.android U=0 visible=false mode=fullscreen translucent=true sz=1}
            mActivityComponent=com.plexapp.android/com.plexapp.plex.activities.SplashActivity
          * Recent #6: Task{cb0648c #5131 type=undefined A=10066:com.netflix.ninja U=0 visible=false mode=undefined translucent=true sz=0}
            mActivityComponent=com.netflix.ninja/.MainActivity
    """.trimIndent()

    @Test fun parseKeepsStandardAppsInOrder() {
        val apps = RecentApps.parse(dump)
        assertEquals(listOf("com.disney.disneyplus", "com.google.android.youtube.tv", "com.plexapp.android"),
            apps.map { it.packageName })
        assertEquals(5155, apps[0].taskId)
    }

    @Test fun previousSkipsTheAppOnScreen() {
        val apps = RecentApps.parse(dump)
        assertEquals("com.disney.disneyplus", RecentApps.current(apps)?.packageName)
        assertEquals("com.google.android.youtube.tv", RecentApps.previous(apps)?.packageName)
    }

    @Test fun fromHomePreviousIsTheLastUsed() {
        val apps = RecentApps.parse(dump.replace("U=0 visible=true mode=fullscreen translucent=false sz=1}\n    mActivityComponent=com.disney",
            "U=0 visible=false mode=fullscreen translucent=false sz=1}\n    mActivityComponent=com.disney"))
        assertNull(RecentApps.current(apps))
        assertEquals("com.disney.disneyplus", RecentApps.previous(apps)?.packageName)
    }

    @Test fun appsClosedByTheTvStillCount() {
        val apps = RecentApps.parse(dump.replace("com.plexapp.android U=0 visible=false mode=fullscreen translucent=true sz=1",
            "com.plexapp.android U=0 visible=false mode=fullscreen translucent=true sz=0"))
        assertTrue(apps.any { it.packageName == "com.plexapp.android" })
    }

    @Test fun emptyDumpHasNoPrevious() {
        assertNull(RecentApps.previous(RecentApps.parse("")))
    }

    @Test fun doubleTapInsideTheWindow() {
        val taps = TapDetector(400)
        assertFalse(taps.press(1_000))
        assertTrue(taps.press(1_300))
        assertFalse(taps.expire(1_400)) // ya se usó como doble
    }

    @Test fun singleTapWhenTheWindowEnds() {
        val taps = TapDetector(400)
        assertFalse(taps.press(1_000))
        assertFalse(taps.expire(1_200))
        assertTrue(taps.expire(1_400))
        assertFalse(taps.press(2_000)) // arranca de nuevo
    }

    @Test fun slowSecondTapIsANewFirstTap() {
        val taps = TapDetector(400)
        assertFalse(taps.press(1_000))
        assertFalse(taps.press(1_500))
        assertTrue(taps.press(1_700))
    }
}
