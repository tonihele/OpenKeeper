/* Copyright (C) 2003-2014 Michael Scheerer. All Rights Reserved. */

/*
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package toniarts.openkeeper.audio.plugins.decoder.tag.id3v2;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import toniarts.openkeeper.audio.plugins.decoder.MediaQueue;
import toniarts.openkeeper.audio.plugins.decoder.MpxReader;
import toniarts.openkeeper.audio.plugins.decoder.tag.Tag;
import toniarts.openkeeper.audio.plugins.decoder.tag.TagContent;
import toniarts.openkeeper.audio.plugins.decoder.tag.TagException;
import toniarts.openkeeper.audio.plugins.decoder.tag.id3v1.ID3v1;

/**
 * The
 * <code>ID3v2</code> class is the main class of this IDv2 system.
 * <p>
 * Note that this ID3v2 system requires an ID3v2 tag starting at the first byte
 * of the inputstream, although the ID3v2 specification doesn't demand this.
 *
 * @author Michael Scheerer
 */
public final class ID3v2 extends Tag {

    /**
     * String frameContent keys
     */
    public final static String S_LENGTH_IN_MILLISECONDS = "String lengthInMilliseconds", S_ORIGIN_ARTIST = "String originArtist", S_BPM = "String beatsPerMinute",
            S_LYRICIST = "String lyricist", S_ORIGIN_LYRICIST = "String originLyricist", S_TIME = "String time",
            S_ORIGIN_YEAR = "String originYear",
            S_PLAYLIST_DELAY = "String playlistDelay", S_ORIGIN_TITLE = "String originTitle", S_SUBTITLE = "String subtitle",
            S_CONTENT_GROUP = "String frameContentGroup", S_INITIAL_KEY = "String initialKey", S_LANGUAGE = "String language",
            S_FILE_TYPE = "String fileType", S_FILENAME = "String filename", S_FILE_OWNER = "String fileOwner", S_BAND = "String band", S_MEDIA_TYPE = "String mediaType",
            S_CONDUCTOR = "String conductor", S_REMIXER = "String remixer", S_PART_OF_SET = "String partOfSet", S_PUBLISHER = "String publisher",
            S_INTERNET_RADIOSTATION_NAME = "String internetRadiostationName", S_INTERNET_RADIOSTATION_OWNER = "String internetRadiostationOwner", S_FILESIZE = "String filesize",
            S_AUDIO_WEBPAGE = "String audioWebpage", S_COMMERCIAL_INFO = "String commercialInfo", S_ARTIST_WEBPAGE = "String artistWebpage",
            S_AUDIOSOURCE_WEBPAGE = "String audioSourceWebpage", S_INTERNET_RADIOSTATION_WEBPAGE = "String internetRadiostationWebpage", S_PAYMENT_WEBPAGE = "String paymentWebpage",
            S_PUBLISHER_WEBPAGE = "String publisherWebpage";
    /**
     * Binary frameContent keys
     */
    public final static String BA_EVENT_TIMING_CODES = "byte[] eventTimingCodes", BA_LOOKUP_TABLE = "byte[] lookupTable", BA_SYNCHRONIZED_LYRICS = "byte[] synchronizedLyrics", BA_RELATIVE_VOLUME_ADJUSTMENT = "byte[] relativeVolumeAdjustment",
            BA_EQUALISATION = "byte[] equalisation", BA_REVERB = "byte[] reverb", BA_PLAYCOUNTER = "byte[] playcounter", BA_POPULARIMETER = "byte[] popularimeter", BA_RECOMMENDED_BUFFER_SIZE = "byte[] recommendedBufferSize",
            BA_SYNCHRONIZED_TEMPO_CODES = "byte[] synchronizedTempoCodes", BA_OWNERSHIP = "byte[] ownership", BA_COMMERCIAL = "byte[] commercial", BA_CD_IDENTIFIER = "byte[] cdIdentifier",
            BA_POSITION_SYNCHRONIZATION = "byte[] positionSynchronization";
    /**
     * TagContent frameContent keys
     */
    public final static String PRIVATE_DATA = "org.ljmf.Information privateData", GROUP_IDENTIFICATION_REGISTRATION = "org.ljmf.Information groupIdentificationRegistration",
            ENCRYPTION_METHOD_REGISTRATION = "org.ljmf.Information encryptionMethodRegistration", TERMS_OF_USE = "org.ljmf.Information termsOfUse",
            COMMENTS = "org.ljmf.Information comments",
            UNSYNCHRONIZED_LYRICS = "org.ljmf.Information unsynchronizedLyrics",
            USER_DEFINED_URL = "org.ljmf.Information userDefinedUrl", UNIQUE_FILE_IDENTIFIER = "org.ljmf.Information uniqueFileIdentifier",
            GENERAL_ENCAPSULATED_OBJECT = "org.ljmf.Information generalEncapsulatedObject", USER_DEFINED_TEXT = "org.ljmf.Information userDefinedText";
    private final static short S_TYPE = 1,
            B_TYPE = 2, A_TYPE = 4, S_SUBTYPE = 8, B_SUBTYPE = 16, S_DESCRIPTION = 32,
            B_DESCRIPTION = 64, A_DESCRIPTION = 128, S_CONTENT = 256, B_CONTENT = 512,
            A_CONTENT = 1024;
    private Header header;
    private MediaQueue frames;
    private MediaQueue keys;
    private FrameContent frameContent;
    private TagContent tagContent;
    private byte[] encodedData;
    private int i;
    private boolean checkCharEncodingType;
    private int tagSize;
    private int version;
    private int skippedDataLength;
    private MpxReader info;

