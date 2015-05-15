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
 * <code>Decoder</code> class is the base class for any audio decoder class of
 * any plug-in. The underlying framework converts audio formats indirectly to
 * each other by means of the following determined and uncompressed transmission
 * format: raw, signed, 16 bit, big-endian, pcm (pulse code modulated = digital)
 * audio. In addition, the sample size in bits of this format is fixed to 16
 * bits. This framework may be further developed to support various pcm
 * transmission formats with or higher sample sizes without having outdated
 * plug-in compatibility problems.
 *
 * <p>
 * The technical term "raw" means, that audio data doesn't contain any header
 * (meta) information bytes describing the audio format.
 *
 * <p>
 * The technical term "sample size in bits" determines the value range of the
 * audio sample values. The higher the sample size, the better is the precision
 * of the audio digitalization (quantization). The value range of the audio
 * sample values can either be positive only (unsigned) or not (signed).
 *
 * <p>
 * The technical term "big-/little-endian" refers to which bytes are most
 * significant in multi-byte audio data types. In big-endian architectures, the
 * leftmost bytes (those with a lower address) are most significant. In
 * little-endian architectures, the rightmost bytes are most significant. For
 * example, consider the number 1025 (2 to the tenth power plus one) stored in a
 * 4-byte integer and represented as big-endian: <br>
 * 00000000 00000000 00000100 00000001
 *
 * <p>
 * <table><tr><td><b>Address</b></td><td><b>Big-Endian</b></td><td><b>Little-Endian</b></td></tr>
 * <tr><td>00<br>01<br>02<br>03</td><td>00000000<br>00000000<br>00000100<br>00000001</td><td>00000001<br>00000100<br>00000000<br>00000000</td></tr>
 * </table>
 *
 * <p>
 * The same number stored in a 4-byte integer and represented as little-endian
 * would result in: <br>
 * 00000001 00000100 00000000 00000000
 *
 * <p>
 * Many mainframe computers, particularly IBM mainframes, use a big-endian
 * architecture. Most modern computers, including PCs, use the little-endian
 * system. The PowerPC system is big-endian because it can understand both
 * systems. Converting data between the two systems is sometimes referred to as
 * the NUXI problem. Imagine the word UNIX stored in two 2-byte words. In a
 * big-endian systems, it would be stored as UNIX. In a little-endian system, it
 * would be stored as NUXI.
 *
 * <p>
 * Note that the example above shows only big- and little-endian byte orders.
 * The bit ordering within each byte can also be big- or little-endian, and some
 * architectures actually use big-endian ordering for bits and little-endian
 * ordering for bytes, or vice versa. The terms big-endian and little-endian are
 * derived from the Lilliputians of Gulliver's Travels, whose major political
 * issue was whether soft-boiled eggs should be opened on the big side or the
 * little side. Likewise, the big-/little-endian computer debate has much more
 * to do with political issues than technological merits.
 *
 * <p>
 * To create a hypothetical Mp3 audio plug-in it's required to derive this class
 * according to the following listing:
 *
 * <blockquote>
 * <pre>
 * package org.ljmf.audio.codec.decoder.mp3;<p>
 *
 * import java.io.*;<p>
 *
 * import org.ljmf.media.*; import org.ljmf.audio.codec.decoder.*;<p>
 *
 * public final class MyMp3Decoder extends Decoder {<p>
 *
 * private boolean timeMode;<p>
 *
 * private InputStream stream;<p>
 *
 * private boolean frequencyMode;<p>
 *
 * private boolean playtimeInMicroseconds;<p>
 *
 * private long framesMinusOne;<p>
 *
 *     //====================<p>
 *
 * public MyMp3Decoder(MediaInformation info, InputStream stream) {
 * super(info.getPreConfiguration(), info, stream);<p>
 *
 * this.stream = stream;<p>
 *
 * MyEqualizerInitializing(get(FA_EQUALIZE)); // get(FA_EQUALIZE) is a command
 * query MySynthesizerInitializing();
 * MyChannelsInitializing(get(FA_CHANNEL_MAPPING)); // get(FA_CHANNEL_MAPPING)
 * is a command query MyVariableBitratinInitializing(stream, info);
 * MyErrorCorrectionInitializing();
 * MyDecodingProcessorInitializing(MyGetBitstreamProcessor(stream), info);
 * MyDataAnalyzerInitializing(((Integer)
 * info.get(AudioInformation.I_DEVICE_SAMPLE_RATE)).intValue(), ((Integer)
 * info.get(AudioInformation.I_SAMPLE_RATE)).intValue());<p>
 *
 *         // An example of a command query processing:<p>
 *
 * Object a = get(S_AUDIO_DATA_ANALYZE_MODE); // get(S_AUDIO_DATA_ANALYZE_MODE)
 * is a command query<p>
 *
 * if (((String) a).equals(FREQUENCY)) {
 * info.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, SPECTRUM); frequencyMode
 * = true; } if (((String) a).equals(TIME)) {
 * info.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, TIME); timeMode = true;
 * } playtimeInMicroseconds = ((Long)
 * info.get(MediaInformation.L_MICROSECONDS)).longValue(); }<p>
 *
 * public Object put(Object key, Object value) throws NullPointerException {<p>
 *
 *         // An example of a command query processing:<p>
 *
 * if (key.equals(S_AUDIO_DATA_ANALYZE_MODE)) { if (((String)
 * value).equals(FREQUENCY)) {
 * information.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, FREQUENCY);
 * frequencyMode = true; } if (((String) value).equals(TIME)) {
 * information.put(AudioInformation.S_AUDIO_DATA_ANALYZE_MODE, TIME); timeMode =
 * true; } }<p>
 *
 *     //====================<p>
 *
 * }<p>
 *
 * public int read(byte b[], int i, int j) throws IOException { int ret = -1;<p>
 *
 * try { ret = MyReadFromOutputFrameBuffer(b, i, j);
 * super.decodeFrame(VALIDATION_EVENT); } catch (Exception e) { throw new
 * InterruptedIOException("End of file or Mp3 decoding error "+e.getMessage());
 * } return ret; }<p>
 *
 * public int read(byte b[]) throws IOException { return read(b, 0, 0); }<p>
 *
 *     //====================<p>
 *
 * public Object updateAnalyzerView() { return MyAnalyzeValuesFromTheDecoder();
 * }<p>
 *
 * public long correctedBytePosition(long position, boolean seeking) { if
 * (framesMinusOne > 0) { if (seeking) { return position +
 * myOffsetUntilAudioData; } return (long) Math.round(frameCount * myByteLength
 * / (double) framesMinusOne); }
 *
 * if (seeking) { position + myOffsetUntilAudioData; } else { position -
 * myOffsetUntilAudioData; }
 *
 * if (position < 0) { position = 0; } return position; }<p>
 *
 * protected long fetchBytePosition(long playtime) { if (myTocObject == null ||
 * playtimeInMicroseconds == 0) { return Decoder.NO_VALUE; } double increment =
 * playtime / (double) playtimeInMicroseconds; return (long)
 * mySplineObject.getValue(myTocObject, increment); }<p>
 *
 * public int decodeFrame(int eventId) throws IOException { int l = 0;
 *
 * l = readFrame(); if (l == EOM_EVENT) { return EOM_EVENT; } myDecode();<p>
 *
 *         //====================<p>
 *
 * return super.decodeFrame(l); }<p>
 *
 * public long seek(long n) throws IOException { if (!((MyMp3Reader)
 * information).audio) { return n; }<p>
 *
 * myFlush = n == 0 ? FLUSH_RANGE_FOR_START : FLUSH_RANGE; myReset();<p>
 *
 * if (myFramesMinusOne > 0 && !((MyMp3Reader) information).video) {<p>
 *
 * int oldFrameCount = myFrameCount;<p>
 *
 * myFrameCount = (int) Math.round(myFramesMinusOne * n / (double)
 * myByteLength); if (myFrameCount > myFramesMinusOne) { myFrameCount =
 * myFramesMinusOne; }<p>
 *
 * if (myLimiter >= frameCount || myPositionsAlreadyCached) { return
 * myCachedPosition[myFrameCount]; } else { for (int j = oldFrameCount; j <
 * myFrameCount;) {<p>
 *
 * int answer = mySkipFrame();<p>
 *
 * if (answer == ACTIVE_SEEKING_FAILED) { return ACTIVE_SEEKING_FAILED; } else
 * if (answer == ACTIVE_SEEKING_ABORTED) { myFrameCount = j; return
 * ACTIVE_SEEKING_ABORTED; } else { j++;<p>
 *
 * if (j > myLimiter) { myPosition += myNewFrameSize; myCachedPosition[j] =
 * myPosition; myLimiter = j; if (myLimiter >= myFramesMinusOne) {
 * myPositionsAlreadyCached = true; } } } } return ACTIVE_SEEKING_READY; } }
 * return n; }<p>
 *
 * public void close() { super.close();<p>
 *
 *         //====================<p>
 *
 * }<p>
 *
 *     //====================<p>
 *
 * }
 * </pre>
 * </blockquote>
 *
 * <p>
 * It is recommended to combine several codecs in one plug-in for one media
 * container format. An example is to combine the format codecs Mp1 (audio), Mp2
 * (audio), Mp3 (audio), Mpeg1 (video), Mpeg2 (video), and Mpeg4 (video) in one
 * "Mpeg" plug-in. Also codecs should be reused to work in different plug-ins.
 *
 * @author	Michael Scheerer
 */
