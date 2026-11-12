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

/**
 * The
 * <code>AudioHashkeys</code> interface contains keys, which are used by the
 * <code>AudioInformation</code> class and the
 * <code>AudioControl</code> class.
 *
 * @author	Michael Scheerer
 */
public interface AudioHashkeys {

    /**
     * The predefined analyze mode string for frequency to amplitude audio
     * analyze data.
     */
    public final static String FREQUENCY = "Frequency";
    /**
     * The predefined analyze mode string for time to amplitude audio analyze
     * data.
     */
    public final static String TIME = "Time";
    /**
     * The audio data analyze mode flag key of the current media source. The
     * flag forces to enable a spezific analyze mode to examine media data.
     * Examples are spectrum to amplitude and time to amplitude audio data
     * analyze.
     */
    public final static String S_AUDIO_DATA_ANALYZE_MODE = "String audioDataAnalyzeMode";
    /**
     * The predefined gain value key. The value range is [0.0, 1.0].
     */
    public final static String F_GAIN = "Float gain";
    /**
     * The predefined volume value key. The value range is [0.0, 1.0].
     */
    public final static String F_VOLUME = "Float volume";
    /**
     * The predefined pan value key. The value range is [-1.0, 1.0].
     */
    public final static String F_PAN = "Float pan";
    /**
     * The predefined balance value key. The value range is [-1.0, 1.0].
     */
    public final static String F_BALANCE = "Float balance";
    /**
     * The predefined mute value key.
     */
    public final static String B_MUTE = "Boolean mute";
    /**
     * The predefined equalize value array key. The value range of one array
     * value is [0, Inf.). A value greater than 1 amplifies a frequency band of
     * a signal, otherwise it attenuates the signal within a frequency band.
     */
    public final static String FA_EQUALIZE = "float[] equalize";
    /**
     * The predefined channel mapping array value key. This array is a
     * derivation of a matrix in the sense:
     * <p>
     * int[] array = { matrix[0][0], matrix[0][float value], ...... matrix[float
     * value][0], matrix[float value][float value], ...... };
     * <p>
     * The (float) value range of one array value is [0.0, 1.0]. A channel
     * mapping matrix contains the assignments of the decoder supported multi
     * channels to the input channels of the audio device. A channel may be a
     * subwoofer channel inside a surround channel configuration.
     * <p>
     * Some examples:
     * <p>
     * Following matrix shows a stereo configuration with the audio device
     * running 2 reserved audio channels and a media content supporting stereo:
     * <p>
     * <code>public final static int STEREO[][] = { 1, 0, 0, 1 };</code>
     * <p>
     * Following matrix shows a inverted stereo configuration (swapping the left
     * channel with the rigth one) with the audio device running 2 reserved
     * audio channels and a media content supporting stereo:
     * <p>
     * <code>public final static int INVERTED[][] = { 0, 1, 1, 0 };</code>
     * <p>
     * Following matrix shows a left channel configuration with the audio device
     * running 1 reserved audio channels and a media content supporting dual
     * (language) channel content:
     * <p>
     * <code>public final static int LEFT_CHANNEL[][] = { 1, 0 };</code>
     * <p>
     * Following matrix shows a rigth channel configuration with the audio
     * device running 1 reserved audio channels and a media content supporting
     * dual (language) channel content:
     * <p>
     * <code>public final static int RIGTH_CHANNEL[][] = { 0, 1 };</code>
     * <p>
     * Following matrix shows a downmix configuration with the audio device
     * running 1 reserved audio channels and a media content supporting stereo:
     * <p>
     * <code>public final static int DOWNMIX[][] = { 1, 1 };</code>
     * <p>
     * Following matrix shows a stereo configuration with the audio device
     * running 2 reserved audio channels and a media content supporting stereo.
     * One channel has a volume value of 0.53 and the other channel contains a
     * value of 0.23.
     * <p>
     * <code>public final static int STEREO[][] = { 0.53, 0 , 0, 0.23 };</code>
     */
    public final static String FA_CHANNEL_MAPPING = "float[] channelMapping";
}