    /**
     * Provides access to ID3v2 tag.
     *
     * @param in Input stream to read from. Stream position must be set to
     * beginning of file (i.e. position of ID3v2 tag)
     * @exception IOException if an I/O errors occur
     */
    public ID3v2(InputStream stream, MpxReader info) throws IOException {
        super();
        this.info = info;
        // open file and read tag (if present)
        try {
            readHeader(stream);
            put(S_TAG_FORMAT, ("ID3v2." + version));
            buildKeyMapTable();
            loadFrames(stream);
        } catch (TagException e) {
            // tag error
            close();
            throw new IOException(e.getMessage());
        }
    }

    private void buildKeyMapTable() {
        if (version == 2) {
            // text frameContent
            put(S_ALBUM, "TAL");
            put(S_ARTIST, "TP1");
            put(S_ORIGIN_ARTIST, "TOA");
            put(S_TRACK, "TRK");
            put(S_BPM, "TBP");
            put(S_LYRICIST, "TXT");
            put(S_ORIGIN_LYRICIST, "TOL");
            put(S_ENCODER, "TEN");
            put(S_COMPOSER, "TCM");
            put(S_TIME, "TIM");
            put(S_YEAR, "TYE");
            put(S_ORIGIN_YEAR, "TOR");
            put(S_COPYRIGHT, "TCR");
            put(S_DATE, "TDA");
            put(S_GENRE, "TCO");
            put(S_PLAYLIST_DELAY, "TDY");
            put(S_TITLE, "TT2");
            put(S_ORIGIN_TITLE, "TOT");
            put(S_SUBTITLE, "TT3");
            put(S_CONTENT_GROUP, "TT1");
            put(S_INITIAL_KEY, "TKE");
            put(S_LANGUAGE, "TLA");
            put(S_LENGTH_IN_MILLISECONDS, "TLE");
            put(S_FILE_TYPE, "TFT");
            put(S_FILENAME, "TOF");
            put(S_BAND, "TP2");
            put(S_MEDIA_TYPE, "TMT");
            put(S_CONDUCTOR, "TP3");
            put(S_REMIXER, "TP4");
            put(S_PART_OF_SET, "TPA");
            put(S_PUBLISHER, "TPB");
            put(S_DATE, "TRD");
            put(S_FILESIZE, "TSI");
            put(S_ISRC, "TRC");
            put(S_COPYRIGHT_WEBPAGE, "WCP");
            put(S_AUDIO_WEBPAGE, "WAF");
            put(S_COMMERCIAL_INFO, "WCM");
            put(S_ARTIST_WEBPAGE, "WAR");
            put(S_AUDIOSOURCE_WEBPAGE, "WAS");
            put(S_PUBLISHER_WEBPAGE, "WPB");

            // binary frameContent
            put(BA_EVENT_TIMING_CODES, "ETC");
            put(BA_LOOKUP_TABLE, "MLL");
            put(BA_SYNCHRONIZED_LYRICS, "SLT");
            put(BA_RELATIVE_VOLUME_ADJUSTMENT, "RVA");
            put(BA_EQUALISATION, "EQU");
            put(BA_REVERB, "REV");
            put(BA_PLAYCOUNTER, "CNT");
            put(BA_POPULARIMETER, "POP");
            put(BA_RECOMMENDED_BUFFER_SIZE, "BUF");
            put(BA_SYNCHRONIZED_TEMPO_CODES, "STC");
            put(BA_CD_IDENTIFIER, "MCI");

            // tag frameContent
            put(GENERAL_ENCAPSULATED_OBJECT, "GEO");
            put(PICTURE, "PIC");
            put(COMMENTS, "COM");
            put(UNSYNCHRONIZED_LYRICS, "ULT");
            put(USER_DEFINED_URL, "WXX");
            put(UNIQUE_FILE_IDENTIFIER, "UFI");
            put(USER_DEFINED_TEXT, "TXX");
        } else {
            // text frameContent
            put(S_ALBUM, "TALB");
            put(S_ARTIST, "TPE1");
            put(S_ORIGIN_ARTIST, "TOPE");
            put(S_TRACK, "TRCK");
            put(S_BPM, "TBPM");
            put(S_LYRICIST, "TEXT");
            put(S_ORIGIN_LYRICIST, "TOLY");
            put(S_ENCODER, "TENC");
            put(S_COMPOSER, "TCOM");
            put(S_TIME, "TIME");
            put(S_YEAR, "TYER");
            put(S_ORIGIN_YEAR, "TORY");
            put(S_COPYRIGHT, "TCOP");
            put(S_DATE, "TDAT");
            put(S_GENRE, "TCON");
            put(S_PLAYLIST_DELAY, "TDLY");
            put(S_TITLE, "TIT2");
            put(S_ORIGIN_TITLE, "TOAL");
            put(S_SUBTITLE, "TIT3");
            put(S_CONTENT_GROUP, "TIT1");
            put(S_INITIAL_KEY, "TKEY");
            put(S_LANGUAGE, "TLAN");
            put(S_LENGTH_IN_MILLISECONDS, "TLEN");
            put(S_FILE_TYPE, "TFLT");
            put(S_FILENAME, "TOFN");
            put(S_FILE_OWNER, "TOWN");
            put(S_BAND, "TPE2");
            put(S_MEDIA_TYPE, "TMED");
            put(S_CONDUCTOR, "TPE3");
            put(S_REMIXER, "TPE4");
            put(S_PART_OF_SET, "TPOS");
            put(S_PUBLISHER, "TPUB");
            put(S_DATE, "TRDA");
            put(S_INTERNET_RADIOSTATION_NAME, "TRSN");
            put(S_INTERNET_RADIOSTATION_OWNER, "TRSO");
            put(S_FILESIZE, "TSIZ");
            put(S_ISRC, "TSRC");
            put(S_COPYRIGHT_WEBPAGE, "WCOP");
            put(S_AUDIO_WEBPAGE, "WOAF");
            put(S_COMMERCIAL_INFO, "WCOM");
            put(S_ARTIST_WEBPAGE, "WOAR");
            put(S_AUDIOSOURCE_WEBPAGE, "WOAS");
            put(S_INTERNET_RADIOSTATION_WEBPAGE, "WORS");
            put(S_PAYMENT_WEBPAGE, "WPAY");
            put(S_PUBLISHER_WEBPAGE, "WPUP");

            // binary frameContent
            put(BA_EVENT_TIMING_CODES, "ETCO");
            put(BA_LOOKUP_TABLE, "MLLT");
            put(BA_SYNCHRONIZED_LYRICS, "SYLT");
            put(BA_RELATIVE_VOLUME_ADJUSTMENT, "RVAD");
            put(BA_EQUALISATION, "EQUA");
            put(BA_REVERB, "RVRB");
            put(BA_PLAYCOUNTER, "PCNT");
            put(BA_POPULARIMETER, "POPM");
            put(BA_RECOMMENDED_BUFFER_SIZE, "RBUF");
            put(BA_SYNCHRONIZED_TEMPO_CODES, "SYTC");
            put(BA_OWNERSHIP, "OWNE");
            put(BA_COMMERCIAL, "COMR");
            put(BA_CD_IDENTIFIER, "MCDI");
            put(BA_POSITION_SYNCHRONIZATION, "POSS");

            // tag frameContent
            put(PRIVATE_DATA, "PRIV");
            put(GROUP_IDENTIFICATION_REGISTRATION, "GRID");
            put(ENCRYPTION_METHOD_REGISTRATION, "ENCR");
            put(TERMS_OF_USE, "USER");
            put(GENERAL_ENCAPSULATED_OBJECT, "GEOB");
            put(PICTURE, "APIC");
            put(COMMENTS, "COMM");
            put(UNSYNCHRONIZED_LYRICS, "USLT");
            put(USER_DEFINED_URL, "WXXX");
            put(UNIQUE_FILE_IDENTIFIER, "UFID");
            put(USER_DEFINED_TEXT, "TXXX");
        }
    }

