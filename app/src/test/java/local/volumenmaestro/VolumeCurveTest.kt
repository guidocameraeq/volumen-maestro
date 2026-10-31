package local.volumenmaestro

import local.volumenmaestro.audio.VolumeCurve
import local.volumenmaestro.settings.Config
import local.volumenmaestro.settings.CurveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeCurveTest {
    private fun db(index: Int, config: Config) = VolumeCurve.gainToDb(VolumeCurve.gain(index, 100, false, config))

    @Test fun maxIsAlwaysFullSignal() {
        for (curve in CurveType.entries) {
            assertEquals(1f, VolumeCurve.gain(100, 100, false, Config(curve = curve)), 1e-6f)
        }
    }

    @Test fun evenDbHalfPointFiveGivesMinus25At50() {
        assertEquals(-25f, db(50, Config(curve = CurveType.EVEN_DB, dbPerPoint = 0.5f)), 0.01f)
        assertEquals(-49.5f, db(1, Config(curve = CurveType.EVEN_DB, dbPerPoint = 0.5f)), 0.01f)
        assertEquals(-35f, db(50, Config(curve = CurveType.EVEN_DB, dbPerPoint = 0.7f)), 0.01f)
    }

    @Test fun smoothIsSquare() {
        assertEquals(0.25f, VolumeCurve.gain(50, 100, false, Config(curve = CurveType.SMOOTH)), 1e-6f)
    }

    @Test fun zeroIsSilenceOrCurveMinimum() {
        assertEquals(0f, VolumeCurve.gain(0, 100, false, Config(zeroIsSilence = true)), 0f)
        val min = Config(curve = CurveType.EVEN_DB, dbPerPoint = 0.5f, zeroIsSilence = false)
        assertEquals(db(1, min), db(0, min), 0.01f)
    }

    @Test fun mutedIsSilence() {
        assertEquals(0f, VolumeCurve.gain(80, 100, true, Config()), 0f)
    }

    @Test fun amplifierIsMonotonicAndFinerInTheMiddle() {
        val c = Config(curve = CurveType.AMPLIFIER)
        var prev = -1000f
        for (i in 1..100) {
            val d = db(i, c)
            assertTrue("no monótona en $i", d >= prev)
            prev = d
        }
        val middleStep = db(51, c) - db(50, c)
        val edgeStep = db(100, c) - db(99, c)
        assertTrue(middleStep < edgeStep)
    }
}
