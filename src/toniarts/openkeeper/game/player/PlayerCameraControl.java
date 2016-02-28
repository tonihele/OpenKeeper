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
package toniarts.openkeeper.game.player;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.io.IOException;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.control.IContainer;
import toniarts.openkeeper.view.PlayerCamera;
import toniarts.openkeeper.world.MapLoader;

/**
 *
 * @author ArchDemon
 */


public class PlayerCameraControl extends Control {
    private PlayerCamera camera;
    private Vector3f from, to;
    private float tick = 0;
    public static final float SPEED = 1.5f;

    /**
     * Constructor used for Serialization.
     */
    public PlayerCameraControl() {
    }

    public PlayerCameraControl(PlayerCamera camera) {
        this.camera = camera;
        from = this.camera.getLookAt();
    }

    public PlayerCamera getCamera() {
        return camera;
    }

    public void setCamera(PlayerCamera camera) {
        this.camera = camera;
    }

    @Override
    public void setParent(IContainer parent) {
        super.setParent(parent);

        if (parent instanceof ActionPoint) {
            ActionPoint ap = (ActionPoint) parent;
            to = MapLoader.getCameraPositionOnMapPoint((int) ((ap.getStart().x + ap.getEnd().x) / 2),
                    (int) ((ap.getStart().y + ap.getEnd().y) / 2));
        }
    }

    @Override
    public void controlUpdate(float tpf) {
        if (parent == null || camera == null || !enabled) {
            return;
        }

        if (tick > 1) {
            parent.removeControl(this);
            return;
        }

        Vector3f look = FastMath.interpolateLinear(tick, from, to);
        camera.setLookAt(look);

        tick += tpf * SPEED;
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
