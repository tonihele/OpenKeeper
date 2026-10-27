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
package toniarts.openkeeper.view.minimap;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullscreenMapPreviewTest {

    private static Camera cameraAt(float tileX, float tileY, int mapWidth, int mapHeight) {
        Camera camera = new Camera(256, 384);
        camera.setFrustumPerspective(43.6f, 256f / 384f, 0.1f, 10f);
        FullscreenMapPreview.placeCamera(camera, new Vector2f(tileX, tileY), mapWidth, mapHeight);
        return camera;
    }

    @Test
    void northIsUpAndEastIsRight() {
        Camera camera = cameraAt(20.5f, 20.5f, 85, 85);

        Vector3f centre = camera.getScreenCoordinates(new Vector3f(20.5f, 0, 20.5f));
        Vector3f north = camera.getScreenCoordinates(new Vector3f(20.5f, 0, 19.5f));
        Vector3f east = camera.getScreenCoordinates(new Vector3f(21.5f, 0, 20.5f));

        assertEquals(128f, centre.x, 0.5f);
        assertEquals(192f, centre.y, 0.5f);
        assertTrue(north.y > centre.y, "north is up (jME screen y grows upwards)");
        assertEquals(centre.x, north.x, 0.5f);
        assertTrue(east.x > centre.x, "east is right");
        assertEquals(centre.y, east.y, 0.5f);
    }

    @Test
    void cameraStaysInsideThePlayableArea() {
        Camera corner = cameraAt(0.2f, 0.3f, 85, 40);
        assertEquals(1f, corner.getLocation().x, 1e-5f);
        assertEquals(1f, corner.getLocation().z, 1e-5f);

        Camera farCorner = cameraAt(84.9f, 39.9f, 85, 40);
        assertEquals(83f, farCorner.getLocation().x, 1e-5f);
        assertEquals(38f, farCorner.getLocation().z, 1e-5f);
    }

    @Test
    void cameraLooksStraightDown() {
        Camera camera = cameraAt(10f, 10f, 85, 85);
        assertEquals(0f, camera.getDirection().x, 1e-5f);
        assertEquals(-1f, camera.getDirection().y, 1e-5f);
        assertEquals(0f, camera.getDirection().z, 1e-5f);
    }
}
