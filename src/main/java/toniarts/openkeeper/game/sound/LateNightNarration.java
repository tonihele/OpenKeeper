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
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/** The native late-night reminders, scheduled against the client's local wall clock. */
public final class LateNightNarration {
    private final Clock clock;
    private ZonedDateTime lastMinute;

    public LateNightNarration(Clock clock) {
        this.clock = clock;
    }

    /**
     * Checks the current minute once, including when narration is suppressed.
     * Missed or suppressed reminders are not replayed later.
     */
    public Reminder poll(boolean enabled) {
        ZonedDateTime minute = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        if (minute.equals(lastMinute)) {
            return null;
        }
        lastMinute = minute;
        if (!enabled) {
            return null;
        }
        // Native text references are zero-based; MentorType/audio event IDs are one-based.
        MentorType speech = switch (minute.getHour() * 60 + minute.getMinute()) {
            // DK II uses a separate Halloween message at midnight on October 31.
            case 0 -> minute.getMonth() == Month.OCTOBER && minute.getDayOfMonth() == 31
                    ? null : MentorType.SILLY008;
            case 60 -> MentorType.SILLY009;
            case 120 -> MentorType.SILLY010;
            case 180 -> MentorType.SILLY012;
            case 195 -> MentorType.SILLY011;
            default -> null;
        };
        return speech == null ? null : new Reminder(speech, minute.toInstant(), minute.plusMinutes(1).toInstant());
    }

    /** A queued reminder stops being relevant when its scheduled minute ends. */
    public boolean isCurrent(Reminder reminder) {
        Instant now = clock.instant();
        return !now.isBefore(reminder.start()) && now.isBefore(reminder.end());
    }

    public record Reminder(MentorType speech, Instant start, Instant end) {
    }
}
