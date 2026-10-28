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
 * <code>BitStream</code> class acts as a buffered input stream from which you
 * can get the data bitwise. It stores the data in 8 bit registers. An audio
 * sync algorithm is implemented, which scans for the next audio header. Also an
 * implementing of active seeking in case of variable bitrate is done. The
 * result is a really frame exact playtime and seek position calculation.
 *
 * @author Michael Scheerer
 */
abstract class BitStream extends BitrateVariation {

    final static int BYTELENGTH = 8;
    private final static int MAX_FRAMESIZE = 1732;
    private final static int AUDIO_SYNC_PATTERN = 0xFFE00000; // MPEG 2.5 compliant
    final static int BITMASK[] = {
        0x00, 0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, 0xFF,};
    int bound;
    private byte[] buf;
    private byte[] b;
    private int byteIdx;
    private int bitIdx;
    private boolean header;
    private final InputStream source;

    /**
     * Constructs an instance of
     * <code>BitStream</code> with a
     * <code>MediaInformation</code> object managing all necessary objects
     * needed for the global decoder system. The
     * <code>BitStream</code> class acts also as a
     * <code>MediaControl</code> object containing presettings.
     *
     * @param info the <code>Frame</code> object containing the neccesary
     * informations about the source
     * @param in the input stream
     */
    BitStream(Frame info, InputStream in) {
        super(info, in);

        buf = new byte[MAX_FRAMESIZE + 1];
        b = new byte[4];
        source = in;
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        super.close();
        buf = null;
        b = null;
    }

    @Override
    final int skipFrame() throws IOException {
        Frame info = (Frame) information;
        int i;

        do {
            i = nextHeader();
            if (i == -1) {
                return ACTIVE_SEEKING_FAILED;
            }
            info.protectionBit = i >>> 16 & 0x1;
            info.bitrateIndex = i >>> 12 & 0xf;
            info.paddingBit = i >>> 9 & 0x1;
            info.mode = i >>> 6 & 0x3;
            headerlessFrameSize = info.calculateFrameSize();
            if (source.available() < headerlessFrameSize) {
                return ACTIVE_SEEKING_ABORTED;
            }
            source.skip(headerlessFrameSize);
        } while (!header);

        return headerlessFrameSize;
    }

    @Override
    final int readFrame() throws IOException {
        Frame info = (Frame) information;
        int i;

        do {
            i = nextHeader();
            if (i == -1) {
                return Events.EOM_EVENT;
            }
            info.protectionBit = i >>> 16 & 0x1;
            info.bitrateIndex = i >>> 12 & 0xF;
            info.paddingBit = i >>> 9 & 0x1;
            info.mode = i >>> 6 & 0x3;
            info.modeExtension = i >>> 4 & 0x3;
            bound = 0;
            if (info.mode == JOINT_STEREO) {
                bound = (info.modeExtension << 2) + 4;
            }
            if (bound > info.numberOfSubbands || bound == 0) {
                bound = info.numberOfSubbands;
            }
            headerlessFrameSize = info.calculateFrameSize();
            if (!load(headerlessFrameSize)) {
                return Events.EOM_EVENT;
            }
        } while (!header);

        bitIdx = BYTELENGTH;
        byteIdx = 0;

        if (info.isProtected()) {
            info.setChecksum((short) get(16));
            ((CRC16) info.getCrc()).update(i, 16);
        }
        return Events.VALIDATION_EVENT;
    }

    private int nextHeader() throws IOException {
        Frame info = (Frame) information;

        int j = source.read(b, 0, 4);

        if (j != 4) {
            return -1;
        }

        int i = b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;

        if ((i & AUDIO_SYNC_PATTERN) == AUDIO_SYNC_PATTERN && (i >>> 10 & 0x3) == ((Frame) info).orgSampleFrequency && (i >>> 17 & 0x3) == ((Frame) info).orgLayer && (i >>> 19 & 0x3) == ((Frame) info).orgVersion) {
            header = true;
            return i;
        }
        try {
            do {
                do {
                    i <<= 8;
                } while (((i |= readByte() & 0xFF) & AUDIO_SYNC_PATTERN) != AUDIO_SYNC_PATTERN);
            } while ((i >>> 10 & 0x3) != ((Frame) info).orgSampleFrequency || (i >>> 17 & 0x3) != ((Frame) info).orgLayer || (i >>> 19 & 0x3) != ((Frame) info).orgVersion);
        } catch (IOException e) {
            if (e instanceof InterruptedIOException) {
                return -1;
            } else {
                throw e;
            }
        }
        header = true;
        return i;
    }

    final int get1() {
        bitIdx--;
        int val = buf[byteIdx] >>> bitIdx & 1;

        if (bitIdx == 0) {
            bitIdx = BYTELENGTH;
            byteIdx++;
        }
        return val;
    }

    final int get(int i) {
        int val = buf[byteIdx] & BITMASK[bitIdx];

        bitIdx -= i;

        if (bitIdx <= 0) {
            bitIdx += BYTELENGTH;
            byteIdx++;
            val <<= BYTELENGTH;
            val |= buf[byteIdx] & 0xFF;
            if (bitIdx <= 0) {
                bitIdx += BYTELENGTH;
                byteIdx++;
                val <<= BYTELENGTH;
                val |= buf[byteIdx] & 0xFF;
            }
        }

        return val >>> bitIdx;
    }

    final void loadDataBlock(byte[] data, int writePos, int dataBlockSize) {
        int l = writePos + dataBlockSize;

        int k = l - data.length;

        int m = dataBlockSize - k;

        if (l <= data.length) {
            System.arraycopy(buf, byteIdx, data, writePos, dataBlockSize);
        } else {
            System.arraycopy(buf, byteIdx, data, writePos, m);
            System.arraycopy(buf, byteIdx + m, data, 0, k);
        }

        byteIdx += dataBlockSize;
    }

    private boolean load(int j) throws IOException {
        int l;
        int i = 0;

        l = source.read(buf, i, j);
        if (l == -1) {
            return false;
        }
        return true;
    }

    private byte readByte() throws IOException {
        int b;

        b = source.read();
        if (b == -1) {
            throw new InterruptedIOException("Excepted end of stream");
        }
        return (byte) b;
    }
}
