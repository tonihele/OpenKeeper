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
package toniarts.openkeeper.view.control;

import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import java.nio.FloatBuffer;

/**
 * Animates a lava surface mesh with small traveling sine waves by displacing
 * the vertex heights each frame based on the elapsed time.
 */
public class LavaWaveControl extends AbstractControl {

    private static final float WAVE_AMPLITUDE = 0.10f;
    private static final float WAVE_LENGTH = 3.0f;
    private static final float WAVE_SPEED = 3.0f;

    private Mesh mesh;
    private FloatBuffer positionData;
    private float[] baseY;
    private float[] baseX;
    private float[] baseZ;
    private float time;

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        return new LavaWaveControl();
    }

    @Override
    protected void controlUpdate(float tpf) {
        time += tpf;

        if (mesh == null) {
            init();
            if (mesh == null) {
                return;
            }
        }

        float freq = (float) (Math.PI * 2 / WAVE_LENGTH);
        for (int i = 0; i < baseX.length; i++) {
            float offset = WAVE_AMPLITUDE
                    * (float) (Math.sin((baseX[i] - time * WAVE_SPEED) * freq)
                    * Math.cos(baseZ[i] * freq));
            positionData.put(i * 3 + 1, baseY[i] + offset);
        }

        // The buffer position must be reset for a full upload
        positionData.rewind();
        mesh.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
        mesh.updateBound();
    }

    private void init() {
        if (!(getSpatial() instanceof Geometry)) {
            return;
        }

        mesh = ((Geometry) getSpatial()).getMesh();
        VertexBuffer position = mesh.getBuffer(VertexBuffer.Type.Position);
        if (position == null) {
            mesh = null;
            return;
        }
        position.setUsage(VertexBuffer.Usage.Dynamic);
        positionData = (FloatBuffer) position.getData();
        int count = positionData.limit() / 3;
        FloatBuffer base = positionData.duplicate();
        baseY = new float[count];
        baseX = new float[count];
        baseZ = new float[count];
        for (int i = 0; i < count; i++) {
            baseY[i] = base.get(i * 3 + 1);
            baseX[i] = base.get(i * 3);
            baseZ[i] = base.get(i * 3 + 2);
        }
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {
    }
}
