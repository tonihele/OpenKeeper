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
package toniarts.openkeeper.tools.convert.wad;

/**
 * Stores the wad file entry structure<br>
 * Converted to JAVA from C code, C code by anonymous
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WadFileEntry {

    public enum WadFileEntryType {

        NOT_COMPRESSED, // 0
        COMPRESSED, // 4
        UNKOWN;
    }
    private int unk1;
    private int nameOffset;
    private int nameSize;
    private int offset;
    private int compressedSize;
    private WadFileEntryType type;
    private int size; // Uncompressed
    private int[] unknown2 = new int[3];

    public int getUnk1() {
        return unk1;
    }

    protected void setUnk1(int unk1) {
        this.unk1 = unk1;
    }

    public int getNameOffset() {
        return nameOffset;
    }

    protected void setNameOffset(int nameOffset) {
        this.nameOffset = nameOffset;
    }

    public int getNameSize() {
        return nameSize;
    }

    protected void setNameSize(int nameSize) {
        this.nameSize = nameSize;
    }

    public int getOffset() {
        return offset;
    }

    protected void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCompressedSize() {
        return compressedSize;
    }

    protected void setCompressedSize(int compressedSize) {
        this.compressedSize = compressedSize;
    }

    public WadFileEntryType getType() {
        return type;
    }

    protected void setType(WadFileEntryType type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    public int[] getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int[] unknown2) {
        this.unknown2 = unknown2;
    }

    /**
     * Is this file entry compressed or not
     *
     * @return true if the file entry is compressed
     */
    boolean isCompressed() {
        return (type == WadFileEntryType.COMPRESSED);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.offset;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WadFileEntry other = (WadFileEntry) obj;
        if (this.offset != other.offset) {
            return false;
        }
        return true;
    }
}
