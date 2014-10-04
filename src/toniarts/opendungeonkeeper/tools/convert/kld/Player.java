/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kld;

/**
 * Barely started placeholder for the container class for the mapnamePlayer.kld
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class Player {

    //
    // struct Player
    // int x00;
    // int x04;
    // uint8_t x08[158];
    // uint16_t xa6;
    // uint8_t xa8;
    // uint16_t xa9;
    // uint16_t xab;
    // char name[32]; // ad
    private int startingGold; // x00
    private int unknown2; // x04, I suspect that this is AI = 1, Human player = 0
    private short[] unknown3; // x08
    private int unknown4; // xa6
    private short playerId; // 0xa8
    private int unknown5; // xa9
    private int unknown6; // xab
    private String name; // ad

    public int getStartingGold() {
        return startingGold;
    }

    protected void setStartingGold(int startingGold) {
        this.startingGold = startingGold;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }

    public int getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(int unknown4) {
        this.unknown4 = unknown4;
    }

    public short getPlayerId() {
        return playerId;
    }

    protected void setPlayerId(short idPlayer) {
        this.playerId = idPlayer;
    }

    public int getUnknown5() {
        return unknown5;
    }

    protected void setUnknown5(int unknown5) {
        this.unknown5 = unknown5;
    }

    public int getUnknown6() {
        return unknown6;
    }

    protected void setUnknown6(int unknown6) {
        this.unknown6 = unknown6;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }
}
