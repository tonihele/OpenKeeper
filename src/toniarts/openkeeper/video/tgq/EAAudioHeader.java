/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.video.tgq;

/**
 * Common audio header for one EA audio stream
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EAAudioHeader {

    public enum Platform {

        PC, MACINTOSH, PLAYSTATION_2, GAME_CUBE, XBOX, XENON_XBOX_360, PSP, UNKNOWN
    }

    public enum Compression {

        PCM_16_I_LE("PCM Signed 16-bit Interleaved (LE)"),
        PCM_16_I_BE("PCM Signed 16-bit Interleaved (BE)"),
        PCM_8_I("PCM Signed 8-bit Interleaved"),
        MICRO_TALK_10_1("MicroTalk 10:1"),
        VAG_ADPCM("VAG ADPCM"),
        PCM_16_P_BE("PCM Signed 16-bit Planar (BE)"),
        PCM_16_P_LE("PCM Signed 16-bit Planar (LE)"),
        PCM_8_P("PCM Signed 8-bit Planar"),
        EA_XA_ADPCM("EA XA ADPCM"),
        PCM_U8_I("PCM Unsigned 8-bit Interleaved"),
        CD_XA("CD XA"),
        MP1("MP3 Layer 1"),
        MP2("MP3 Layer 2"),
        MP3("MP3 Layer 3"),
        GAME_CUBE_ADPCM("Game Cube ADPCM"),
        PCM_24_I_LE("PCM Signed 24-bit Interleaved (LE)"),
        XBOX_ADPCM("XBOX ADPCM"),
        PCM_24_I_BE("PCM Signed 24-bit Interleaved (BE)"),
        MICRO_TALK_5_1("MicroTalk 5:1"),
        EA_LAYER_3("EALayer3"),
        UNKNOWN("Uknown");

        private Compression(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
        private final String description;
    }
    private Platform platform;
    private Compression compression;
    private int bitsPerSample;
    private int sampleRate;
    private int numberOfChannels;
    private int numberOfSamplesInStream;

    public Platform getPlatform() {
        return platform;
    }

    protected void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Compression getCompression() {
        return compression;
    }

    protected void setCompression(Compression compression) {
        this.compression = compression;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    protected void setBitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    protected void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    protected void setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = numberOfChannels;
    }

    public int getNumberOfSamplesInStream() {
        return numberOfSamplesInStream;
    }

    protected void setNumberOfSamplesInStream(int numberOfSamplesInStream) {
        this.numberOfSamplesInStream = numberOfSamplesInStream;
    }
}
