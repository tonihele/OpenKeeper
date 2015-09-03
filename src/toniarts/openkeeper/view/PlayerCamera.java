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
package toniarts.openkeeper.view;

import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Player camera movement is modeled here
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCamera {

    private final Camera cam;
    private static final float ZOOM_SPEED = 25f;
    private static final float MOVE_SPEED = 25f;
    private static final float ROTATION_SPEED = 5f;

    public PlayerCamera(Camera cam) {
        this.cam = cam;
    }

    protected void zoomCamera(float value, boolean mouse) {

        // Zooming is here moving towards the looking at position
        Vector3f vel = new Vector3f();
        Vector3f pos = cam.getLocation().clone();
        cam.getDirection(vel);
        vel.multLocal(mouse ? value : value * ZOOM_SPEED);
        pos.addLocal(vel);

        cam.setLocation(pos);
    }

    protected void moveCamera(float value, boolean sideways) {

        // Moving is strafing over the map plane
        Vector3f vel = new Vector3f();
        Vector3f pos = cam.getLocation().clone();
        if (sideways) {
            cam.getLeft(vel);
        } else {
            cam.getUp(vel);
        }
        vel.multLocal(value * MOVE_SPEED, 0, value * MOVE_SPEED);
        pos.addLocal(vel);

        cam.setLocation(pos);
    }

    protected void rotateCamera(float value) {

        // TODO: Now spinning around the Y axis
        // Rotating is circling around the camera look at point, or similar
        Matrix3f mat = new Matrix3f();
        mat.fromAngleNormalAxis(ROTATION_SPEED * value, Vector3f.UNIT_Y.clone());

        Vector3f up = cam.getUp();
        Vector3f left = cam.getLeft();
        Vector3f dir = cam.getDirection();

        mat.mult(up, up);
        mat.mult(left, left);
        mat.mult(dir, dir);

        Quaternion q = new Quaternion();
        q.fromAxes(left, up, dir);
        q.normalizeLocal();

        cam.setAxes(q);
    }
}
