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

/**
 * The
 * <code>CRC16</code> class performs the computing of the CRC-16 error
 * correction of a data stream. The class contains a simple table-less
 * algorithm, which is approximately 40% faster than the one used inside the
 * software simulation of the Motion Picture Expert Group and has roughly half
 * the performance of the best table-based algorithms during decoding MPEG
 * audio. In relation of the global MPEG audio decoding process the performance
 * difference between the implemented and a table-based algorithm is negligible.
 *
 * @author	Michael Scheerer
 */
final class CRC16 {

    private final static short POLY = (short) 0x8005;
    private short crc;

    // Creates a new CRC16 class.
    CRC16() {
        crc = -1;
    }

    // Resets CRC-16 to initial value.
    void reset() {
        crc = -1;
    }

    // Returns CRC-16 value.
    long getValue() {
        return (long) crc;
    }

    // Updates CRC-16 with specified byte.
    // Parameter i is the byte to update the checksum with.
    // Parameter j is the used length of the byte.
    // Returns the calculatet new CRC-word.
    int update(int i, int j) {
        i &= (1 << j) - 1;
        crc ^= i << 16 - j;

        while (j-- != 0) {
            if ((crc >>> 15 & 1) != 0) {
                crc <<= 1;
                crc ^= POLY;
            } else {
                crc <<= 1;
            }
        }
        return crc;
    }
}
