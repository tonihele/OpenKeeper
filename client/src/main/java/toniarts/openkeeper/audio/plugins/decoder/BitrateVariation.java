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
 * <code>BitrateVariation</code> class provides all necessary VBR related
 * methods, which can be used by other classes outside this package.
 *
 * @author Michael Scheerer
 */
abstract class BitrateVariation extends Output {

    final static int FLUSH_RANGE = 4;
    final static int FLUSH_RANGE_FOR_START = 2;
    int headerlessFrameSize;
    private long[] positionOfFrame;
    private long position;
    private final long byteLength;
    private int limiter;
    private boolean completeCached;
    private boolean fastSeek;
    private int bitRate;

    /**
     * Constructs an instance of
     * <code>BitrateVariation</code> with a
     * <code>Frame</code> object managing all necessary objects needed for the
     * global decoder system.
     *
     * @param info the <code>Frame</code> object containing the neccesary
     * informations about the source
     * @param in the input stream
     */
    BitrateVariation(Frame info, InputStream in) {
        super(info, in);

        fastSeek = ((Boolean) get(B_FAST_SEEKING));
        info.put(B_FAST_SEEKING, fastSeek);

        frameCount = startFrameCount = info.startFrameCount;
        framesMinusOne = info.framesMinusOne;

        byteLength = ((Long) info.get(AudioInformation.L_GROSS_BYTE_LENGTH));

        bitRate = ((Integer) info.get(AudioInformation.I_BITRATE));

        if (!info.vbr && !info.xing || fastSeek) {
            framesMinusOne = 0;
        }
        if (framesMinusOne > 0) {
            positionOfFrame = info.positionOfFrame;
            frameCount = info.startFrameCount;
            limiter = frameCount;
            position = positionOfFrame[frameCount];
        }
    }

    abstract int readFrame() throws IOException;

    abstract void decode();

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
     * @return the previous value of the specified key in this hashtable, * *      * or <code>null</code> if it did not have one
     * @exception NullPointerException if the key or value is <code>null</code>
     */
    @Override
    public final Object put(String key, Object value) throws NullPointerException {
        if (key.equals(B_FAST_SEEKING)) {
            fastSeek = ((Boolean) value);

            if (!((MpxReader) information).vbr && !((MpxReader) information).xing || fastSeek) {
                framesMinusOne = 0;
            }
            if (framesMinusOne > 0) {
                positionOfFrame = ((MpxReader) information).positionOfFrame;
                frameCount = ((MpxReader) information).startFrameCount;
                limiter = frameCount;
                position = positionOfFrame[frameCount];
            }
            information.put(B_FAST_SEEKING, value);
        }
        return super.put(key, value);
    }

    /**
     * Decodes an audio frame and returns an event id number representing three
     * of the
     * <code>org.ljmf.Events</code>. Following events have to be returned:
     * EOM_EVENT in case of an error or end of media, SKIP_EVENT in case of
     * dropping a frame. Any overwritten
     * <code>decodeFrame()</code> method must perform a
     * <code>super</code> call to ensure an analyzer view update.
     *
     * @param an event id number as an integer value send from an overwritten
     * method
     * @return an event id number as an integer value
     * @exception IOException if an I/O error occurs
     * @exception InterruptedIOException if the decoding process is interrupted
     * caused by malformed media data
     */
    @Override
    public final int decodeFrame(int eventId) throws IOException {
        int l;

        l = readFrame();
        if (l == Events.EOM_EVENT) {
            return Events.EOM_EVENT;
        }

        // 1.
        // If single frames with free format ocures, skip these frames.
        // 2.
        // Free format is not allowed in single frames. This would be equal to VBR.
        // Some test files are switching the channel numbers!
        if (!(info.dataBlockSize <= 0 && !info.freeMode || info.mode == SINGLE_CHANNEL && channels > 1 || info.mode != SINGLE_CHANNEL && channels == 1)) {
            decode();
        }
        if (framesMinusOne > 0) {
            frameCount++;
            if (frameCount > framesMinusOne) {
                frameCount = 0;
                completeCached = true;
            }
            if (frameCount > limiter && !completeCached) {
                position += headerlessFrameSize + 4;
                positionOfFrame[frameCount] = position;
                limiter = frameCount;
            }
        }
        return super.decodeFrame(l);
    }

