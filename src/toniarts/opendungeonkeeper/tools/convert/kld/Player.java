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
    
    /**
     * struct PlayerBlock {
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
    
}
