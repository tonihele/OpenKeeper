/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Stub for the container class for the Doors.kwd
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class Door {
//struct DoorBlock {
//  char name[32];
//  ArtResource ref[5];
//  uint8_t unk[164];
//};

    private String name;
    private ArtResource[] ref;
    private short[] unknown;

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

    public short[] getUnknown() {
        return unknown;
    }

    public void setUnknown(short[] unknown) {
        this.unknown = unknown;
    }
}
