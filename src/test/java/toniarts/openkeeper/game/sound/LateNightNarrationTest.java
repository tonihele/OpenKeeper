/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.sound;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class LateNightNarrationTest {
    @ParameterizedTest
    @CsvSource({"00:00, SILLY007", "01:00, SILLY008", "02:00, SILLY009",
        "03:00, SILLY011", "03:15, SILLY010"})
    void usesTheNativeScheduleEvenWhenFirstObservedMidMinute(String time, MentorType speech) {
        var clock = new MutableClock("2026-11-04T" + time + ":37Z");
        var narration = new LateNightNarration(clock);
        var reminder = narration.poll(true);
        assertEquals(speech, reminder.speech());
        assertTrue(narration.isCurrent(reminder));
        assertNull(narration.poll(true));
        clock.advance(10);
        assertNull(narration.poll(true));
    }

    @Test
    void usesTheClientsTimeZoneRatherThanUtc() {
        Clock utc = Clock.fixed(Instant.parse("2026-11-03T23:00:30Z"), ZoneId.of("UTC"));
        assertNull(new LateNightNarration(utc).poll(true));
        assertEquals(MentorType.SILLY007,
                new LateNightNarration(utc.withZone(ZoneId.of("Europe/Brussels"))).poll(true).speech());
    }

    @Test
    void followsMidnightAndNewDatesWithoutAPlayDurationRequirement() {
        var clock = new MutableClock("2026-11-03T23:59:59Z");
        var narration = new LateNightNarration(clock);
        assertNull(narration.poll(true));
        clock.advance(1);
        assertEquals(MentorType.SILLY007, narration.poll(true).speech());
        clock.advance(86400);
        assertEquals(MentorType.SILLY007, narration.poll(true).speech());
    }

    @Test
    void doesNotCatchUpMissedMinutesOrNagThroughoutTheNight() {
        var clock = new MutableClock("2026-11-04T00:59:59Z");
        var narration = new LateNightNarration(clock);
        assertNull(narration.poll(true));
        clock.advance(62);
        assertNull(narration.poll(true));
        clock.set("2026-11-04T02:01:00Z");
        assertNull(narration.poll(true));
        clock.set("2026-11-04T03:14:59Z");
        assertNull(narration.poll(true));
        clock.advance(1);
        assertEquals(MentorType.SILLY010, narration.poll(true).speech());
        clock.set("2026-11-04T04:00:00Z");
        assertNull(narration.poll(true));
    }

    @Test
    void suppressionConsumesTheMinuteWithoutReplayOnResume() {
        var clock = new MutableClock("2026-11-04T02:00:15Z");
        var narration = new LateNightNarration(clock);
        assertNull(narration.poll(false));
        clock.advance(10);
        assertNull(narration.poll(true));
        clock.set("2026-11-04T03:00:10Z");
        assertEquals(MentorType.SILLY011, narration.poll(true).speech());
    }

    @Test
    void queuedReminderExpiresAtTheMinuteBoundaryAndOnClockRewind() {
        var clock = new MutableClock("2026-11-04T03:15:00Z");
        var narration = new LateNightNarration(clock);
        var reminder = narration.poll(true);
        clock.advance(59);
        assertTrue(narration.isCurrent(reminder));
        clock.advance(1);
        assertFalse(narration.isCurrent(reminder));
        clock.set("2026-11-04T03:14:59Z");
        assertFalse(narration.isCurrent(reminder));
        clock.set("2026-11-05T03:15:00Z");
        assertFalse(narration.isCurrent(reminder));
    }

    @Test
    void leavesHalloweenMidnightForItsSeparateNativeMessage() {
        var clock = new MutableClock("2026-10-31T00:00:30Z");
        var narration = new LateNightNarration(clock);
        assertNull(narration.poll(true));
        clock.set("2026-10-31T01:00:30Z");
        assertEquals(MentorType.SILLY008, narration.poll(true).speech());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(String now) {
            set(now);
        }

        void set(String value) {
            now = Instant.parse(value);
        }

        void advance(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }
    }
}
