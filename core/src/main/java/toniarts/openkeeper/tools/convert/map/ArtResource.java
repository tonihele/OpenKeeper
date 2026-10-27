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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * Placeholder for the container class for the ArtResource
 *
 * @author Wizand Petteri Loisko <petteri.loisko@gmail.com>
 * @author Toni Helenius <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public final class ArtResource {

    public static final String KEY_ID = "id";
    public static final String KEY_WIDTH = "width";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_FRAMES = "frames";
    public static final String KEY_SCALE = "scale";
    public static final String KEY_FPS = "fps";
    public static final String KEY_START_DIST = "startDist";
    public static final String KEY_END_DIST = "endDist";
    public static final String KEY_START_AF = "startAf";
    public static final String KEY_END_AF = "endAf";

    public enum ArtResourceFlag implements IFlagEnum {

        UNKNOWN(0x0001), // FIXME unknown flag. Maybe CAN_USE_ALTERNATIVE_ANIMATION
        PLAYER_COLOURED(0x0002), // if ArtResourceType = ADDITIVE_ALPHA || ALPHA
        ANIMATING_TEXTURE(0x0004),
        HAS_START_ANIMATION(0x0008),
        HAS_END_ANIMATION(0x0010),
        RANDOM_START_FRAME(0x0020),
        ORIGIN_AT_BOTTOM(0x0040),
        DOESNT_LOOP(0x0080), // if ANIMATING_TEXTURE but in all ArtResourceType
        FLAT(0x0100),
        DOESNT_USE_PROGRESSIVE_MESH(0x0200),
        UNKNOWN_1(0x0400), // FIXME unknown flag. In creature Imp (animIdle1)
        UNKNOWN_2(0x8000), // FIXME unknown flag. In creature Imp (animMelee1)
        USE_ANIMATING_TEXTURE_FOR_SELECTION(0x10000),
        PRELOAD(0x20000),
        BLOOD(0x40000);
        private final long flagValue;

        private ArtResourceFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum ArtResourceType implements IValueEnum {

        NONE(0),
        SPRITE(1), // width, height, frames if ANIMATING_TEXTURE
        ALPHA(2), // width, height, frames if ANIMATING_TEXTURE
        ADDITIVE_ALPHA(3), // width, height, frames if ANIMATING_TEXTURE
        TERRAIN_MESH(4), // unknown_1, unknown_2, unknown_3. In editor no attributes
        MESH(5), // scale, frames if ANIMATING_TEXTURE, unknown_1
        ANIMATING_MESH(6), // frames, fps, startDist, endDist, startAf if HAS_START_ANIMATION, endAf if HAS_END_ANIMATION
        PROCEDURAL_MESH(7), // id, unknown_1, unknown_2
        MESH_COLLECTION(8), // unknown_1, unknown_2, unknown_3. In editor no attributes
        UNKNOWN(12); // unknown_1, unknown_2, unknown_3. In editor no attributes. FIXME unknown flag

        private ArtResourceType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
    private String name;
    private EnumSet<ArtResourceFlag> flags;
    private ArtResourceType type;
    private short sometimesOne;

    private Map<String, Number> data = null;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public EnumSet<ArtResourceFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<ArtResourceFlag> flags) {
        this.flags = flags;
    }

    public ArtResourceType getType() {
        return type;
    }

    protected void setType(ArtResourceType type) {
        this.type = type;
    }

    public short getSometimesOne() {
        return sometimesOne;
    }

    protected void setSometimesOne(short sometimesOne) {
        this.sometimesOne = sometimesOne;
    }

    protected void setData(String key, Number value) {
        if (data == null) {
            data = new HashMap<>();
        }

        data.put(key, value);
    }

    public Collection<String> getDataKeys() {
        if (data != null) {
            return data.keySet();
        }

        return Collections.emptySet();
    }

    public <T extends Number> T getData(String key) {
        if (data == null) {
            return null;
        }

        Number s = data.get(key);
        return (T) s;
    }

    @Override
    public String toString() {
        return name;
    }
}
