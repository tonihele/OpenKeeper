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
    private byte type;
    private byte start_af;
    private byte end_af;
    private byte sometimesOne;

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
class Image extends ResourceType{}
class Mesh extends ResourceType {}
class Animation extends ResourceType {}
class Proc extends ResourceType {}
class Terrain extends ResourceType {}