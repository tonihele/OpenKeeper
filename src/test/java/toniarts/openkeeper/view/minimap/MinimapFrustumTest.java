/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimapFrustumTest {

    @Test
    void cameraLookingStraightDownHitsTheGroundPlaneAtAllFourCorners() {
        Camera camera = new Camera(800, 600);
        camera.setLocation(new Vector3f(0, 10, 0));
        camera.lookAtDirection(Vector3f.UNIT_Y.negate(), Vector3f.UNIT_Z);
        camera.setFrustumPerspective(45f, 800f / 600f, 1f, 100f);

        Vector3f[] corners = MinimapFrustum.groundCorners(camera);

        assertEquals(4, corners.length);
        for (Vector3f corner : corners) {
            assertNotNull(corner);
            assertEquals(0f, corner.y, 1e-3f);
        }
    }

    @Test
    void cameraLookingAtTheHorizonMissesTheGroundPlaneOnAtLeastOneCorner() {
        Camera camera = new Camera(800, 600);
        camera.setLocation(new Vector3f(0, 10, 0));
        // Looking dead level - the top half of the frustum points above the horizon.
        camera.lookAtDirection(Vector3f.UNIT_X, Vector3f.UNIT_Y);
        camera.setFrustumPerspective(45f, 800f / 600f, 1f, 100f);

        Vector3f[] corners = MinimapFrustum.groundCorners(camera);

        boolean anyMissed = false;
        for (Vector3f corner : corners) {
            if (corner == null) {
                anyMissed = true;
            }
        }
        assertTrue(anyMissed, "expected at least one frustum corner to miss the ground plane");
    }

}
