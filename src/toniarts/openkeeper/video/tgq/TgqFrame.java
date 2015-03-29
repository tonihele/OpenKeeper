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
import java.nio.IntBuffer;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.BitReader;
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
    private final static int DC_VLC_BITS = 9;
    private static final short[][] dcLuminanceVlc = {
        {18, 15, 0}, {-1, -1, 0}, {-1, -1, 1}, {-1, -1, 2}, {-1, -1, 3}, {-1, -1, 4},
        {-1, -1, 5}, {-1, -1, 6}, {-1, -1, 7}, {-1, -1, 8}, {9, -1, 0}, {8, 10, 0},
        {7, 11, 0}, {6, 12, 0}, {5, 13, 0}, {17, 14, 0}, {18, 15, 0}, {1, 4, 0},
        {2, 3, 0}
    };
    private static final short[][] dcCrominanceVlc = {
        {18, 16, 0}, {-1, -1, 0}, {-1, -1, 1}, {-1, -1, 2}, {-1, -1, 3}, {-1, -1, 4},
        {-1, -1, 5}, {-1, -1, 6}, {-1, -1, 7}, {-1, -1, 8}, {9, -1, 0}, {8, 10, 0},
        {7, 11, 0}, {6, 12, 0}, {5, 13, 0}, {4, 14, 0}, {3, 15, 0}, {18, 16, 0},
        {1, 2, 0}
    };
    private int[] lastDc = {0, 0, 0};
    private static final Logger logger = Logger.getLogger(TgqFrame.class.getName());

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

        // Decode the macroblocks
        for (int y = 0; y < (height + 15) / 16; y++) {
            for (int x = 0; x < (width + 15) / 16; x++) {
                decodeBlock(buf);
            }
        }
    }

    private void calculateDequantizationTable(short quantizer) {

        // This is different from the specs -> can use integer math
        int qScale = (215 - 2 * quantizer) * 5;
        dequantizationTable[0] = (baseTable[0] * baseTable2[0]) >> 11;
        for (int i = 1; i < 64; i++) {
            dequantizationTable[i] = (baseTable[i] * baseTable2[i] * qScale + 32) >> 14;
        }
    }

    private int[] decodeBlock(ByteBuffer buf) {
        int[] block = new int[64];
        IntBuffer blockBuffer = IntBuffer.wrap(block);
        for (int n = 0; n < 6; n++) {
            mpeg1DecodeBlock(buf, (IntBuffer) blockBuffer.position(n), n);
        }
        return block;
    }

    /**
     * Decodes a block
     *
     * @param buf data
     * @param component one of 4 luma + 2 chroma components
     */
    private void mpeg1DecodeBlock(ByteBuffer buf, IntBuffer blockBuffer, int componentIndex) {

        // Create a bit stream
        BitReader bitReader = new BitReader(buf);

        // DC coefficient
        int component = (componentIndex <= 3 ? 0 : componentIndex - 4 + 1);
        int diff = decodeDc(bitReader, component);
        if (diff >= 0xFFFF) {
            throw new RuntimeException("Invalid data!");
        }
        int dc = lastDc[component];
        dc += diff;
        lastDc[component] = dc;
//        bitReader.skip(bitReader.remaining());
        bitReader.stop();
        blockBuffer.put(dc * dequantizationTable[0]);
        int i = 0;
        if (bitReader.readInt() > 0xBFFFFFFF) {

            // Now quantify & encode AC coefficients
            while (true) {
            }
        }

    }

    private int decodeDc(BitReader bitReader, int component) {
        int code = 0;

        if (component == 0) {
            code = decodeVlc(bitReader, DC_VLC_BITS, dcLuminanceVlc);
//            code = getVlc(bitReader, ff_dc_lum_vlc.table, DC_VLC_BITS, 2);
//            bitReader.readNBit(5);
//            code = 6;
        } else {
            code = decodeVlc(bitReader, DC_VLC_BITS, dcCrominanceVlc);
            //code = getVlc(gb, ff_dc_chroma_vlc.table, DC_VLC_BITS, 2);
        }
        if (code < 0) {
            logger.severe("Invalid dc code!");
            return 0xFFFF;
        }

        int diff;
        if (code == 0) {
            diff = 0;
        } else {
            diff = bitReader.readNBit(code);
        }
        return diff;
    }

    public short decodeVlc(BitReader bitReader, int maxLength, short tab[][]) {
        int idx1 = 0, idx = 0;
        int readLength = -1;
        int mask = (1 << maxLength + 1);
        int bits = bitReader.checkNBit(maxLength + 1); // Make sure that enough bits are available

        while (idx != -1) {	// NIL ????
            mask >>>= 1;    // next bit
            idx1 = idx;     // notice
            idx = ((bits & mask) != 0) ? tab[idx][1] : tab[idx][0]; // get next index
            readLength++;	// count the length
        }

        // Advance the reader by the real length
        bitReader.readNBit(readLength);
        return (tab[idx1][2]);
    }

//    private int getVlc(BitReader bitReader, short[][] table, int bits, int maxDepth) {
//        int code = 0;
//        do {
//            int n, nb_bits;
//            int index;
//
//            index = SHOW_UBITS(name, gb, bits);
//            code = table[index][0];
//            n = table[index][1];
//
//            if (maxDepth > 1 && n < 0) {
//                LAST_SKIP_BITS(name, gb, bits);
//                UPDATE_CACHE(name, gb);
//
//                nb_bits = -n;
//
//                index = SHOW_UBITS(name, gb, nb_bits) + code;
//                code = table[index][0];
//                n = table[index][1];
//                if (maxDepth > 2 && n < 0) {
//                    LAST_SKIP_BITS(name, gb, nb_bits);
//                    UPDATE_CACHE(name, gb);
//
//                    nb_bits = -n;
//
//                    index = SHOW_UBITS(name, gb, nb_bits) + code;
//                    code = table[index][0];
//                    n = table[index][1];
//                }
//            }
//            SKIP_BITS(name, gb, n);
//        } while (true);
//        return code;
//    }
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
