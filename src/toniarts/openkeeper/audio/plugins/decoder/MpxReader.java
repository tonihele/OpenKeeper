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
import toniarts.openkeeper.audio.plugins.decoder.tag.Tag;
import toniarts.openkeeper.audio.plugins.decoder.tag.id3v1.ID3v1;
import toniarts.openkeeper.audio.plugins.decoder.tag.id3v2.ID3v2;

/**
 * The
 * <code>MpxReader</code> class provides all necessary audio format detection
 * related methods. The
 * <code>readInformation</code> method must be used by other classes outside
 * this package. The
 * <code>MpxReader</code> class acts also as a
 * <code>AudioHeader</code> class storing audio header related data.
 *
 * @author	Michael Scheerer
 */
public class MpxReader extends Frame {

    private final static byte SYSTEM_SYNC_PATTERN = 0x1; // Prefix
    // ISO/IEC 13818-2 6.3.3
    private final static short SEQUENCE_HEADER_CODE = 0x1B3; // 435 //00000001 10110011
    // ISO/IEC 13818-1 2.5.3.4
    private final static short PACK_START_CODE = 0x1BA; // 442 //00000001 10111010
    private final static short PRE_SCAN_DEPTH = 100;
    private Tag tag; // Doesn't need a close or clear call
    private int skippedDataLength;
    private int verificationDepth;
    private boolean tagSettedSkippedDataLength;
    private boolean decoderInitialized;
    private int j;
    private Spline spline;
    private boolean noLenghtData;
    /**
     * Indicates the version of this MPEG audio format.
     */
    protected String versionString;
    /**
     * Bitpattern buffer
     */
    protected byte[] b;
    /**
     * Standart counter and bit register
     */
    protected int i;
    /**
     * Skip buffer
     */
    protected int skip;

    /**
     * Constructor for the MpxReader object
     */
    public MpxReader() {
        super();
        b = new byte[4];
    }

    /**
     * Returns an
     * <code>MediaInformation</code> object containing informations about the
     * detected media format or throws an
     * <code>UnsupportedMediaException</code>, if the current decoder don't
     * support this media format. The implementation of this method must be able
     * to mark the stream, read enough data to verify its own format, load the
     * <code>MediaInformation</code> object with data, and reset the read
     * pointer of the stream to its original position in case of an unknown
     * format. The first argument is the audio source input stream and the
     * second argument is a boolean flag indicating if the end of media is
     * determinable. The source input stream may be multiplexed.
     *
     * @param stream the audio input stream
     * @param eomDeterminable true if the end of media is determinable
     * @return the <code>MediaInformation</code> object containing all relevant
     * information related to the audio source
     * @exception UnsupportedMediaException if this media format can't be
     * handled
     */
    public MediaInformation readInformation(InputStream stream, boolean eomDeterminable) throws UnsupportedMediaException {

        try {
            stream.mark(Context.getInputBufferSize()); // reset only if no valid (ID3/audio) format -> preparing for the next format
            if (!preVerifyStartCode(stream)) {
                stream.reset();
            } else {
                stream.reset();
                throw new UnsupportedMediaException("Supported videoformat");
            }
        } catch (IOException e) {
            b[0] = b[1] = b[2] = b[3] = 0;
            i = 0;
            try {
                stream.reset();
            } catch (Exception e1) {
            }
        }
        int buffer = 0;

        skippedDataLength = 0;
        try {
            stream.mark(Context.getInputBufferSize()); // reset only if no valid (ID3/audio) format -> preparing for the next format
            verificationDepth = buffer = Context.getVerificationDepth();
            Context.setVerificationDepth(50000);
            handleId3V2(stream);
        } catch (Exception e) {
            if (!tagSettedSkippedDataLength) {
                try {
                    stream.reset();
                } catch (Exception e1) {
                }
            }
        }
        stream.mark(Context.getInputBufferSize());
        readAudioInformation(stream);
        try {
            stream.reset();
        } catch (Exception e) {
        }
        loadMediaInformation();
        Context.setVerificationDepth(buffer);
        return this;
    }

    /**
     * Sets the data size until the first audio frame and a flag indicating if
     * skippedDataLength was setted from the tag in case of an error.
     *
     * @param skippedDataLength the skipped data length
     */
    public void setSkippedDataLength(int skippedDataLength) {
        if (skippedDataLength > 0) {
            this.skippedDataLength += skippedDataLength;
            Context.setVerificationDepth(skippedDataLength + verificationDepth);
            this.tagSettedSkippedDataLength = true;
        }
    }

