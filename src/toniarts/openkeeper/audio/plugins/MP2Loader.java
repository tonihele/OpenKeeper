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
package toniarts.openkeeper.audio.plugins;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.audio.plugins.decoder.AudioInformation;
import toniarts.openkeeper.audio.plugins.decoder.Decoder;
import toniarts.openkeeper.audio.plugins.decoder.MediaInformation;
import toniarts.openkeeper.audio.plugins.decoder.MpxReader;
import toniarts.openkeeper.audio.plugins.decoder.UnsupportedMediaException;

/**
 * Plays MPx files, not MP3s though
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MP2Loader implements AssetLoader {

    public static final String FILE_EXTENSION = "mp2";

    private boolean readStream = false;
    private AudioBuffer audioBuffer;
    private AudioStream audioStream;
    private AudioData audioData;

    private static final Logger LOGGER = Logger.getLogger(MP2Loader.class.getName());

    /**
     * Masks the real input stream to decode the MP2<br>
     * The last read repeats a bit, that is a known issue:
     * http://hub.jmonkeyengine.org/forum/topic/streaming-audio-with-audionode-plays-ending-wrongly/
     */
    private static class MPxStream extends InputStream {

        private final Decoder decoder;
        private final InputStream inputStream;

        public MPxStream(InputStream inputStream, Decoder decoder) {
            this.decoder = decoder;
            this.inputStream = inputStream;
        }

        /**
         * Reads up the audio source to len bytes of data from the decoded audio
         * stream into an array of bytes. If the argument b is
         * <code>null</code>, -1 is returned.
         *
         * @param b the buffer into which the data is read
         * @param i the start offset of the data
         * @param j the maximum number of bytes read
         * @return the total number of bytes read into, or -1 is there is no
         * more data because the end of the stream has been reached.
         * @exception IOException if an input or output error occurs
         */
        @Override
        public int read(byte b[], int i, int j) throws IOException {
            if (decoder == null) {
                return -1;
            }
            return decoder.read(b, i, j);
        }

        /**
         * Reads up to a specified maximum number of bytes of data from the
         * decoded audio stream, putting them into the given byte array. If the
         * argument b is null, -1 is returned.
         *
         * @param b the buffer into which the data is read
         * @return the total number of bytes read into the buffer, or -1 if
         * there is no more data because the end of the stream has been reached
         * @exception IOException if an input or output error occurs
         */
        @Override
        public int read(byte b[]) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }

    private void readDataChunkForBuffer(Decoder decoder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = decoder.read(buffer)) > -1) {
            baos.write(buffer, 0, length);
        }
        audioBuffer.updateData(BufferUtils.createByteBuffer(baos.toByteArray()));
    }

    private void readDataChunkForStream(InputStream inputStream, Decoder decoder) {
        audioStream.updateData(new MPxStream(inputStream, decoder), 0);
    }

    private AudioData load(InputStream inputStream, boolean stream) throws IOException {

        readStream = stream;
        if (readStream) {
            audioStream = new AudioStream();
            audioData = audioStream;
        } else {
            audioBuffer = new AudioBuffer();
            audioData = audioBuffer;
        }
        try {

            MpxReader reader = new MpxReader();
            MediaInformation info = reader.readInformation(inputStream, true);
            Decoder decoder = reader.getDecoder(inputStream, true);

            // Setup audio
            audioData.setupFormat((int) info.get(AudioInformation.I_CHANNEL_NUMBER),
                    (int) decoder.get(AudioInformation.I_SAMPLE_SIZE),
                    (int) info.get(AudioInformation.I_SAMPLE_RATE));

            // Read the file
            while (true) {
                if (readStream) {
                    readDataChunkForStream(inputStream, decoder);
                } else {
                    readDataChunkForBuffer(decoder);
                }
                return audioData;
            }
        } catch (IOException | UnsupportedMediaException ex) {
            LOGGER.log(Level.SEVERE, "Failed to read a frame!", ex);
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
        } catch (IOException e) {
            return new AudioBuffer(); // Failed
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }
}
