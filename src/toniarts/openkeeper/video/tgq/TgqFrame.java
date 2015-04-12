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
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.BitReader;
import toniarts.openkeeper.tools.convert.Utils;

/**
 * Holds a TGQ frame (one texture that is)<br>
 * References:<br>
 * <ul>
 * <li>FFMPEG project; eatgi.c, mpeg12.c</li>
 * <li>JAVA MPEG Player by J.Anders</li>
 * </ul>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TgqFrame implements Comparable<TgqFrame> {

    private final int width;
    private final int height;
    private final int frameIndex;
    private int[] dequantizationTable = new int[64];
    private final static short scanTable[] = { // zigzagDirect
        0, 1, 8, 16, 9, 2, 3, 10,
        17, 24, 32, 25, 18, 11, 4, 5,
        12, 19, 26, 33, 40, 48, 41, 34,
        27, 20, 13, 6, 7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36,
        29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63
    };
    private final static short[] scanTablePermutated = new short[64];
    private final static short[] scanTableRasterEnd = new short[64];

    static {
        for (int i = 0; i < 64; i++) {
            short j = scanTable[i];
            scanTablePermutated[i] = j;
        }

        short end = -1;
        for (int i = 0; i < 64; i++) {
            short j = scanTablePermutated[i];
            if (j > end) {
                end = j;
            }
            scanTableRasterEnd[i] = end;
        }
    }
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
    private final static int TEX_VLC_BITS = 28;
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
    private static final short DCT_ESCAPE = -2;
    private static final short EOB = -5; /* end of block */

    public final short dctCoeff[][] = {
        {128, 141, 0, 0}, {-1, -1, EOB, EOB}, {-1, -1, 0, 1}, {-1, -1, 1, 1}, {-1, -1, 0, 2}, {-1, -1, 2, 1},
        {-1, -1, 0, 3}, {-1, -1, 3, 1}, {-1, -1, 4, 1}, {-1, -1, 1, 2}, {-1, -1, 5, 1}, {-1, -1, 6, 1},
        {-1, -1, 7, 1}, {-1, -1, 0, 4}, {-1, -1, 2, 2}, {-1, -1, 8, 1}, {-1, -1, 9, 1}, {-1, -1, DCT_ESCAPE, DCT_ESCAPE},
        {-1, -1, 0, 5}, {-1, -1, 0, 6}, {-1, -1, 1, 3}, {-1, -1, 3, 2}, {-1, -1, 10, 1}, {-1, -1, 11, 1},
        {-1, -1, 12, 1}, {-1, -1, 13, 1}, {-1, -1, 0, 7}, {-1, -1, 1, 4}, {-1, -1, 2, 3}, {-1, -1, 4, 2},
        {-1, -1, 5, 2}, {-1, -1, 14, 1}, {-1, -1, 15, 1}, {-1, -1, 16, 1}, {-1, -1, 0, 8}, {-1, -1, 0, 9},
        {-1, -1, 0, 10}, {-1, -1, 0, 11}, {-1, -1, 1, 5}, {-1, -1, 2, 4}, {-1, -1, 3, 3}, {-1, -1, 4, 3},
        {-1, -1, 6, 2}, {-1, -1, 7, 2}, {-1, -1, 8, 2}, {-1, -1, 17, 1}, {-1, -1, 18, 1}, {-1, -1, 19, 1},
        {-1, -1, 20, 1}, {-1, -1, 21, 1}, {-1, -1, 0, 12}, {-1, -1, 0, 13}, {-1, -1, 0, 14}, {-1, -1, 0, 15},
        {-1, -1, 1, 6}, {-1, -1, 1, 7}, {-1, -1, 2, 5}, {-1, -1, 3, 4}, {-1, -1, 5, 3}, {-1, -1, 9, 2},
        {-1, -1, 10, 2}, {-1, -1, 22, 1}, {-1, -1, 23, 1}, {-1, -1, 24, 1}, {-1, -1, 25, 1}, {-1, -1, 26, 1},
        {-1, -1, 0, 16}, {-1, -1, 0, 17}, {-1, -1, 0, 18}, {-1, -1, 0, 19}, {-1, -1, 0, 20}, {-1, -1, 0, 21},
        {-1, -1, 0, 22}, {-1, -1, 0, 23}, {-1, -1, 0, 24}, {-1, -1, 0, 25}, {-1, -1, 0, 26}, {-1, -1, 0, 27},
        {-1, -1, 0, 28}, {-1, -1, 0, 29}, {-1, -1, 0, 30}, {-1, -1, 0, 31}, {-1, -1, 0, 32}, {-1, -1, 0, 33},
        {-1, -1, 0, 34}, {-1, -1, 0, 35}, {-1, -1, 0, 36}, {-1, -1, 0, 37}, {-1, -1, 0, 38}, {-1, -1, 0, 39},
        {-1, -1, 0, 40}, {-1, -1, 1, 8}, {-1, -1, 1, 9}, {-1, -1, 1, 10}, {-1, -1, 1, 11}, {-1, -1, 1, 12},
        {-1, -1, 1, 13}, {-1, -1, 1, 14}, {-1, -1, 1, 15}, {-1, -1, 1, 16}, {-1, -1, 1, 17}, {-1, -1, 1, 18},
        {-1, -1, 6, 3}, {-1, -1, 11, 2}, {-1, -1, 12, 2}, {-1, -1, 13, 2}, {-1, -1, 14, 2}, {-1, -1, 15, 2},
        {-1, -1, 16, 2}, {-1, -1, 27, 1}, {-1, -1, 28, 1}, {-1, -1, 29, 1}, {-1, -1, 30, 1}, {-1, -1, 31, 1},
        {99, 98, 0, 0}, {130, 114, 0, 0}, {115, 132, 0, 0}, {116, 135, 0, 0}, {-1, 117, 0, 0}, {118, 215, 0, 0},
        {119, 200, 0, 0}, {120, 185, 0, 0}, {121, 170, 0, 0}, {122, 162, 0, 0}, {123, 17, 0, 0}, {124, 151, 0, 0},
        {125, 148, 0, 0}, {126, 145, 0, 0}, {127, 142, 0, 0}, {128, 141, 0, 0}, {101, 100, 0, 0}, {102, 108, 0, 0},
        {131, 137, 0, 0}, {103, 113, 0, 0}, {136, 133, 0, 0}, {134, 139, 0, 0}, {105, 104, 0, 0}, {107, 106, 0, 0},
        {110, 109, 0, 0}, {140, 138, 0, 0}, {112, 111, 0, 0}, {1, 2, 0, 0}, {143, 3, 0, 0}, {4, 5, 0, 0},
        {155, 6, 0, 0}, {144, 146, 0, 0}, {8, 7, 0, 0}, {9, 10, 0, 0}, {149, 147, 0, 0}, {12, 11, 0, 0},
        {13, 15, 0, 0}, {152, 150, 0, 0}, {14, 16, 0, 0}, {18, 22, 0, 0}, {158, 153, 0, 0}, {157, 154, 0, 0},
        {25, 19, 0, 0}, {156, 159, 0, 0}, {21, 20, 0, 0}, {24, 23, 0, 0}, {26, 28, 0, 0}, {166, 160, 0, 0},
        {161, 164, 0, 0}, {27, 32, 0, 0}, {163, 165, 0, 0}, {31, 29, 0, 0}, {33, 30, 0, 0}, {40, 34, 0, 0},
        {167, 180, 0, 0}, {172, 168, 0, 0}, {175, 169, 0, 0}, {35, 47, 0, 0}, {171, 177, 0, 0}, {41, 36, 0, 0},
        {176, 173, 0, 0}, {174, 179, 0, 0}, {37, 44, 0, 0}, {46, 38, 0, 0}, {39, 43, 0, 0}, {178, 181, 0, 0},
        {42, 45, 0, 0}, {49, 48, 0, 0}, {50, 65, 0, 0}, {186, 182, 0, 0}, {183, 195, 0, 0}, {189, 184, 0, 0},
        {52, 51, 0, 0}, {54, 53, 0, 0}, {190, 187, 0, 0}, {192, 188, 0, 0}, {56, 55, 0, 0}, {58, 57, 0, 0},
        {193, 191, 0, 0}, {60, 59, 0, 0}, {62, 61, 0, 0}, {196, 194, 0, 0}, {64, 63, 0, 0}, {67, 66, 0, 0},
        {201, 197, 0, 0}, {203, 198, 0, 0}, {207, 199, 0, 0}, {69, 68, 0, 0}, {71, 70, 0, 0}, {204, 202, 0, 0},
        {73, 72, 0, 0}, {75, 74, 0, 0}, {208, 205, 0, 0}, {210, 206, 0, 0}, {77, 76, 0, 0}, {79, 78, 0, 0},
        {211, 209, 0, 0}, {81, 80, 0, 0}, {82, 97, 0, 0}, {212, 226, 0, 0}, {213, 224, 0, 0}, {218, 214, 0, 0},
        {84, 83, 0, 0}, {219, 216, 0, 0}, {221, 217, 0, 0}, {86, 85, 0, 0}, {88, 87, 0, 0}, {222, 220, 0, 0},
        {90, 89, 0, 0}, {92, 91, 0, 0}, {225, 223, 0, 0}, {94, 93, 0, 0}, {96, 95, 0, 0}
    };
    private int[] lastDc = {0, 0, 0};
