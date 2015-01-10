/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.sound;

/**
 * *Bank.map file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
class BankMapFileEntry {

    private long unknown1;
    private int unknown2;
    private short unknown3[]; // 3

    public long getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(long unknown1) {
        this.unknown1 = unknown1;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }
}
