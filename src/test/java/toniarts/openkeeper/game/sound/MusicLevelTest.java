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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MusicLevelTest {

    @Test
    void startsSilent() {
        MusicLevel level = new MusicLevel();
        level.update();

        assertEquals(0, level.getIntensity());
    }

    @Test
    void highestRequestWinsAndDecaysToLevelOne() {
        MusicLevel level = new MusicLevel();
        level.raise(MusicLevel.LEVEL_THREE);
        level.raise(MusicLevel.LEVEL_TWO);
        level.update();
        assertEquals(60, level.getIntensity());

        level.update();
        assertEquals(20, level.getIntensity());

        level.update();
        assertEquals(20, level.getIntensity());
    }

    @Test
    void onlySetSilencesTheMusic() {
        MusicLevel level = new MusicLevel();
        level.raise(MusicLevel.LEVEL_FOUR);
        level.update();
        assertEquals(80, level.getIntensity());

        level.set(MusicLevel.SILENT);
        level.update();
        assertEquals(0, level.getIntensity());
    }
}