    /**
     * If necessary excludes the tag data and/or skipped data length from the
     * byte position or calculates a variable bitrate correction to determine
     * the actual playtime or seek position of the media. Normally the playtime
     * position calculation is different from the one of the seek position.
     *
     * @param seeking seeking flag to distinguish the different calculation of
     * the playtime and seek position
     * @param position the current byte position
     * @return the corrected byte position
     */
    @Override
    public final long correctedBytePosition(long position, boolean seeking) {
        if (framesMinusOne > 0) {
            if (seeking) {
                return ((MpxReader) information).correctedBytePosition(position, true);
            }
            return (long) Math.round(frameCount * byteLength / (double) framesMinusOne);
        }

        position = ((MpxReader) information).correctedBytePosition(position, seeking);

        if (position < 0) {
            position = 0;
        }
        return position;
    }

    /**
     * Performs a seek operation and in consequents of this possible buffer
     * flush operations. There are two models of seeking media data:<br>
     * Passive and active seeking.
     * <p>
     * The first model is used for example in case of constant bitrating for
     * getting a better performance. It delegates the seeking operation to the
     * framework. In this case, it has to return a decoder buffer corrected byte
     * position parameter derived from the incoming byte position parameter
     * <code>position</code>.
     * <p>
     * The second model can be used in case of variable bitrating. In this case
     * the seek operation must be done by the decoder itself. The method returns
     * <code>ACTIVE_SEEKING_READY</code>,
     * <code>ACTIVE_SEEKING_FAILED</code> or
     * <code>ACTIVE_SEEKING_ABORTED</code> to force the framework ignore his own
     * seek strategy.
     * <p>
     * An active seeking algorithm with caching is implemented. So the method
     * only returns
     * <code>ACTIVE_SEEKING_READY</code>,
     * <code>ACTIVE_SEEKING_FAILED</code> or
     * <code>ACTIVE_SEEKING_ABORTED</code> after uncached conditions. The
     * decoder performs a decoder buffer flush during a seek operation.
     * <p>
     * Note that only layer 3 isn't audio frame cuttable, because here audio
     * data belonging to one audio time slice is related to many audio frames.
     * (byte resevoire technique). So a soft cutting technique is imlemented
     * here: Before starting after seeking or looping, 8 audio frames are
     * decoded silently. It prevents the production of scratch noises, which are
     * caused of an incorrectly decoding process needing the audio data of the
     * frames before. The underlying problem is a kind of the "chicken egg
     * problem":<br>
     * You need chickens to get eggs and you need eggs to get chickens. You need
     * an audio frame for the next audio frame to decode later correctly. The
     * audio codec layer 2+ from Andre Buschmann seems to be the better solution
     * as layer 3 (for many further reasons).
     *
     * @param position the position to seek as a value between 0 and the media
     * size in bytes
     * @return the corrected incoming byte position parameter
     * <code>position</code> or an integer id if the seek operation is done
     * @exception IOException if an IO error occurs
     * @see #ACTIVE_SEEKING_READY
     * @see #ACTIVE_SEEKING_FAILED
     * @see #ACTIVE_SEEKING_ABORTED
     */
    @Override
    public final long seek(long n) throws IOException {
        if (!((Frame) information).audio) {
            return n;
        }
        flush = n == 0 ? FLUSH_RANGE_FOR_START : FLUSH_RANGE;
        reset();

        if (framesMinusOne > 0 && !((Frame) information).video) {

            int oldFrameCount = frameCount;

            frameCount = (int) Math.round(framesMinusOne * n / (double) byteLength);

            if (frameCount > framesMinusOne) {
                frameCount = framesMinusOne;
            }

            if (limiter >= frameCount || completeCached) {
                return positionOfFrame[frameCount];
            }

            for (int j = oldFrameCount; j < frameCount && !completeCached;) {

                int answer = skipFrame();

                if (answer == ACTIVE_SEEKING_FAILED) {
                    return ACTIVE_SEEKING_FAILED;
                } else if (answer == ACTIVE_SEEKING_ABORTED) {
                    frameCount = j;
                    return ACTIVE_SEEKING_ABORTED;
                } else {
                    j++;
                    if (j > limiter) {
                        position += headerlessFrameSize + 4;
                        positionOfFrame[j] = position;
                        limiter = j;
                        if (limiter >= framesMinusOne) {
                            completeCached = true;
                        }
                    }
                }
            }
            return ACTIVE_SEEKING_READY;
        }
        if (((MpxReader) information).vbr || ((MpxReader) information).xing) {
            long m = n;

            // n = correctPositionWithToc(n); some mp3 contains crap
            if (n == Decoder.NO_VALUE) {
                n = m;
            }
        }
        return n;
    }

