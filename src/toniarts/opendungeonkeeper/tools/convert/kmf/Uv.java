/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

/**
 * UV coordinates used in mesh sprites
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Uv {

    private final short[] uv;

    public Uv(short x, short y) {
        uv = new short[2];
        uv[0] = x;
        uv[1] = y;
    }

    public short[] getUv() {
        return uv;
    }
}
