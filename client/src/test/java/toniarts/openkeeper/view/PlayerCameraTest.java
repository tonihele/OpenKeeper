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
package toniarts.openkeeper.view;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.lang.reflect.Field;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.view.minimap.MinimapCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerCameraTest {

    private static void set(Thing.Camera presets, String field, Object value) throws ReflectiveOperationException {
        Field f = Thing.Camera.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(presets, value);
    }

    private static PlayerCamera playerCamera(Camera camera) throws ReflectiveOperationException {
        Thing.Camera presets = new Thing.Camera();
        set(presets, "zoomValue", 12f);
        set(presets, "lensValue", 1f);
        set(presets, "anglePitch", 300);
        set(presets, "flags", EnumSet.noneOf(Thing.Camera.CameraFlag.class));
        return new PlayerCamera(camera, presets);
    }

    private static Thing.Camera mode(int pitch, float zoom, float zoomMin, float zoomMax, float lens, float focusHeight)
            throws ReflectiveOperationException {
        Thing.Camera mode = new Thing.Camera();
        set(mode, "anglePitch", pitch);
        set(mode, "zoomValue", zoom);
        set(mode, "zoomValueMin", zoomMin);
        set(mode, "zoomValueMax", zoomMax);
        set(mode, "lensValue", lens);
        set(mode, "position", new javax.vecmath.Vector3f(0, 0, focusHeight));
        set(mode, "flags", EnumSet.noneOf(Thing.Camera.CameraFlag.class));
        return mode;
    }

    @Test
    void switchingModeKeepsLookAtAndRotationAndFollowsTheRecord() throws ReflectiveOperationException {
        Thing.Camera standard = mode(220, 10f, 4f, 13f, 2f, 1f);
        Thing.Camera far = mode(240, 29f, 4f, 52f, 10f, 1f);
        Thing.Camera overhead = mode(0, 6f, 4f, 13f, 2f, 0f);
        Camera camera = new Camera(800, 600);
        PlayerCamera playerCamera = new PlayerCamera(camera, standard);
        playerCamera.setLookAt(20.5f, 30.5f);
        playerCamera.rotateAround(1.234f);
        float yaw = MinimapCoordinates.cameraYawRadians(camera);
        float height = camera.getLocation().y;

        playerCamera.switchTo(far, 0f);

        assertEquals(20.5f, playerCamera.getLookAt().x, 1e-3f);
        assertEquals(30.5f, playerCamera.getLookAt().z, 1e-3f);
        assertEquals(yaw, MinimapCoordinates.cameraYawRadians(camera), 1e-3f);
        assertEquals(FastMath.PI * 240 / 1024, FastMath.acos(-camera.getDirection().y), 1e-3f);
        // Zoom scales with the lens, 2 -> 10
        assertEquals(height * 5f, camera.getLocation().y, 1e-2f);

        playerCamera.switchTo(overhead, 0f);

        assertEquals(20.5f, playerCamera.getLookAt().x, 1e-3f);
        assertEquals(30.5f, playerCamera.getLookAt().z, 1e-3f);
        assertEquals(0f, playerCamera.getLookAt().y, 1e-6f);
        assertEquals(-1f, camera.getDirection().y, 1e-3f);
        // 5 * height / 5 back to the original, but the range is 4 - 13
        assertEquals(Math.min(13f, height), camera.getLocation().y, 1e-2f);
    }

    @Test
    void switchingModeBlendsOverTime() throws ReflectiveOperationException {
        Thing.Camera standard = mode(220, 10f, 4f, 13f, 2f, 1f);
        Thing.Camera overhead = mode(0, 6f, 4f, 13f, 2f, 0f);
        Camera camera = new Camera(800, 600);
        PlayerCamera playerCamera = new PlayerCamera(camera, standard);

        playerCamera.switchTo(overhead, 0.5f);
        playerCamera.update(0.25f);

        assertEquals(FastMath.PI * 220 / 1024 / 2, FastMath.acos(-camera.getDirection().y), 1e-3f);

        playerCamera.update(0.25f);
        playerCamera.update(1f);

        assertEquals(-1f, camera.getDirection().y, 1e-3f);
    }

    @Test
    void resetYawToNorthFacesNorthKeepingPitchAndLookAt()throws ReflectiveOperationException {
        Camera camera = new Camera(800, 600);
        PlayerCamera playerCamera = playerCamera(camera);
        playerCamera.setLookAt(20.5f, 30.5f);
        playerCamera.rotateAround(1.234f);
        float pitch = camera.getDirection().y;
        Vector3f lookAt = playerCamera.getLookAt();

        playerCamera.resetYawToNorth();

        float yaw = MinimapCoordinates.cameraYawRadians(camera);
        assertEquals(0f, FastMath.sin(yaw), 1e-4f);
        assertEquals(1f, FastMath.cos(yaw), 1e-4f);
        assertEquals(pitch, camera.getDirection().y, 1e-4f);
        assertEquals(lookAt.x, playerCamera.getLookAt().x, 1e-3f);
        assertEquals(lookAt.z, playerCamera.getLookAt().z, 1e-3f);
    }

    @Test
    void resetYawToNorthWorksFromEveryDirection() throws ReflectiveOperationException {
        for (float angle = -3f; angle <= 3f; angle += 0.7f) {
            Camera camera = new Camera(800, 600);
            PlayerCamera playerCamera = playerCamera(camera);
            playerCamera.rotateAround(angle);

            playerCamera.resetYawToNorth();

            assertEquals(1f, FastMath.cos(MinimapCoordinates.cameraYawRadians(camera)), 1e-4f, "after turning by " + angle);
        }
    }
}
