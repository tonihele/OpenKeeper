/*
 * Copyright (C) 2014-2025 OpenKeeper
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AudioSpecTest {

    @Test
    void pitchIsHeldToTheRangeJmeAccepts() {
        AudioSpec spec = AudioSpec.stereo("a.wav", 1.0f);

        assertEquals(2.0f, spec.withPitch(2.27f).pitch());
        assertEquals(0.5f, spec.withPitch(0.01f).pitch());
        assertEquals(1.25f, spec.withPitch(1.25f).pitch());
    }
}