    /**
     * Parses informations about the detected audioformat.
     *
     * @param stream the input stream
     * @exception UnsupportedMediaException if this media format can't be
     * handled
     */
    protected final void readAudioInformation(InputStream stream) throws UnsupportedMediaException {
        i = 0;

        do {
            try {
                while (((i = getInt()) >>> 10 & 0x3) == 3 || (i >>> 17 & 0x3) == 0 || (i >>> 19 & 0x3) == 1 || (AUDIO_SYNC_PATTERN != (i & AUDIO_SYNC_PATTERN))) {
                    while (read(stream, (byte) 0xFF)) {
                        if (skippedDataLength >= Context.getVerificationDepth()) {
                            throw new UnsupportedMediaException("Out of verify limit");
                        }
                    }

                    j = stream.read(b, 1, 3);
                    incrementSkippedDataLength(j);

                    if (skippedDataLength >= Context.getVerificationDepth() || j < 0) {
                        throw new UnsupportedMediaException("Out of verify limit");
                    }
                }
            } catch (Exception e) {
                throw new UnsupportedMediaException("Unexpected end of stream");
            }

            if (freeMode && !freeModeInitialized) {
                if (!(orgVersion == (i >>> 19 & 0x3) && orgSampleFrequency == (i >>> 10 & 0x3) && orgLayer == (i >>> 17 & 0x3))) {
                    b[0] = b[1] = b[2] = b[3] = 0;
                    continue;
                }
                headerlessFrameSize = skippedDataLength - 4;
                determineBitrate();
                freeModeInitialized = true;
            }

            orgVersion = i >>> 19 & 0x3;

            version = Math.abs(orgVersion - 2);

            frequencyIndex = orgSampleFrequency = i >>> 10 & 0x3;

            layer = 4 - (orgLayer = i >>> 17 & 0x3);

            protectionBit = i >>> 16 & 0x1;

            if (protectionBit == 0) {
                crc = new CRC16();
                put(B_BIT_PROTECTION, true);
            }

            Object o;

            if (layer == 3) {
                throw new UnsupportedMediaException("Mp3 not allowed");
            }

            bitrateIndex = i >>> 12 & 0xF;

            paddingBit = i >>> 9 & 0x1;

            mode = i >>> 6 & 0x3;

            modeExtension = i >>> 4 & 0x3;

            if ((i >>> 3 & 0x1) == 1) {
                copyright = true;
            }
            if ((i >>> 2 & 0x1) == 1) {
                original = true;
            }
            emphasis = i & 0x3;

            if (layer == 1 || layer == 3) {
                numberOfSubbands = 32;
            } else {
                int j = bitrateIndex;

                if (mode != SINGLE_CHANNEL) {
                    if (j == 4) {
                        j = 1;
                    } else {
                        j -= 4;
                    }
                }

                if (version == 1) { // MPEG-1
                    if (frequencyIndex == 1 && j >= 3 || j >= 3 && j <= 5) {
                        tableGroup = 0;
                        numberOfSubbands = 27;
                    } else if (frequencyIndex != 1 && j >= 6) {
                        tableGroup = 0;
                        numberOfSubbands = 30;
                    } else if (frequencyIndex != 2 && j <= 2) {
                        tableGroup = 1;
                        numberOfSubbands = 8;
                    } else {
                        tableGroup = 1;
                        numberOfSubbands = 12;
                    }
                } else {
                    tableGroup = 2; // MPEG-2 LSF
                    numberOfSubbands = 30;
                }
                if (version == 0 || version == 2) {
                    numberOfSubbands = 30;
                }
            }
            if (layer == 1) {
                timePerFrame = 384 / (double) FREQUENCY[version][frequencyIndex] / 2D;
            } else {
                timePerFrame = 1152 / (double) FREQUENCY[version][frequencyIndex] / 2D;
            }
            if (version == 1) {
                timePerFrame *= 2D;
            }
            put(F_FRAME_RATE, new Float(1 / timePerFrame));
        } while (checkFramesize(stream) == null);

        switch (version) {
            case 0:
                versionString = "2";
                break;

            case 1:
                versionString = "1";
                break;

            case 2:
                versionString = "2.5";
                break;
        }
        handleChannelSettings();
    }

