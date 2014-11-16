/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Enumeration for material value found in map files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum Material implements IValueEnum {

    NONE(0),
    FLESH(1),
    ROCK(2),
    WOOD(3),
    METAL1(4),
    METAL2(5),
    MAGIC(6),
    GLASS(7);

    private Material(int id) {
        this.id = id;
    }

    @Override
    public int getValue() {
        return id;
    }
    private int id;
}
