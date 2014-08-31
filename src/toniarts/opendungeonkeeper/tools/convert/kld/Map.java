/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kld;

/**
 * Barely started placeholder for the container class for the levelnameMap.kld
 * 
 *
 * @author Wizand Petteri Loisko
 * petteri.loisko@gmail.com
 * 
 * Thank you https://github.com/werkt
 */
public class Map {
    
//  uint8_t x00; /* c */
//  uint8_t x01;
//  uint8_t x02;
//  uint8_t x03; /* padding */
    
    private byte x00;
    private byte x01;
    private byte x02;
    private byte x03;

    public byte getX00() {
        return x00;
    }

    public void setX00(byte x00) {
        this.x00 = x00;
    }

    public byte getX01() {
        return x01;
    }

    public void setX01(byte x01) {
        this.x01 = x01;
    }

    public byte getX02() {
        return x02;
    }

    public void setX02(byte x02) {
        this.x02 = x02;
    }

    public byte getX03() {
        return x03;
    }

    public void setX03(byte x03) {
        this.x03 = x03;
    }
    
    
    
    
    
}
