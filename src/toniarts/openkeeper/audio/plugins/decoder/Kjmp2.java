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
package toniarts.openkeeper.audio.plugins.decoder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import toniarts.openkeeper.tools.convert.Utils;

/**
 * Original header:
 * ****************************************************************************
 * * kjmp2 -- a minimal MPEG-1/2 Audio Layer II decoder library ** * version 1.1
 * **
 * ******************************************************************************
 * * Copyright (C) 2006-2013 Martin J. Fiedler <martin.fiedler@gmx.net> ** * **
 * * This software is provided 'as-is', without any express or implied ** *
 * warranty. In no event will the authors be held liable for any damages ** *
 * arising from the use of this software. ** * ** * Permission is granted to
 * anyone to use this software for any purpose, ** * including commercial
 * applications, and to alter it and redistribute it ** * freely, subject to the
 * following restrictions: ** * 1. The origin of this software must not be
 * misrepresented; you must not ** * claim that you wrote the original software.
 * If you use this software ** * in a product, an acknowledgment in the product
 * documentation would ** * be appreciated but is not required. ** * 2. Altered
 * source versions must be plainly marked as such, and must not ** * be
 * misrepresented as being the original software. ** * 3. This notice may not be
 * removed or altered from any source ** * distribution. **
 * *****************************************************************************
 *
 * Converted to JAVA code<br>
 * For each file you need its own Kjmp2 instance
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Kjmp2 {

    public static final int KJMP2_MAX_FRAME_SIZE = 1440;  // The maximum size of a frame
    public static final int KJMP2_SAMPLES_PER_FRAME = 1152;  // The number of samples per frame
    // Mode constants
    private final int STEREO = 0;
    private final int JOINT_STEREO = 1;
    private final int DUAL_CHANNEL = 2;
    private final int MONO = 3;
// Sample rate table
    private static final int sampleRates[] = {
        44100, 48000, 32000, 0, // MPEG-1
        22050, 24000, 16000, 0 // MPEG-2
    };
// Bitrate table
    private static final short bitrates[] = {
        32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, // MPEG-1
        8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 // MPEG-2
    };
// Scale factor base values (24-bit fixed-point)
    private static final int scfBase[] = {0x02000000, 0x01965FEA, 0x01428A30};
// Synthesis window
    private static final int d[] = {
        0x00000, 0x00000, 0x00000, 0x00000, 0x00000, 0x00000, 0x00000, -0x00001,
        -0x00001, -0x00001, -0x00001, -0x00002, -0x00002, -0x00003, -0x00003, -0x00004,
        -0x00004, -0x00005, -0x00006, -0x00006, -0x00007, -0x00008, -0x00009, -0x0000A,
        -0x0000C, -0x0000D, -0x0000F, -0x00010, -0x00012, -0x00014, -0x00017, -0x00019,
        -0x0001C, -0x0001E, -0x00022, -0x00025, -0x00028, -0x0002C, -0x00030, -0x00034,
        -0x00039, -0x0003E, -0x00043, -0x00048, -0x0004E, -0x00054, -0x0005A, -0x00060,
        -0x00067, -0x0006E, -0x00074, -0x0007C, -0x00083, -0x0008A, -0x00092, -0x00099,
        -0x000A0, -0x000A8, -0x000AF, -0x000B6, -0x000BD, -0x000C3, -0x000C9, -0x000CF,
        0x000D5, 0x000DA, 0x000DE, 0x000E1, 0x000E3, 0x000E4, 0x000E4, 0x000E3,
        0x000E0, 0x000DD, 0x000D7, 0x000D0, 0x000C8, 0x000BD, 0x000B1, 0x000A3,
        0x00092, 0x0007F, 0x0006A, 0x00053, 0x00039, 0x0001D, -0x00001, -0x00023,
        -0x00047, -0x0006E, -0x00098, -0x000C4, -0x000F3, -0x00125, -0x0015A, -0x00190,
        -0x001CA, -0x00206, -0x00244, -0x00284, -0x002C6, -0x0030A, -0x0034F, -0x00396,
        -0x003DE, -0x00427, -0x00470, -0x004B9, -0x00502, -0x0054B, -0x00593, -0x005D9,
        -0x0061E, -0x00661, -0x006A1, -0x006DE, -0x00718, -0x0074D, -0x0077E, -0x007A9,
        -0x007D0, -0x007EF, -0x00808, -0x0081A, -0x00824, -0x00826, -0x0081F, -0x0080E,
        0x007F5, 0x007D0, 0x007A0, 0x00765, 0x0071E, 0x006CB, 0x0066C, 0x005FF,
        0x00586, 0x00500, 0x0046B, 0x003CA, 0x0031A, 0x0025D, 0x00192, 0x000B9,
        -0x0002C, -0x0011F, -0x00220, -0x0032D, -0x00446, -0x0056B, -0x0069B, -0x007D5,
        -0x00919, -0x00A66, -0x00BBB, -0x00D16, -0x00E78, -0x00FDE, -0x01148, -0x012B3,
        -0x01420, -0x0158C, -0x016F6, -0x0185C, -0x019BC, -0x01B16, -0x01C66, -0x01DAC,
        -0x01EE5, -0x02010, -0x0212A, -0x02232, -0x02325, -0x02402, -0x024C7, -0x02570,
        -0x025FE, -0x0266D, -0x026BB, -0x026E6, -0x026ED, -0x026CE, -0x02686, -0x02615,
        -0x02577, -0x024AC, -0x023B2, -0x02287, -0x0212B, -0x01F9B, -0x01DD7, -0x01BDD,
        0x019AE, 0x01747, 0x014A8, 0x011D1, 0x00EC0, 0x00B77, 0x007F5, 0x0043A,
        0x00046, -0x003E5, -0x00849, -0x00CE3, -0x011B4, -0x016B9, -0x01BF1, -0x0215B,
        -0x026F6, -0x02CBE, -0x032B3, -0x038D3, -0x03F1A, -0x04586, -0x04C15, -0x052C4,
        -0x05990, -0x06075, -0x06771, -0x06E80, -0x0759F, -0x07CCA, -0x083FE, -0x08B37,
        -0x09270, -0x099A7, -0x0A0D7, -0x0A7FD, -0x0AF14, -0x0B618, -0x0BD05, -0x0C3D8,
        -0x0CA8C, -0x0D11D, -0x0D789, -0x0DDC9, -0x0E3DC, -0x0E9BD, -0x0EF68, -0x0F4DB,
        -0x0FA12, -0x0FF09, -0x103BD, -0x1082C, -0x10C53, -0x1102E, -0x113BD, -0x116FB,
        -0x119E8, -0x11C82, -0x11EC6, -0x120B3, -0x12248, -0x12385, -0x12467, -0x124EF,
        0x1251E, 0x124F0, 0x12468, 0x12386, 0x12249, 0x120B4, 0x11EC7, 0x11C83,
        0x119E9, 0x116FC, 0x113BE, 0x1102F, 0x10C54, 0x1082D, 0x103BE, 0x0FF0A,
        0x0FA13, 0x0F4DC, 0x0EF69, 0x0E9BE, 0x0E3DD, 0x0DDCA, 0x0D78A, 0x0D11E,
        0x0CA8D, 0x0C3D9, 0x0BD06, 0x0B619, 0x0AF15, 0x0A7FE, 0x0A0D8, 0x099A8,
        0x09271, 0x08B38, 0x083FF, 0x07CCB, 0x075A0, 0x06E81, 0x06772, 0x06076,
        0x05991, 0x052C5, 0x04C16, 0x04587, 0x03F1B, 0x038D4, 0x032B4, 0x02CBF,
        0x026F7, 0x0215C, 0x01BF2, 0x016BA, 0x011B5, 0x00CE4, 0x0084A, 0x003E6,
        -0x00045, -0x00439, -0x007F4, -0x00B76, -0x00EBF, -0x011D0, -0x014A7, -0x01746,
        0x019AE, 0x01BDE, 0x01DD8, 0x01F9C, 0x0212C, 0x02288, 0x023B3, 0x024AD,
        0x02578, 0x02616, 0x02687, 0x026CF, 0x026EE, 0x026E7, 0x026BC, 0x0266E,
        0x025FF, 0x02571, 0x024C8, 0x02403, 0x02326, 0x02233, 0x0212B, 0x02011,
        0x01EE6, 0x01DAD, 0x01C67, 0x01B17, 0x019BD, 0x0185D, 0x016F7, 0x0158D,
        0x01421, 0x012B4, 0x01149, 0x00FDF, 0x00E79, 0x00D17, 0x00BBC, 0x00A67,
        0x0091A, 0x007D6, 0x0069C, 0x0056C, 0x00447, 0x0032E, 0x00221, 0x00120,
        0x0002D, -0x000B8, -0x00191, -0x0025C, -0x00319, -0x003C9, -0x0046A, -0x004FF,
        -0x00585, -0x005FE, -0x0066B, -0x006CA, -0x0071D, -0x00764, -0x0079F, -0x007CF,
        0x007F5, 0x0080F, 0x00820, 0x00827, 0x00825, 0x0081B, 0x00809, 0x007F0,
        0x007D1, 0x007AA, 0x0077F, 0x0074E, 0x00719, 0x006DF, 0x006A2, 0x00662,
        0x0061F, 0x005DA, 0x00594, 0x0054C, 0x00503, 0x004BA, 0x00471, 0x00428,
        0x003DF, 0x00397, 0x00350, 0x0030B, 0x002C7, 0x00285, 0x00245, 0x00207,
        0x001CB, 0x00191, 0x0015B, 0x00126, 0x000F4, 0x000C5, 0x00099, 0x0006F,
        0x00048, 0x00024, 0x00002, -0x0001C, -0x00038, -0x00052, -0x00069, -0x0007E,
        -0x00091, -0x000A2, -0x000B0, -0x000BC, -0x000C7, -0x000CF, -0x000D6, -0x000DC,
        -0x000DF, -0x000E2, -0x000E3, -0x000E3, -0x000E2, -0x000E0, -0x000DD, -0x000D9,
        0x000D5, 0x000D0, 0x000CA, 0x000C4, 0x000BE, 0x000B7, 0x000B0, 0x000A9,
        0x000A1, 0x0009A, 0x00093, 0x0008B, 0x00084, 0x0007D, 0x00075, 0x0006F,
        0x00068, 0x00061, 0x0005B, 0x00055, 0x0004F, 0x00049, 0x00044, 0x0003F,
        0x0003A, 0x00035, 0x00031, 0x0002D, 0x00029, 0x00026, 0x00023, 0x0001F,
        0x0001D, 0x0001A, 0x00018, 0x00015, 0x00013, 0x00011, 0x00010, 0x0000E,
        0x0000D, 0x0000B, 0x0000A, 0x00009, 0x00008, 0x00007, 0x00007, 0x00006,
        0x00005, 0x00005, 0x00004, 0x00004, 0x00003, 0x00003, 0x00002, 0x00002,
        0x00002, 0x00002, 0x00001, 0x00001, 0x00001, 0x00001, 0x00001, 0x00001
    };
///////////// Table 3-B.2: Possible quantization per subband ///////////////////
// Quantizer lookup, step 1: bitrate classes
    private static final byte quantLutStep1[][] = {
        // 32, 48, 56, 64, 80, 96,112,128,160,192,224,256,320,384 <- bitrate
        {0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2}, // mono
        // 16, 24, 28, 32, 40, 48, 56, 64, 80, 96,112,128,160,192 <- BR / chan
        {0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 2, 2} // stereo
    };
// Quantizer lookup, step 2: bitrate class, sample rate -> B2 table idx, sblimit
    private static final byte QUANT_TAB_A = (27 | 64);   // Table 3-B.2a: high-rate, sblimit = 27
    private static final byte QUANT_TAB_B = (30 | 64); // Table 3-B.2b: high-rate, sblimit = 30
    private static final byte QUANT_TAB_C = 8; // Table 3-B.2c:  low-rate, sblimit =  8
    private static final byte QUANT_TAB_D = 12; // Table 3-B.2d:  low-rate, sblimit = 12
    private static final byte quantLutStep2[][] = {
        //   44.1 kHz,      48 kHz,      32 kHz
        {QUANT_TAB_C, QUANT_TAB_C, QUANT_TAB_D}, // 32 - 48 kbit/sec/ch
        {QUANT_TAB_A, QUANT_TAB_A, QUANT_TAB_A}, // 56 - 80 kbit/sec/ch
        {QUANT_TAB_B, QUANT_TAB_A, QUANT_TAB_B}, // 96+     kbit/sec/ch
    };
// Quantizer lookup, step 3: B2 table, subband -> nbal, row index
// (upper 4 bits: nbal, lower 4 bits: row index)
    private static final byte quantLutStep3[][] = {
        // low-rate table (3-B.2c and 3-B.2d)
        {0x44, 0x44, // SB  0 -  1
            0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34 // SB  2 - 12
        },
        // high-rate table (3-B.2a and 3-B.2b)
        {0x43, 0x43, 0x43, // SB  0 -  2
            0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, // SB  3 - 10
            0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, // SB 11 - 22
            0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20 // SB 23 - 29
        },
        // MPEG-2 LSR table (B.2 in ISO 13818-3)
        {0x45, 0x45, 0x45, 0x45, // SB  0 -  3
            0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34, // SB  4 - 10
            0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, // SB 11 -
            0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24 //       - 29
        }
    };
// Quantizer lookup, step 4: table row, allocation[] value -> quant table index
    private static final byte quantLutStep4[][] = {
        {0, 1, 2, 17},
        {0, 1, 2, 3, 4, 5, 6, 17},
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 17},
        {0, 1, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17},
        {0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17},
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}
    };
// Quantizer specification structure

    private static class QuantizerSpec {

        private final short nlevels;
        private final byte grouping;
        private final byte cwBits;

        public QuantizerSpec(short nlevels, byte grouping, byte cwBits) {
            this.nlevels = nlevels;
            this.grouping = grouping;
            this.cwBits = cwBits;
        }
    };
// Quantizer table
    private static final QuantizerSpec quantizerTable[] = {
        new QuantizerSpec((short) 3, (byte) 1, (byte) 5), //  1
        new QuantizerSpec((short) 5, (byte) 1, (byte) 7), //  2
        new QuantizerSpec((short) 7, (byte) 0, (byte) 3), //  3
        new QuantizerSpec((short) 9, (byte) 1, (byte) 10), //  4
        new QuantizerSpec((short) 15, (byte) 0, (byte) 4), //  5
        new QuantizerSpec((short) 31, (byte) 0, (byte) 5), //  6
        new QuantizerSpec((short) 63, (byte) 0, (byte) 6), //  7
        new QuantizerSpec((short) 127, (byte) 0, (byte) 7), //  8
        new QuantizerSpec((short) 255, (byte) 0, (byte) 8), //  9
        new QuantizerSpec((short) 511, (byte) 0, (byte) 9), // 10
        new QuantizerSpec((short) 1023, (byte) 0, (byte) 10), // 11
        new QuantizerSpec((short) 2047, (byte) 0, (byte) 11), // 12
        new QuantizerSpec((short) 4095, (byte) 0, (byte) 12), // 13
        new QuantizerSpec((short) 8191, (byte) 0, (byte) 13), // 14
        new QuantizerSpec((short) 16383, (byte) 0, (byte) 14), // 15
        new QuantizerSpec((short) 32767, (byte) 0, (byte) 15), // 16
        new QuantizerSpec((short) 65535, (byte) 0, (byte) 16) // 17
    };
////////////////////////////////////////////////////////////////////////////////
// STATIC VARIABLES AND FUNCTIONS                                             //
////////////////////////////////////////////////////////////////////////////////
    private int bitWindow;
    private int bitsInWindow;
    private ByteBuffer framePos;

    private int showBits(int bitCount) {
        return (bitWindow >> (24 - (bitCount)));
    }

    private int getBits(int bitCount) {
        int result = showBits(bitCount);
        bitWindow = (bitWindow << bitCount) & 0xFFFFFF;
        bitsInWindow -= bitCount;
        while (bitsInWindow < 16) {
            bitWindow |= (Utils.toUnsignedByte(framePos.get())) << (16 - bitsInWindow);
            bitsInWindow += 8;
        }
        return result;
    }
////////////////////////////////////////////////////////////////////////////////
// INITIALIZATION                                                             //
////////////////////////////////////////////////////////////////////////////////
    private static final int n[][] = new int[64][32];  // N[i][j] as 8-bit fixed-point

    static {

        // Compute N[i][j]
        for (int i = 0; i < 64; ++i) {
            for (int j = 0; j < 32; ++j) {
                n[i][j] = (int) (256.0 * Math.cos(((16 + i) * ((j << 1) + 1)) * 0.0490873852123405));
            }
        }
    }

    /**
     * A small class to hold the context of a single MP2 file
     */
    private class Kjmp2Context {

        private int v[][] = new int[2][1024];
        private int vOffs = 0;
    };

    /**
     * Returns the sample rate of a MP2 stream.
     *
     * @param frame Points to at least the first three bytes of a frame from the
     * stream.
     * @return The sample rate of the stream in Hz, or zero if the stream isn't
     * valid.
     */
    public static int kjmp2GetSampleRate(
            byte[] frame) {
        if (frame == null) {
            return 0;
        }
        if ((Utils.toUnsignedByte(frame[0]) != 0xFF) // No valid syncword?
                || ((Utils.toUnsignedByte(frame[1]) & 0xF6) != 0xF4) // No MPEG-1/2 Audio Layer II?
                || ((Utils.toUnsignedByte(frame[2]) - 0x10) >= 0xE0)) // Invalid bitrate?
        {
            return 0;
        }
        return sampleRates[(((Utils.toUnsignedByte(frame[1]) & 0x08) >> 1) ^ 4) // MPEG-1/2 switch
                + ((Utils.toUnsignedByte(frame[2]) >> 2) & 3)];         // Actual rate
    }

