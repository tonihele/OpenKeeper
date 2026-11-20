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
package toniarts.openkeeper.game.logic;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureMood;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Slap anger is a one-off delta and is applied as is. Measured in the original
 * game: a Salamander (slap anger 3000) gets unhappy after exactly 6 slaps and
 * angry after 5 more.
 */
class SlapAngerTest {

    private static final int SALAMANDER_ANGER_SLAP = 3000;
    private static final int UNHAPPY_THRESHOLD = CreatureMood.toRuntimeThreshold(50);

    @Test
    void slapAngerIsAppliedAsIs() {
        CreatureMood mood = SlapSystem.applySlapAnger(new CreatureMood(), SALAMANDER_ANGER_SLAP);

        assertEquals(SALAMANDER_ANGER_SLAP, mood.other);
    }

    @Test
    void sixSlapsMakeASalamanderUnhappy() {
        CreatureMood mood = slap(5);
        assertEquals(0, mood.getState(UNHAPPY_THRESHOLD));

        mood = SlapSystem.applySlapAnger(mood, SALAMANDER_ANGER_SLAP);
        assertEquals(1, mood.getState(UNHAPPY_THRESHOLD));
    }

    @Test
    void elevenSlapsMakeASalamanderAngry() {
        CreatureMood mood = slap(10);
        assertEquals(1, mood.getState(UNHAPPY_THRESHOLD));

        mood = SlapSystem.applySlapAnger(mood, SALAMANDER_ANGER_SLAP);
        assertEquals(2, mood.getState(UNHAPPY_THRESHOLD));
    }

    @Test
    void negativeSlapAngerCalmsDown() {
        CreatureMood mood = SlapSystem.applySlapAnger(new CreatureMood(0, 0, 0, 0, 0, 1000), -250);

        assertEquals(750, mood.other);
    }

    private static CreatureMood slap(int times) {
        CreatureMood mood = new CreatureMood();
        for (int i = 0; i < times; i++) {
            mood = SlapSystem.applySlapAnger(mood, SALAMANDER_ANGER_SLAP);
        }
        return mood;
    }
}
