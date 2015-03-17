/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.kmf;

import javax.vecmath.Vector3f;

/**
 * Common vertex
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Vertex {

    private Uv uv;
    private Vector3f normal;

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
