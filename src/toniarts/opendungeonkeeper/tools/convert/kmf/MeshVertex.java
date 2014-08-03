/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

import javax.vecmath.Vector3f;

/**
 * MeshVertex
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MeshVertex {

    private short geomIndex;
    private Uv uv;
    private Vector3f normal;

    public short getGeomIndex() {
        return geomIndex;
    }

    public void setGeomIndex(short geomIndex) {
        this.geomIndex = geomIndex;
    }

    public Uv getUv() {
        return uv;
    }

    public void setUv(Uv uv) {
        this.uv = uv;
    }

    public Vector3f getNormal() {
        return normal;
    }

    public void setNormal(Vector3f normal) {
        this.normal = normal;
    }
}
