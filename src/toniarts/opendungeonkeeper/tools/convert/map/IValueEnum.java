/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * A small interface for enums that have an ID value in the map files<br>
 * Can be easily converted to enum values then.
 *
 * @see KwdFile.#parseEnum(int, java.lang.Class)
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IValueEnum {

    /**
     * Get the ID value associated to a enum value
     *
     * @return the id value
     */
    public int getValue();
}
