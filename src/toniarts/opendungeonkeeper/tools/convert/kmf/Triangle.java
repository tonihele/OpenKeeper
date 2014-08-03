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

    private final byte[] triangle;

    public Triangle(byte x, byte y, byte z) {
        triangle = new byte[3];
        triangle[0] = x;
        triangle[1] = y;
        triangle[2] = z;
    }

    public byte[] getTriangle() {
        return triangle;
    }
}
