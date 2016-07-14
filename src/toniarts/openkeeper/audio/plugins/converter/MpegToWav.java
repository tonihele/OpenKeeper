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
package toniarts.openkeeper.audio.plugins.converter;

import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import toniarts.openkeeper.audio.plugins.decoder.AudioInformation;
import toniarts.openkeeper.audio.plugins.decoder.Decoder;
import toniarts.openkeeper.audio.plugins.decoder.MediaInformation;
import toniarts.openkeeper.audio.plugins.decoder.MpxReader;

/**
 * Crude example of converting MPx to WAV with the Doppio library
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MpegToWav {

    public static void main(String[] args) {
        ByteBuffer header = ByteBuffer.wrap(new byte[]{
            /*  0 */'R', 'I', 'F', 'F',
            /*  4 */ 0, 0, 0, 0, /* cksize */
            /*  8 */ 'W', 'A', 'V', 'E',
            /* 12 */ 'f', 'm', 't', ' ',
            /* 16 */ 16, 0, 0, 0, /* cksize */
            /* 20 */ 1, 0, /* wFormatTag = 1 (PCM) */
            /* 22 */ 2, 0, /* nChannels = 2 */
            /* 24 */ 0, 0, 0, 0, /* nSamplesPerSec */
            /* 28 */ 0, 0, 0, 0, /* nAvgBytesPerSec */
            /* 32 */ 4, 0, /* nBlockAlign = 4 */
            /* 34 */ 16, 0, /* wBitsPerSample */
            /* 36 */ 'd', 'a', 't', 'a',
            /* 40 */ 0, 0, 0, 0, /* cksize */});
        header.order(ByteOrder.LITTLE_ENDIAN);

        if (args.length < 2) {
            System.out.printf("Usage: <input.mp2> [<output.wav>]\n");
            return;
        }

        try (FileInputStream fin = new FileInputStream(args[0])) {

            MpxReader reader = new MpxReader();
            MediaInformation info = reader.readInformation(fin, true);
            Decoder decoder = reader.getDecoder(fin, true);

            try (RandomAccessFile fout = new RandomAccessFile(args[1], "rw")) {
                System.out.printf("Decoding %s into %s ...\n", args[0], args[1]);
                int outBytes = 0;

                // Read info
                int nOfCh = (int) info.get(AudioInformation.I_CHANNEL_NUMBER);
                int rate = (int) info.get(AudioInformation.I_SAMPLE_RATE);

                // Header
                header.putInt(24, rate);
                rate <<= nOfCh;
                header.putInt(28, rate);
                header.putShort(22, (short) nOfCh);
                header.putShort(32, (short) (nOfCh * 16 / 8));
                fout.write(header.array());

                // Output
                byte[] buffer = new byte[8192];
                int length;
                while ((length = decoder.read(buffer)) > -1) {
                    fout.write(buffer, 0, length);
                    outBytes += length;
                }

                // Finish of the header
                fout.seek(0);
                header.putInt(40, outBytes);
                header.putInt(4, outBytes + 36);
                fout.write(header.array());

                System.out.printf("Done.\n");
            } catch (Exception e) {
                System.out.printf("Could not open output file %s!\n%s\n", args[1], e);
            }
        } catch (Exception e) {
            System.out.printf("Could not open input file %s!\n", args[0]);
        }
    }
}
