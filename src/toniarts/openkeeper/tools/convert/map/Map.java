/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.map;

/**
 * Barely started placeholder for the container class for the levelnameMap.kld
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class Map {

    // Lookup into Terrain kwd with id (BYTE at Terrain Block 0x1d6)
    private short terrainId;
    // Lookup into Players kld with id (BYTE at Player Block 0xa8)
    private short playerId;
    // Only a '2' bit here is interpreted to do anything special at load, 1 may indicate 'valid', but it is not interpreted as such
    private short flag;
    private short unknown;

    public short getTerrainId() {
        return terrainId;
    }

    protected void setTerrainId(short terrainId) {
        this.terrainId = terrainId;
    }

    public short getPlayerId() {
        return playerId;
    }

    protected void setPlayerId(short playerId) {
        this.playerId = playerId;
    }

    public short getFlag() {
        return flag;
    }

    protected void setFlag(short flag) {
        this.flag = flag;
    }

    public short getUnknown() {
        return unknown;
    }

    protected void setUnknown(short unknown) {
        this.unknown = unknown;
    }
}