//    private int[] blockLastIndex = new int[12];
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

        // Create a bit stream
        BitReader bitReader = new BitReader(buf);

        // Decode the macroblocks
        for (int y = 0; y < (height + 15) / 16; y++) {
            for (int x = 0; x < (width + 15) / 16; x++) {
                decodeBlock(bitReader);
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

    private int[][] decodeBlock(BitReader bitReader) {
        int[][] block = new int[6][64];
        for (int n = 0; n < 6; n++) {
            mpeg1DecodeBlock(bitReader, block[n], n);
        }
        return block;
    }

    /**
     * Decodes a block
     *
     * @param bitReader data
     * @param componentIndex one of 4 luma + 2 chroma components
     */
    private void mpeg1DecodeBlock(BitReader bitReader, int[] block, int componentIndex) {

        // DC coefficient
        int component = (componentIndex <= 3 ? 0 : componentIndex - 4 + 1);
        int diff = decodeDc(bitReader, component);
        if (diff >= 0xFFFF) {
            throw new RuntimeException("Invalid data!");
        }
        int dc = lastDc[component];
        dc += diff;
        lastDc[component] = dc;
        block[0] = dc * dequantizationTable[0];

//        System.out.println("Bit index: " + bitReader.position());
        short[] vlc = decodeVlc(bitReader, TEX_VLC_BITS, dctCoeff);
        int i = 0;
        int j = 0;
        while (vlc[2] != EOB) {

            int level;
            if (vlc[2] == DCT_ESCAPE) {

                int run = bitReader.readNBit(6);
                level = bitReader.readNBit(8);
                if (level == -128) {
                    level = bitReader.readNBit(8) - 256;
                } else if (level == 0) {
                    level = bitReader.readNBit(8);
                }
                i += run + 1;
                j = scanTablePermutated[i];
                if (level < 0) {
                    level = -level;
                    level = (level * dequantizationTable[j]) >> 4;
                    level = (level - 1) | 1;
                    level = -level;
                } else {
                    level = (level * dequantizationTable[j]) >> 4;
                    level = (level - 1) | 1;
                }
            } else {
                i += vlc[2] + 1;
                level = bitReader.read1Bit() == 0 ? vlc[3] : -vlc[3];
                j = scanTablePermutated[i];
                level = (level * dequantizationTable[j]) >> 4;
                level = (level - 1) | 1;
            }

            block[j] = level;
//            System.out.println("Position: " + j);
//            System.out.println("Level: " + level);
            vlc = decodeVlc(bitReader, TEX_VLC_BITS, dctCoeff);
        }
    }

    private int decodeDc(BitReader bitReader, int component) {
        int code;

        if (component == 0) {
            code = decodeVlc(bitReader, DC_VLC_BITS, dcLuminanceVlc)[2];
        } else {
            code = decodeVlc(bitReader, DC_VLC_BITS, dcCrominanceVlc)[2];
        }
        if (code < 0) {
            logger.severe("Invalid DC code!");
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

    public short[] decodeVlc(BitReader bitReader, int maxLength, short tab[][]) {
        int idx1 = 0, idx = 0;
        int readLength = -1;
        int mask = (1 << maxLength + 1);
        int bits = bitReader.fork().readNBit(maxLength + 1); // Make sure that enough bits are available

        while (idx != -1) {	// NIL ????
            mask >>>= 1;    // next bit
            idx1 = idx;     // notice
            idx = ((bits & mask) != 0) ? tab[idx][1] : tab[idx][0]; // get next index
            readLength++;	// count the length
        }

        // Advance the reader by the real length
        bitReader.skip(readLength);
        return (tab[idx1]);
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
