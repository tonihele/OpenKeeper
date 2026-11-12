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
package toniarts.openkeeper.video;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * The movie sound follows the master volume (#436)
 */
class MovieVolumeTest {

    @Test
    void halfVolumeHalvesTheSamples() {
        ByteBuffer pcm = pcm(1000, -1000, Short.MAX_VALUE, Short.MIN_VALUE);

        TgqPlayer.scalePcm16(pcm, 0.5f);

        assertArrayEquals(new short[]{500, -500, 16384, -16384}, samples(pcm));
    }

    @Test
    void fullVolumeLeavesTheSamplesAlone() {
        ByteBuffer pcm = pcm(1000, -1000, Short.MAX_VALUE, Short.MIN_VALUE);

        TgqPlayer.scalePcm16(pcm, 1.0f);

        assertArrayEquals(new short[]{1000, -1000, Short.MAX_VALUE, Short.MIN_VALUE}, samples(pcm));
    }

    @Test
    void zeroVolumeIsSilent() {
        ByteBuffer pcm = pcm(1000, -1000, Short.MAX_VALUE, Short.MIN_VALUE);

        TgqPlayer.scalePcm16(pcm, 0.0f);

        assertArrayEquals(new short[]{0, 0, 0, 0}, samples(pcm));
    }

    @Test
    void onlyTheRemainingSamplesAreScaled() {
        ByteBuffer pcm = pcm(1000, 1000);
        pcm.position(2);

        TgqPlayer.scalePcm16(pcm, 0.5f);

        pcm.position(0);
        assertArrayEquals(new short[]{1000, 500}, samples(pcm));
    }

    private static ByteBuffer pcm(int... samples) {
        ByteBuffer pcm = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int sample : samples) {
            pcm.putShort((short) sample);
        }
        pcm.flip();
        return pcm;
    }

    private static short[] samples(ByteBuffer pcm) {
        ByteBuffer copy = pcm.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        short[] result = new short[copy.remaining() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = copy.getShort();
        }
        return result;
    }

}
