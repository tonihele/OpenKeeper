/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

/**
 * Triangle used in mesh sprites
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Triangle {

    private final short[] triangle;

    public Triangle(short x, short y, short z) {
        triangle = new short[3];
        triangle[0] = x;
        triangle[1] = y;
        triangle[2] = z;
    }

    public short[] getTriangle() {
        return triangle;
    }
}
