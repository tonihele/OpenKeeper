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

        private int type;

        private SoundType(int type) {
            this.type = type;
        }
        
        @Override
        public int getValue() {
            return this.type;
        }
    } 
    
    private int indexSize;
    private int size;
    private int sampling_rate; // 22050
    private int unknown2; // 16
    private SoundType type; // 36 on mp2 (64kbit/s mono), 37 on mp2 (112kbit/s stereo), 2 on wav, 0 on blanks
    private int unknown3;
    private int nSamples;
    private int unknown4;
    private long dataOffset;

    public int getIndexSize() {
        return indexSize;
    }

    protected void setIndexSize(int indexSize) {
        this.indexSize = indexSize;
    }

    public int getSize() {
        return size;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    public int getSamplingRate() {
        return sampling_rate;
    }

    protected void setSamplingRate(int samplingRate) {
        this.sampling_rate = samplingRate;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
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
