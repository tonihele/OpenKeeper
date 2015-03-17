/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.kmf;

import javax.vecmath.Vector3f;

/**
 * KMF Grop wrapper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Grop {

    //ELEM
    /**
     * Name of the [mesh] asset, without the KMF extension
     */
    private String name;
    private Vector3f pos;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public Vector3f getPos() {
        return pos;
    }

    protected void setPos(Vector3f pos) {
        this.pos = pos;
    }
}
