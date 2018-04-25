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
 *
 * @author ArchDemon
 */

public class PossessionCamera {
    private final Camera camera;
    //private static final float ZOOM_SPEED = 25f;
    private float speed;
    private float oscillate;
    private static final float ROTATION_SPEED = 5f;
    private static final float Y_ANGLE_MAX = 0.8f;

    public PossessionCamera(Camera cam, float speed, float oscillate) {
        this.camera = cam;
        this.speed = speed;
        this.oscillate = oscillate;
    }

    protected void move(float value, boolean sideways) {

        // Moving is strafing over the map plane
        Vector3f vel = camera.getLeft().clone();
        Vector3f pos = camera.getLocation().clone();
        if (!sideways) {
            vel.crossLocal(Vector3f.UNIT_Y);
        }

        vel.multLocal(value * speed, 0, value * speed);

        pos.addLocal(vel);

        camera.setLocation(pos);
    }

    protected void rotate(float value, boolean sideways) {

        Matrix3f mat = new Matrix3f();
        if (sideways) {
            mat.fromAngleNormalAxis(ROTATION_SPEED * value, Vector3f.UNIT_Y.clone());
        } else {
            mat.fromAngleNormalAxis(ROTATION_SPEED * value, camera.getLeft().clone());
        }

        Vector3f up = camera.getUp();
        Vector3f left = camera.getLeft();
        Vector3f dir = camera.getDirection();

        mat.mult(up, up);
        mat.mult(left, left);
        mat.mult(dir, dir);

        if (dir.y < -Y_ANGLE_MAX || dir.y > Y_ANGLE_MAX) {
            return;
        }

        Quaternion q = new Quaternion();
        q.fromAxes(left, up, dir);
        q.normalizeLocal();

        camera.setAxes(q);
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getOscillate() {
        return oscillate;
    }

    public void setOscillate(float oscillate) {
        this.oscillate = oscillate;
    }
}
