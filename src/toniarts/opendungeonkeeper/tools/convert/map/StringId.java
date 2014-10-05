/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 *
 * Adapted from C-code
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class StringId {

//    struct StringIds {
//      uint32_t ids[5];
//      uint8_t x14[4];
//     };
    private final int ids[];
    private final short x14[];

    public StringId(int[] ids, short[] x14) {
        this.ids = ids;
        this.x14 = x14;
    }

    public int[] getIds() {
        return ids;
    }

    public short[] getX14() {
        return x14;
    }
}