////////////////////////////////////////////////////////////////////////////////
// DECODE HELPER FUNCTIONS                                                    //
////////////////////////////////////////////////////////////////////////////////
    private QuantizerSpec readAllocation(int sb, int b2Table) {
        int tableIdx = quantLutStep3[b2Table][sb];
        tableIdx = quantLutStep4[tableIdx & 15][getBits(tableIdx >> 4)];
        return tableIdx != 0 ? (quantizerTable[tableIdx - 1]) : null;
    }

    private void readSamples(
            QuantizerSpec q, int scalefactor, int[] sample) {
        int idx, adj, scale;
        int val;
        if (q == null) {
            // No bits allocated for this subband
            sample[0] = sample[1] = sample[2] = 0;
            return;
        }

        // Resolve scalefactor
        if (scalefactor == 63) {
            scalefactor = 0;
        } else {
            adj = scalefactor / 3;
            scalefactor = (scfBase[scalefactor % 3] + ((1 << adj) >> 1)) >> adj;
        }

        // Decode samples
        adj = q.nlevels;
        if (q.grouping != 0) {

            // Decode grouped samples
            val = getBits(q.cwBits);
            sample[0] = val % adj;
            val /= adj;
            sample[1] = val % adj;
            sample[2] = val / adj;
        } else {
            // Decode direct samples
            for (idx = 0; idx < 3; ++idx) {
                sample[idx] = getBits(q.cwBits);
            }
        }

        // Postmultiply samples
        scale = 65536 / (adj + 1);
        adj = ((adj + 1) >> 1) - 1;
        for (idx = 0; idx < 3; ++idx) {
            // Step 1: renormalization to [-1..1]
            val = (adj - sample[idx]) * scale;
            // Step 2: apply scalefactor
            sample[idx] = (val * (scalefactor >> 12) // Upper part
                    + ((val * (scalefactor & 4095) + 2048) >> 12)) // Lower part
                    >> 12;  // Scale adjust
        }
    }
