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
    // Sprite resources have no FPS field. Their normalized phase advances on
    // the 20 Hz effect clock, using integer steps through one full cycle.
    private static final int PHASE_SCALE = 1 << 12;
    private static final int TICKS_PER_SECOND = 20;
    private int phase;
    private double tickRemainder;
    private boolean looping = true;
    private ColorRGBA color;
    private int frames = 1;

    private final class EffectGeometryControl extends AbstractControl {

        @Override
        protected void controlUpdate(float tpf) {

            if (frames > 1 && tpf > 0 && Float.isFinite(tpf)) {
                tickRemainder += (double) tpf * TICKS_PER_SECOND;
                // Absorb float tpf rounding at an exact effect-tick boundary.
                double ticks = Math.floor(tickRemainder + 0.000001);
                if (ticks > 0) {
                    tickRemainder = Math.max(0, tickRemainder - ticks);
                    int step = Math.max(1, PHASE_SCALE / frames);
                    if (looping) {
                        phase = (int) ((phase + (ticks % PHASE_SCALE) * step) % PHASE_SCALE);
                    } else {
                        phase = (int) Math.min(PHASE_SCALE, phase + ticks * step);
                    }
                    updateFrame();
                }
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            //particleMesh.update(p, vp.getCamera(), Matrix3f.IDENTITY);
        }
    }

    public EffectGeometry(String name) {
        this(name, false);
    }

    /**
     * @param flat true for a FLAT-flagged art resource - lies the geometry
     * down on the floor instead of standing it upright.
     */
    public EffectGeometry(String name, boolean flat) {
        super(name);

        this.setBatchHint(BatchHint.Never);
        // ignore world transform, unless user sets inLocalSpace
        //this.setIgnoreTransform(true);

        // particles neither receive nor cast shadows
        this.setShadowMode(RenderQueue.ShadowMode.Off);

        // particles are usually transparent
        this.setQueueBucket(RenderQueue.Bucket.Transparent);

        particleMesh = new EffectMesh(flat);
        this.setMesh(particleMesh);

        controls.add(new EffectGeometryControl());
    }

    public void setSelectRandomImage(boolean value) {
        if (value) {
            phase = FastMath.nextRandomInt(0, frames - 1) * Math.max(1, PHASE_SCALE / frames);
            tickRemainder = 0;
            updateFrame();
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
        this.frames = Math.max(1, frames);
        phase = 0;
        tickRemainder = 0;
        particleMesh.setFrames(this.frames);
        updateFrame();
    }

    /** Keep a one-shot sprite on its final frame until its effect expires. */
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    private void updateFrame() {
        int frame = Math.min(frames - 1, (int) ((long) phase * frames / PHASE_SCALE));
        particleMesh.update(frame);
    }

    @Override
    public void setLocalTranslation(Vector3f localTranslation) {
        super.setLocalTranslation(localTranslation);

        //p.position.set(localTranslation);
        //this.particleMesh.update(p, null, Matrix3f.ZERO);
    }
}
