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

/**
 * The
 * <code>TagContent</code> class contains the information related to a tag and
 * is designed to be as flexible as possible to reduce the number of to handled
 * cases when the tag is rather more structured. <p>
 *
 * It provides storage for - a type (for example a MIME-type or a language,
 * Text) - a subtype (text or binary) - a description (text) - the content (text
 * or binary).
 *
 * @author Michael Scheerer
 */
public final class TagContent extends HashMap<String, Object> implements Information {

    /**
     * String content keys
     */
    public final static String S_TYPE = "String type", S_SUBTYPE_TEXT = "String subtypeText",
            S_DESCRIPTION = "String description", S_CONTENT_TEXT = "String contentText";
    /**
     * Binary content keys
     */
    public final static String BA_SUBTYPE_BINARY = "byte[] subtypeBinary", BA_CONTENT_BINARY = "byte[] contentBinary";

    /**
     * Constructs a new
     * <code>TagContent</code> with the specified contents.
     *
     * @param m existing mappings
     */
    public TagContent(Map<String, Object> m) {
        super(m);
    }

    /**
     * Constructs a new, empty hashtable. A default capacity and load factor is
     * used. Note that the hashtable will automatically grow when it gets full.
     */
    public TagContent() {
        super();
    }

    /**
     * Sets the type of the tag as a string representation. This could be for
     * example the MIME-type of an "org.ljmf.Information picture"
     * <code>Object</code>.
     *
     * @param type the type of the tag as a string representation
     */
    public void setType(String type) {
        if (type == null) {
            return;
        }
        put(S_TYPE, type);
    }

    /**
     * Gets the type of the tag as a string representation. This could be for
     * example the MIME-type of an "org.ljmf.Information picture"
     * <code>Object</code>.
     *
     * @return the type of the tag as a string representation
     */
    public String getType() {
        return (String) get(S_TYPE);
    }

    /**
     * Sets the sub type of the tag as a text representation. This could be for
     * example the file name of an "org.ljmf.Information
     * generalEncapsulatedObject"
     * <code>Object</code>. This kind of meta information describes a file. The
     * type in this example is the MIME-typ, so the type together with the sub
     * type is a complete definition of this object.
     *
     * @param type the sub type of the tag as a text representation
     */
    public void setSubtype(String subtype) {
        if (subtype == null) {
            return;
        }
        put(S_SUBTYPE_TEXT, subtype);
    }

    /**
     * Sets the sub type of the tag as a binary representation. This could be
     * for example the binary coding of the picture content type of an
     * "org.ljmf.Information picture"
     * <code>Object</code>, which describes a coverart. The type in this example
     * is the MIME-typ, so the type together with the sub type is a complete
     * definition of this object.
     *
     * @param type the sub type of the tag as a binary representation
     */
    public void setSubtype(byte[] subtype) {
        if (subtype == null) {
            return;
        }
        put(BA_SUBTYPE_BINARY, subtype);
    }

    /**
     * Gets the text represented sub type of the tag. This could be for example
     * the file name of an "org.ljmf.Information generalEncapsulatedObject"
     * <code>Object</code>. This kind of meta information describes a file. The
     * type in this example is the MIME-typ, so the type together with the sub
     * type is a complete definition of this object.
     *
     * @return the sub type of the tag as a text representation
     */
    public String getTextSubtype() {
        return (String) get(S_SUBTYPE_TEXT);
    }

    /**
     * Gets the binary represented sub type of the tag. This could be for
     * example the binary coding of the picture content type of an
     * "org.ljmf.Information picture"
     * <code>Object</code>, which describes a coverart. The type in this example
     * is the MIME-typ, so the type together with the sub type is a complete
     * definition of this object.
     *
     * @return the sub type of the tag as a binary representation
     */
    public byte[] getBinarySubtype() {
        return (byte[]) get(BA_SUBTYPE_BINARY);
    }

    /**
     * Sets the description of the tag as a text representation. This could be
     * for example the describtion of a coverart. represented by an
     * "org.ljmf.Information picture"
     * <code>Object</code>.
     *
     * @param type the description of the tag as a text representation
     */
    public void setDescription(String desc) {
        if (desc == null) {
            return;
        }
        put(S_DESCRIPTION, desc);
    }

    /**
     * Gets the description of the tag as a text representation. This could be
     * for example the describtion of a coverart. represented by an
     * "org.ljmf.Information picture"
     * <code>Object</code>.
     *
     * @return the description of the tag as a text representation
     */
    public String getDescription() {
        return (String) get(S_DESCRIPTION);
    }

    /**
     * Sets the content of the tag as a text representation. This could be for
     * example the lyric text represented by an "org.ljmf.Information
     * unsynchronizedLyrics"
     * <code>Object</code>.
     *
     * @param type the content of the tag as a text representation
     */
    public void setContent(String content) {
        if (content == null) {
            return;
        }
        put(S_CONTENT_TEXT, content);
    }

    /**
     * Sets the content of the tag as a binary representation. This could be for
     * example image data of a coverart represented by an "org.ljmf.Information
     * picture"
     * <code>Object</code>.
     *
     * @param type the content of the tag as a binary representation
     */
    public void setContent(byte[] content) {
        if (content == null) {
            return;
        }
        put(BA_CONTENT_BINARY, content);
    }

    /**
     * Gets the the text represented content of the tag. This could be for
     * example the lyric text represented by an "org.ljmf.Information
     * unsynchronizedLyrics"
     * <code>Object</code>.
     *
     * @param type the content of the tag as a text representation
     */
    public String getTextContent() {
        return (String) get(S_CONTENT_TEXT);
    }

    /**
     * Gets the binary represented content of the tag. This could be for example
     * image data of a coverart represented by an "org.ljmf.Information picture"
     * <code>Object</code>.
     *
     * @return the content of the tag as a binary representation
     */
    public byte[] getBinaryContent() {
        return (byte[]) get(BA_CONTENT_BINARY);
    }

    /**
     * Creates a copy of this object. This is a relatively expensive operation.
     *
     * @return a clone of this instance
     */
    @Override
    public Object clone() {
        return new TagContent(this);
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
    @Override
    public Map<String, Object> getHashtable() {
        return (Map<String, Object>) this.clone();
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    public void close() {
        clear();
    }
}
