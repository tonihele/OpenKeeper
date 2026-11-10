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
 * <code>AudioInformation</code> class acts as a base class of an information
 * container to describe a general audio format.
 * <p>
 *
 * In case of the media type 'not audio', all predefined audio related values of
 * the hashkeys should be removed with a call of the
 * <code>clearAudioContent()</code> method inside the codec plug-in.
 *
 * @author	Michael Scheerer
 */
public class AudioInformation extends MediaInformation implements AudioHashkeys {

    /**
     * The audio format key dependent of the device.
     */
    public final static String S_DEVICE_AUDIO_FORMAT = "String deviceAudioFormat";
    /**
     * The multi channel mode key dependent of the device.
     */
    public final static String S_MULTI_CHANNEL_MODE = "String multiChannelMode";
    /**
     * The device buffer size key of the device. For example the audio device
     * behind any play time counter contains any buffer, so the play time
     * counter must be adjusted.
     */
    public final static String I_DEVICE_BUFFER_SIZE = "Integer deviceBufferSize";
    /**
     * The device device sample rate value key in HZ.
     */
    public final static String I_DEVICE_SAMPLE_RATE = "Integer deviceSampleRate";
    /**
     * The device channel number key dependent of the device.
     */
    public final static String I_DEVICE_CHANNEL_NUMBER = "Integer deviceChannelNumber";
    /**
     * The sample size in bits value key dependent of the media source. The
     * value is presetted to 16.
     */
    public final static String I_SAMPLE_SIZE = "Integer sampleSize";
    /**
     * The sample rate value key in HZ dependent of the media source.
     */
    public final static String I_SAMPLE_RATE = "Integer sampleRate";
    /**
     * The maximum codec sample rate value key in HZ dependent of the media
     * source.
     */
    public final static String I_MAXIMUM_CODEC_SAMPLE_RATE = "Integer maximumCodecSampleRate";
    /**
     * The output buffer size value key dependent of the media source.
     */
    public final static String I_OUTPUT_BUFFER_SIZE = "Integer outputBufferSize";
    /**
     * The frame size in bytes key dependent of the media source.
     */
    public final static String I_FRAME_SIZE = "Integer frameSize";
    /**
     * The frame rate key dependent of the media source.
     */
    public final static String F_FRAME_RATE = "Float frameRate";
    /**
     * The frame number key dependent of the media source.
     */
    public final static String I_FRAME_NUMBER = "Integer frameNumber";
    /**
     * The media channel number key dependent of the media source.
     */
    public final static String I_CHANNEL_NUMBER = "Integer channelNumber";
    /**
     * The big-endian key of the current, internally used, uncompressed pcm
     * audio data endian flag. The big-endian flag is presetted to true.
     */
    public final static String B_BIG_ENDIAN = "Boolean bigEndian";
    /**
     * The signed key of the current, internally used, uncompressed pcm audio
     * data signed flag. The signed flag is presetted to true.
     */
    public final static String B_SIGNED = "Boolean signed";
    /**
     * The vbr value key of the current audio content. This value indicates, if
     * the audio content is variable bitrated.
     */
    public final static String B_VBR_AUDIO = "Boolean vbrAudio";

    /**
     * Constructor for the
     * <code>AudioInformation</code> object
     */
    public AudioInformation() {
        super();
        super.put(S_DEVICE_AUDIO_FORMAT, "PCM signed 16-bit");
        super.put(S_MULTI_CHANNEL_MODE, "Mono");
        super.put(B_VBR_AUDIO, false);
        super.put(B_SIGNED, true); // Defaultvalue
        super.put(B_BIG_ENDIAN, false); // Defaultvalue
        super.put(B_MUTE, false);
        super.put(I_SAMPLE_RATE, -1);
        super.put(I_MAXIMUM_CODEC_SAMPLE_RATE, 44100);
        super.put(I_SAMPLE_SIZE, 16); // Defaultvalue
        super.put(I_DEVICE_SAMPLE_RATE, -1);
        super.put(F_FRAME_RATE, -1f);
        super.put(I_FRAME_SIZE, -1);
        super.put(I_FRAME_NUMBER, -1);
        super.put(I_CHANNEL_NUMBER, -1);
        super.put(I_DEVICE_CHANNEL_NUMBER, -1);
        super.put(I_OUTPUT_BUFFER_SIZE, -1);
        super.put(I_DEVICE_BUFFER_SIZE, -1);
        super.put(FA_CHANNEL_MAPPING, new float[0]);
        super.put(F_BALANCE, -1f);
        super.put(F_GAIN, -1f);
        super.put(F_VOLUME, -1f);
        super.put(FA_EQUALIZE, new float[0]);
    }

    /**
     * Maps the specified
     * <code>key</code> to the specified
     * <code>value</code>. Neither the key nor the value can be
     * <code>null</code>. The value can be obtained by calling the get method
     * with a key that is equal to the original key. The specific value is then
     * used to set decoder or audio device related properties.
     *
     * @param key the hashtable key
     * @param value the value
     * @return the previous value of the specified key in this hashtable, * * *
     * or <code>null</code> if it did not have one
     * @exception NullPointerException if the key or value is <code>null</code>
     */
    @Override
    public Object put(String key, Object value) throws NullPointerException {
        if (key.equals(I_SAMPLE_RATE) && get(I_DEVICE_SAMPLE_RATE) != null && ((Integer) get(I_DEVICE_SAMPLE_RATE)) == -1) {
            super.put(I_DEVICE_SAMPLE_RATE, value);
        }
        if (key.equals(I_CHANNEL_NUMBER) && get(I_DEVICE_CHANNEL_NUMBER) != null && ((Integer) get(I_DEVICE_CHANNEL_NUMBER)) == -1) {
            super.put(I_DEVICE_CHANNEL_NUMBER, value);
        }
        return super.put(key, value);
    }

    /**
     * Clears only the audio related hashtable content.
     */
    protected void clearAudioContent() {
        put(B_AUDIO, false);
        remove(S_MULTI_CHANNEL_MODE);
        remove(B_SIGNED);
        remove(B_VBR_AUDIO);
        remove(S_DEVICE_AUDIO_FORMAT);
        remove(B_BIG_ENDIAN);
        remove(B_MUTE);
        remove(I_SAMPLE_RATE);
        remove(I_DEVICE_SAMPLE_RATE);
        remove(I_SAMPLE_SIZE);
        remove(F_FRAME_RATE);
        remove(I_FRAME_SIZE);
        remove(I_FRAME_NUMBER);
        remove(I_CHANNEL_NUMBER);
        remove(I_DEVICE_CHANNEL_NUMBER);
        remove(I_OUTPUT_BUFFER_SIZE);
        remove(I_DEVICE_BUFFER_SIZE);
        remove(FA_CHANNEL_MAPPING);
        remove(F_BALANCE);
        remove(F_GAIN);
        remove(F_VOLUME);
        remove(FA_EQUALIZE);
        remove(I_MAXIMUM_CODEC_SAMPLE_RATE);
    }
}
