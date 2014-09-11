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

    private int geomIndex;
    private Uv uv;
    private Vector3f normal;

    public int getGeomIndex() {
        return geomIndex;
    }

    protected void setGeomIndex(int geomIndex) {
        this.geomIndex = geomIndex;
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