////////////////////////////////////////////////////////////////////////////////
// FRAME DECODE FUNCTION                                                      //
////////////////////////////////////////////////////////////////////////////////
    private final QuantizerSpec allocation[][] = new QuantizerSpec[2][32];
    private final int scfsi[][] = new int[2][32];
    private final int scalefactor[][][] = new int[2][32][3];
    private final int sample[][][] = new int[2][32][3];
    private final int u[] = new int[512];
    private final Kjmp2Context mp2 = new Kjmp2Context();
    private int mode;
    private int samplingFrequency;

    /**
     * Decode one frame of audio
     *
     * @param frame A pointer to the frame to decode. It *must* be a complete
     * frame, because no error checking is done!
     * @param pcm A pointer to the output PCM data. kjmp2DecodeFrame() will
     * always return 1152 (=KJMP2_SAMPLES_PER_FRAME) interleaved samples in a
     * native-endian 16-bit signed format.
     * @return The number of bytes in the current frame. In a valid stream,
     * frame + kjmp2DecodeFrame(..., frame, ...) will point to the next frame,
     * if frames are consecutive in memory.<br>Note: pcm may be NULL. In this
     * case, kjmp2DecodeFrame() will return the size of the frame without
     * actually decoding it.
     */
    public int kjmp2DecodeFrame(
            byte[] frame,
            ShortBuffer pcm) {
        int bitRateIndexMinus1;
        int paddingBit;
        int frameSize;
        int bound, sblimit;
        int sb, ch, gr, part, idx, nch, i, j, sum;
        int tableIdx;

        // General sanity check
        if (frame == null) {
            return 0;
        }

        // Check for valid header: syncword OK, MPEG-Audio Layer 2
        if ((Utils.toUnsignedByte(frame[0]) != 0xFF) || ((Utils.toUnsignedByte(frame[1]) & 0xF6) != 0xF4)) {
            return 0;
        }

        // Set up the bitstream reader
        bitWindow = Utils.toUnsignedByte(frame[2]) << 16;
        bitsInWindow = 8;
        framePos = ByteBuffer.wrap(Arrays.copyOfRange(frame, 3, frame.length - 2));

        // Read the rest of the header
        bitRateIndexMinus1 = getBits(4) - 1;
        if (bitRateIndexMinus1 > 13) {
            return 0;  // Invalid bit rate or 'free format'
        }
        samplingFrequency = getBits(2);
        if (samplingFrequency == 3) {
            return 0;
        }
        if ((Utils.toUnsignedByte(frame[1]) & 0x08) == 0) {  // MPEG-2
            samplingFrequency += 4;
            bitRateIndexMinus1 += 14;
        }
        paddingBit = getBits(1);
        getBits(1);  // Discard private_bit
        mode = getBits(2);

        // Parse the mode_extension, set up the stereo bound
        if (mode == JOINT_STEREO) {
            bound = (getBits(2) + 1) << 2;
        } else {
            getBits(2);
            bound = (mode == MONO) ? 0 : 32;
        }

        // Discard the last 4 bits of the header and the CRC value, if present
        getBits(4);
        if ((Utils.toUnsignedByte(frame[1]) & 1) == 0) {
            getBits(16);
        }

        // Compute the frame size
        frameSize = (144000 * bitrates[bitRateIndexMinus1]
                / sampleRates[samplingFrequency]) + paddingBit;
        if (pcm == null) {
            return frameSize;  // No decoding
        }
        // Prepare the quantizer table lookups
        if ((samplingFrequency & 4) != 0) {
            // MPEG-2 (LSR)
            tableIdx = 2;
            sblimit = 30;
        } else {
            // MPEG-1
            tableIdx = (mode == MONO) ? 0 : 1;
            tableIdx = quantLutStep1[tableIdx][bitRateIndexMinus1];
            tableIdx = quantLutStep2[tableIdx][samplingFrequency];
            sblimit = tableIdx & 63;
            tableIdx >>= 6;
        }
        if (bound > sblimit) {
            bound = sblimit;
        }

        // Read the allocation information
        for (sb = 0; sb < bound; ++sb) {
            for (ch = 0; ch < 2; ++ch) {
                allocation[ch][sb] = readAllocation(sb, tableIdx);
            }
        }
        for (sb = bound; sb < sblimit; ++sb) {
            allocation[0][sb] = allocation[1][sb] = readAllocation(sb, tableIdx);
        }

        // Read scale factor selector information
        nch = (mode == MONO) ? 1 : 2;
        for (sb = 0; sb < sblimit; ++sb) {
            for (ch = 0; ch < nch; ++ch) {
                if (allocation[ch][sb] != null) {
                    scfsi[ch][sb] = getBits(2);
                }
            }
            if (mode == MONO) {
                scfsi[1][sb] = scfsi[0][sb];
            }
        }

        // Read scale factors
        for (sb = 0; sb < sblimit; ++sb) {
            for (ch = 0; ch < nch; ++ch) {
                if (allocation[ch][sb] != null) {
                    switch (scfsi[ch][sb]) {
                        case 0:
                            scalefactor[ch][sb][0] = getBits(6);
                            scalefactor[ch][sb][1] = getBits(6);
                            scalefactor[ch][sb][2] = getBits(6);
                            break;
                        case 1:
                            scalefactor[ch][sb][0] =
                                    scalefactor[ch][sb][1] = getBits(6);
                            scalefactor[ch][sb][2] = getBits(6);
                            break;
                        case 2:
                            scalefactor[ch][sb][0] =
                                    scalefactor[ch][sb][1] =
                                    scalefactor[ch][sb][2] = getBits(6);
                            break;
                        case 3:
                            scalefactor[ch][sb][0] = getBits(6);
                            scalefactor[ch][sb][1] =
                                    scalefactor[ch][sb][2] = getBits(6);
                            break;
                    }
                }
            }
            if (mode == MONO) {
                for (part = 0; part < 3; ++part) {
                    scalefactor[1][sb][part] = scalefactor[0][sb][part];
                }
            }
        }

        // Coefficient input and reconstruction
        for (part = 0; part < 3; ++part) {
            for (gr = 0; gr < 4; ++gr) {

                // Read the samples
                for (sb = 0; sb < bound; ++sb) {
                    for (ch = 0; ch < 2; ++ch) {
                        readSamples(allocation[ch][sb], scalefactor[ch][sb][part], sample[ch][sb]);
                    }
                }
                for (sb = bound; sb < sblimit; ++sb) {
                    readSamples(allocation[0][sb], scalefactor[0][sb][part], sample[0][sb]);
                    for (idx = 0; idx < 3; ++idx) {
                        sample[1][sb][idx] = sample[0][sb][idx];
                    }
                }
                for (ch = 0; ch < 2; ++ch) {
                    for (sb = sblimit; sb < 32; ++sb) {
                        for (idx = 0; idx < 3; ++idx) {
                            sample[ch][sb][idx] = 0;
                        }
                    }
                }

                // Synthesis loop
                for (idx = 0; idx < 3; ++idx) {
                    // Shifting step
                    mp2.vOffs = tableIdx = (mp2.vOffs - 64) & 1023;

                    for (ch = 0; ch < (mode == MONO ? 1 : 2); ++ch) {
                        // Matrixing
                        for (i = 0; i < 64; ++i) {
                            sum = 0;
                            for (j = 0; j < 32; ++j) {
                                sum += n[i][j] * sample[ch][j][idx];  // 8b*15b=23b
                            }                        // Intermediate value is 28 bit (23 + 5), clamp to 14b
                            mp2.v[ch][tableIdx + i] = (sum + 8192) >> 14;
                        }

                        // Construction of U
                        for (i = 0; i < 8; ++i) {
                            for (j = 0; j < 32; ++j) {
                                u[(i << 6) + j] = mp2.v[ch][(tableIdx + (i << 7) + j) & 1023];
                                u[(i << 6) + j + 32] = mp2.v[ch][(tableIdx + (i << 7) + j + 96) & 1023];
                            }
                        }

                        // Apply window
                        for (i = 0; i < 512; ++i) {
                            u[i] = (u[i] * d[i] + 32) >> 6;
                        }

                        // Output samples
                        for (j = 0; j < 32; ++j) {
                            sum = 0;
                            for (i = 0; i < 16; ++i) {
                                sum -= u[(i << 5) + j];
                            }
                            sum = (sum + 8) >> 4;
                            if (sum < -32768) {
                                sum = -32768;
                            }
                            if (sum > 32767) {
                                sum = 32767;
                            }
                            pcm.put(((mode == MONO ? idx << 5 : idx << 6) | (mode == MONO ? j : (j << 1)) | ch) + pcm.position(), (short) sum);
                        }
                    } // End of synthesis channel loop
                } // End of synthesis sub-block loop

                // Adjust PCM output pointer: decoded 3 * 32 = 96 stereo samples
                pcm.position(pcm.position() + (mode == MONO ? 96 : 192));
            } // Decoding of the granule finished
        }
        return frameSize;
    }

    /**
     * Get the number of channels
     *
     * @return number of channels
     */
    public int getNumberOfChannels() {
        return (mode == MONO ? 1 : 2);
    }

    /**
     * Get the sample rate
     *
     * @return sample rate of the file
     */
    public int kjmp2GetSampleRate() {
        return sampleRates[samplingFrequency];
    }
}
