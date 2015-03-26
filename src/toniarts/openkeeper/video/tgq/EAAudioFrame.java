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
 * EA audio frame holder<br>
 * References: FFMPEG, adpcm.c
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EAAudioFrame {

    private static int EA_ADPCM_TABLE[] = {
        0, 240, 460, 392,
        0, 0, -208, -220,
        0, 1, 3, 4,
        7, 8, 10, 11,
        0, -1, -3, -4
    };
    private final EAAudioHeader header;
    private int numberOfSamples;
    private int codedSamples;
    private ByteBuffer pcm;

    public EAAudioFrame(EAAudioHeader header, byte[] data) {
        this.header = header;

        // Proceed to decode the frame so that we have only PCM
        decodeFrame(data);
    }

    private void decodeFrame(byte[] data) {
        if (header.getCompression() == EAAudioHeader.Compression.EA_XA_ADPCM) {

            // Always 2 channels!
            if (header.getNumberOfChannels() != 2) {
                throw new RuntimeException(header.getCompression() + " required 2 channels!");
            }

            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // 12 bytes header
            codedSamples = buf.getInt();
            codedSamples -= codedSamples % 28;
            numberOfSamples = (data.length - 12) / 30 * 28;
            short currentLeftSample = buf.getShort();
            short previousLeftSample = buf.getShort();
            short currentRightSample = buf.getShort();
            short previousRightSample = buf.getShort();

            // Output
            pcm = ByteBuffer.allocateDirect(codedSamples * 4);
            pcm.order(ByteOrder.LITTLE_ENDIAN); // JME takes PCM_LE

            // Decoding
            for (int count1 = 0; count1 < codedSamples / 28; count1++) {
                int b = Utils.toUnsignedByte(buf.get());
                int coeff1l = EA_ADPCM_TABLE[ b >> 4];
                int coeff2l = EA_ADPCM_TABLE[(b >> 4) + 4];
                int coeff1r = EA_ADPCM_TABLE[ b & 0x0F];
                int coeff2r = EA_ADPCM_TABLE[(b & 0x0F) + 4];

                b = Utils.toUnsignedByte(buf.get());
                int shiftLeft = 20 - (b >> 4);
                int shiftRight = 20 - (b & 0x0F);

                for (int count2 = 0; count2 < 28; count2++) {
                    b = Utils.toUnsignedByte(buf.get());
                    int nextLeftSample = signExtend(b >> 4, 4) << shiftLeft;
                    int nextRightSample = signExtend(b, 4) << shiftRight;

                    nextLeftSample = (nextLeftSample
                            + (currentLeftSample * coeff1l)
                            + (previousLeftSample * coeff2l) + 0x80) >> 8;
                    nextRightSample = (nextRightSample
                            + (currentRightSample * coeff1r)
                            + (previousRightSample * coeff2r) + 0x80) >> 8;

                    previousLeftSample = currentLeftSample;
                    currentLeftSample = Integer.valueOf(nextLeftSample).shortValue();
                    previousRightSample = currentRightSample;
                    currentRightSample = Integer.valueOf(nextRightSample).shortValue();
                    pcm.putShort(currentLeftSample);
                    pcm.putShort(currentRightSample);
                }
            }
            pcm.rewind();
        } else {
            throw new RuntimeException("Compression not supported! Can't decode the audio frame!");
        }
    }

    /**
     * http://stackoverflow.com/questions/29265800/sign-extension-bit-shifting-in-java-help-understanding-a-c-code-bit
     *
     * @param val value
     * @param bits bits to shift
     * @return
     */
    public static int signExtend(int val, int bits) {
        int shift = 32 - bits;
        int s = val << shift;
        return s >> shift;
    }

    public EAAudioHeader getHeader() {
        return header;
    }

    /**
     * PCM little endian bytebuffer
     *
     * @return PCM audio
     */
    public ByteBuffer getPcm() {
        return pcm;
    }
}
