/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.convert.kmf;

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
    //GEOM
    private List<Vector3f> geometries;

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

    /**
     * Unit cube scale for culling
     *
     * @return cube scale
     */
    public float getScale() {
        return scale;
    }

    protected void setScale(float scale) {
        this.scale = scale;
    }

    public List<MeshControl> getControls() {
        return controls;
    }

    protected void setControls(List<MeshControl> controls) {
        this.controls = controls;
    }

    public List<MeshSprite> getSprites() {
        return sprites;
    }

    protected void setSprites(List<MeshSprite> sprites) {
        this.sprites = sprites;
    }

    public List<Vector3f> getGeometries() {
        return geometries;
    }

    protected void setGeometries(List<Vector3f> geometries) {
        this.geometries = geometries;
    }
}
