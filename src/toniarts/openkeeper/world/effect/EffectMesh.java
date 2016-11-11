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

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;

/**
 *
 * @author ArchDemon
 */
public class EffectMesh extends Mesh {

    private float scale = 1;
    private int frames = 1;
    private boolean uniqueTexCoords = false;
    boolean facingVelocity = false;
    Vector3f faceNormal = Vector3f.UNIT_Y;

    public EffectMesh(int frames, float scale) {
        this();
        setFrames(frames);
        setScale(scale);
    }
    
    public EffectMesh(int frames) {
        this();
        setFrames(frames);
    }

    public EffectMesh() {
        super();

        //setMode(Mesh.Mode.Triangles);

        setBuffer(VertexBuffer.Type.Position, 3, new float[]{
            -scale / 2, 0,     0,
            scale / 2,  0,     0,
            scale / 2,  scale, 0,
            -scale / 2, scale, 0
        });

        setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 1,
                                                             1, 1,
                                                             1, 0,
                                                             0, 0});

        setBuffer(VertexBuffer.Type.Normal, 3, new float[]{0, 0, 1,
                                                           0, 0, 1,
                                                           0, 0, 1,
                                                           0, 0, 1});

        setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2,
                                                          0, 2, 3});

        updateBound();
        /*

        // set colors
        ByteBuffer cb = BufferUtils.createByteBuffer(4 * 4);
        buf = getBuffer(VertexBuffer.Type.Color);
        if (buf != null) {
            buf.updateData(cb);
        } else {
            VertexBuffer cvb = new VertexBuffer(VertexBuffer.Type.Color);
            cvb.setupData(VertexBuffer.Usage.Stream, 4, VertexBuffer.Format.UnsignedByte, cb);
            cvb.setNormalized(true);
            setBuffer(cvb);
        }

        updateCounts();
        */
    }

    public final void setFrames(int frames) {
        this.frames = frames;
        if (this.frames != 1){
            uniqueTexCoords = true;
            getBuffer(VertexBuffer.Type.TexCoord).setUsage(VertexBuffer.Usage.Stream);
        }
    }

    private void setScale(float scale) {
        this.scale = scale;
        
        VertexBuffer pvb = getBuffer(VertexBuffer.Type.Position);
        FloatBuffer positions = (FloatBuffer) pvb.getData();
        
        positions.clear();
        
        positions.put(new float[]{
            -scale / 2, 0,     0,
            scale / 2,  0,     0,
            scale / 2,  scale, 0,
            -scale / 2, scale, 0
        });
        
        pvb.updateData(positions);
    }

    public void update(int frameIndex) {
        /*
        VertexBuffer cvb = getBuffer(VertexBuffer.Type.Color);
        ByteBuffer colors = (ByteBuffer) cvb.getData();
        */
        VertexBuffer tvb = getBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer texcoords = (FloatBuffer) tvb.getData();

        texcoords.clear();

        if (uniqueTexCoords) {
            float startX = ((float) frameIndex) / frames;
            float startY = 0f;
            float endX   = startX + (1f / frames);
            float endY   = 1f;

            texcoords.put(new float[]{startX, endY,
                                      endX,endY,
                                      endX, startY,
                                      startX, startY});
        }

//        int abgr = p.color.asIntABGR();
//        colors.putInt(abgr);
//        colors.putInt(abgr);
//        colors.putInt(abgr);
//        colors.putInt(abgr);

        if (!uniqueTexCoords) {
            texcoords.clear();
        } else {
            texcoords.clear();
            tvb.updateData(texcoords);
        }

        // force renderer to re-send data to GPU
        //cvb.updateData(colors);
    }

}
