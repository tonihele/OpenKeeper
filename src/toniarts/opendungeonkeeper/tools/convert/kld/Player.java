/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kld;


/**
 * Barely started placeholder for the container class for the mapnamePlayer.kld
 * 
 *
 * @author Wizand Petteri Loisko
 * petteri.loisko@gmail.com
 * 
 * Thank you https://github.com/werkt
 */
public class Player {
    
    private PlayerBlock playerBlock;

    public PlayerBlock getPlayerBlock() {
        return playerBlock;
    }

    public void setPlayerBlock(PlayerBlock playerBlock) {
        this.playerBlock = playerBlock;
    }
    
    
    
}
class PlayerBlock {
  /**
  struct PlayerBlock {
  int x00;
  int x04;
  uint8_t x08[158];
  uint16_t xa6;
  uint8_t xa8;
  uint16_t xa9;
  uint16_t xab;
  char name[32]; // ad 
    };
     */
    private int x00;
    private int x04;
    private byte[] x08;
    private short xa6;
    private byte idPlayer; // 0xa8
    private short xa9;
    private short xab;
    private  String name;

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

    public byte[] getX08() {
        return x08;
    }

    public void setX08(byte[] x08) {
        this.x08 = x08;
    }

    public short getXa6() {
        return xa6;
    }

    public void setXa6(short xa6) {
        this.xa6 = xa6;
    }

    public byte getIdPlayer() {
        return idPlayer;
    }

    public void setIdPlayer(byte idPlayer) {
        this.idPlayer = idPlayer;
    }

    public short getXa9() {
        return xa9;
    }

    public void setXa9(short xa9) {
        this.xa9 = xa9;
    }

    public short getXab() {
        return xab;
    }

    public void setXab(short xab) {
        this.xab = xab;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    
    
}
