package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderRulesTest {
    @Test fun beforeSevenPmUsesSameLocalDay() {
        val now=ZonedDateTime.parse("2026-10-11T18:00:00+02:00[Europe/Paris]")
        assertEquals(ZonedDateTime.parse("2026-10-11T19:00:00+02:00[Europe/Paris]"),nextLearningReminder(now))
    }
    @Test fun exactSevenPmAndLaterUseNextDay() {
        for(hour in listOf(19,23)) {
            val now=ZonedDateTime.of(2026,10,11,hour,0,0,0,ZoneId.of("Europe/Paris"))
            assertEquals(LocalDate.of(2026,10,12),nextLearningReminder(now).toLocalDate())
            assertEquals(LocalTime.of(19,0),nextLearningReminder(now).toLocalTime())
        }
    }
    @Test fun springClockChangeKeepsNineteenLocalInsteadOfAddingTwentyFourHours() {
        val now=ZonedDateTime.parse("2026-03-28T20:00:00+01:00[Europe/Paris]")
        assertEquals(ZonedDateTime.parse("2026-03-29T19:00:00+02:00[Europe/Paris]"),nextLearningReminder(now))
    }
    @Test fun autumnClockChangeKeepsNineteenLocal() {
        val now=ZonedDateTime.parse("2026-10-24T20:00:00+02:00[Europe/Paris]")
        assertEquals(ZonedDateTime.parse("2026-10-25T19:00:00+01:00[Europe/Paris]"),nextLearningReminder(now))
    }
    @Test fun timezoneAndYearBoundaryUseLocalCalendar() {
        val now=ZonedDateTime.parse("2026-12-31T23:00:00+09:00[Asia/Tokyo]")
        val next=nextLearningReminder(now)
        assertEquals(LocalDate.of(2027,1,1),next.toLocalDate());assertEquals(now.zone,next.zone);assertEquals(19,next.hour)
    }
    @Test fun notificationNeverNavigatesIntoAnotherAccount() {
        assertTrue(canOpenLearningReminder("guest",null));assertTrue(canOpenLearningReminder("owner","owner"))
        assertFalse(canOpenLearningReminder("owner",null));assertFalse(canOpenLearningReminder("owner","other"))
        assertFalse(canOpenLearningReminder("guest","owner"));assertFalse(canOpenLearningReminder("",null))
    }
}