public abstract class Decoder extends AudioInformation implements AudioHashkeys, Hashkeys, Events {

    private InputStream stream;
    protected Frame information;
    /**
     * Marks the integer id for no returned value, which is returned from the
     * <code>fetchPlaytime</code> method.
     */
    public final static int NO_VALUE = -1;
    /**
     * Marks the integer id for the active seeking ready value, which is
     * returned from the
     * <code>seek</code> method.
     */
    public final static int ACTIVE_SEEKING_READY = -1;
    /**
     * Marks the integer id for the active seeking failed value, which is
     * returned from the
     * <code>seek</code> method. An active seeking operation failes in case of a
     * corrupted media stream.
     */
    public final static int ACTIVE_SEEKING_FAILED = -2;
    /**
     * Marks the integer id for the active seeking aborted value, which is
     * returned from the
     * <code>seek</code> method. An active seeking operation aborts in case of
     * an progressive streaming buffer underflow.
     */
    public final static int ACTIVE_SEEKING_ABORTED = -3;

    /**
     * Constructs an instance of
     * <code>Decoder</code> with a
     * <code>MediaInformation</code> object containing all necessary
     * informations about the media source.
     *
     * @param control the <code>MediaControl</code> object containing all
     * requests related to the audio decoding
     * @param info the <code>MediaInformation</code> object containing all
     * necessary informations about the media source
     * @param stream the input stream
     */
    protected Decoder(Frame info, InputStream stream) {
        this.information = info;
        this.stream = stream;

        put(S_AUDIO_DATA_ANALYZE_MODE, FREQUENCY);
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
     * @param an event id number as an integer value send by an overwritten
     * method
     * @return an event id number as an integer value
     * @exception IOException if an I/O error occurs
     * @exception InterruptedIOException if the decoding process is interrupted
     * caused by malformed media data
     */
    public int decodeFrame(int eventId) throws IOException {
//        if (eventId != EOM_EVENT && eventId != SKIP_EVENT && stream != null) {
//            if (stream instanceof PacketInputStream) {
//                ((PacketInputStream) stream).getEncodedSystemStream().updateAnalyzerView(updateAnalyzerView());
//            } else if (stream instanceof MediaInputStream) {
//                ((MediaInputStream) stream).updateAnalyzerView(updateAnalyzerView());
//            }
//        }
        return eventId;
    }

    /**
     * Reads up the audio source to len bytes of data from the decoded audio
     * stream into an array of bytes. If the argument b is
     * <code>null</code>, -1 is returned. The audio decoder must implement this
     * method in the way, that this method reads raw, unsigned, 16 bit,
     * big-endian, pcm audio data. This framework may be further developed to
     * support additional pcm output formats.
     *
     *
     * @param b the buffer into which the data is read
     * @param i the start offset of the data
     * @param j the maximum number of bytes read
     * @return the total number of bytes read into, or -1 is there is no more
     * data because the end of the stream has been reached
     * @exception IOException if an input or output error occurs
     * @exception InterruptedIOException if the decoding process is interrupted
     * caused by malformed media data
     */
    public abstract int read(byte b[], int i, int j) throws IOException;

    /**
     * Reads up to a specified maximum number of bytes of data from the decoded
     * audio stream, putting them into the given byte array. If the argument b
     * is null, -1 is returned. The audio decoder must implement this method in
     * the way, that this method reads raw, unsigned, 16 bit, big-endian, pcm
     * audio data. This framework may be further developed to support additional
     * pcm output formats. It is very important to mention, that the most left
     * byte of a pcm audio sample, which is currently always 16 bit sized, must
     * be the first written byte of the framework typical byte array
     * representation of audio samples.
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is
     * no more data because the end of the stream has been reached
     * @exception IOException if an input or output error occurs
     * @exception InterruptedIOException if the decoding process is interrupted
     * caused by malformed media data
     */
    public abstract int read(byte b[]) throws IOException;

    /**
     * Updates audio analyze data or analyze mode dependent keywords, if an
     * analyze mode switch have to be induced. The first single float data value
     * of the first and second channel are the first two float array values.
     *
     * @return the float array containing all values of the analyze data values
     * in the range of -1 to 1 or analyze mode dependent keywords in the form of
     * a <code>String</code>, if an analyze mode switch have to be induced
     */
    public abstract Object updateAnalyzerView();

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
    public abstract long correctedBytePosition(long position, boolean seeking);

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
     * the seek operation must be done from the decoder itself. The method
     * returns
     * <code>ACTIVE_SEEKING_READY</code>,
     * <code>ACTIVE_SEEKING_FAILED</code> or
     * <code>ACTIVE_SEEKING_ABORTED</code> to force the framework ignore his own
     * seek strategy. Note that a decoder must perform a decoder buffer flush
     * during a seek operation.
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
    public abstract long seek(long position) throws IOException;

    /**
     * Sets the decoder plugin to a given playtime length in microseconds.
     *
     * @param playtimeLength the given playtime length in microseconds
     */
    public abstract void setPlaytimeLength(long playtimeLength);

    /**
     * Sets the decoder plugin to a given playtime in microseconds. Note that
     * this method activates a decoding and repainting of the video image
     * assigned to the seek position at the choosen playtime in case of video
     * playback.
     *
     * @param playtime the given playtime in microseconds
     */
    public abstract void setCurrentPlaytime(long playtime);

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
     * playtime since the last video key frame on index 1 in microseconds * * *
     * or <code>NO_VALUE</code>
     * @see #NO_VALUE
     */
    public abstract long[] fetchPlaytime();

    /**
     * Fetches the byte position of the current selected and streamed media
     * source at a given playtime in microseconds. If no value is fetched, the
     * method returns
     * <code>NO_VALUE</code>.
     *
     * @param playtime the playtime, which correspondents to the returned byte
     * position
     * @return the byte position or <code>NO_VALUE</code>
     * @see #NO_VALUE
     */
    public abstract long fetchBytePosition(long playtime);

    @Override
    public void close() {
        clear();
    }
}
