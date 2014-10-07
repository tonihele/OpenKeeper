/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.awt.Color;
import javax.vecmath.Vector3f;

/**
 * Adapted from C-code
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Light {
//    struct LightBlock {
//      int m_kPos[3]; /* 0x1000 = 1.0 */
//      unsigned int radius; /* same */
//      unsigned int flags; /* 10 */
//      unsigned char color[4]; /* bgr? */
//      };

    private Vector3f mKPos;
    private float radius;
    private int flags;
    private Color color;

    public Vector3f getmKPos() {
        return mKPos;
    }

    protected void setmKPos(Vector3f mKPos) {
        this.mKPos = mKPos;
    }

    public float getRadius() {
        return radius;
    }

    protected void setRadius(float radius) {
        this.radius = radius;
    }

    public int getFlags() {
        return flags;
    }

    protected void setFlags(int flags) {
        this.flags = flags;
    }

    public Color getColor() {
        return color;
    }

    protected void setColor(Color color) {
        this.color = color;
    }
}
