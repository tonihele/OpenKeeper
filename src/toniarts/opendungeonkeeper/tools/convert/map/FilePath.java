/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container for file paths found in in the KWD file
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class FilePath {

    private MapDataTypeEnum id; // unsigned int
    private int unknown2;
    private String path; // 64

    public MapDataTypeEnum getId() {
        return id;
    }

    protected void setId(MapDataTypeEnum id) {
        this.id = id;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public String getPath() {
        return path;
    }

    protected void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return getId() + ", " + getPath();
    }
}
