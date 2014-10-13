/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for Shots.kwd<br>
 * Very much WIP
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Shot {

    private String name;
    private short shotId;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public short getShotId() {
        return shotId;
    }

    protected void setShotId(short shotId) {
        this.shotId = shotId;
    }

    @Override
    public String toString() {
        return name;
    }
}
