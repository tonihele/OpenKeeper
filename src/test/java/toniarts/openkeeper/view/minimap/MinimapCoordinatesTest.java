/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimapCoordinatesTest {

    @Test
    void worldToTileFromVector3fDropsYAndDividesByTileWidth() {
        Vector2f tile = MinimapCoordinates.worldToTile(new Vector3f(12.5f, 99f, 7.25f));
        assertEquals(12.5f, tile.x, 1e-6f);
        assertEquals(7.25f, tile.y, 1e-6f);
    }

    @Test
    void worldToTileFromVector2fMatchesVector3fOverload() {
        Vector2f tile = MinimapCoordinates.worldToTile(new Vector2f(3f, -4.5f));
        assertEquals(3f, tile.x, 1e-6f);
        assertEquals(-4.5f, tile.y, 1e-6f);
    }

    @Test
    void cameraYawRadiansIsZeroFacingPositiveZ() {
        Camera camera = new Camera(800, 600);
        camera.setLocation(Vector3f.ZERO);
        camera.lookAtDirection(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        assertEquals(0f, MinimapCoordinates.cameraYawRadians(camera), 1e-5f);
    }

    @Test
    void cameraYawRadiansIsQuarterTurnFacingPositiveX() {
        Camera camera = new Camera(800, 600);
        camera.setLocation(Vector3f.ZERO);
        camera.lookAtDirection(Vector3f.UNIT_X, Vector3f.UNIT_Y);
        assertEquals(FastMath.HALF_PI, MinimapCoordinates.cameraYawRadians(camera), 1e-5f);
    }

}