    /**
     * Returns the value to which the specified key is mapped in this hashtable.
     * The values are either control flags or informations stored in the tag.
     *
     * @param key the hashtable key
     * @return the value to which the key is mapped in this hashtable; null if
     * the key is not mapped to any value in this hashtable
     */
    @Override
    public Object get(Object key) {
        Object ob = super.get(key);

        if (key.equals(S_TAG_FORMAT)) {
            return ob;
        }
        computeFlags(key);
        if (i > 0) { // tag (multi binary/text combined) like content
            if (loadTagContent((String) ob, checkCharEncodingType)) {
                fillTagContent();
                return tagContent;
            }
        } else { // Only text or binary like content
            boolean checkCharEncodingType = false;

            if (((String) key).startsWith("S")) {
                checkCharEncodingType = true;
            }
            if (!loadTagContent((String) ob, checkCharEncodingType)) {
                return null;
            }
            // Only text or binary like content
            if (checkCharEncodingType) {
                String s = frameContent.read();

                if (((String) key).endsWith("genre")) {
                    s = convertGenreString(s);
                }
                tagContent.setContent(s);
                return tagContent.getTextContent();
            } else {
                tagContent.setContent(encodedData);
                return tagContent.getBinaryContent();
            }
        }
        return null;
    }

    private boolean loadTagContent(String key, boolean checkCharEncodingType) {
        Frame frm;

        if (frames == null || key == null) {
            return false;
        }

        try {
            frm = (Frame) frames.getElementAt(keys.indexOf(key));
        } catch (Exception e) {
            frm = null;
        }

        if (frm == null) {
            return false;
        }

        encodedData = frm.getContent();

        if (encodedData == null) {
            return false;
        } else {
            if (tagContent != null) {
                tagContent.close();
            } else {
                tagContent = new TagContent();
            }
            if (frameContent != null) {
                frameContent.close();
            }
            frameContent = new FrameContent(encodedData, checkCharEncodingType);
        }
        return true;
    }

