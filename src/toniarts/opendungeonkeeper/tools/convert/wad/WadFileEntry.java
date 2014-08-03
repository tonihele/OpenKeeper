/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.wad;

/**
 * Stores the wad file entry structure<br>
 * Converted to JAVA from C code, C code by anonymous
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WadFileEntry {

    private int unk1;
    private int nameOffset;
    private int nameSize;
    private int offset;
    private int compressedSize;
    private int type;
    private int size;
    private int[] unknown2 = new int[3];

    public int getUnk1() {
        return unk1;
    }

    public void setUnk1(int unk1) {
        this.unk1 = unk1;
    }

    public int getNameOffset() {
        return nameOffset;
    }

    public void setNameOffset(int nameOffset) {
        this.nameOffset = nameOffset;
    }

    public int getNameSize() {
        return nameSize;
    }

    public void setNameSize(int nameSize) {
        this.nameSize = nameSize;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(int compressedSize) {
        this.compressedSize = compressedSize;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int[] getUnknown2() {
        return unknown2;
    }

    public void setUnknown2(int[] unknown2) {
        this.unknown2 = unknown2;
    }

    /**
     * Is this file entry compressed or not
     *
     * @return true if the file entry is compressed
     */
    boolean isCompressed() {

        //Or figure out from the type? Type 4?
        return size != 0;
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
