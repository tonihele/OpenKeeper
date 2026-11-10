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
package toniarts.openkeeper.view;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author ArchDemon
 */
public abstract class PossessionCameraControl extends AbstractControl {

    public static enum Direction {

        ENTRANCE, EXIT
    };
    private static final float SPEED = 0.5f;

    private final Camera camera;
    private final Direction direction;
    private Quaternion qFrom, qTo;
    private Vector3f pFrom, pTo;
    private float tick = 0;

    public PossessionCameraControl(Camera camera, Direction direction) {
        this.camera = camera;
        this.direction = direction;
        if (direction == Direction.ENTRANCE) {
            qFrom = camera.getRotation().clone();
            pFrom = camera.getLocation().clone();
        } else {
            qTo = camera.getRotation().clone();
            pTo = camera.getLocation().clone();
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        if (spatial != null) {
            if (direction == Direction.ENTRANCE) {
                qTo = spatial.getLocalRotation().clone();
                pTo = spatial.getLocalTranslation().clone();
            } else {
                qFrom = spatial.getLocalRotation().clone();
                pFrom = spatial.getLocalTranslation().clone();
            }
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!isEnabled() || spatial == null) {
            return;
        }

        if (tick > 1) {
            spatial.removeControl(this);
            onExit();
            return;
        }

        moveCamera();

        tick += tpf * SPEED;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // nothing to render
    }

    private void moveCamera() {
        Vector3f location = FastMath.interpolateLinear(tick, pFrom, pTo);
        camera.setLocation(location);
        Quaternion rotation = new Quaternion(qFrom, qTo, tick);
        camera.setRotation(rotation);
    }

    abstract public void onExit();
}
