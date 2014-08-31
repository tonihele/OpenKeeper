/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.sdt;

/**
 * Stores the sdt file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SdtFileEntry {

    private int indexSize;
    private int size;
    private int unknown1;
    private int unknown2;
    private int unknown3;
    private int unknown4;
    private long dataOffset;

    protected int getIndexSize() {
        return indexSize;
    }

    protected void setIndexSize(int indexSize) {
        this.indexSize = indexSize;
    }

    protected int getSize() {
        return size;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    protected int getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(int unknown1) {
        this.unknown1 = unknown1;
    }

    protected int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    protected int getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(int unknown3) {
        this.unknown3 = unknown3;
    }

    protected int getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(int unknown4) {
        this.unknown4 = unknown4;
    }

    protected long getDataOffset() {
        return dataOffset;
    }

    protected void setDataOffset(long dataOffset) {
        this.dataOffset = dataOffset;
    }
}
