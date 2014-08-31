/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kcs;

import javax.vecmath.Vector3f;

/**
 * Stores the KCS file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KcsEntry {

    private Vector3f unknown1;
    private Vector3f unknown2;
    private Vector3f unknown3;
    private Vector3f unknown4;
    private float unknown5;
    private float unknown6;

    public Vector3f getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(Vector3f unknown1) {
        this.unknown1 = unknown1;
    }

    public Vector3f getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(Vector3f unknown2) {
        this.unknown2 = unknown2;
    }

    public Vector3f getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(Vector3f unknown3) {
        this.unknown3 = unknown3;
    }

    public Vector3f getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(Vector3f unknown4) {
        this.unknown4 = unknown4;
    }

    public float getUnknown5() {
        return unknown5;
    }

    protected void setUnknown5(float unknown5) {
        this.unknown5 = unknown5;
    }

    public float getUnknown6() {
        return unknown6;
    }

    protected void setUnknown6(float unknown6) {
        this.unknown6 = unknown6;
    }
}