    private void fillTagContent() {
        if ((i & S_TYPE) == S_TYPE) {
            if ((i & A_TYPE) == A_TYPE) {
                tagContent.setType(frameContent.read(false));
            } else {
                tagContent.setType(frameContent.read());
            }
        }
        if ((i & B_TYPE) == B_TYPE) {
            if ((i & A_TYPE) == A_TYPE) {
                try {
                    tagContent.setType(frameContent.read(3, false));
                } catch (UnsupportedEncodingException e) {
                    tagContent.setType(new String(frameContent.read(3)));
                }
            } else {
                tagContent.setType(new String(frameContent.read(3)));
            }
        }
        if ((i & S_SUBTYPE) == S_SUBTYPE) {
            tagContent.setSubtype(frameContent.read());
        }
        if ((i & B_SUBTYPE) == B_SUBTYPE) {
            tagContent.setSubtype(frameContent.read(1));
        }
        if ((i & S_DESCRIPTION) == S_DESCRIPTION) {
            tagContent.setDescription(frameContent.read());
        }
        if ((i & S_CONTENT) == S_CONTENT) {
            if ((i & A_CONTENT) == A_CONTENT) {
                tagContent.setContent(frameContent.read(false));
            } else {
                tagContent.setContent(frameContent.read());
            }
        }
        if ((i & B_CONTENT) == B_CONTENT) {
            tagContent.setContent(frameContent.read(-1));
        }
    }

