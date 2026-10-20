/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view.effect.procedural;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.Random;

/**
 * Keeps a {@link LightningBoltMesh} alive: re-rolls its shape and brightness
 * every few frames and turns it to the camera every frame
 */
public final class LightningBoltControl extends AbstractControl {

    /**
     * How long the bolt holds one shape, a real one does not keep still
     */
    private static final float FLICKER_INTERVAL = 0.05f;
    private static final float MIN_INTENSITY = 0.55f;

    private final LightningBoltMesh mesh;
    private final Random random;
    private final Vector3f cameraLocation = new Vector3f();
    private float timeToFlicker;

    public LightningBoltControl(LightningBoltMesh mesh, Random random) {
        this.mesh = mesh;
        this.random = random;
    }

    @Override
    protected void controlUpdate(float tpf) {
        timeToFlicker -= tpf;
        if (timeToFlicker <= 0) {
            timeToFlicker = FLICKER_INTERVAL;
            mesh.randomizePath(random);
            mesh.setIntensity(MIN_INTENSITY + random.nextFloat() * (1 - MIN_INTENSITY));
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        spatial.worldToLocal(vp.getCamera().getLocation(), cameraLocation);
        mesh.faceCamera(cameraLocation);
    }

}
