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
import java.lang.reflect.*;
import static toniarts.openkeeper.audio.plugins.decoder.AudioInformation.B_BIG_ENDIAN;

/**
 * The
 * <code>Output</code> class implements all output related methods of the
 * decoder. Furthermore all output channel and equalizing related settings are
 * performed
 *
 * @author Michael Scheerer
 */
abstract class Output extends Initializer {

    final static int OBUFFERSIZE = 2304;
    final static int SBLIMIT = 32;
    final static int SHIFT_LEVEL = 4;
    private final static float[] ZERO_FLOATS = new float[SBLIMIT];
    final static int STANDARD = 0,
            LEFT_CHANNEL = 1,
            RIGHT_CHANNEL = 2;
    private final static float STANDARD_ARRAY[] = {
        1, 1
    };
    private final static float MONO_ARRAY[] = {
        1
    };
    final static int STEREO = 0x0,
            JOINT_STEREO = 0x1,
            DUAL_CHANNEL = 0x2,
            SINGLE_CHANNEL = 0x3;
    int whichChannels;
    int channels;
    int occupiedNumSubbands;
    int numSubbands;
    int flush;
    boolean layer1;
    int frameCount;
    int startFrameCount;
    int framesMinusOne;
    Frame info;
    float[][] subbandValues;
    float[][] frameBufferValues;
    int[] pointer;
    int counter;
    private float[] frameBufferZeroValues;
    private byte[] buffer, zeroBuffer;
    private int obuffersize;
    private int frameBufferAnalyzerSize;
    private int readPos, size;
    private int flushCount = 0;
    private boolean frequencyMode;
    private boolean switched;
    private boolean bigEndian;

