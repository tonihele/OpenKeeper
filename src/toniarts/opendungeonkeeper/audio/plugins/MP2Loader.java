/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.audio.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioStream;
import com.jme3.util.BufferUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.opendungeonkeeper.audio.plugins.decoder.Kjmp2;
import toniarts.opendungeonkeeper.tools.convert.Utils;

/**
 * Plays MP2 files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MP2Loader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(MP2Loader.class.getName());
    private static final int KJMP2_MAX_FRAME_SIZE = 1440;
    private static final int MAX_BUFSIZE = 1000 * KJMP2_MAX_FRAME_SIZE;
    private static final int KJMP2_SAMPLES_PER_FRAME = 1152;
    private boolean readStream = false;
    private AudioBuffer audioBuffer;
    private AudioStream audioStream;
    private AudioData audioData;

    /**
     * Masks the real input stream to decode the MP2<br>
     * FIXME: Somehow the last read repeats a bit, even if I fill the rest of
     * buffer with zeroes or synchronize, dunno
     */
    private class MP2Stream extends InputStream {

        private final InputStream is;
        private int bufSize;
        private byte[] buffer;
        private final Kjmp2 decoder;
        private boolean eof = false;
        private int bufpos = 0;
        private int inOffset = 0;
        private boolean desync = false;
        private final ByteBuffer samples;

        public MP2Stream(InputStream is, int bufSize, byte[] buffer, int numberOfChannels, Kjmp2 decoder) {
            this.is = is;
            this.bufSize = bufSize;
            this.buffer = buffer;
            this.decoder = decoder;
            samples = ByteBuffer.allocate(KJMP2_SAMPLES_PER_FRAME * 2 * numberOfChannels);
            samples.order(ByteOrder.LITTLE_ENDIAN);
            samples.position(samples.limit());
        }

        @Override
        public int read() throws IOException {

            if (samples.position() == samples.limit() && (!eof || bufSize > 4)) {
                int bytes;

                if (!eof && (bufSize < KJMP2_MAX_FRAME_SIZE)) {
                    buffer = Arrays.copyOfRange(buffer, bufpos, bufpos + bufSize + 1);
                    buffer = Arrays.copyOf(buffer, MAX_BUFSIZE);

                    bufpos = 0;
                    inOffset += bufSize;
                    bytes = is.read(buffer, bufSize, MAX_BUFSIZE - bufSize);
                    if (bytes > 0) {
                        bufSize += bytes;
                    } else {
                        eof = true;
                    }
                }

                if (bufSize > 4) {

                    // Rewind and gather more
                    samples.clear();

                    bytes = (int) decoder.kjmp2DecodeFrame(Arrays.copyOfRange(buffer, bufpos, bufpos + KJMP2_MAX_FRAME_SIZE + 1), samples.asShortBuffer());
                    if ((bytes < 4) || (bytes > KJMP2_MAX_FRAME_SIZE) || (bytes > bufSize)) {
                        if (desync) {
                            logger.log(Level.WARNING, "Stream error detected at file offset {0}{1}!", new Object[]{inOffset, bufpos});
                        }
                        desync = true;
                        bytes = 1;
                    } else {
                        desync = false;
                    }
                    bufSize -= bytes;
                    bufpos += bytes;
                }
            }

            // Check exit
            if (samples.position() != samples.limit()) {
                return Utils.toUnsignedByte(samples.get());
            } else {
                return -1;
            }
        }

        @Override
        public void close() throws IOException {
            is.close();
        }
    }

    private void readDataChunkForBuffer(InputStream is, int bufSize, byte[] buffer, int numberOfChannels, Kjmp2 decoder) throws IOException {

        boolean eof = false;
        int bufpos = 0;
        int inOffset = 0;
        boolean desync = false;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (!eof || (bufSize > 4)) {
            int bytes;

            // Output
            ByteBuffer samples = ByteBuffer.allocate(KJMP2_SAMPLES_PER_FRAME * 2 * numberOfChannels);
            samples.order(ByteOrder.LITTLE_ENDIAN);

            if (!eof && (bufSize < KJMP2_MAX_FRAME_SIZE)) {
                buffer = Arrays.copyOfRange(buffer, bufpos, bufpos + bufSize + 1);
                buffer = Arrays.copyOf(buffer, MAX_BUFSIZE);

                bufpos = 0;
                inOffset += bufSize;
                bytes = is.read(buffer, bufSize, MAX_BUFSIZE - bufSize);
                if (bytes > 0) {
                    bufSize += bytes;
                } else {
                    eof = true;
                }
            } else {
                bytes = (int) decoder.kjmp2DecodeFrame(Arrays.copyOfRange(buffer, bufpos, bufpos + KJMP2_MAX_FRAME_SIZE + 1), samples.asShortBuffer());
                if ((bytes < 4) || (bytes > KJMP2_MAX_FRAME_SIZE) || (bytes > bufSize)) {
                    if (desync) {
                        logger.log(Level.WARNING, "Stream error detected at file offset {0}{1}!", new Object[]{inOffset, bufpos});
                    }
                    desync = true;
                    bytes = 1;
                } else {
                    baos.write(samples.array());
                    desync = false;
                }
                bufSize -= bytes;
                bufpos += bytes;
            }
        }
        audioBuffer.updateData(BufferUtils.createByteBuffer(baos.toByteArray()));
    }

    private void readDataChunkForStream(InputStream is, int bufSize, byte[] buffer, int numberOfChannels, Kjmp2 kjmp2) throws IOException {
        audioStream.updateData(new MP2Stream(is, bufSize, buffer, numberOfChannels, kjmp2), 0);
    }

    private AudioData load(InputStream inputStream, boolean stream) throws IOException {

        // Load the MPx file
        byte buffer[] = new byte[KJMP2_MAX_FRAME_SIZE * 100];

        readStream = stream;
        if (readStream) {
            audioStream = new AudioStream();
            audioData = audioStream;
        } else {
            audioBuffer = new AudioBuffer();
            audioData = audioBuffer;
        }
        try {

            // Create new KJMP2 instance
            Kjmp2 kjmp2 = new Kjmp2();

            // Read a frame
            int bufSize = inputStream.read(buffer);
            int frameSize = kjmp2.kjmp2DecodeFrame(buffer, null);
            if (frameSize == 0) {

                // Invalid header
                throw new IOException("Not a valid MP2 file!");
            }

            // Setup audio
            int numberOfChannels = kjmp2.getNumberOfChannels();
            audioData.setupFormat(numberOfChannels, 16, kjmp2.kjmp2GetSampleRate());

            // Read the file
            while (true) {
                if (readStream) {
                    readDataChunkForStream(inputStream, bufSize, buffer, numberOfChannels, kjmp2);
                } else {
                    readDataChunkForBuffer(inputStream, bufSize, buffer, numberOfChannels, kjmp2);
                }
                return audioData;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new IOException("Failed to read a frame!");
        }
    }

    @Override
    public Object load(AssetInfo info) throws IOException {
        AudioData data;
        InputStream inputStream = null;
        try {
            inputStream = info.openStream();
            data = load(inputStream, ((AudioKey) info.getKey()).isStream());
            if (data instanceof AudioStream) {
                inputStream = null;
            }
            return data;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
