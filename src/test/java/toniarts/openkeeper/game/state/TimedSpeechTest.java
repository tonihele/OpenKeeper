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
package toniarts.openkeeper.game.state;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.sound.LateNightNarration;
import static org.junit.jupiter.api.Assertions.*;

class TimedSpeechTest {
    @Test
    void expiredReminderDoesNotDelayTheFollowingLevelSpeech() {
        var narration = new LateNightNarration(Clock.fixed(Instant.parse("2026-11-04T02:00:30Z"), ZoneId.of("UTC")));
        var reminder = narration.poll(true);
        var afterMinute = new LateNightNarration(Clock.fixed(Instant.parse("2026-11-04T02:01:00Z"), ZoneId.of("UTC")));
        Queue<SoundState.Speech> queue = new ArrayDeque<>();
        queue.add(new SoundState.Speech("synthetic", reminder.speech().getId(), null,
                () -> afterMinute.isCurrent(reminder)));
        var levelSpeech = new SoundState.Speech("synthetic-level", 1, null, () -> true);
        queue.add(levelSpeech);
        assertSame(levelSpeech, SoundState.pollRelevantSpeech(queue));
        assertTrue(queue.isEmpty());
    }

    @Test
    void validReminderKeepsQueueOrderWithoutInterruptingTheFirstSpeech() {
        Queue<SoundState.Speech> queue = new ArrayDeque<>();
        var levelSpeech = new SoundState.Speech("synthetic-level", 1, null, () -> true);
        boolean[] enabled = {true};
        var reminder = new SoundState.Speech("synthetic", 1174, null, () -> enabled[0]);
        queue.add(levelSpeech);
        queue.add(reminder);
        assertSame(levelSpeech, SoundState.pollRelevantSpeech(queue));
        enabled[0] = false; // Voice/menu suppression changes while waiting behind other narration.
        assertNull(SoundState.pollRelevantSpeech(queue));
        assertTrue(queue.isEmpty());
    }
}
