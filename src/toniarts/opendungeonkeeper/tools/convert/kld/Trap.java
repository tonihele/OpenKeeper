/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kld;

import toniarts.opendungeonkeeper.tools.convert.ArtResource;

/**
 * Stub for the container class for the Trap
 * 
 *
 * @author Wizand Petteri Loisko
 * petteri.loisko@gmail.com
 * 
 * Thank you https://github.com/werkt
 */
public class Trap {
//  char name[32];
//  ArtResource ref[5];
//  uint8_t data[127];

    private String name;
    private ArtResource[] ref;
    private byte[] data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArtResource[] getRef() {
        return ref;
    }

    public void setRef(ArtResource[] ref) {
        this.ref = ref;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    
    
}
