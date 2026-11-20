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
package toniarts.openkeeper.tools.convert.map;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.ResourceChunkReader;
import toniarts.openkeeper.view.effect.EffectControl;

import static org.junit.jupiter.api.Assertions.*;

class EffectHeightLimitsTest {
    @Test
    void signedBinaryHeightBandSpawnsAroundItsOriginInsteadOfHundredsOfTilesAway() {
        var bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putShort((short) -96).putShort((short) 32).flip();
        Effect effect = new Effect();
        KwdFile.readEffectHeightLimits(new ResourceChunkReader(bytes), effect);
        assertEquals(-96, effect.getLowerHeightLimit());
        assertEquals(32, effect.getUpperHeightLimit());
        boolean below = false;
        boolean above = false;
        for (int i = 0; i < 256; i++) {
            float height = EffectControl.randomOriginOffset(effect).y;
            assertTrue(height >= -0.375f && height <= 0.125f);
            below |= height < 0;
            above |= height > 0;
        }
        assertTrue(below);
        assertTrue(above);
    }
}
