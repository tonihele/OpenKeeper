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

import toniarts.openkeeper.audio.plugins.decoder.tag.Tag;

/**
 * The
 * <code>Frame</code> class acts as an information container to describe the
 * content of an MPEG audio framet Note that MPEG4 audio format specific
 * informations are excluded, because it needs an codec-system of its own. The
 * informations stored inside an instanced object of this class are only
 * senseful for an MPEG1, MPEG2 or MPEG2.5 audio format.
 *
 * @author Michael Scheerer
 */
public abstract class Frame extends AudioInformation /*extends FormatReader*/ {

    final static int RESERVED = -1;
    final static int MAX_BITRATE = 641000;
    final static int STEREO = 0x0,
            JOINT_STEREO = 0x1,
            DUAL_CHANNEL = 0x2,
            SINGLE_CHANNEL = 0x3;
    final static int FREQUENCY[][] = {
        {
            22050, 24000, 16000, RESERVED
        }, {
            44100, 48000, 32000, RESERVED
        }, {
            11025, 12000, 8000, RESERVED
        }
    };
    final static int RATE[][][] = {
        {
            {
                RESERVED, 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 176000, 192000, 224000, 256000, RESERVED
            }, {
                RESERVED, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, RESERVED
            }, {
                RESERVED, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, RESERVED
            }
        }, {
            {
                RESERVED, 32000, 64000, 96000, 128000, 160000, 192000, 224000, 256000, 288000, 320000, 352000, 384000, 416000, 448000, RESERVED
            }, {
                RESERVED, 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 224000, 256000, 320000, 384000, RESERVED
            }, {
                RESERVED, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 224000, 256000, 320000, RESERVED
            }
        }, {
            {
                RESERVED, 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 176000, 192000, 224000, 256000, RESERVED
            }, {
                RESERVED, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, RESERVED
            }, {
                RESERVED, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, RESERVED
            }
        }
    };
    final static int AUDIO_SYNC_PATTERN = 0xFFE00000; // MPEG 2.5 compliant
    /**
     * Global mpeg2 flags, used in mpeg audio/video/system
     */
    public boolean audio = true, video, system;
    int layer;
    int version;
    int orgVersion;
    int orgLayer;
    int orgSampleFrequency;
    short checksum;
    int dataBlockSize;
    int mode;
    int modeExtension;
    int paddingBit;
    boolean copyright;
    boolean original;
    int emphasis;
    boolean xing;
    boolean vbr;
    boolean info;
    boolean lame;
    boolean freeMode;
    boolean freeModeInitialized;
    int protectionBit;
    double toc[];
    int bitrateIndex;
    int frequencyIndex;
    int headerlessFrameSize;
    CRC16 crc;
    int numberOfSubbands;
    int channels;
    int framesMinusOne;
    long netByteLength;
    long microseconds;
    double timePerFrame;
    double framesPerTime;
    int startFrameCount;
    int tableGroup;
    Tag contentInfo;
    long[] positionOfFrame;
    int frameDelayStart;
    int frameDelayEnd;
    int delayStart;
    int delayEnd;
    private int bitRate;
    private int lastBitRate;
    private int lastDataBlockSize;

    /**
     * Returns the layer of a MPEG audio source.
     *
     * @return layer the layer of a MPEG audio source
     */
    protected final int getLayer() {
        return layer;
    }

    final int getFrequency() {
        return FREQUENCY[version][frequencyIndex];
    }

    final int getVersion() {
        return version;
    }

    final int getBitrate() {
        if (freeMode) {
            return bitRate;
        }
        return RATE[version][layer - 1][bitrateIndex];
    }

    final int calculateFrameSize() {
        if (bitrateIndex == 0) {
            freeMode = true;
            if (freeModeInitialized) {
                return calculateFrameSize(bitRate);
            }
            return headerlessFrameSize;
        }
        return calculateFrameSize(RATE[version][layer - 1][bitrateIndex]);
    }

    private int calculateFrameSize(int bitrate) {
        if (layer == 1) {
            headerlessFrameSize = 12 * bitrate / FREQUENCY[version][frequencyIndex] + paddingBit;
            headerlessFrameSize <<= 2;
        } else {
            if (version == 1 || layer == 2) {
                headerlessFrameSize = 144 * bitrate / FREQUENCY[version][frequencyIndex] + paddingBit;
            } else {
                headerlessFrameSize = 72 * bitrate / FREQUENCY[version][frequencyIndex] + paddingBit;
            }
        }
        headerlessFrameSize -= 4; // minus header
        if (layer == 3) {
            if (version == 1) {
                dataBlockSize = headerlessFrameSize - (mode != 3 ? 32 : 17) - (protectionBit == 0 ? 2 : 0);
            } else {
                dataBlockSize = headerlessFrameSize - (mode != 3 ? 17 : 9) - (protectionBit == 0 ? 2 : 0);
            }
        } else {
            dataBlockSize = headerlessFrameSize - (protectionBit == 0 ? 2 : 0);
        }
        return headerlessFrameSize;
    }

    final void determineBitrate() {
        int headerlessFrameSizeCurrent;

        for (bitRate = 0; bitRate < MAX_BITRATE; bitRate += 1000) {
            if (layer == 1) {
                headerlessFrameSizeCurrent = 12 * bitRate / FREQUENCY[version][frequencyIndex] + paddingBit;
                headerlessFrameSizeCurrent <<= 2;
            } else {
                if (version == 1 || layer == 2) {
                    headerlessFrameSizeCurrent = 144 * bitRate / FREQUENCY[version][frequencyIndex] + paddingBit;
                } else {
                    headerlessFrameSizeCurrent = 72 * bitRate / FREQUENCY[version][frequencyIndex] + paddingBit;
                }
            }
            headerlessFrameSizeCurrent -= 4; // minus header
            if (layer == 3) {
                if (version == 1) {
                    dataBlockSize = headerlessFrameSizeCurrent - (mode != 3 ? 32 : 17) - (protectionBit == 0 ? 2 : 0);
                } else {
                    dataBlockSize = headerlessFrameSizeCurrent - (mode != 3 ? 17 : 9) - (protectionBit == 0 ? 2 : 0);
                }
            } else {
                dataBlockSize = headerlessFrameSizeCurrent - (protectionBit == 0 ? 2 : 0);
            }
            if (headerlessFrameSizeCurrent >= headerlessFrameSize) {
                bitRate = lastBitRate;
                dataBlockSize = lastDataBlockSize;
                return;
            }
            lastBitRate = bitRate;
            lastDataBlockSize = dataBlockSize;
        }
    }

    final void setChecksum(short b) {
        checksum = b;
    }

    final boolean isProtected() {
        return protectionBit == 0 && crc != null;
    }

    final CRC16 getCrc() {
        return crc;
    }

    final boolean checksumOk() {
        if (crc != null && protectionBit == 0) {
            boolean b = checksum == (short) crc.getValue();

            crc.reset();
            return b;
        }
        return true;
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
//        super.close();
        crc = null;
        positionOfFrame = null;
        toc = null;
    }
}
