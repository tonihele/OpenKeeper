/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.map;

import java.awt.Color;
import java.util.EnumSet;
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

    /**
     * Light flags
     */
    public enum LightFlag implements IFlagEnum {

        FLICKER(0x0002),
        PULSE(0x0004),
        PLAYER_COLOURED(0x0080);
        private final long flagValue;

        private LightFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };
    private Vector3f mKPos;
    private float radius;
    private EnumSet<LightFlag> flags;
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

    public EnumSet<LightFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<LightFlag> flags) {
        this.flags = flags;
    }

    public Color getColor() {
        return color;
    }

    protected void setColor(Color color) {
        this.color = color;
    }
}
