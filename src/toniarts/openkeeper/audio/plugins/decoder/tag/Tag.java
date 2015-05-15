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
package toniarts.openkeeper.audio.plugins.decoder.tag;

import java.util.*;
import toniarts.openkeeper.audio.plugins.decoder.Information;
import toniarts.openkeeper.audio.plugins.decoder.Information;

/**
 * The
 * <code>Tag</code> class provides methods to obtain the content of media tag
 * informations, which describe possible extra informations of a media source.
 * So it's possible to obtain, add and change tag informations with get and put
 * methods. Predefined base hashkeys are defined in this class, but content
 * related hashkeys must be defined inside derived classes. Note that all
 * derived classes of this class are not used within this library itself.
 * Instead media codec plug-ins of this library uses the tag system. Also the
 * tag system can contain several tag formats like IDv1, IDv2 or Ogg Vorbis Tag,
 * but a plug-in system of these different formats are not supported, because
 * the tag system classes are referenced and instanced directly inside the
 * codecs.
 *
 * <p>
 * All tag related values of the hashkeys are <b>not</b> predefined with default
 * values.
 *
 * <p>
 * To create a hypothetical IDv2 tag plug-in it's required to derive this class
 * according to the following listing:
 *
 * <blockquote>
 * <pre>
 * package org.ljmf.audio.codec.tag.id3v2;<p>
 *
 * import java.io.*; import java.util.*; import java.util.zip.*;<p>
 *
 * import org.ljmf.audio.codec.tag.*;<p>
 *
 * public final class MyIDv2Tag extends Tag {<p>
 *
 * private int version;<p>
 *
 * private int tagSize;<p>
 *
 * private myFrameContent frameContent;<p>
 *
 * private TagContent tagContent;<p>
 *
 *     //====================<p>
 *
 * public MyIDv2Tag(InputStream stream) throws IOException, TagException {
 * super();<p>
 *
 * try { readHeader(stream); } catch (TagException e) { close(); return; }<p>
 *
 * put(S_TAG_FORMAT, new String("ID3v2"));<p>
 *
 * myLoadFrames(stream);<p>
 *
 * buildKeyMapTable(); }<p>
 *
 * private void readHeader(InputStream stream) throws TagException, IOException
 * { header = new MyHeader(stream, this); tagSize += header.getSize(); version =
 * header.getVersion(); }<p>
 *
 * private void buildKeyMapTable() { if (version == 2) { //text frameContent
 * put(S_ALBUM, "TAL"); put(S_ARTIST, "TP1");
 *
 *         //====================<p>
 *
 *         //binary frameContent put(BA_EVENT_TIMING_CODES, "ETC");
 * put(BA_LOOKUP_TABLE, "MLL");
 *
 *         //====================<p>
 *
 *         //tag frameContent put(GENERAL_ENCAPSULATED_OBJECT, "GEO"); put(PICTURE,
 * "PIC");
 *
 *         //====================<p>
 *
 * } else {
 *
 *         //====================<p>
 *
 * }
 * }
 *
 * public long correctedBytePosition(long position, boolean seeking) { if
 * (seeking) { position += tagSize; } else { position -= tagSize; }<p>
 *
 * if (position < 0) { position = 0; } }<p>
 *
 * public String toString() { StringBuffer buffer = new StringBuffer(); int
 * counter = 0, counter1 = 0; Object key; Object value;<p>
 *
 * Enumeration ekeys1 = keys(); if (!ekeys1.hasMoreElements()) {
 * buff.append("{}"); return buff.toString(); } for (key = ekeys1.nextElement();
 * ekeys1.hasMoreElements(); key = ekeys1.nextElement()) { value = get(key); if
 * (value != null) { counter++; } }<p>
 *
 * Enumeration ekeys = keys(); buffer.append("{"); for (key =
 * ekeys.nextElement(); ekeys.hasMoreElements(); key = ekeys.nextElement()) {
 * value = get(key); if (value != null) { buffer.append(key+"=");
 * buffer.append(value); counter1++; if (counter1 < counter) { buffer.append(",
 * "); } } }<p>
 *
 * buffer.append("}"); return buffer.toString();
 *     }
 * <p>
 *
 * public Object get(Object key) { Object ob = super.get(key);
 *
 * if (key.equals(S_TAG_FORMAT)) { return ob; }<p>
 *
 *         //====================<p>
 *
 * boolean textContent = myDetermineContentType()<p>
 *
 * if (myIsMixedBinaryAndTextContent) { if (loadTagContent((String) ob,
 * textContent)) { fillTagContent(); return tagContent; } } else if
 * (myIsOnlyBinaryOrOnlyTextContent) { textContent = false;<p>
 *
 * if (((String) key).startsWith("S")) { textContent = true; } if
 * (!loadTagContent((String) ob, textContent)) { return null; } // Only text or
 * binary like content if (textContent) { String s = frameContent.read();<p>
 *
 * tagContent.setContent(s); return tagContent.getTextContent(); } else {
 * tagContent.setContent(encodedData); return tagContent.getBinaryContent(); } }
 * return null; }<p>
 *
 * private void loadTagContent(String key, boolean checkCharEncodingType) {
 * Frame frame = (Frame) myFrames.getElementAt(keys.indexOf(key));<p>
 *
 * encodedData = frame.getContent();<p>
 *
 * if (encodedData == null) { return false; } else { if (tagContent != null) {
 * tagContent.close(); } else { tagContent = new TagContent(); } if
 * (frameContent != null) { frameContent.close(); } frameContent = new
 * myFrameContent(encodedData, myDetermineContentType()); } }<p>
 *
 * private void fillTagContent() {
 *
 *         //====================<p>
 *
 * tagContent.setType(frameContent.read());
 *
 *         //====================<p>
 *
 * tagContent.setSubtype(frameContent.read(1));
 *
 *         //====================<p>
 *
 * tagContent.setDescription(frameContent.read(2));
 *
 *         //====================<p>
 *
 * tagContent.setContent(frameContent.read(3));
 *
 *         //====================<p>
 *
 * }<p>
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
 * @author Michael Scheerer
 */