    private long correctPositionWithToc(long position) {
        long playtime = (long) Math.round(position * 8000000D / (double) bitRate);

        return fetchBytePosition(playtime);
    }

    /**
     * Sets the decoder plugin to a given playtime length in microseconds.
     *
     * @param playtimeLength the given playtime length in microseconds
     */
    @Override
    public final void setPlaytimeLength(long playtimeLength) {
        if (playtimeLength <= 0 || !((Frame) information).audio || ((Frame) information).video) {
            return;
        }

        ((MpxReader) information).setPlaytimeLength(playtimeLength);

        bitRate = ((Integer) info.get(AudioInformation.I_BITRATE));
    }

    /**
     * Sets the decoder plugin to a given playtime in microseconds. Note that
     * this method activates a decoding and repainting of the video image
     * assigned to the seek position at the choosen playtime in case of video
     * playback.
     *
     * @param playtime the given playtime in microseconds
     */
    @Override
    public final void setCurrentPlaytime(long playtime) {
        if (!((Frame) information).audio) {
            return;
        }

        flush = FLUSH_RANGE_FOR_START;

        reset();

        if (framesMinusOne > 0) {
            frameCount = (int) Math.round(framesMinusOne * playtime / (double) ((Frame) information).microseconds);

            if (frameCount > framesMinusOne) {
                frameCount = framesMinusOne;
            }
        }
    }

    /**
     * Fetches the playtime of the current selected and streamed media source at
     * a given byte position under following constraints:
     * <ul>
     * <li>The source must be suspended and repositioned by an internet protocol
     * induced seek</li>
     * <li>This framework is running in streaming mode</li>
     * </ul>
     * or
     * <ul>
     * <li>The source is setted but is not playing back</li>
     * <li>This framework is running in streaming mode</li>
     * </ul>
     *
     * If no value is fetched, the method returns a long array containing always
     * <code>NO_VALUE</code>. In addition the source stream will be resetted to
     * the old read position.
     *
     * @return a long array with the current playtime on index 0 and the current
     * playtime since the last video key frame on index 1 in microseconds * *      * or <code>NO_VALUE</code>
     * @see #NO_VALUE
     */
    @Override
    public final long[] fetchPlaytime() {
        long[] ret = new long[1];

        ret[0] = Decoder.NO_VALUE;
        return ret;
    }

    /**
     * Fetches the byte position at a given playtime in microseconds under
     * following conditions:
     * <ul>
     * <li>The source is suspended and
     * <code>MediaInformation.B_BYTE_POSITION_FETCHABLE</code> returns true</li>
     * </ul>
     * If no value is fetched, the method returns
     * <code>NO_VALUE</code>.
     *
     * @param playtime the playtime, which correspondents to the returned byte
     * position
     * @return the byte position
     * @see org.ljmf.media.MediaInformation#B_BYTE_POSITION_FETCHABLE
     * @see #NO_VALUE
     */
    @Override
    public final long fetchBytePosition(long playtime) {
        return ((MpxReader) information).fetchBytePosition(playtime);
    }

    @Override
    void reset() {
        filter1.reset();
        if (filter2 != null) {
            filter2.reset();
        }
        super.reset();
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        super.close();
        positionOfFrame = null;
    }
}
