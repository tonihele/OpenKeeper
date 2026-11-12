/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.ClientEvents;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import static org.junit.jupiter.api.Assertions.*;

class TimeLimitSpeechTest {
    @Test
    void selectsLocalizedCountRecordingsAndQueuesThemAsMentorSpeech() {
        var queued = new ArrayList<Integer>();
        var player = new GameEventSoundPlayer(null, queued::add);
        for (int seconds : new int[]{1800, 1200, 600, 300, 240, 180, 120, 60,
                10, 9, 8, 7, 6, 5, 4, 3, 2, 1}) {
            player.newEvent(ClientEvents.GAME_EVENT,
                    new GameEvent(GameEventType.TIME_LIMIT_WARNING, null, seconds, null, 0, null, null));
        }
        assertEquals(java.util.stream.IntStream.rangeClosed(1, 18).boxed().toList(), queued);
        assertEquals(0, TimeLimitSpeech.forSeconds(0));
        assertEquals(0, TimeLimitSpeech.forSeconds(30));
    }
}