public abstract class Tag extends Hashtable implements Information {

    /**
     * The predefined tag version key.
     */
    public final static String S_TAG_FORMAT = "String tagFormat";
    /**
     * The predefined album value key.
     */
    public final static String S_ALBUM = "String album";
    /**
     * The predefined artist value key.
     */
    public final static String S_ARTIST = "String artist";
    /**
     * The predefined composer value key.
     */
    public final static String S_COMPOSER = "String composer";
    /**
     * The predefined comment value key.
     */
    public final static String S_COMMENT = "String comment";
    /**
     * The predefined description value key.
     */
    public final static String S_DESCRIPTION = "String description";
    /**
     * The predefined track (number) value key.
     */
    public final static String S_TRACK = "String track";
    /**
     * The predefined year (of publishing) value key.
     */
    public final static String S_YEAR = "String year";
    /**
     * The predefined date (of publishing) value key.
     */
    public final static String S_DATE = "String date";
    /**
     * The predefined title value key.
     */
    public final static String S_TITLE = "String title";
    /**
     * The predefined performer value key.
     */
    public final static String S_PERFORMER = "String performer";
    /**
     * The predefined copyright text value key.
     */
    public final static String S_COPYRIGHT = "String copyrightText";
    /**
     * The predefined copyright web site value key.
     */
    public final static String S_COPYRIGHT_WEBPAGE = "String copyrightWebpage";
    /**
     * The predefined genre text value key.
     */
    public final static String S_GENRE = "String genre";
    /**
     * The predefined isrc (International Standard Recording Code) value key.
     */
    public final static String S_ISRC = "String isrc";
    /**
     * The predefined encoder value key.
     */
    public final static String S_ENCODER = "String encoder";
    /**
     * The predefined Picture tag content value key.
     */
    public final static String PICTURE = "org.ljmf.Information picture";

    /**
     * Creates a new instance. Tag information is completely read the first time
     * it is requested and written after
     * <code>update()</code>.
     */
    public Tag() {
        put(S_TAG_FORMAT, new String(""));
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
    public abstract long correctedBytePosition(long position, boolean seeking);

    /**
     * Frees all system resources, which are bounded to this object.
     */
    public void close() {
        clear();
    }

    /**
     * Returns a
     * <code>Hashtable</code> representation of this
     * <code>Information</code>
     * <code>Object</code>.
     *
     * @return a <code>Hashtable</code> representation of
     * this <code>Information</code> <code>Object</code>
     */
    public Hashtable getHashtable() {
        return (Hashtable) this.clone();
    }

    /**
     * Creates a copy of this hashtable. The keys and values themselves are
     * cloned. This is a relatively expensive operation.
     *
     * @return a clone of this instance
     */
    public Object clone() {
        class BaseTag extends Hashtable implements Information {

            public Hashtable getHashtable() {
                return (Hashtable) this.clone();
            }
        }
        ;

        BaseTag hash = new BaseTag();

        int i = size();

        if (i == 0) {
            return hash;
        }

        Enumeration ekeys = keys();
        Object key, value;

        for (int j = 0; j < i; j++) {
            key = ekeys.nextElement();
            value = get(key);
            if (value != null) {
                if (value instanceof TagContent) {
                    value = ((TagContent) value).clone();
                }
                hash.put(key, value);
            }
        }
        return hash;
    }
}
