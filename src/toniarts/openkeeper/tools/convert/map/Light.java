/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import java.awt.Color;
import java.util.EnumSet;
import javax.vecmath.Vector3f;
import toniarts.openkeeper.tools.convert.IFlagEnum;

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

        UNKNOWN(0x0001),  // FIXME unknown flag
        FLICKER(0x0002),
        PULSE(0x0004),
        PLAYER_COLOURED(0x0080),
        COLOR_RED(0x00000800), // If this is set, add 256 to the red value of the light
        COLOR_GREEN(0x00001000), // If this is set, add 256 to the green value of the light
        COLOR_BLUE(0x00002000); // If this is set, add 256 to the blue value of the light
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
