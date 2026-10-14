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

import toniarts.openkeeper.utils.Point;

/**
 * Where the full-screen map sits on the screen, and which tile is under a
 * screen position. Drawing and picking both use this class, so they cannot
 * disagree. The layout is authored for 640x480 and fitted (not stretched)
 * into the real screen, centred. Coordinates are top-left origin, Y down.
 */
public final class MapLayout {

    private static final float REFERENCE_WIDTH = 640;
    private static final float REFERENCE_HEIGHT = 480;
    private static final float MAP_X = 41;
    private static final float MAP_Y = 33;
    private static final float MAP_SIZE = 384;

    private final int screenWidth;
    private final int screenHeight;

    /**
     * Map area on the screen: left, top and side length (it is square)
     */
    private final float mapX;
    private final float mapY;
    private final float mapSize;

    public MapLayout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        float scale = Math.min(screenWidth / REFERENCE_WIDTH, screenHeight / REFERENCE_HEIGHT);
        mapX = (screenWidth - REFERENCE_WIDTH * scale) / 2f + MAP_X * scale;
        mapY = (screenHeight - REFERENCE_HEIGHT * scale) / 2f + MAP_Y * scale;
        mapSize = MAP_SIZE * scale;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public float getMapX() {
        return mapX;
    }

    public float getMapY() {
        return mapY;
    }

    public float getMapSize() {
        return mapSize;
    }

    /**
     * The tile under a screen position, or {@code null} if that is not on
     * the map (outside the map area, or in the rock border of a non-square map)
     */
    public Point pickTile(float screenX, float screenY, int mapWidth, int mapHeight) {
        float pixelX = (screenX - mapX) / mapSize * MinimapRasteriser.RASTER_SIZE;
        float pixelY = (screenY - mapY) / mapSize * MinimapRasteriser.RASTER_SIZE;
        return MinimapClickResolver.resolveTile(pixelX, pixelY, true, MinimapRasteriser.FitGeometry.of(mapWidth, mapHeight),
                0, 0, 0, mapWidth, mapHeight);
    }
}
