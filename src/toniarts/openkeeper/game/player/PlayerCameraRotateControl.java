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
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.control.IContainer;
import toniarts.openkeeper.view.PlayerCamera;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * TODO need testing. What about `relative` ?
 * @author ArchDemon
 */
public class PlayerCameraRotateControl extends Control {
    private PlayerCamera camera;
    private int time;
    private float angle;
    private boolean relative;
    //private float tick = 0;
    //public static final float SPEED = 1.5f;

    /**
     * Constructor used for Serialization.
     */
    public PlayerCameraRotateControl() {
    }

    public PlayerCameraRotateControl(PlayerCamera camera, boolean relative, int angle, int time) {
        this.camera = camera;
        this.relative = relative;
        this.angle = angle * FastMath.DEG_TO_RAD;
        this.time = time;
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
            Vector3f location = WorldUtils.ActionPointToVector3f(ap);
            camera.setLookAt(location);
        }
    }

    @Override
    public void updateControl(float tpf) {
        if (parent == null || camera == null) {
            return;
        }

        if (time > 0) {
            camera.rotateAround(angle * tpf);
            time -= tpf;
        } else {
            parent.removeControl(this);
        }

        //tick += tpf;
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
