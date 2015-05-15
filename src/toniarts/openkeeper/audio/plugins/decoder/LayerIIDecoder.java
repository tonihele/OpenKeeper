/* Copyright (C) 2003-2014 Michael Scheerer. All Rights Reserved. */

/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package toniarts.openkeeper.audio.plugins.decoder;

import java.io.*;

/**
 * The
 * <code>LayerIIDecoder</code> class handles the main decoding of all MPEG Layer
 * 2 audio formats.
 *
 * @author	Michael Scheerer
 */
class LayerIIDecoder extends LayerIDecoder {
    // ISO/IEC 11172-3 Annex B Table B.4

    private final static double E[] = {
        1.333333333333333, 1.6, 1.142857142857142, 1.777777777777777, 1.066666666666667, 1.032258064516129, 1.015873015873015, 1.007874015748031, 1.00392156862745, 1.001956947162426, 1.000977517106549, 1.000488519785051, 1.0002442002442, 1.00012208521548, 1.000061038881767, 1.000030518518509, 1.000015259021896
    };
    // ISO/IEC 11172-3 Annex B Table B.4
    private final static double F[] = {
        0.5, 0.5, 0.25, 0.5, 0.125, 0.0625, 0.03125, 0.015625, 0.0078125, 0.00390625, 0.001953125, 0.0009765625, 0.00048828125, 0.000244140625, 0.0001220703125, 0.00006103515625, 0.000030517578125
    };
    // ISO/IEC 11172-3 Annex B Table B.4
    private final static byte ALLOCATION[] = {
        5, 7, 3, 10, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    };
    // ISO/IEC 11172-3 Annex B Table B.4
    private final static int STEPPING[] = {
        3, 5, 7, 9, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535
    };
    // [tableGroup][subbandgroup][value]
    private final static byte NBAL[][] = {
        {
            4, 4, 3, 2 // ISO/IEC 11172-3 Annex B Table B.2a/2b
        }, {
            4, 3 // ISO/IEC 11172-3 Annex B Table B.2c/2d
        }, {
            4, 3, 2 // ISO/IEC 13818-3 Annex B Table B.2e
        }
    };
    // [tableGroup][subbandgroup][value]
    private final static byte INDEX[][][] = {
        {
            {
                0, 0, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
            }, {
                0, 0, 1, 2, 3, 4, 5, 6, 7, 8, // ISO/IEC 11172-3 Annex B Table B.2a/2b/4
                9, 10, 11, 12, 13, 16
            }, {
                0, 0, 1, 2, 3, 4, 5, 16
            }, {
                0, 0, 1, 16
            }
        }, {
            {
                0, 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
            }, {
                0, 0, 1, 3, 4, 5, 6, 7// ISO/IEC 11172-3 Annex B Table B.2c/2d/4
            }
        }, {
            {
                0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
            }, {
                0, 0, 1, 3, 4, 5, 6, 7// ISO/IEC 11172-3/13818-3 Annex B Table B.2a/2b/4
            }, {
                0, 0, 1, 3
            }
        }
    };
    // [tableGroup][subband]
    private final static byte SUBBAND_GROUP[][] = {
        { // 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29
            0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3
        }, {
            0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
        }, {
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
        }
    };
    private double[][] scale = new double[3][SBLIMIT];
    private double[][] scale1 = new double[3][SBLIMIT];
    private double[][] sam = new double[3][SBLIMIT];
    private double[][] sam1 = new double[3][SBLIMIT];
    private int[] scfsi = new int[SBLIMIT];
    private int[] scfsi1 = new int[SBLIMIT];
    private int tableGroup;

    LayerIIDecoder(Frame info, InputStream in) {
        super(info, in);
        tableGroup = info.tableGroup;
    }

    @Override
    final boolean readBitAllocation() {
        for (sb = 0; sb < bound; sb++) {
            int l = NBAL[tableGroup][SUBBAND_GROUP[tableGroup][sb]];

            bitAlloc = get(l);
            if (crc != null) {
                crc.update(bitAlloc, l);
            }
            alloc[sb] = bitAlloc;
            if (channels == 2) {
                bitAlloc1 = get(l);
                if (crc != null) {
                    crc.update(bitAlloc1, l);
                }
                alloc1[sb] = bitAlloc1;
            }
        }
        for (sb = bound; sb < numSubbands; sb++) {
            int l = NBAL[tableGroup][SUBBAND_GROUP[tableGroup][sb]];

            bitAlloc = get(l);
            if (crc != null) {
                crc.update(bitAlloc, l);
            }
            alloc[sb] = bitAlloc;
            if (channels == 2) {
                alloc1[sb] = bitAlloc;
            }
        }
        readScaleFactorSelection();
        return true;
    }

