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
package toniarts.openkeeper.view.effect;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author ArchDemon
 */
public class EffectGeometry extends Geometry {
    private EffectMesh particleMesh;
    private int frameIndex = 0;
    private ColorRGBA color;
    private int frames = 1;

    private final class EffectGeometryControl extends AbstractControl {

        @Override
        protected void controlUpdate(float tpf) {

            particleMesh.update(frameIndex);

            if (frames > 1) {
                frameIndex++;
                if (frameIndex >= frames) {
                    frameIndex = 0;
                }
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            //particleMesh.update(p, vp.getCamera(), Matrix3f.IDENTITY);
        }
    }

    public EffectGeometry(String name) {
        super(name);

        this.setBatchHint(BatchHint.Never);
        // ignore world transform, unless user sets inLocalSpace
        //this.setIgnoreTransform(true);

        // particles neither receive nor cast shadows
        this.setShadowMode(RenderQueue.ShadowMode.Off);

        // particles are usually transparent
        this.setQueueBucket(RenderQueue.Bucket.Transparent);

        particleMesh = new EffectMesh();
        this.setMesh(particleMesh);

        controls.add(new EffectGeometryControl());
    }

    public void setSelectRandomImage(boolean value) {
        if (value) {
            frameIndex = FastMath.nextRandomInt(0, frames);
        }
    }

    /**
     * For serialization only. Do not use.
     */
    public EffectGeometry() {
        super();
        setBatchHint(BatchHint.Never);
    }

    /**
     * Get the number of images along the X axis (width).
     *
     * @return the number of images along the X axis (width).
     *
     * @see PG#setFrames(int)
     */
    public int getFrames() {
        return frames;
    }

    /**
     * Set the number of images along the X axis (width).
     *
     * <p>To determine
     * how multiple particle images are selected and used, see the
     *
     * @param frames the number of images along the X axis (width).
     */
    public void setFrames(int frames) {
        this.frames = frames;
        particleMesh.setFrames(this.frames);
    }

    @Override
    public void setLocalTranslation(Vector3f localTranslation) {
        super.setLocalTranslation(localTranslation);

        //p.position.set(localTranslation);
        //this.particleMesh.update(p, null, Matrix3f.ZERO);
    }
}
