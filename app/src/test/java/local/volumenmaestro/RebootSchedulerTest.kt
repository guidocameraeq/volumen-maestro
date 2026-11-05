package local.volumenmaestro

import local.volumenmaestro.settings.Config
import local.volumenmaestro.system.RebootScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDate
import java.time.LocalDateTime

class RebootSchedulerTest {
    // 08/11/2026 es domingo.
    private val sunday = LocalDate.of(2026, 11, 8)
    private val monday = sunday.plusDays(1)
    private val config = Config(rebootEnabled = true) // domingo, lunes y jueves desde las 22:00

    private fun at(day: LocalDate, hour: Int, minute: Int = 0) = day.atTime(hour, minute)

    @Test fun defaultsAreSundayMondayThursdayAt22() {
        val d = Config()
        assertEquals(setOf(SUNDAY, MONDAY, THURSDAY), d.rebootDays)
        assertEquals(22, d.rebootHour)
        assertEquals(0, d.rebootMinute)
        assertFalse(d.rebootOncePerWeek)
    }

    @Test fun afterHourOnChosenDayReboots() {
        assertTrue(RebootScheduler.shouldReboot(at(sunday, 22, 30), config, null))
    }

    @Test fun beforeHourDoesNotReboot() {
        assertFalse(RebootScheduler.shouldReboot(at(sunday, 21, 59), config, null))
        assertFalse(RebootScheduler.shouldReboot(at(sunday, 15, 0), config, null))
    }

    @Test fun exactlyAtHourReboots() {
        assertTrue(RebootScheduler.shouldReboot(at(sunday, 22, 0), config, null))
    }

    @Test fun notChosenDayDoesNotReboot() {
        val tuesday = monday.plusDays(1)
        assertFalse(RebootScheduler.shouldReboot(at(tuesday, 23, 0), config, null))
    }

    @Test fun afterMidnightCountsAsPreviousNight() {
        // Martes a la 1: sigue siendo la noche del lunes.
        val tuesday1am = at(monday.plusDays(1), 1, 0)
        assertEquals(monday, RebootScheduler.nightOf(tuesday1am, config.rebootDays, 22, 0))
        assertTrue(RebootScheduler.shouldReboot(tuesday1am, config, null))
    }

    @Test fun nightEndsAt6() {
        val tuesday6am = at(monday.plusDays(1), 6, 0)
        assertNull(RebootScheduler.nightOf(tuesday6am, config.rebootDays, 22, 0))
    }

    @Test fun earlyMondayIsSundayNight() {
        // Lunes a las 2: es la noche del domingo (aunque el lunes también esté elegido).
        assertEquals(sunday, RebootScheduler.nightOf(at(monday, 2, 0), config.rebootDays, 22, 0))
    }

    @Test fun onlyOncePerNight() {
        assertFalse(RebootScheduler.shouldReboot(at(sunday, 23, 30), config, lastNight = sunday))
        assertFalse(RebootScheduler.shouldReboot(at(monday, 1, 0), config, lastNight = sunday))
    }

    @Test fun eachChosenNightWithDefaultMode() {
        assertTrue(RebootScheduler.shouldReboot(at(monday, 22, 30), config, lastNight = sunday))
    }

    @Test fun oncePerWeekSkipsTheOtherNights() {
        val weekly = config.copy(rebootDays = setOf(SATURDAY, SUNDAY, MONDAY), rebootOncePerWeek = true)
        val saturday = sunday.minusDays(1)
        assertFalse(RebootScheduler.shouldReboot(at(sunday, 23, 0), weekly, lastNight = saturday))
        assertFalse(RebootScheduler.shouldReboot(at(monday, 23, 0), weekly, lastNight = saturday))
        assertTrue(RebootScheduler.shouldReboot(at(saturday.plusDays(7), 23, 0), weekly, lastNight = saturday))
    }

    @Test fun disabledNeverReboots() {
        assertFalse(RebootScheduler.shouldReboot(at(sunday, 23, 0), config.copy(rebootEnabled = false), null))
    }

    @Test fun earlyHourWindowEndsSameMorning() {
        val c = config.copy(rebootHour = 2)
        assertTrue(RebootScheduler.shouldReboot(at(sunday, 3, 0), c, null))
        assertFalse(RebootScheduler.shouldReboot(at(sunday, 7, 0), c, null))
    }

    @Test fun nextNightIsTonightIfNotDone() {
        assertEquals(at(sunday, 22, 0), RebootScheduler.nextNight(at(sunday, 10, 0), config, null))
        assertEquals(at(sunday, 22, 0), RebootScheduler.nextNight(at(sunday, 23, 0), config, null))
    }

    @Test fun nextNightSkipsDoneNight() {
        assertEquals(at(monday, 22, 0), RebootScheduler.nextNight(at(sunday, 23, 0), config, lastNight = sunday))
        // Martes: el próximo es el jueves.
        assertEquals(at(monday.plusDays(3), 22, 0), RebootScheduler.nextNight(at(monday.plusDays(1), 12, 0), config, monday))
    }

    @Test fun nextNightWeeklyWaitsSevenDays() {
        val weekly = config.copy(rebootOncePerWeek = true)
        assertEquals(at(sunday.plusDays(7), 22, 0), RebootScheduler.nextNight(at(sunday, 23, 0), weekly, lastNight = sunday))
    }

    @Test fun noDaysMeansNoNextNight() {
        assertNull(RebootScheduler.nextNight(at(sunday, 10, 0), config.copy(rebootDays = emptySet()), null))
    }
}