    @Override
    final void readScaleFactors() {
        for (sb = 0; sb < numSubbands; sb++) {
            if (alloc[sb] != 0) {
                switch (scfsi[sb]) {
                    case 0: // '\0'
                        scale[0][sb] = SCALEFACTOR[get(6)];
                        scale[1][sb] = SCALEFACTOR[get(6)];
                        scale[2][sb] = SCALEFACTOR[get(6)];
                        break;

                    case 1: // '\001'
                        scale[0][sb] = scale[1][sb] = SCALEFACTOR[get(6)];
                        scale[2][sb] = SCALEFACTOR[get(6)];
                        break;

                    case 2: // '\002'
                        scale[0][sb] = scale[1][sb] = scale[2][sb] = SCALEFACTOR[get(6)];
                        break;

                    case 3: // '\003'
                        scale[0][sb] = SCALEFACTOR[get(6)];
                        scale[1][sb] = scale[2][sb] = SCALEFACTOR[get(6)];
                        break;
                }
            }
            if (channels == 2) {
                if (alloc1[sb] != 0) {
                    switch (scfsi1[sb]) {
                        case 0: // '\0'
                            scale1[0][sb] = SCALEFACTOR[get(6)];
                            scale1[1][sb] = SCALEFACTOR[get(6)];
                            scale1[2][sb] = SCALEFACTOR[get(6)];
                            break;

                        case 1: // '\001'
                            scale1[0][sb] = scale1[1][sb] = SCALEFACTOR[get(6)];
                            scale1[2][sb] = SCALEFACTOR[get(6)];
                            break;

                        case 2: // '\002'
                            scale1[0][sb] = scale1[1][sb] = scale1[2][sb] = SCALEFACTOR[get(6)];
                            break;

                        case 3: // '\003'
                            scale1[0][sb] = SCALEFACTOR[get(6)];
                            scale1[1][sb] = scale1[2][sb] = SCALEFACTOR[get(6)];
                            break;
                    }
                }
            }
        }
    }

    final void readScaleFactorSelection() {
        for (sb = 0; sb < numSubbands; sb++) {
            if (alloc[sb] != 0) {
                scfsi[sb] = get(2);
                if (crc != null) {
                    crc.update(scfsi[sb], 2);
                }
            }
            if (channels == 2) {
                if (alloc1[sb] != 0) {
                    scfsi1[sb] = get(2);
                    if (crc != null) {
                        crc.update(scfsi1[sb], 2);
                    }
                }
            }
        }
    }

    @Override
    final void decodeSampleData() {
        double f = 0;
        int allc;
        int allc1;

        for (gr = 0; gr < GRLIMIT; gr++) {
            for (sb = 0; sb < numSubbands; sb++) {
                if ((allc = alloc[sb]) != 0) {

                    int index = INDEX[tableGroup][SUBBAND_GROUP[tableGroup][sb]][allc];

                    int k = ALLOCATION[index];

                    if (index == 0 || index == 1 || index == 3) {

                        int x;

                        int steps = STEPPING[index];

                        if (steps == 9) {
                            x = 4;
                        } else if (steps == 5) {
                            x = 3;
                        } else {
                            x = 2;
                        }

                        k = get(k);
                        for (scf = 0; scf < 3; scf++) {
                            sam[scf][sb] = k % steps;
                            k /= steps;
                        }

                        for (scf = 0; scf < 3; scf++) {
                            f = (sam[scf][sb] - (1 << x - 1)) / (double) (1 << x - 1);
                            sam[scf][sb] = (f + F[index]) * E[index];
                        }
                    } else {
                        for (scf = 0; scf < 3; scf++) {
                            f = (get(k) - (1 << k - 1)) / (double) (1 << k - 1);
                            sam[scf][sb] = (f + F[index]) * E[index];
                        }
                    }
                }
                if ((allc1 = alloc1[sb]) != 0 && sb < bound) {

                    int index = INDEX[tableGroup][SUBBAND_GROUP[tableGroup][sb]][allc1];

                    int k = ALLOCATION[index];

                    if (index == 0 || index == 1 || index == 3) {

                        int x;

                        int steps = STEPPING[index];

                        if (steps == 9) {
                            x = 4;
                        } else if (steps == 5) {
                            x = 3;
                        } else {
                            x = 2;
                        }

                        k = get(k);
                        for (scf = 0; scf < 3; scf++) {
                            sam1[scf][sb] = k % steps;
                            k /= steps;
                        }
                        for (scf = 0; scf < 3; scf++) {
                            f = (sam1[scf][sb] - (1 << x - 1)) / (double) (1 << x - 1);
                            sam1[scf][sb] = (f + F[index]) * E[index];
                        }
                    } else {
                        for (scf = 0; scf < 3; scf++) {
                            f = (get(k) - (1 << k - 1)) / (double) (1 << k - 1);
                            sam1[scf][sb] = (f + F[index]) * E[index];
                        }
                    }
                }
            }
            for (sb = bound; sb < numSubbands; sb++) {
                for (scf = 0; scf < 3; scf++) {
                    sam1[scf][sb] = sam[scf][sb];
                }
            }
            for (scf = 0; scf < 3; scf++) {
                for (sb = 0; sb < occupiedNumSubbands; sb++) {
                    if (alloc[sb] != 0) {
                        f = sam[scf][sb];
                        f *= scale[gr >>> 2][sb];
                        if (whichChannels == STANDARD || whichChannels == LEFT_CHANNEL) {
                            filter1.samples[sb] = f * filter1.eq[sb];
                        }
                    }
                    if (alloc1[sb] != 0) {
                        f = sam1[scf][sb];
                        f *= scale1[gr >>> 2][sb];
                        if (whichChannels == STANDARD) {
                            filter2.samples[sb] = f * filter2.eq[sb];
                        } else if (whichChannels == RIGHT_CHANNEL) {
                            filter1.samples[sb] = f * filter1.eq[sb];
                        }
                    }
                }
                filter1.synthesize(this);
                if (outputChannels == 2) {
                    filter2.synthesize(this);
                }
            }
        }
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        super.close();
        if (scale != null) {
            for (scf = 0; scf < scale.length; scf++) {
                scale[scf] = null;
            }
        } else {
            return;
        }
        scale = null;
        for (scf = 0; scf < scale1.length; scf++) {
            scale1[scf] = null;
        }
        scale1 = null;
        for (scf = 0; scf < sam.length; scf++) {
            sam[scf] = null;
        }
        sam = null;
        for (scf = 0; scf < sam1.length; scf++) {
            sam1[scf] = null;
        }
        sam1 = null;
        scfsi = null;
        scfsi1 = null;
    }
}