    /**
     * Constructs an instance of
     * <code>Output</code>. The
     * <code>Output</code> class acts also as a
     * <code>Control</code> object containing presettings.
     *
     * @param info the <code>Frame</code> object containing the neccesary
     * informations about the source
     * @param in the input stream
     */
    Output(Frame info, InputStream in) {
        super(info, in);

        this.info = (Frame) info;

        Object a = get(S_AUDIO_DATA_ANALYZE_MODE);

        if (((String) a).equals(FREQUENCY)) {
            info.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, FREQUENCY);
            frequencyMode = true;
        }
        if (((String) a).equals(TIME)) {
            info.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, TIME);
            frequencyMode = false;
        }

        Object o = get(FA_CHANNEL_MAPPING);
        int length = 0;

        if (((Frame) info).layer == 1) {
            obuffersize = OBUFFERSIZE / 3;
            layer1 = true;
        } else if (((Frame) info).layer == 3 && ((Frame) info).version != 1) {
            obuffersize = OBUFFERSIZE >> 1;
        } else {
            obuffersize = OBUFFERSIZE;
        }

        try {
            length = Array.getLength(o);
        } catch (Exception e) {
        }
        if (length > 0) {
            setChannels(o);
        } else {
            if (outputChannels == 1) {
                setChannels(MONO_ARRAY);
            } else {
                setChannels(STANDARD_ARRAY);
            }
        }
        frameBufferAnalyzerSize = obuffersize >>> SHIFT_LEVEL;

        frameBufferValues = new float[outputChannels][frameBufferAnalyzerSize];
        frameBufferZeroValues = new float[frameBufferAnalyzerSize];
        subbandValues = new float[outputChannels][SBLIMIT];

        pointer = new int[outputChannels];
        resetBufferPointer();

        occupiedNumSubbands = ((Frame) info).numberOfSubbands;

        bigEndian = (Boolean) get(B_BIG_ENDIAN);

        // low pass filter for e. g. 8khz Java 1 audio device
        setNumberOfOccupiedSubbands();
    }

    private void setNumberOfOccupiedSubbands() {
        Frame i = (Frame) information;

        int frequency = ((Integer) i.get(AudioInformation.I_SAMPLE_RATE)).intValue();

        int deviceFrequencyLimit = ((Integer) info.get(AudioInformation.I_DEVICE_SAMPLE_RATE)).intValue();

        if (deviceFrequencyLimit >= frequency) {
            return;
        }

        float buff = deviceFrequencyLimit * SBLIMIT / (float) frequency;

        int subn = Math.round(buff);

        subn = (subn / 2) * 2;

        occupiedNumSubbands = subn;

        if (i.getLayer() == 3) {
            occupiedNumSubbands--;
        }
    }

    private void setChannels(Object value) {
        Frame i = (Frame) information;

        channels = i.channels;

        if (Array.getLength(value) == 0) {
            information.put(FA_CHANNEL_MAPPING, value);
            return;
        }
        if (i.mode == DUAL_CHANNEL && (Array.getFloat(value, 0) == 1) && (Array.getFloat(value, 1) == 0)) {
            whichChannels = LEFT_CHANNEL;
            channels = 2;
        } else if (i.mode == DUAL_CHANNEL && (Array.getFloat(value, 1) == 0) && (Array.getFloat(value, 0) == 1)) {
            whichChannels = RIGHT_CHANNEL;
            channels = 2;
        } else {
            whichChannels = STANDARD;
            if (outputChannels == 1) {
                value = MONO_ARRAY;
            } else {
                value = STANDARD_ARRAY;
            }
        }
        information.put(FA_CHANNEL_MAPPING, value);
    }

    /**
     * Maps the specified key to the specified value in this control. Neither
     * the key nor the value can be
     * <code>null</code>. The value can be obtained by calling the get method
     * with a key that is equal to the original key. The specific value is then
     * used to set decoder or audio device related properties. This methos acts
     * as the main control (mcc = management configuration control) of these
     * audio plugin.
     *
     * @param key the hashtable key
     * @param value the value
     * @return the previous value of the specified key in this hashtable, * * *
     * or <code>null</code> if it did not have one
     * @exception NullPointerException if the key or value is <code>null</code>
     */
    @Override
    public Object put(Object key, Object value) throws NullPointerException {
        if (key.equals(S_AUDIO_DATA_ANALYZE_MODE)) {
            if (((String) value).equals(FREQUENCY)) {
                information.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, FREQUENCY);
                frequencyMode = true;
            }
            if (((String) value).equals(TIME)) {
                information.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, TIME);
                frequencyMode = false;
            }
            switched = true;
        }
        if (key.equals(FA_EQUALIZE)) {
            setEqualizer(value);
        } else if (key.equals(FA_CHANNEL_MAPPING)) {
            flush = BitrateVariation.FLUSH_RANGE_FOR_START;
            setChannels(value);
        }
        return super.put(key, value);
    }

    /**
     * Skips a decoding of one audio frame.
     *
     * @return -1 if an IOException occurs, frame length otherwise
     */
    abstract int skipFrame() throws IOException;

    /**
     * Reads up the audio source to len bytes of data from the decoded audio
     * stream into an array of bytes. If the argument b is
     * <code>null</code>, -1 is returned. The audio decoder must implement this
     * method in the way, that this method reads raw, unsigned, 16 bit,
     * big-endian, pcm audio data. It is very important to mention, that the
     * most left byte of a pcm audio sample, which is currently always 16 bit
     * sized, must be the first written byte of the framework typical byte array
     * representation of audio samples. This framework may be further developed
     * to support additional pcm output formats. The method includes a patch to
     * avoid scratch noises during looping or seeking. So the mp3 design bug
     * (often called byteresevoire technique) can't be heared any more. These
     * noises can't be produced if playing mp1/mp2.
     *
     * @param b the buffer into which the data is read
     * @param i the start offset of the data
     * @param j the maximum number of bytes read
     * @return the total number of bytes read into, or -1 is there is no more
     * data because the end of the stream has been reached
     * @exception IOException if an input or output error occurs
     * @exception InterruptedIOException if the decoding process is interrupted
     * caused from malformed media data
     */
    @Override
    public final int read(byte b[], int i, int j) throws IOException {
        int diff = 0;
        int paddingEnd = 0;
        int length = j;
        int writePos = i;
        int l1;
        int l2;

        if (buffer == null) {
            buffer = new byte[obuffersize * outputChannels];
            zeroBuffer = new byte[buffer.length];
            flush = 0;
        }
        if (framesMinusOne > 0 && info.lame) {
            l1 = info.frameDelayStart - frameCount;
            l2 = frameCount + info.frameDelayEnd - framesMinusOne;

            if (l1 >= 0) {
                if (readPos == 0) {
                    readPos = info.delayStart << 1;
                }
            } else if (l2 >= 0) {
                paddingEnd = info.delayEnd << 1;
            }
        }
        while (length - (diff = size - paddingEnd - readPos) >= 0) {
            try {
                if (framesMinusOne > 0 && info.lame) {
                    l1 = info.frameDelayStart - frameCount;
                    l2 = frameCount + info.frameDelayEnd - framesMinusOne;
                    int l3 = 0;

                    if (l1 >= 0 || l2 >= 0) {
                        if (l1 >= 0) {
                            l3 = l1;
                            if (readPos == 0) {
                                readPos = info.delayStart << 1;
                            }
                        } else if (l2 >= 0) {
                            if (l2 > 0) {
                                l3 = info.frameDelayEnd;
                            }
                            paddingEnd = info.delayEnd << 1;
                        }
                        diff = size - paddingEnd - readPos;
                        for (int k = 0; k < l3; k++) {
                            resetBufferPointer();
                            if (decodeFrame(Events.VALIDATION_EVENT) == Events.EOM_EVENT) {
                                return -1;
                            }
                        }
                    }
                }
                if (flush == 0) {
                    System.arraycopy(buffer, readPos, b, writePos, diff);
                } else {
                    System.arraycopy(zeroBuffer, readPos, b, writePos, diff);
                }
                writePos += diff;
                resetBufferPointer();
                if (decodeFrame(Events.VALIDATION_EVENT) == Events.EOM_EVENT) {
                    return -1;
                }
            } catch (NullPointerException e) {
                throw new InterruptedIOException(e.getMessage());
            }
            size = pointer[0];
            if (size == 0) {
                paddingEnd = 0;
            }
            length -= diff;
        }
        if (flush == 0) {
            System.arraycopy(buffer, readPos, b, writePos, length);
        } else { // avoid noises (not mp1/mp2) (bugfix of mp3 design bug (byteresevoir).
            System.arraycopy(zeroBuffer, readPos, b, writePos, length);
            flush();
        }
        readPos += length;
        return j;
    }

    /**
     * Reads up the audio source to len bytes of data from the decoded audio
     * stream into an array of bytes. If the argument b is
     * <code>null</code>, -1 is returned. The audio decoder must implement this
     * method in the way, that this method reads raw, unsigned, 16 bit,
     * big-endian, pcm audio data. This framework may be further developed to
     * support additional PCM output formats.
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is
     * no more data because the end of the stream has been reached
     * @exception IOException if an input or output error occurs
     * @exception InterruptedIOException if the decoding process is interrupted
     * caused by malformed media data
     */
    @Override
    public final int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        super.close();
        int i;

        if (subbandValues != null) {
            for (i = 0; i < subbandValues.length; i++) {
                subbandValues[i] = null;
            }
        }
        subbandValues = null;
        if (frameBufferValues != null) {
            for (i = 0; i < frameBufferValues.length; i++) {
                frameBufferValues[i] = null;
            }
        }
        frameBufferValues = null;
        frameBufferZeroValues = null;
        buffer = null;
        zeroBuffer = null;
        pointer = null;
    }

    /**
     * Updates audio analyzing data or analyze mode dependent keywords, if an
     * analysing mode switch have to be induced.
     *
     * @return the float array containing all values of the analyze data values
     * in the range of -1 to 1 or analyze mode dependent keywords in the form of
     * a <code>String</code>, if an analysing mode switch have to be induced
     */
    @Override
    public final Object updateAnalyzerView() {

        if (subbandValues == null) {
            return new float[occupiedNumSubbands];
        }

        if (switched) {
            if (frequencyMode) {
                switched = false;
                counter = 0;
                return FREQUENCY;
            } else {
                switched = false;
                counter = 0;
                return TIME;
            }
        }

        float[] result = null;

        int i, j, k = 0;

        int size;

        if (frequencyMode) {
            size = SBLIMIT * outputChannels;

            result = new float[size];

            for (i = 0; i < SBLIMIT; i++) {
                for (j = 0; j < outputChannels; j++) {
                    result[k++] = subbandValues[j][i];
                    subbandValues[j][i] = 0;
                }
            }
        } else {
            size = frameBufferAnalyzerSize * outputChannels;

            result = new float[size];

            for (i = 0; i < frameBufferAnalyzerSize; i++) {
                for (j = 0; j < outputChannels; j++) {
                    result[k++] = frameBufferValues[j][i];
                    frameBufferValues[j][i] = 0;
                }
            }
        }

        counter = 0;

        return result;
    }

    private void resetBufferPointer() {
        counter = 0;
        readPos = 0;

        for (int i = 0; i < outputChannels; i++) {
            pointer[i] = 2 * i;
        }
    }

    void reset() {
        resetBufferPointer();
        System.arraycopy(zeroBuffer, 0, buffer, 0, obuffersize * outputChannels);

        for (int i = 0; i < outputChannels; i++) {
            System.arraycopy(ZERO_FLOATS, 0, subbandValues[i], 0, SBLIMIT);
            System.arraycopy(frameBufferZeroValues, 0, frameBufferValues[i], 0, frameBufferAnalyzerSize);
        }
    }

    final void setBuffer(double f, double s, int i, int channelNumber) {
        if (f > 1) {
            f = 1;
        }
        if (f < -1) {
            f = -1;
        }

        if (s > 1) {
            s = 1;
        }
        if (s < -1) {
            s = -1;
        }

        if (frequencyMode) {
            subbandValues[channelNumber][i] += s;
        } else {
            int index = counter >>> (SHIFT_LEVEL - 2 + outputChannels);

            if (frameBufferValues[channelNumber].length <= index) {
                index--;
            }
            frameBufferValues[channelNumber][index] = (float) f;
        }

        f *= 32767;

        short w = (short) f;
        // one short with little-endian order to 2 bytes in big-endian order!!

        byte b1;
        byte b2;
        if (bigEndian) {
            b1 = (byte) (w >>> 8);
            b2 = (byte) w;
        } else {
            b1 = (byte) w;
            b2 = (byte) (w >>> 8);
        }

        int p = pointer[channelNumber];

        buffer[p] = b1;
        buffer[p + 1] = b2;

        p += outputChannels << 1;
        pointer[channelNumber] = p;

        counter++;
    }

    private void flush() {
        flushCount++;

        if (flush == flushCount) {
            flushCount = 0;
            flush = 0;
        }
    }
}
