/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.function.IntSupplier;
import toniarts.openkeeper.audio.plugins.decoder.AudioInformation;
import toniarts.openkeeper.audio.plugins.decoder.Decoder;
import toniarts.openkeeper.audio.plugins.decoder.MediaInformation;
import toniarts.openkeeper.audio.plugins.decoder.MpxReader;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/**
 * An endless stream of 16 bit little endian PCM that plays the music of a
 * {@link SoundSequencer} gaplessly. Each segment is decoded as it plays and cut
 * to its exact length, so consecutive segments join without clicks. Silence is
 * produced while the music is stopped.<br>
 * The stream is read on the audio thread, the intensity supplier must be
 * thread safe.
 *
 * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/file-formats.md#sdt-sound-archive">Why sounds are cut to their PCM length</a>
 */
public final class SequencedSoundStream extends InputStream {

    public static final int SAMPLE_RATE = 22050;
    private static final int BYTES_PER_SAMPLE = 2;
    /**
     * How long a stopped linked sequence stays silent before trying to resume
     */
    private static final int RETRY_MILLIS = 100;

    private static final Logger logger = System.getLogger(SequencedSoundStream.class.getName());

    private final AssetManager assetManager;
    private final SoundSequencer sequencer;
    private final IntSupplier intensity;
    private final int channels;

    private InputStream source;
    private Decoder decoder;
    private int sourceChannels;
    private long sourceBytesLeft;
    private boolean exactLength;
    private float volume;
    private long silenceFrames;
    private byte[] sourceBuffer = new byte[0];
    private volatile boolean closed;

    /**
     * @param assetManager asset manager to load the converted sounds from
     * @param sequencer the sequence to play
     * @param intensity current music intensity (0-100)
     * @param channels output channels, sounds are up- or down-mixed to this
     */
    public SequencedSoundStream(AssetManager assetManager, SoundSequencer sequencer, IntSupplier intensity, int channels) {
        this.assetManager = assetManager;
        this.sequencer = sequencer;
        this.intensity = intensity;
        this.channels = channels;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        return read(b, 0, 1) == -1 ? -1 : Byte.toUnsignedInt(b[0]);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            return -1;
        }
        int frameSize = channels * BYTES_PER_SAMPLE;
        int frames = len / frameSize;
        if (frames == 0) {
            return 0;
        }

        int written = 0;
        while (written < frames) {
            int wanted = frames - written;
            int got;
            if (silenceFrames > 0) {
                got = (int) Math.min(wanted, silenceFrames);
                Arrays.fill(b, off + written * frameSize, off + (written + got) * frameSize, (byte) 0);
                silenceFrames -= got;
            } else if (decoder != null) {
                got = readSource(b, off + written * frameSize, wanted);
            } else {
                startNext();
                continue;
            }
            written += got;
        }
        return written * frameSize;
    }

    /**
     * Decodes from the current sound into the output
     *
     * @return frames written, may be 0 when the sound just ended
     */
    private int readSource(byte[] b, int off, int frames) throws IOException {
        int sourceFrameSize = sourceChannels * BYTES_PER_SAMPLE;
        long framesLeft = sourceBytesLeft / sourceFrameSize;
        int toRead = (int) Math.min(frames, framesLeft);
        int bytes = toRead * sourceFrameSize;
        if (sourceBuffer.length < bytes) {
            sourceBuffer = new byte[bytes];
        }

        int read = 0;
        boolean endOfData = false;
        while (read < bytes) {
            int n = decoder.read(sourceBuffer, read, bytes - read);
            if (n < 0) {
                endOfData = true;
                break;
            }
            read += n;
        }
        if (endOfData && !exactLength) {
            toRead = read / sourceFrameSize;
            bytes = toRead * sourceFrameSize;
            sourceBytesLeft = bytes;
        } else {
            // Keep the timing even if the data ends early
            Arrays.fill(sourceBuffer, read, bytes, (byte) 0);
        }

        convert(sourceBuffer, toRead, b, off);
        sourceBytesLeft -= bytes;
        if (sourceBytesLeft < sourceFrameSize) {
            endSource();
        }
        return toRead;
    }

    private void convert(byte[] src, int frames, byte[] dst, int off) {
        for (int i = 0; i < frames; i++) {
            int left = sample(src, (i * sourceChannels) * BYTES_PER_SAMPLE);
            int right = sourceChannels > 1 ? sample(src, (i * sourceChannels + 1) * BYTES_PER_SAMPLE) : left;
            if (channels == 1) {
                putSample(dst, off + i * BYTES_PER_SAMPLE, (left + right) / 2);
            } else {
                putSample(dst, off + i * 2 * BYTES_PER_SAMPLE, left);
                putSample(dst, off + (i * 2 + 1) * BYTES_PER_SAMPLE, right);
            }
        }
    }

    private static int sample(byte[] b, int pos) {
        return (short) ((b[pos] & 0xff) | (b[pos + 1] << 8));
    }

    private void putSample(byte[] b, int pos, int value) {
        int v = Math.round(value * volume);
        v = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
        b[pos] = (byte) v;
        b[pos + 1] = (byte) (v >> 8);
    }

    private void startNext() {
        SoundEventGroup group = sequencer.next(intensity.getAsInt());
        SoundSample sample = group != null ? group.pickSample() : null;
        if (sample == null || sample.getAssetPath() == null) {
            silenceFrames = millisToFrames(RETRY_MILLIS);
            return;
        }

        volume = SoundRng.volume(group) / 100f;
        try {
            openSource(sample);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to play " + sample.getAssetPath(), e);
            endSource();
            silenceFrames = millisToFrames(1000);
        }
    }

    private void openSource(SoundSample sample) throws Exception {
        String path = AssetsConverter.SOUNDS_FOLDER + '/' + sample.getAssetPath();
        source = new BufferedInputStream(assetManager.locateAsset(new AssetKey<>(path)).openStream());
        MpxReader reader = new MpxReader();
        MediaInformation info = reader.readInformation(source, true);
        decoder = reader.getDecoder(source, true);
        sourceChannels = (int) info.get(AudioInformation.I_CHANNEL_NUMBER);
        int pcmBytes = sample.getArchive() != null ? sample.getArchive().getClipPcmBytes(sample.getClip()) : 0;
        exactLength = pcmBytes > 0;
        sourceBytesLeft = exactLength ? pcmBytes : Long.MAX_VALUE;
    }

    private void endSource() {
        if (decoder != null) {
            decoder.close();
            decoder = null;
        }
        if (source != null) {
            try {
                source.close();
            } catch (IOException e) {
                // Nothing to do
            }
            source = null;
        }
        sourceBytesLeft = 0;
    }

    private static long millisToFrames(long millis) {
        return millis * SAMPLE_RATE / 1000;
    }

    @Override
    public synchronized void close() {
        closed = true;
        endSource();
    }
}
