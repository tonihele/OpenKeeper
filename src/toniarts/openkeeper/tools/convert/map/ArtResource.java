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

import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import java.util.EnumSet;

/**
 * Placeholder for the container class for the ArtResource
 *
 * @author Wizand Petteri Loisko <petteri.loisko@gmail.com>, Toni Helenius
 * <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public class ArtResource {

    public enum ArtResourceFlag implements IFlagEnum {

        PLAYER_COLOURED(0x0002),
        ANIMATING_TEXTURE(0x0004),
        HAS_START_ANIMATION(0x0008),
        HAS_END_ANIMATION(0x0010),
        RANDOM_START_FRAME(0x0020),
        ORIGIN_AT_BOTTOM(0x0040),
        DOESNT_LOOP(0x0080),
        FLAT(0x0100),
        DOESNT_USE_PROGRESSIVE_MESH(0x0200),
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

    public enum Type implements IValueEnum {

        NONE(0),
        SPRITE(1),
        ALPHA(2),
        ADDITIVE_ALPHA(3),
        TERRAIN_MESH(4),
        MESH(5),
        ANIMATING_MESH(6),
        PROCEDURAL_MESH(7),
        MESH_COLLECTION(8);

        private Type(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
    private String name;
    private ResourceType settings;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ResourceType getSettings() {
        return settings;
    }

    protected void setSettings(ResourceType settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return name;
    }

    public class ResourceType {

        private EnumSet<ArtResourceFlag> flags; // 0
        private Type type;
        private short startAf; // Start animation frame
        private short endAf; // End animation frame
        private short sometimesOne;

        public EnumSet<ArtResourceFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<ArtResourceFlag> flags) {
            this.flags = flags;
        }

        public Type getType() {
            return type;
        }

        protected void setType(Type type) {
            this.type = type;
        }

        public short getStartAf() {
            return startAf;
        }

        protected void setStartAf(short startAf) {
            this.startAf = startAf;
        }

        public short getEndAf() {
            return endAf;
        }

        protected void setEndAf(short endAf) {
            this.endAf = endAf;
        }

        public short getSometimesOne() {
            return sometimesOne;
        }

        protected void setSometimesOne(short sometimesOne) {
            this.sometimesOne = sometimesOne;
        }
    }

    /**
     * struct { uint32_t width; // fixed uint32_t height; // fixed int16_t
     * frames; } image;
     */
    public class Image extends ResourceType {

        private float width; // Fixed, scale
        private float height; // Fixed, scale
        private int frames;

        public float getWidth() {
            return width;
        }

        protected void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        protected void setHeight(float height) {
            this.height = height;
        }

        public int getFrames() {
            return frames;
        }

        protected void setFrames(int frames) {
            this.frames = frames;
        }
    }

    /**
     * struct { int32_t scale; // fixed uint16_t frames; } mesh;
     */
    public class Mesh extends ResourceType {

        private float scale;
        private int frames;

        public float getScale() {
            return scale;
        }

        protected void setScale(float scale) {
            this.scale = scale;
        }

        public int getFrames() {
            return frames;
        }

        protected void setFrames(int frames) {
            this.frames = frames;
        }
    }

    /**
     * struct { uint32_t frames; // 4 uint32_t fps; // 8 uint16_t start_dist; //
     * c uint16_t end_dist; // e } anim;
     */
    public class Animation extends ResourceType {

        private int frames; //4
        private int fps; // 8
        private int startDist; // c
        private int endDist; // e

        public int getFrames() {
            return frames;
        }

        protected void setFrames(int frames) {
            this.frames = frames;
        }

        public int getFps() {
            return fps;
        }

        protected void setFps(int fps) {
            this.fps = fps;
        }

        public int getStartDist() {
            return startDist;
        }

        protected void setStartDist(int startDist) {
            this.startDist = startDist;
        }

        public int getEndDist() {
            return endDist;
        }

        protected void setEndDist(int endDist) {
            this.endDist = endDist;
        }
    }

    /**
     * struct { uint32_t id; } proc;
     */
    public class Proc extends ResourceType {

        private int id;

        public int getId() {
            return id;
        }

        protected void setId(int id) {
            this.id = id;
        }
    }

    /**
     * struct { uint32_t x00; uint32_t x04; uint8_t frames; } terrain;
     */
    public class TerrainResource extends ResourceType {

        private int x00;
        private int x04;
        private short frames;

        public int getX00() {
            return x00;
        }

        protected void setX00(int x00) {
            this.x00 = x00;
        }

        public int getX04() {
            return x04;
        }

        protected void setX04(int x04) {
            this.x04 = x04;
        }

        public short getFrames() {
            return frames;
        }

        protected void setFrames(short frames) {
            this.frames = frames;
        }
    }
}
