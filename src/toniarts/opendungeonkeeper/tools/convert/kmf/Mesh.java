/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kmf;

import java.util.List;
import javax.vecmath.Vector3f;

/**
 * KMF Mesh wrapper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Mesh {

    //HEAD
    private String name;
    private Vector3f pos;
    private float scale;
    //CTRL
    private List<MeshControl> controls;
    //SPRS
    private List<MeshSprite> sprites;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vector3f getPos() {
        return pos;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public List<MeshControl> getControls() {
        return controls;
    }

    public void setControls(List<MeshControl> controls) {
        this.controls = controls;
    }

    public List<MeshSprite> getSprites() {
        return sprites;
    }

    public void setSprites(List<MeshSprite> sprites) {
        this.sprites = sprites;
    }
}
