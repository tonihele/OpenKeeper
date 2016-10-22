/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.world.effect;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

/**
 *
 * @author ArchDemon
 */
public class ParticleMesh extends Mesh {

    private float scale = 1;

    /**
     * Serialization only. Do not use.
     */
    public ParticleMesh(){
    }

    public ParticleMesh(float scale){
        updateGeometry(scale, true);
    }

    /**
     * Create a quad with the given width and height. The quad
     * is always created in the XY plane.
     *
     * @param scale
     * @param flipCoords If true, the texture coordinates will be flipped
     * along the Y axis.
     */
    public ParticleMesh(float scale, boolean flipCoords){
        updateGeometry(scale, flipCoords);
    }

    public float getScale() {
        return scale;
    }

    public void updateGeometry(float scale, float height){
        updateGeometry(scale, false);
    }

    public void updateGeometry(float scale, boolean flipCoords) {
        this.scale = scale;

        setBuffer(VertexBuffer.Type.Position, 3, new float[]{
            0,          0,         0,
            1 * scale,  0,         0,
            1 * scale,  1 * scale, 0,
            0,          1 * scale, 0
        });


        if (flipCoords){
            setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 1,
                                                    1, 1,
                                                    1, 0,
                                                    0, 0});
        }else{
            setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 0,
                                                    1, 0,
                                                    1, 1,
                                                    0, 1});
        }
        setBuffer(VertexBuffer.Type.Normal, 3, new float[]{0, 0, 1,
                                              0, 0, 1,
                                              0, 0, 1,
                                              0, 0, 1});
        if (scale < 0){
            setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 2, 1,
                                                 0, 3, 2});
        }else{
            setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2,
                                                 0, 2, 3});
        }

        updateBound();
    }
}
