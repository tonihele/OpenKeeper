/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.video.tgq;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import toniarts.openkeeper.tools.convert.Utils;

/**
 * Holds a TGQ frame (one texture that is)<br>
 * References: FFMPEG, eatgi.c
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TgqFrame implements Comparable<TgqFrame> {

    private final int width;
    private final int height;
    private final int frameIndex;
    private int[] dequantizationTable = new int[64];
    private final static int[] baseTable = {
        4096, 2953, 3135, 3483, 4096, 5213, 7568, 14846,
        2953, 2129, 2260, 2511, 2953, 3759, 5457, 10703,
        3135, 2260, 2399, 2666, 3135, 3990, 5793, 11363,
        3483, 2511, 2666, 2962, 3483, 4433, 6436, 12625,
        4096, 2953, 3135, 3483, 4096, 5213, 7568, 14846,
        5213, 3759, 3990, 4433, 5213, 6635, 9633, 18895,
        7568, 5457, 5793, 6436, 7568, 9633, 13985, 27432,
        14846, 10703, 11363, 12625, 14846, 18895, 27432, 53809};
    private final static int[] baseTable2 = {
        8, 16, 19, 22, 26, 27, 29, 34,
        16, 16, 22, 24, 27, 29, 34, 37,
        19, 22, 26, 27, 29, 34, 34, 38,
        22, 22, 26, 27, 29, 34, 37, 40,
        22, 26, 27, 29, 32, 35, 40, 48,
        26, 27, 29, 32, 35, 40, 48, 58,
        26, 27, 29, 34, 38, 46, 56, 69,
        27, 29, 35, 38, 46, 56, 69, 83};

    public TgqFrame(byte[] data, int frameIndex) {
        this.frameIndex = frameIndex;

        // Read width & height from the header
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        width = buf.getShort();
        height = buf.getShort();

        // Decode
        decodeFrame(buf);
    }

    private void decodeFrame(ByteBuffer buf) {
        short quantizer = Utils.toUnsignedByte(buf.get());
        buf.position(buf.position() + 3); // Skip 3 bytes
        calculateDequantizationTable(quantizer);
    }

    private void calculateDequantizationTable(short quantizer) {

        // This is different from the specs -> can use integer math
        int qScale = (215 - 2 * quantizer) * 5;
        dequantizationTable[0] = (baseTable[0] * baseTable2[0]) >> 11;
        for (int i = 1; i < 64; i++) {
            dequantizationTable[i] = (baseTable[i] * baseTable2[i] * qScale + 32) >> 14;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "Video frame: " + frameIndex + ", " + width + "x" + height;
    }

    @Override
    public int compareTo(TgqFrame o) {
        return Integer.compare(frameIndex, o.frameIndex);
    }
}
