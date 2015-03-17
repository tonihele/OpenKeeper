/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.sound;

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

    public int getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(int unknown1) {
        this.unknown1 = unknown1;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
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
