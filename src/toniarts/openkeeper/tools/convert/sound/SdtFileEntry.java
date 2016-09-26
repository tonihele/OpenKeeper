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
package toniarts.openkeeper.tools.convert.sound;

import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * Stores the sdt file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SdtFileEntry {

    public enum SoundType implements IValueEnum {

        NONE(0),
        WAV(2),
        MP2_MONO(36),
        MP2_STEREO(37);

        private final int type;

        private SoundType(int type) {
            this.type = type;
        }

        @Override
        public int getValue() {
            return this.type;
        }
    }

    private int headerSize; // dataSize of header data include this field (exclude data field)
    private int dataSize;
    private String name;
    private int sampleRate; // 22050
    private short bitsPerSample; // 16 bits
    private SoundType type; // 36 on mp2 (64kbit/s mono), 37 on mp2 (112kbit/s stereo), 2 on wav, 0 on blanks
    private int unknown3; // 0 in all files. Compression, byteRate, blockAlign
    private int nSamples;
    private int unknown4; // 0 in all files. DataStart, LoopOffset, LoopLength

    private long dataOffset; // not contains in file structure

    public int getHeaderSize() {
        return headerSize;
    }

    protected void setHeaderSize(int headerSize) {
        this.headerSize = headerSize;
    }

    public int getDataSize() {
        return dataSize;
    }

    protected void setDataSize(int size) {
        this.dataSize = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    protected void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Resolution of sound data in bits
     *
     * @return
     */
    public short getBitsPerSample() {
        return bitsPerSample;
    }

    protected void setBitsPerSample(short bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public SoundType getType() {
        return type;
    }

    public void setType(SoundType type) {
        this.type = type;
    }

    public int getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(int unknown3) {
        this.unknown3 = unknown3;
    }

    public int getnSamples() {
        return nSamples;
    }

    protected void setnSamples(int nSamples) {
        this.nSamples = nSamples;
    }

    public int getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(int unknown4) {
        this.unknown4 = unknown4;
    }

    public long getDataOffset() {
        return dataOffset;
    }

    protected void setDataOffset(long dataOffset) {
        this.dataOffset = dataOffset;
    }
}