    /* If necessary excludes the tag data and/or skipped data length from the byte position
     * or calculates a variable bitrate correction to determine the
     * actual playtime or seek position of the media. Normally the playtime position
     * calculation is different from the one of the seek position.
     *
     * @param seeking          seeking flag to distinguish the different calculation of the playtime and seek position
     * @param position         the current byte position
     * @return                 the corrected byte position
     */
    public long correctedBytePosition(long position, boolean seeking) {
        if (seeking) {
            position += skippedDataLength;
        } else {
            position -= skippedDataLength;
        }

        if (position < 0) {
            position = 0;
        }

        if (tag != null) {
            return tag.correctedBytePosition(position, seeking);
        }
        return position;
    }

    /**
     * Sets the decoder plugin to a given playtime length in microseconds.
     *
     * @param playtimeLength the given playtime length in microseconds
     */
    protected void setPlaytimeLength(long playtimeLength) {
        if (xing || vbr || info) {
            return;
        }
        microseconds = playtimeLength;
        if (xing || vbr) {
            put(I_BITRATE, new Integer((int) Math.round(netByteLength * 8000000 / (double) microseconds)));
        }
        put(L_MICROSECONDS, new Long(microseconds));
    }

    /**
     * This method reads the next 8 bits and checks these bits against a
     * reference pattern. It returns
     * <code>false</code> if the readed pattern equals the reference pattern.
     *
     * @param stream the input stream
     * @param pattern the reference pattern
     * @return                 <code>false</code> if the readed pattern equals the reference
     * pattern
     * @exception IOException if an I/O error occurs
     */
    private boolean read(InputStream stream, byte pattern) throws IOException {
        int buf = stream.read();

        incrementSkippedDataLength(1);

        if (buf == -1) {
            return false;
        }
        return (b[0] = (byte) buf) != pattern;
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
     * <code>org.ljmf.audio.codec.decoder.Decoder.NO_VALUE</code>. In addition
     * the source stream will be resetted to the old read position.
     *
     * @return a long array with the current playtime on index 0 and the current
     * playtime since the last video key frame on index 1 in microseconds * * *
     * or <code>org.ljmf.audio.codec.decoder.Decoder.NO_VALUE</code>
     * @see org.ljmf.audio.codec.Decoder#NO_VALUE
     */
    protected long[] fetchPlaytime() {
        long[] ret = new long[1];

        ret[0] = Decoder.NO_VALUE;
        return ret;
    }

    /**
     * Fetches the byte position of the current selected and streamed media
     * source at a given playtime in microseconds. If no value is fetched, the
     * method returns
     * <code>org.ljmf.audio.codec.decoder.Decoder.NO_VALUE</code>.
     *
     * @param playtime the playtime, which correspondents to the returned byte
     * position
     * @return the byte position * * * *
     * or <code>org.ljmf.audio.codec.decoder.Decoder.NO_VALUE</code>
     * @see org.ljmf.audio.codec.decoder.Decoder#NO_VALUE
     */
    protected final long fetchBytePosition(long playtime) {
        if (toc == null || microseconds == 0) {
            return Decoder.NO_VALUE;
        }
        double inc = playtime / (double) microseconds;

        return (long) spline.getValue(toc, inc);
    }

    private void loadMediaInformation() {
        put(I_MAXIMUM_CODEC_SAMPLE_RATE, new Integer(48000));
        put(B_AUDIO, true);
        put(S_MEDIA_TYPE, "Audio");
        put(S_MEDIA_FORMAT, "Mpeg" + versionString + "-Layer" + new Integer(layer).toString());
        put(S_MEDIA_FILE_FORMAT, "MP" + new Integer(layer).toString());

        if (mode == STEREO) {
            put(S_MULTI_CHANNEL_MODE, "Stereo");
        } else if (mode == JOINT_STEREO) {
            put(S_MULTI_CHANNEL_MODE, "Joint Stereo");
        } else if (mode == DUAL_CHANNEL) {
            put(S_MULTI_CHANNEL_MODE, "Dual Channel");
        }
        put(B_ORIGINAL, original);
        put(B_COPYRIGHT, copyright);
        if (video) {
            vbr = false;
            xing = false;
            info = false;
            noLenghtData = false;
        }
        if (freeMode) {
            put(I_BITRATE, new Integer(getBitrate()));
        }
        if (noLenghtData) { // can only be triggered in case of xing header
            netByteLength = correctedBytePosition(((Long) get(AudioInformation.L_GROSS_BYTE_LENGTH)).longValue(), false);
            put(I_BITRATE, new Integer((int) (netByteLength * 8 / timePerFrame / (double) (framesMinusOne + 1))));
            put(L_BYTE_LENGTH, new Long(netByteLength));
        } else if (!vbr && !xing && !info) {
            netByteLength = correctedBytePosition(((Long) get(AudioInformation.L_GROSS_BYTE_LENGTH)).longValue(), false);
            put(L_BYTE_LENGTH, new Long(netByteLength));
        } else if (vbr || xing || info) {
            put(L_BYTE_LENGTH, new Long(netByteLength));
        }
        if (toc != null && microseconds > 0) {
            spline = new Spline();
        }
    }

    private AudioInformation checkFramesize(InputStream stream) throws UnsupportedMediaException {
        calculateFrameSize();
        if (freeMode && !freeModeInitialized) {
            b[0] = b[1] = b[2] = b[3] = 0;
            return null;
        }
        skip = headerlessFrameSize - dataBlockSize;
        if (skip < 0) {
            skip = 0;
        }
        try {
            stream.skip(skip);
            stream.read(b, 0, 4);
            incrementSkippedDataLength(skip + 4);

            xing = ((char) b[0] == 'X' && (char) b[1] == 'i' && (char) b[2] == 'n' && (char) b[3] == 'g');
            info = ((char) b[0] == 'I' && (char) b[1] == 'n' && (char) b[2] == 'f' && (char) b[3] == 'o');
            vbr = ((char) b[0] == 'V' && (char) b[1] == 'B' && (char) b[2] == 'R' && (char) b[3] == 'I');

            if (freeMode) {
                xing = info = vbr = false;
            }
            if (!vbr && !xing && !info) { // Doublecheck: Exist the next frame, with the same version, layer and samplingfrequency?
                skip = headerlessFrameSize - skip - 4;
                if (skip < 0) {
                    skip = 0;
                }
                stream.skip(skip);
                stream.read(b, 0, 4);
                incrementSkippedDataLength(skip + 4);
                if (((i = getInt()) >>> 10 & 0x3) != orgSampleFrequency || (i >>> 17 & 0x3) != orgLayer || (i >>> 19 & 0x3) != orgVersion || (AUDIO_SYNC_PATTERN != (i & AUDIO_SYNC_PATTERN))) {
                    b[0] = b[1] = b[2] = b[3] = 0;
                    return null;
                }
            }
            put(I_SAMPLE_RATE, new Integer(FREQUENCY[version][frequencyIndex]));
            put(I_BITRATE, new Integer(RATE[version][layer - 1][bitrateIndex]));

            if (vbr) {
                return handleVbr(stream);
            } else if (xing || info) {
                return handleXing(stream);
            }
        } catch (IOException e) {
            throw new UnsupportedMediaException("IO error ");
        }
        return this;
    }

    private AudioInformation handleVbr(InputStream stream) throws IOException {
        stream.skip(6); // version id, delay ans quality indicator
        stream.read(b, 0, 4);
        netByteLength = (long) getInt();

        stream.read(b, 0, 4);
        framesMinusOne = getInt() - 1;

        positionOfFrame = new long[framesMinusOne + 1];
        startFrameCount++;

        positionOfFrame[1] = headerlessFrameSize + 4;

        b[0] = b[1];
        b[1] = b[2];
        b[2] = b[3];
        b[3] = (byte) stream.read();
        microseconds = (long) getInt() * 100L;

        framesPerTime = (framesMinusOne + 1) * 1000000 / (double) microseconds;
        if (microseconds > 0) {
            put(I_BITRATE, new Integer((int) Math.round(netByteLength * 8000000 / (double) microseconds)));
        }
        put(F_FRAME_RATE, new Float(framesPerTime));
        put(I_FRAME_NUMBER, new Integer(framesMinusOne + 1));
        put(B_VBR_AUDIO, true);
        put(L_MICROSECONDS, new Long(microseconds));

        b[0] = b[3];
        b[1] = (byte) stream.read();

        int tableEntries = getShort();

        stream.read(b, 0, 2);

        int tableScalefactor = getShort();

        stream.read(b, 0, 2);

        int tableEntrySize = getShort();

        stream.skip(2); // frames per table / frame stride in frames / frames per seekpoint to next seek point

        incrementSkippedDataLength(22);

        toc = new double[tableEntries];

        if (tableEntrySize == 1) {
            incrementSkippedDataLength(toc.length);
            for (i = 0; i < toc.length; i++) {
                toc[i] = (byte) stream.read() * tableScalefactor;
            }
        } else if (tableEntrySize == 2) {
            incrementSkippedDataLength(toc.length * 2);
            for (i = 0; i < toc.length; i++) {
                stream.read(b, 0, 2);
                toc[i] = getShort() * tableScalefactor;
            }
        } else if (tableEntrySize == 3) {
            incrementSkippedDataLength(toc.length * 3);
            for (i = 0; i < toc.length; i++) {
                stream.read(b, 0, 3);
                toc[i] = getShortInt(b) * tableScalefactor;
            }
        } else {
            incrementSkippedDataLength(toc.length * 4);
            for (i = 0; i < toc.length; i++) {
                stream.read(b, 0, 4);
                toc[i] = getInt() * tableScalefactor;
            }
        }
        for (i = 1; i < toc.length; i++) {
            toc[i] += toc[i - 1];
        }
        return this;
    }

    private AudioInformation handleXing(InputStream stream) throws IOException {
        int bitRate;
        long grossByteLength = ((Long) get(AudioInformation.L_GROSS_BYTE_LENGTH)).longValue();

        stream.read(b, 0, 4);

        i = getInt();

        stream.read(b, 0, 4);
        if ((i & 0x0001) != 0) {
            framesMinusOne = getInt() - 1;
        } else {
            xing = false;
            info = false;
            return this;
        }
        positionOfFrame = new long[framesMinusOne + 1];
        startFrameCount++;

        positionOfFrame[1] = headerlessFrameSize + 4;

        stream.read(b, 0, 4);

        incrementSkippedDataLength(12);

        if ((i & 0x0002) != 0) {
            netByteLength = (long) getInt();
        } else {
            noLenghtData = true;
        }
        if ((i & 0x0004) != 0) {
            toc = new double[100];

            incrementSkippedDataLength(100);
            for (j = 0; j < 100; j++) {
                toc[j] = stream.read();
            }
        }
        if ((i & 0x0008) != 0) {
            stream.read(b, 0, 4); // quality indicator
        }
        incrementSkippedDataLength(4);
        stream.read(b, 0, 4);
        lame = ((char) b[0] == 'L' && (char) b[1] == 'A' && (char) b[2] == 'M' && (char) b[3] == 'E') || ((char) b[0] == 'G' && (char) b[1] == 'O' && (char) b[2] == 'G' && (char) b[3] == 'O');

        if (lame) {
            stream.skip(17);
            stream.read(b, 0, 3);
            i = getShortInt(b);
            int samplesPerFrame;

            if (layer == 1) {
                samplesPerFrame = 384 / 2;
            } else {
                samplesPerFrame = 1152 / 2;
            }
            if (version == 1) {
                samplesPerFrame *= 2;
            }
            if (mode != SINGLE_CHANNEL) {
                samplesPerFrame *= 2;
            }
            delayStart = i >>> 12 & 0xFFF;
            delayEnd = i & 0xFFF;
            frameDelayStart = delayStart / samplesPerFrame;
            frameDelayEnd = delayEnd / samplesPerFrame;
            delayStart %= samplesPerFrame;
            delayEnd %= samplesPerFrame;
            incrementSkippedDataLength(20);
        }
        if (!noLenghtData && !info) { // info == cbr, main header info is more relyable
            bitRate = (int) (netByteLength * 8 / timePerFrame / (double) (framesMinusOne + 1));
            put(I_BITRATE, new Integer(bitRate));
        }
        put(I_FRAME_NUMBER, new Integer(framesMinusOne + 1));
        if (video) {
            toc = null;
            return this;
        }
        microseconds = (long) Math.round(timePerFrame * (double) (framesMinusOne + 1) * 1000000D);
        put(L_MICROSECONDS, new Long(microseconds));

        if (toc != null) {
            for (j = 0; j < toc.length; j++) {
                toc[j] = toc[j] * grossByteLength / 256;
            }
        }
        if (!info) { // !cbr
            put(B_VBR_AUDIO, true);
        }
        return this;
    }

    private void handleChannelSettings() {
        if (mode == SINGLE_CHANNEL) {
            channels = 1;
        } else {
            channels = 2;
        }
        put(I_CHANNEL_NUMBER, new Integer(channels));
        if (mode == DUAL_CHANNEL) {
            put(I_DEVICE_CHANNEL_NUMBER, new Integer(1));
        }
        if (layer == 1) {
            put(I_OUTPUT_BUFFER_SIZE, new Integer(Output.OBUFFERSIZE * channels / 3));
        } else if (layer == 3 && version != 1) {
            put(I_OUTPUT_BUFFER_SIZE, new Integer(Output.OBUFFERSIZE * channels / 2));
        } else {
            put(I_OUTPUT_BUFFER_SIZE, new Integer(Output.OBUFFERSIZE * channels));
        }
    }

    private void handleId3V2(InputStream stream) throws IOException {
        put(TAG, (tag = new ID3v2(stream, this)));
    }

    private void handleId3V1(InputStream stream) throws IOException {
        put(TAG, (tag = new ID3v1(stream)));
    }

    private int getInt() {
        return b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;
    }

    /**
     * Gets an integer value added bitwise from a short int array.
     *
     * @param b the input array
     * @return the integer value
     */
    protected final static int getShortInt(byte b[]) {
        return b[0] << 16 & 0xFF0000 | b[1] << 8 & 0xFF00 | b[2] & 0xFF;
    }

    private int getShort() {
        return b[0] << 8 & 0xFF00 | b[1] & 0xFF;
    }

    /**
     * Instances a
     * <code>Decoder</code> object based on informations about the detected
     * media format or
     * <code>null</code>, if the decoder can't be instanced. The first argument
     * is the media source input stream and the second argument is a boolean
     * flag indicating if the end of media is determinable. The source input
     * stream may be multiplexed.
     *
     * @param stream the input stream
     * @param eomDeterminable true if the end of media is determinable
     * @return the <code>Decoder</code> object based on informations about the
     * detected media format or <code>null</code>
     */
    public Decoder getDecoder(InputStream inputstream, boolean eomDeterminable) throws UnsupportedMediaException {
        if (eomDeterminable && tag == null && !video) {
            long length = ((Long) get(L_GROSS_BYTE_LENGTH)).longValue();

            length = correctedBytePosition(length, false);

            inputstream.mark((int) length);
            try {
                inputstream.skip(length - 128 + skippedDataLength);
                handleId3V1(inputstream);
            } catch (Exception e) {
            }
            try {
                inputstream.reset();
            } catch (Exception e) {
            }
        }

        Decoder dec = null;

        switch (layer) {
            case 3: // '\003'
                throw new UnsupportedMediaException("No MP3 decoder!");

            case 2: // '\002'
                dec = new LayerIIDecoder(this, inputstream);
                break;

            case 1: // '\001'
                dec = new LayerIDecoder(this, inputstream);
        }
        decoderInitialized = true;
        return dec;
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        super.close();
        b = null;
        if (spline != null) {
            spline.close();
        }
        spline = null;
    }

    private void incrementSkippedDataLength(int i) {
        if (decoderInitialized) {
            return;
        }
        skippedDataLength += i;
    }

    private boolean preVerifyStartCode(InputStream stream) throws IOException {
        int count = 0;

        b[0] = b[1] = b[2] = b[3] = 0;

        count += stream.read(b, 0, 3);

        while ((i = getShortInt(b)) != SYSTEM_SYNC_PATTERN) {
            b[0] = b[1];
            b[1] = b[2];
            b[2] = (byte) stream.read();

            count++;
            if (count >= Math.min(Context.getVerificationDepth(), PRE_SCAN_DEPTH)) {
                return false;
            }
        }
        i = i << 8 | stream.read() & 0xFF;

        if (i == SEQUENCE_HEADER_CODE || i == PACK_START_CODE) {
            b[0] = b[1] = b[2] = b[3] = 0;
            i = 0;
            return true;
        }
        b[0] = b[1] = b[2] = b[3] = 0;
        i = 0;
        return false;
    }
}
