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

import java.util.*;

/**
 * The
 * <code>MediaInformation</code> class acts as an information container to
 * describe any possible media format. So it's possible to obtain, add and
 * change tag informations with get and put methods with predefined hashkeys.
 * Common base hashkeys are defined in this class, but audio, video, codec and
 * tag related hashkeys must be defined inside derived classes. The interface
 * <code>Hashkeys</code> contain predefined keys, which are used by both, the
 * <code>MediaInformation</code> class and the
 * <code>MediaControl</code> class.
 * <p>
 *
 * Note that the stored informations are only available after the
 * decoder/demultiplexer initialization. That means inside a
 * decoder/demultiplexer constructor it's impossible to obtain bitrate or
 * playtime informations unless they are directly created by the decoder
 * plug-in.
 * <p>
 *
 * In case of the media type 'video', all video related values accessible by the
 * hashkeys are predefined with default values. In case of the media type
 * 'audio', all audio related values accessible by the hashkeys are predefined
 * with default values. In case of the media type 'tag', all tag related values
 * accessible by the hashkeys are <b>not</b> predefined with default values and
 * the tag objects itself are <b>not</b> preinitialized but will be deleted by
 * the framework. All codec related values of the hashkeys are <b>not</b>
 * predefined with default values. They must be defined inside the application,
 * which uses this framework.
 *
 * @author	Michael Scheerer
 */
public class MediaInformation extends Hashtable implements Hashkeys, Information {

    /**
     * The media type key. A media type string should contain a senseful word to
     * distinguish various kinds of media types. Predefined examples: "Audio" or
     * "Video".
     */
    public final static String S_MEDIA_TYPE = "String mediaType";
    /**
     * The media format key.
     */
    public final static String S_MEDIA_FORMAT = "String mediaFormat";
    /**
     * The media file format key.
     */
    public final static String S_MEDIA_FILE_FORMAT = "String mediaFileFormat";
    /**
     * The audio value key of the current media source. This value indicates, if
     * audio content is part of the media.
     */
    public final static String B_AUDIO = "Boolean audio";
    /**
     * The video value key of the current media source. This value indicates, if
     * video content is part of the media.
     */
    public final static String B_VIDEO = "Boolean video";
    /**
     * The system value key of the current media source. This value indicates,
     * if the media source is multiplexed.
     */
    public final static String B_SYSTEM = "Boolean system";
    /**
     * The origin value key of the current media source. This value indicates,
     * if the media source is original.
     */
    public final static String B_ORIGINAL = "Boolean original";
    /**
     * The copyright value key of the current media source. This value
     * indicates, if the media source is copyrighted.
     */
    public final static String B_COPYRIGHT = "Boolean copyright";
    /**
     * The global bit rate key of the current media source.
     */
    public final static String I_BITRATE = "Integer bitrate";
    /**
     * The net length in bytes key of the current media source.
     */
    public final static String L_BYTE_LENGTH = "Long byteLength";
    /**
     * The gross length in bytes key of the current media source.
     */
    public final static String L_GROSS_BYTE_LENGTH = "Long grossByteLength";
    /**
     * The length in microseconds key of the current media source.
     */
    public final static String L_MICROSECONDS = "Long microseconds";
    /**
     * The byte offset key used to determine the playtime length in microseconds
     * to avoid parsing the whole source from the beginning on. This key is only
     * used, if this framework is running in streaming mode. Some codecs don't
     * specify playtime length information capabilities under some conditions.
     * In this case the offset value of the key is always negative.
     */
    public final String L_MICROSECONDS_DETECTION_BYTE_OFFSET = "Long microsecondsDetectionByteOffset";
    /**
     * The tag key of the current media source.
     */
    public final static String TAG = "org.ljmf.Information tag";
    // The <code>MediaControl</code> object encapsulats all decoding and
    // playback presettings.
//    private static MediaControl preConfiguration = new MediaControl(null, null);

    /**
     * Constructs an instance of
     * <code>MediaInformation</code> performing presettings.
     */
    protected MediaInformation() {
        put(B_AUDIO, false);
        put(B_BIT_PROTECTION, false);
        put(B_VIDEO, false);
        put(B_SYSTEM, false);
        put(B_FAST_SEEKING, false);
        put(I_BITRATE, new Integer(-1));
        put(L_GROSS_BYTE_LENGTH, new Long(-1));
        put(L_BYTE_LENGTH, new Long(-1));
        put(L_MICROSECONDS, new Long(-1));
        put(L_MICROSECONDS_DETECTION_BYTE_OFFSET, new Long(-1));
        put(S_MEDIA_TYPE, "");
        put(S_MEDIA_FORMAT, "");
        put(S_MEDIA_FILE_FORMAT, "");
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    public void close() {
        if (get(TAG) != null) {
            ((Hashtable) get(TAG)).clear();
        }
        clear();
    }

    /**
     * Returns the
     * <code>MediaControl</code> object encapsulating all relevant decoding and
     * playback presettings.
     *
     * @return the <code>MediaControl</code> object encapsulating all relevant
     * decoding and playback presettings
     */
//    public final static MediaControl getPreConfiguration() {
//        return preConfiguration;
//    }
    /**
     * Embed an
     * <code>MediaInformation</code> object into another.
     * <p>
     * Note that all keys and values to embed are not cloned.
     *
     * @param information the <code>MediaInformation</code> object which
     * inherits its own content to this target <code>Information</code> object
     */
    public final void embed(MediaInformation information) {
        if (information != null) {
            int i = information.size();
            Enumeration ekeys = information.keys();
            Enumeration evalues = information.elements();

            for (int j = 0; j < i; j++) {
                put(ekeys.nextElement(), evalues.nextElement());
            }
        }
    }

    /**
     * Returns a
     * <code>Hashtable</code> representation of this
     * <code>Information</code> object.
     *
     * @return a <code>Hashtable</code> representation of *
     * this <code>Information</code> object
     */
    public final Hashtable getHashtable() {
        return (Hashtable) this.clone();
    }

    /**
     * Creates a shallow copy of this hashtable. The keys and values themselves
     * are cloned. This is a relatively expensive operation.
     *
     * @return a clone of this instance
     */
    public Object clone() {
        MediaInformation hash = new MediaInformation();

        int i = size();

        Enumeration ekeys = keys();
        Enumeration evalues = elements();
        Object value;

        for (int j = 0; j < i; j++) {
            value = evalues.nextElement();
            if (value instanceof Information) {
                hash.put(ekeys.nextElement(), ((Information) value).getHashtable());
            } else {
                hash.put(ekeys.nextElement(), value);
            }
        }
        return hash;
    }
}
