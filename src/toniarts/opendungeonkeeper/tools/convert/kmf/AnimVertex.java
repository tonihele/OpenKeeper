/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

import javax.vecmath.Vector3f;

/**
 * AnimVertex
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AnimVertex {

    private Uv uv;
    private Vector3f normal;
    private short itabIndex;

    public short getItabIndex() {
        return itabIndex;
    }

    protected void setItabIndex(short itabIndex) {
        this.itabIndex = itabIndex;
    }

    public Uv getUv() {
        return uv;
    }

    protected void setUv(Uv uv) {
        this.uv = uv;
    }

    public Vector3f getNormal() {
        return normal;
    }

    protected void setNormal(Vector3f normal) {
        this.normal = normal;
    }
}
