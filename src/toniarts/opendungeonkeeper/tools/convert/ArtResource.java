/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;



/**
 * Barely started placeholder for the container class for the ArtResource
 *
 * @author Wizand Petteri Loisko
 * petteri.loisko@gmail.com
 * 
 * Thank you https://github.com/werkt
 */


public class ArtResource {
    
    private String name;
    private ResourceType settings;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getSettings() {
        return settings;
    }

    public void setSettings(ResourceType settings) {
        this.settings = settings;
    }
    
    
}
class ResourceType {
    private int flags; /* 0 */
    private byte type;
    private byte start_af;
    private byte end_af;
    private byte sometimesOne;

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getStart_af() {
        return start_af;
    }

    public void setStart_af(byte start_af) {
        this.start_af = start_af;
    }

    public byte getEnd_af() {
        return end_af;
    }

    public void setEnd_af(byte end_af) {
        this.end_af = end_af;
    }

    public byte getSometimesOne() {
        return sometimesOne;
    }

    public void setSometimesOne(byte sometimesOne) {
        this.sometimesOne = sometimesOne;
    }
}

/**
    struct {
      uint32_t width; // fixed 
      uint32_t height; // fixed 
      int16_t frames;
    } image;
 */
class Image extends ResourceType{
    private int width; 
    private int height;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}

/**
   struct {
      int32_t scale; // fixed 
      uint16_t frames;
    } mesh;
 */
class Mesh extends ResourceType {
    private int sacle;
    private short frames;

    public int getSacle() {
        return sacle;
    }

    public void setSacle(int sacle) {
        this.sacle = sacle;
    }

    public short getFrames() {
        return frames;
    }

    public void setFrames(short frames) {
        this.frames = frames;
    }
    
}
/**
     struct {
      uint32_t frames; // 4
      uint32_t fps; // 8 
      uint16_t start_dist; // c 
      uint16_t end_dist; // e 
    } anim;
 */
class Animation extends ResourceType {
    private int frames; //4
    private int fps; // 8
    private short start_dist; // c
    private short end_dist; // e

    public int getFrames() {
        return frames;
    }

    public void setFrames(int frames) {
        this.frames = frames;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public short getStart_dist() {
        return start_dist;
    }

    public void setStart_dist(short start_dist) {
        this.start_dist = start_dist;
    }

    public short getEnd_dist() {
        return end_dist;
    }

    public void setEnd_dist(short end_dist) {
        this.end_dist = end_dist;
    }
}

/**
    struct {
      uint32_t id;
    } proc;
 */
class Proc extends ResourceType {
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
}

/**
     struct {
      uint32_t x00;
      uint32_t x04;
      uint8_t frames;
    } terrain;
 */
class Terrain extends ResourceType {
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