    private void computeFlags(Object key) {
        i = 0;
        checkCharEncodingType = true;
        if (key.equals(UNIQUE_FILE_IDENTIFIER)) {
            checkCharEncodingType = false;
            i = S_DESCRIPTION | B_CONTENT;
        }
        if (key.equals(USER_DEFINED_TEXT)) {
            i = S_DESCRIPTION | S_CONTENT;
        }
        if (key.equals(USER_DEFINED_URL)) {
            i = S_DESCRIPTION | S_CONTENT | A_CONTENT;
        }
        if (key.equals(UNSYNCHRONIZED_LYRICS) || key.equals(COMMENTS)) {
            i = B_TYPE | A_TYPE | S_DESCRIPTION | S_CONTENT;
        }
        if (key.equals(PICTURE)) {
            if (version > 2) {
                i = S_TYPE | A_TYPE | B_SUBTYPE | S_DESCRIPTION | B_CONTENT;
            } else {
                i = B_TYPE | A_TYPE | B_SUBTYPE | S_DESCRIPTION | B_CONTENT;
            }
        }
        if (key.equals(GENERAL_ENCAPSULATED_OBJECT)) {
            i = S_TYPE | A_TYPE | S_SUBTYPE | S_DESCRIPTION | B_CONTENT;
        }
        if (key.equals(TERMS_OF_USE)) {
            i = B_TYPE | A_TYPE | S_CONTENT;
        }
        if (key.equals(ENCRYPTION_METHOD_REGISTRATION) || key.equals(GROUP_IDENTIFICATION_REGISTRATION)) {
            checkCharEncodingType = false;
            i = S_TYPE | B_SUBTYPE | B_CONTENT;
        }
        if (key.equals(PRIVATE_DATA)) {
            checkCharEncodingType = false;
            i = S_TYPE | B_CONTENT;
        }
    }

    // replaces 11111111000000000 (unsynchronized bitpattern) by
    // 11111111          (synchronized bitpattern)
    static byte[] synchronize(byte[] bin) {
        int l = bin.length;

        for (int i = 0; i < bin.length - 1; i++) {
            if (bin[i] == 0xFF && bin[i + 1] == 0x0) {
                l--;
            }
        }
        if (l == bin.length) {
            return bin;
        }
        byte bout[] = new byte[l];

        int readPos = 0;

        for (int i = 0; i < bout.length; i++) {
            if (bin[i] == 0xFF && bin[i + 1] == 0x0) {
                bout[readPos++] = (byte) 0xFF;
                i++;
            } else {
                bout[readPos++] = bin[i];
            }
        }
        return bout;
    }

    /**
     * Obtains a String describing all information keys and values. With the
     * hashkey of a specific information it is possible to obtain the
     * information value.
     *
     * @return a String representation all informations
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }

        boolean first = true;
        StringBuilder buff = new StringBuilder();
        buff.append("{");
        for (Map.Entry<String, Object> entry : entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                if (!first) {
                    buff.append(", ");
                    first = false;
                }
                buff.append(entry.getKey()).append("=");
                buff.append(value);
            }
        }
        buff.append("}");
        return buff.toString();
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
    @Override
    public long correctedBytePosition(long position, boolean seeking) {

        if (seeking) {
            position += skippedDataLength;
        } else {
            position -= skippedDataLength;
        }

        if (position < 0) {
            position = 0;
        }

        return position;
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        int i = 0;

        header = null;

        if (keys != null) {
            keys.close();
        }
        keys = null;
        if (frames != null) {
            for (; i < frames.size(); i++) {
                ((Frame) frames.getElementAt(i)).close();
            }
            frames.close();
        }
        frames = null;
        if (tagContent != null) {
            tagContent.close();
        }
        tagContent = null;
        if (frameContent != null) {
            frameContent.close();
        }
        frameContent = null;
        super.close();
    }

    /**
     * Sets the data size until first audio frame and a flag indicating if
     * skippedDataLength was setted by the tag in case of an error.
     *
     * @param skippedDataLength the skipped data length
     */
    public void setSkippedDataLength(int skippedDataLength) {
        if (skippedDataLength > 0) {
            this.skippedDataLength += skippedDataLength;
        }
    }

