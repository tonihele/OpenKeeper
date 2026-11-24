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
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.ResourceChunkReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraParsingTest {

    /**
     * GlobalVariables.kwd has camera records for the modes 1 to 18, not only
     * the four OpenKeeper has names for. The mode is read as a number
     */
    @Test
    void readsTheModeOfEveryRecord() throws Exception {
        for (int mode : new int[]{1, 3, 6, 12, 18}) {
            var data = ByteBuffer.allocate(84).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < 18; i++) {
                data.putInt(4096 * i);
            }
            data.putInt(0x02 | 0x04);
            data.putShort((short) 0).putShort((short) 0).putShort((short) 220);
            data.putShort((short) mode);
            data.position(0);

            var reader = new ResourceChunkReader(data);
            Thing.Camera camera = KwdFile.readCamera(reader);

            assertEquals(mode, camera.getId());
            assertEquals(220, camera.getAnglePitch());
            assertEquals(12f, camera.getZoomValue());
            assertEquals(EnumSet.of(Thing.Camera.CameraFlag.DISABLE_ROLL, Thing.Camera.CameraFlag.DISABLE_PITCH), camera.getFlags());
            assertEquals(84, reader.position());
        }
    }
}
