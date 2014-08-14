/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

import javax.vecmath.Vector3f;

/**
 * KMF Anim Geom wrapper<br>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AnimGeom {

    /**
     * Frame Base Modulus
     */
    private byte frameBase;
    /**
     * Scaled geometry, just add to position
     */
    private Vector3f geometry;

    public byte getFrameBase() {
        return frameBase;
    }

    protected void setFrameBase(byte frameBase) {
        this.frameBase = frameBase;
    }

    public Vector3f getGeometry() {
        return geometry;
    }

    protected void setGeometry(Vector3f geometry) {
        this.geometry = geometry;
    }
}
