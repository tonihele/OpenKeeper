/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.audio.plugins.coverter;

import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import toniarts.opendungeonkeeper.audio.plugins.decoder.Kjmp2;

/**
 * Crude example of converting MP2 to WAV with the KJMP2 library<br>
 * Converted from Martin J. Fiedler <martin.fiedler@gmx.net> C code with added
 * proper MONO support
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MpegToWav {

    private static final int KJMP2_MAX_FRAME_SIZE = 1440;
    private static final int MAX_BUFSIZE = 100 * KJMP2_MAX_FRAME_SIZE;
    private static final int KJMP2_SAMPLES_PER_FRAME = 1152;

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
        int rate, outBytes, bufsize, bufpos, in_offset, eof, desync;
        byte buffer[] = new byte[MAX_BUFSIZE];
        ByteBuffer samples = null;
        Kjmp2 decoder = new Kjmp2();

        if (args.length < 2) {
            System.out.printf("Usage: <input.mp2> [<output.wav>]\n");
            return;
        }

        try (FileInputStream fin = new FileInputStream(args[0])) {


            bufsize = fin.read(buffer, 0, MAX_BUFSIZE);
            in_offset = bufpos = 0;

            rate = (bufsize > 4) ? Kjmp2.kjmp2GetSampleRate(buffer) : 0;
            if (rate == 0) {
                System.out.printf("Input is not a valid MP2 audio file, exiting.\n");
                return;
            }

            try (RandomAccessFile fout = new RandomAccessFile(args[1], "rw")) {

                System.out.printf("Decoding %s into %s ...\n", args[0], args[1]);
                eof = outBytes = desync = 0;

                // Read the number of channels
                decoder.kjmp2DecodeFrame(buffer, null);
                int nOfCh = decoder.getNumberOfChannels();

                // Header
                header.putInt(24, rate);
                rate <<= nOfCh;
                header.putInt(28, rate);
                header.putShort(22, (short) nOfCh);
                fout.write(header.array());

                while (eof == 0 || (bufsize > 4)) {
                    int bytes;

                    samples = ByteBuffer.allocate(KJMP2_SAMPLES_PER_FRAME * 2 * nOfCh);
                    samples.order(ByteOrder.LITTLE_ENDIAN);

                    if (eof == 0 && (bufsize < KJMP2_MAX_FRAME_SIZE)) {
                        buffer = Arrays.copyOfRange(buffer, bufpos, bufpos + bufsize + 1);
                        buffer = Arrays.copyOf(buffer, MAX_BUFSIZE);

                        bufpos = 0;
                        in_offset += bufsize;
                        bytes = fin.read(buffer, bufsize, MAX_BUFSIZE - bufsize);
                        if (bytes > 0) {
                            bufsize += bytes;
                        } else {
                            eof = 1;
                        }
                    } else {
                        bytes = (int) decoder.kjmp2DecodeFrame(Arrays.copyOfRange(buffer, bufpos, bufpos + KJMP2_MAX_FRAME_SIZE + 1), samples.asShortBuffer());
                        if ((bytes < 4) || (bytes > KJMP2_MAX_FRAME_SIZE) || (bytes > bufsize)) {
                            if (desync == 0) {
                                System.out.printf("Stream error detected at file offset %d.\n", in_offset + bufpos);
                            }
                            desync = bytes = 1;
                        } else {
                            fout.write(samples.array());
                            outBytes += samples.capacity();
                            desync = 0;
                        }
                        bufsize -= bytes;
                        bufpos += bytes;
                    }
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
