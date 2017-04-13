/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
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

    protected void setPos(float x, float y, float z) {
        this.pos = new Vector3f(x, y, z);
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