    private void readHeader(InputStream stream) throws TagException, IOException {
        header = new Header(stream, this);
        skippedDataLength += header.getSkippedDataLength();
        version = header.getVersion();
    }

    private String convertGenreString(String s) {
        boolean started = false;
        char[] c = s.toCharArray();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < c.length; i++) {
            int ci = (int) c[i];
            boolean isNumber = ci >= 48 && ci <= 57; // number must be >= 48 and <= 57

            if (!isNumber && started) {
                break;
            }
            if (isNumber) {
                started = true;
                sb.append(c[i]);
            }
        }

        if (!started) {
            return s;
        }

        int index = -1;

        try {
            index = Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
        }
        if (index < 0 || index >= ID3v1.GENRE.length) {
            return "Unknown";
        }
        return ID3v1.GENRE[index];
    }

    private void loadFrames(InputStream stream) throws IOException, TagException {
        tagSize = header.getTagSize();

        int grossLength = 0;

        int frameLength;

        // read bytes
        byte[] unsynchronizedData = new byte[tagSize];

        stream.read(unsynchronizedData);

        byte[] synchronizedData;

        if (header.isUnsynchronizated()) {
            synchronizedData = synchronize(unsynchronizedData);
        } else {
            synchronizedData = unsynchronizedData;
        }
        if (header.isProtected()) {
            CRC32 crc = new CRC32();

            crc.update(synchronizedData);

            int checksum = (int) crc.getValue();

            if (checksum != header.getCrc()) {
                stream.skip(tagSize);
            }
        }

        frames = new MediaQueue();
        keys = new MediaQueue();

        ByteArrayInputStream bstream = new ByteArrayInputStream(synchronizedData);
        Frame frame = null;

        try {
            frame = new Frame(bstream, this, version);
        } catch (Exception e) {
            info.setSkippedDataLength(tagSize);
            throw new TagException("Frame damaged");
        }

        Object key;

        frameLength = frame.getFrameLength();
        key = frame.getShortKey();

        if (key != null) { // In case of completely damaged ID3 Tags => the normal case!!
            int diff = tagSize;

            if (frameLength > diff && (key.equals("APIC") || key.equals("PIC"))) {

                diff -= frame.getHeaderLength();
                if (diff < 0) {
                    diff = 0;
                }
                stream.read(frame.getContent(), diff, frame.getContent().length - diff);

                if (header.isUnsynchronizated()) {
                    frame.setContent(synchronize(frame.getContent()));
                }
            }
            grossLength = frameLength;
            skippedDataLength += frameLength;
            keys.addElement(key);
            frames.addElement(frame);
        } else {
            info.setSkippedDataLength(tagSize);
            throw new TagException("Frame damaged");
        }

        while (bstream.available() > 0) {
            try {
                frame = new Frame(bstream, this, version);
            } catch (Exception e) {
                info.setSkippedDataLength(tagSize);
                throw new TagException("Frame damaged");
            }
            key = frame.getShortKey();

            frameLength = frame.getFrameLength();

            if (key != null) { // In case of completely damaged ID3 Tags => the normal case!!
                int diff = tagSize - grossLength;

                if (frameLength > diff && (key.equals("APIC") || key.equals("PIC"))) {

                    diff -= frame.getHeaderLength();
                    stream.read(frame.getContent(), diff, frame.getContent().length - diff);

                    if (header.isUnsynchronizated()) {
                        frame.setContent(synchronize(frame.getContent()));
                    }
                }
                grossLength += frameLength;
                skippedDataLength += frameLength;
            } else {
                skippedDataLength = tagSize;
                return;
            }

            int index = keys.indexOf(key);

            if (index == -1) {
                keys.addElement(key);
                frames.addElement(frame);
            }
        }
    }
}
