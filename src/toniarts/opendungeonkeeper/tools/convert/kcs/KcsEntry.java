/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kcs;

import javax.vecmath.Vector3f;

/**
 * Stores the KCS file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KcsEntry {

    private Vector3f position;
    private Vector3f direction;
    private Vector3f left;
    private Vector3f up;
    private int unknown1;
    private int unknown2;

    public Vector3f getPosition() {
        return position;
    }

    protected void setPosition(Vector3f position) {
        this.position = position;
    }

    public Vector3f getDirection() {
        return direction;
    }

    protected void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    public Vector3f getLeft() {
        return left;
    }

    protected void setLeft(Vector3f left) {
        this.left = left;
    }

    public Vector3f getUp() {
        return up;
    }

    protected void setUp(Vector3f up) {
        this.up = up;
    }

    public int getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(int unknown1) {
        this.unknown1 = unknown1;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }
}
