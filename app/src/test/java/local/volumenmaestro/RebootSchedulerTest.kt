package local.volumenmaestro

import local.volumenmaestro.system.RebootScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDateTime

class RebootSchedulerTest {
    // 31/10/2026 es sábado.
    private val saturday10am = LocalDateTime.of(2026, 10, 31, 10, 0)

    @Test fun nextMondayAt5() {
        assertEquals(LocalDateTime.of(2026, 11, 2, 5, 0), RebootScheduler.next(saturday10am, setOf(MONDAY), 5, 0))
    }

    @Test fun laterTodayIsUsed() {
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 30), RebootScheduler.next(saturday10am, setOf(SATURDAY), 23, 30))
    }

    @Test fun passedTodayGoesToNextWeek() {
        assertEquals(LocalDateTime.of(2026, 11, 7, 5, 0), RebootScheduler.next(saturday10am, setOf(SATURDAY), 5, 0))
    }

    @Test fun exactlyNowIsNotNext() {
        assertEquals(LocalDateTime.of(2026, 11, 7, 10, 0), RebootScheduler.next(saturday10am, setOf(SATURDAY), 10, 0))
    }

    @Test fun twiceAWeekPicksNearest() {
        assertEquals(LocalDateTime.of(2026, 11, 2, 5, 0), RebootScheduler.next(saturday10am, setOf(MONDAY, THURSDAY), 5, 0))
        val tuesday = LocalDateTime.of(2026, 11, 3, 12, 0)
        assertEquals(LocalDateTime.of(2026, 11, 5, 5, 0), RebootScheduler.next(tuesday, setOf(MONDAY, THURSDAY), 5, 0))
    }

    @Test fun crossesMonthAndYear() {
        val dec31 = LocalDateTime.of(2026, 12, 31, 22, 0) // jueves
        assertEquals(LocalDateTime.of(2027, 1, 1, 5, 0), RebootScheduler.next(dec31, setOf(FRIDAY), 5, 0))
    }

    @Test fun noDaysMeansNoReboot() {
        assertNull(RebootScheduler.next(saturday10am, emptySet(), 5, 0))
    }
}
