/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Barely started placeholder for the container class for the ArtResource
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class ArtResource {

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

        private int flags; // 0
        private short type;
        private short startAf;
        private short endAf;
        private short sometimesOne;

        public int getFlags() {
            return flags;
        }

        protected void setFlags(int flags) {
            this.flags = flags;
        }

        public short getType() {
            return type;
        }

        protected void setType(short type) {
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

        public void setScale(float scale) {
            this.scale = scale;
        }

        public int getFrames() {
            return frames;
        }

        public void setFrames(int frames) {
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

        public void setId(int id) {
            this.id = id;
        }
    }

    /**
     * struct { uint32_t x00; uint32_t x04; uint8_t frames; } terrain;
     */
    public class TerrainResource extends ResourceType {

        private int x00;
        private int x04;
        private byte frames;

        public int getX00() {
            return x00;
        }

        public void setX00(int x00) {
            this.x00 = x00;
        }

        public int getX04() {
            return x04;
        }

        public void setX04(int x04) {
            this.x04 = x04;
        }

        public byte getFrames() {
            return frames;
        }

        public void setFrames(byte frames) {
            this.frames = frames;
        }
    }
}
