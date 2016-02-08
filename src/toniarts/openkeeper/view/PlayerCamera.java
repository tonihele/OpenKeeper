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

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;

/**
 * Player camera movement is modeled here
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCamera {

    private final Camera cam;
    private final Thing.Camera presets;
    private Vector2f limit;

    public PlayerCamera(Camera cam, Thing.Camera presets) {
        this.cam = cam;
        this.presets = presets;
        if (presets == null) {
            throw new RuntimeException("Very bad");
        }
        initialize();
    }

    /**
     * Load the initial main menu camera position
     */
    private void initialize() {
        // FIXME maybe another default angles
        Quaternion q = new Quaternion().fromAngles(FastMath.PI * (0.5f - presets.getAnglePitch() / 1024f),
                FastMath.PI * (0.75f + presets.getAngleYaw() / 1024f),
                FastMath.PI * (0 + presets.getAngleRoll() / 1024f));

        cam.setRotation(q);
        cam.setLocation(new Vector3f(0, presets.getHeight(), 0));
        // FIXME fov maybe have another formula. Need correct?
        cam.setFrustumPerspective(presets.getFov() * FastMath.RAD_TO_DEG / 4, (float) cam.getWidth() / cam.getHeight(), 0.1f, 100f);
    }

    protected void zoom(float value) {
        if (presets.getFlags().contains(Thing.Camera.CameraFlag.DISABLE_ZOOM)) {
            return;
        }
        // Zooming is here moving towards the looking at position
        Vector3f vel = new Vector3f();
        Vector3f pos = cam.getLocation().clone();
        cam.getDirection(vel);
        vel.multLocal(value);
        pos.addLocal(vel);

        if (pos.y < presets.getHeightMin()) {
            //pos.setY(presets.getHeightMin());
            return;
        } else if (pos.y > presets.getHeightMax()) {
            //pos.setY(presets.getHeightMax());
            return;
        }

        cam.setLocation(pos);
    }

    protected void move(float dx, float dz) {

        // Moving is strafing over the map plane
        Vector3f vel = cam.getLeft();
        if (dx != 0) {
            vel.multLocal(dx);
        } else if (dz != 0) {
            vel.crossLocal(Vector3f.UNIT_Y).multLocal(dz);
        }

        Vector3f look = getLookAt();
        look.addLocal(vel);

        // check limit
        if (look.getX() > limit.x * MapLoader.TILE_WIDTH) {
            look.setX(limit.x * MapLoader.TILE_WIDTH);
        } else if (look.getX() < 0) {
            look.setX(0);
        }

        if (look.getZ() > limit.y * MapLoader.TILE_WIDTH) {
            look.setZ(limit.y * MapLoader.TILE_WIDTH);
        } else if (look.getZ() < 0) {
            look.setZ(0);
        }

        setLookAt(look);
    }

    /**
     * Rotating is circling around the camera look at point, or similar
     *
     * @param value
     */
    protected void rotateAround(float value) {

        Matrix3f mat = new Matrix3f();
        mat.fromAngleNormalAxis(value, Vector3f.UNIT_Y.clone());

        Vector3f up = cam.getUp();
        Vector3f left = cam.getLeft();
        Vector3f dir = cam.getDirection();

        Vector3f pos = cam.getLocation();
        Vector3f look = getLookAt();
        pos.subtractLocal(look);

        mat.mult(up, up);
        mat.mult(left, left);
        mat.mult(dir, dir);
        mat.mult(pos, pos);

        Quaternion q = new Quaternion();
        q.fromAxes(left, up, dir);
        q.normalizeLocal();

        cam.setLocation(look.addLocal(pos));
        cam.setAxes(q);
    }

    public void pith(float value) {
        if (!presets.getFlags().contains(Thing.Camera.CameraFlag.DISABLE_PITCH)) {
            rotate(value, cam.getLeft());
        }
    }

    public void roll(float value) {
        if (!presets.getFlags().contains(Thing.Camera.CameraFlag.DISABLE_ROLL)) {
            rotate(value, cam.getDirection());
        }
    }

    private void rotate(float value, Vector3f axis) {
        Matrix3f mat = new Matrix3f();
        mat.fromAngleNormalAxis(value, axis);

        Vector3f up = cam.getUp();
        Vector3f left = cam.getLeft();
        Vector3f dir = cam.getDirection();

        mat.mult(up, up);
        mat.mult(left, left);
        mat.mult(dir, dir);

        Quaternion q = new Quaternion();
        q.fromAxes(left, up, dir);
        cam.setAxes(q);
    }

    public Vector2f getLookAtPoint() {
        Vector3f dir = cam.getDirection();
        Vector3f pos = cam.getLocation();
        Vector3f result = dir.mult(-pos.y / dir.y).add(pos);
        // Where lookAt camera
        return new Vector2f(result.x, result.z);
    }

    public Vector3f getLookAt() {
        Vector2f l = getLookAtPoint();
        return new Vector3f(l.x, 0, l.y);
    }

    public void setLookAt(Vector2f position) {
        Vector2f pos = getLookAtPoint().subtract(position);
        cam.setLocation(cam.getLocation().subtract(pos.x, 0, pos.y));
    }

    public void setLookAt(float x, float z) {
        setLookAt(new Vector2f(x, z));
    }

    public void setLookAt(Vector3f position) {
        setLookAt(position.x, position.z);
    }

    public float getHeight() {
        return cam.getLocation().y;
    }

    public Vector2f getLimit() {
        return limit;
    }

    public void setLimit(Vector2f limit) {
        this.limit = limit;
    }
}
