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
import com.jme3.math.Vector3f;

import java.io.IOException;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.view.PlayerCamera;

/**
 * Glides the camera to a place. Like DK II (0x44A270) the glide takes 25.6 milliseconds for every squared tile
 * of the distance, at most half a second.
 *
 * @author ArchDemon
 */
public final class PlayerCameraControl extends Control {

    private static final float GLIDE_TIME_PER_SQUARED_TILE = 0.0256f;
    private static final float MAX_GLIDE_TIME = 0.5f;

    private PlayerCamera camera;
    private Vector3f from, to;
    private float tick = 0;
    private float glideTime;

    /**
     * Constructor used for Serialization.
     */
    public PlayerCameraControl() {
    }

    public PlayerCameraControl(PlayerCamera camera, Vector3f to) {
        this.camera = camera;
        this.from = this.camera.getLookAt();
        this.to = to;
        float dx = to.x - from.x;
        float dz = to.z - from.z;
        this.glideTime = Math.min(GLIDE_TIME_PER_SQUARED_TILE * (dx * dx + dz * dz), MAX_GLIDE_TIME);
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

        tick += tpf;
        float progress = tick < glideTime ? tick / glideTime : 1;
        camera.setLookAt(FastMath.interpolateLinear(progress, from, to));
        if (progress >= 1) {
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
