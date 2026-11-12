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
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.ResourceChunkReader;
import static org.junit.jupiter.api.Assertions.*;

class EffectGeneratorParsingTest {
    @Test
    void retainsEmptySlotsAndReadsRectangleDimensions() throws Exception {
        var data = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        data.putInt(10).putInt(20).putInt(0).putInt(0);
        data.putShort((short) 2).putShort((short) 3);
        data.putShort((short) 123).putShort((short) 0).putShort((short) 456).putShort((short) 0);
        data.put((byte) 5).put((byte) 7);
        data.position(0);
        var thing = KwdFile.readEffectGenerator(new ResourceChunkReader(data));
        assertEquals(List.of(123, 0, 456, 0), thing.getEffectIds());
        assertEquals(2, thing.getWidth());
        assertEquals(3, thing.getHeight());
        assertEquals(7, thing.getId());
        assertEquals(5, thing.getFrequency());
    }
}
