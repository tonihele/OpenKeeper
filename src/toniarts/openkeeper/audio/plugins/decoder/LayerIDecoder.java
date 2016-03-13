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
 * <code>LayerIDecoder</code> class handles the main decoding of all MPEG Layer
 * 1 audio formats.
 *
 * @author	Michael Scheerer
 */
class LayerIDecoder extends BitStream {
    // ISO/IEC 11172-3 Annex B Table B.1
    // Array size must be 64, although a scalefactor with index 63
    // is illegal

    final static double SCALEFACTOR[] = {
        2, 1.5874010519682, 1.25992104989487, 1, 0.7937005259841, 0.62996052494744, 0.5, 0.39685026299205, 0.31498026247372, 0.25, 0.19842513149602, 0.15749013123686, 0.125, 0.09921256574801, 0.07874506561843, 0.0625, 0.04960628287401, 0.03937253280921, 0.03125, 0.024803141437, 0.0196862640461, 0.015625, 0.0124015707185, 0.0098431332023, 0.0078125, 0.00620078535925, 0.00492156660115, 0.00390625, 0.00310039267963, 0.00246078330058, 0.001953125, 0.00155019633981, 0.00123039165029, 0.0009765625, 0.00077509816991, 0.00061519582514, 0.00048828125, 0.00038754908495, 0.00030759791257, 0.000244140625, 0.00019377454248, 0.00015379895629, 0.0001220703125, 9.688727124E-005, 7.689947814E-005, 6.103515625E-005, 4.844363562E-005, 3.844973907E-005, 3.051757813E-005, 2.422181781E-005, 1.922486954E-005, 1.525878906E-005, 1.21109089E-005, 9.61243477E-006, 7.62939453E-006, 6.05545445E-006, 4.80621738E-006, 3.81469727E-006, 3.02772723E-006, 2.40310869E-006, 1.90734863E-006, 1.51386361E-006,
        1.20155435E-006, 0
    };
    // C[index] = 2 / (2^(index+1)-1)
    // according to the 14 possible alocation values. Note that the allocation value of 15 is forbidden!
    final static double C[] = {
        2, 0.666666666666667, 0.285714285714285, 0.133333333333333, 0.064516129032258, 0.031746031746031, 0.015748031496062, 0.0078431372549019, 0.0039138943248532, 0.001955034213098, 0.0009770395701, 0.0004884004884, 0.00024417043096, 0.000122077763535, 6.10370189519943E-005
    };
    // General used upper bounds for all layers
    final static int GRLIMIT = 12; // Upper bound of granule number
    final static int FLPGLIMIT = 576; // Upper bound of frequency line number per granule
    final static int FLPSLIMIT = 18; // Upper bound of frequency line number per subband
    final static int SBLIMIT = 32; // Upper bound of subband number per frame
    // General used counters for all layers
    int fl; // frequency line
    int scf; // scalefactor
    int sb; // subband
    int ch; // channels
    int gr; // granules
    int window; // window types
    int sfb; // scalefactor band
    int sfbBlock; // scalefactor block
    CRC16 crc;
    int[] alloc;
    int[] alloc1;
    int bitAlloc;
    int bitAlloc1;
    double[] scale;
    double[] scale1;
    int[] sam;
    int[] sam1;

    LayerIDecoder(Frame info, InputStream in) {
        super(info, in);
        crc = (CRC16) info.crc;
        numSubbands = info.numberOfSubbands;

        if (info.layer < 3) {
            alloc = new int[numSubbands];
            alloc1 = new int[numSubbands];

            sam = new int[numSubbands];
            sam1 = new int[numSubbands];
        }

        if (info.layer < 2) {
            scale = new double[numSubbands];
            scale1 = new double[numSubbands];
        }
    }

    @Override
    void decode() {
        if (!readBitAllocation()) {
            return;
        }
        if (!info.checksumOk()) {
            return;
        }
        readScaleFactors();
        decodeSampleData();
    }

    boolean readBitAllocation() {
        for (sb = 0; sb < bound; sb++) {
            if ((bitAlloc = get(4)) == 15) {
                return false;
            }
            if (crc != null) {
                crc.update(bitAlloc, 4);
            }
            alloc[sb] = bitAlloc;
            if (channels == 2) {
                if ((bitAlloc1 = get(4)) == 15) {
                    return false;
                }
                if (crc != null) {
                    crc.update(bitAlloc1, 4);
                }
                alloc1[sb] = bitAlloc1;
            }
        }
        for (sb = bound; sb < numSubbands; sb++) {
            if ((bitAlloc = get(4)) == 15) {
                return false;
            }
            if (crc != null) {
                crc.update(bitAlloc, 4);
            }
            alloc[sb] = bitAlloc;
            if (channels == 2) {
                alloc1[sb] = bitAlloc;
            }
        }
        return true;
    }

    void readScaleFactors() {
        for (sb = 0; sb < numSubbands; sb++) {
            if (alloc[sb] != 0) {
                scale[sb] = SCALEFACTOR[get(6)];
            }
            if (channels == 2) {
                if (alloc1[sb] != 0) {
                    scale1[sb] = SCALEFACTOR[get(6)];
                }
            }
        }
    }

    void decodeSampleData() {
        double f;
        int allc;

        for (gr = 0; gr < GRLIMIT; gr++) {
            for (sb = 0; sb < bound; sb++) {
                if (alloc[sb] != 0) {
                    sam[sb] = get(alloc[sb] + 1);
                }
                if (alloc1[sb] != 0) {
                    sam1[sb] = get(alloc1[sb] + 1);
                }
            }
            for (sb = bound; sb < numSubbands; sb++) {
                if (alloc[sb] != 0) {
                    sam[sb] = get(alloc[sb] + 1);
                }
                if (channels == 2) {
                    sam1[sb] = sam[sb];
                }
            }
            for (sb = 0; sb < occupiedNumSubbands; sb++) {
                if ((allc = alloc[sb]) != 0) {
                    f = (sam[sb] + 1 - (1 << allc)) * C[allc] * scale[sb];
                    if (whichChannels == STANDARD || whichChannels == LEFT_CHANNEL) {
                        filter1.samples[sb] = f * filter1.eq[sb];
                    }
                }
                if ((allc = alloc1[sb]) != 0) {
                    f = (sam1[sb] + 1 - (1 << allc)) * C[allc] * scale1[sb];
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

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        super.close();
        scale = null;
        scale1 = null;
        alloc = null;
        alloc1 = null;
        sam = null;
        sam1 = null;
    }
}
