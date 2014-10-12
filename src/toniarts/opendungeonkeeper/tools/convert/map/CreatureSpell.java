/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for CreatureSpells.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureSpell {

//    struct CreatureSpellBlock {
//        char name[32];
//        uint8_t data[234];
//        };
    private String name;
    private short[] data;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public short[] getData() {
        return data;
    }

    protected void setData(short[] data) {
        this.data = data;
    }
}
