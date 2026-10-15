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
package toniarts.openkeeper.view.camera;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import java.io.IOException;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.view.PlayerCamera;

/**
 * Orbits the camera around its current look at point: the given angle is spread
 * evenly over the given time, then the control removes itself.
 *
 * @author ArchDemon
 */
public final class PlayerCameraRotateControl extends Control {

    private PlayerCamera camera;
    private float remaining;
    private float rate;

    /**
     * Constructor used for Serialization.
     */
    public PlayerCameraRotateControl() {
    }

    /**
     * @param camera the camera
     * @param relative the angle is relative to the current view. TODO: the
     * absolute variant (turn to a compass heading) is not supported, it is
     * handled like the relative one since the camera does not tell its heading
     * in the angle convention the maps use
     * @param angle the angle in degrees, positive and negative
     * @param time the time to turn the whole angle in, in seconds
     */
    public PlayerCameraRotateControl(PlayerCamera camera, boolean relative, int angle, int time) {
        this.camera = camera;
        this.remaining = time;
        this.rate = time > 0 ? angle * FastMath.DEG_TO_RAD / time : 0;
    }

    public PlayerCamera getCamera() {
        return camera;
    }

    public void setCamera(PlayerCamera camera) {
        this.camera = camera;
    }

    @Override
    public void updateControl(float tpf) {
        if (parent == null || camera == null) {
            return;
        }

        if (remaining > 0) {
            float step = Math.min(tpf, remaining);
            camera.rotateAround(rate * step);
            remaining -= step;
        }
        if (remaining <= 0) {
            parent.removeControl(this);
        }
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
    }
}
