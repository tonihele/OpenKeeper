/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

/**
 * KMF Anim Control wrapper<br>
 * Currently unknown purpose
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AnimControl {

    private short unknown1;
    private short unknown2;
    private int unknown3;

    public short getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(short unknown1) {
        this.unknown1 = unknown1;
    }

    public short getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(short unknown2) {
        this.unknown2 = unknown2;
    }

    public int getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(int unknown3) {
        this.unknown3 = unknown3;
    }
}
