/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

/**
 * MeshVertex
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MeshVertex extends Vertex {

    private int geomIndex;

    public int getGeomIndex() {
        return geomIndex;
    }

    protected void setGeomIndex(int geomIndex) {
        this.geomIndex = geomIndex;
    }
}
